package com.github.libretube.player

import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import com.github.libretube.player.manifest.BaseUrl
import kotlin.math.max
import kotlin.random.Random

/**
 * Holds the state of excluded base URLs to be used to select a base URL based on these exclusions.
 */
@UnstableApi
class BaseUrlExclusionList(
    private val random: Random = Random
) {
    private val excludedServiceLocations = mutableMapOf<String, Long>()
    private val excludedPriorities = mutableMapOf<Int, Long>()
    private val selectionsTaken = mutableMapOf<List<Pair<String, Int>>, BaseUrl>()

    /**
     * Excludes the given base URL.
     *
     * @param baseUrlToExclude The base URL to exclude.
     * @param exclusionDurationMs The duration of exclusion, in milliseconds.
     */
    fun exclude(baseUrlToExclude: BaseUrl, exclusionDurationMs: Long) {
        val excludeUntilMs = SystemClock.elapsedRealtime() + exclusionDurationMs
        addExclusion(baseUrlToExclude.serviceLocation, excludeUntilMs, excludedServiceLocations)
        if (baseUrlToExclude.priority != BaseUrl.PRIORITY_UNSET) {
            addExclusion(baseUrlToExclude.priority, excludeUntilMs, excludedPriorities)
        }
    }

    /**
     * Selects the base URL to use from the given list.
     *
     * The list is reduced by service location and priority of base URLs that have been excluded.
     * The base URL to use is then selected from the remaining base URLs by priority and weight.
     *
     * @param baseUrls The list of base URLs to select from.
     * @return The selected base URL after exclusion or null if all elements have been excluded.
     */
    fun selectBaseUrl(baseUrls: List<BaseUrl>): BaseUrl? {
        val includedBaseUrls = applyExclusions(baseUrls).toMutableList()
        if (includedBaseUrls.size < 2) {
            return includedBaseUrls.firstOrNull()
        }

        includedBaseUrls.sortWith(compareBy<BaseUrl> { it.priority }.thenBy { it.serviceLocation })

        val candidateKeys = mutableListOf<Pair<String, Int>>()
        val lowestPriority = includedBaseUrls[0].priority

        for (baseUrl in includedBaseUrls) {
            if (lowestPriority != baseUrl.priority) {
                if (candidateKeys.size == 1) {
                    return includedBaseUrls[0]
                }
                break
            }
            candidateKeys.add(baseUrl.serviceLocation to baseUrl.weight)
        }

        return selectionsTaken.getOrPut(candidateKeys) {
            selectWeighted(includedBaseUrls.subList(0, candidateKeys.size))
        }
    }

    /**
     * Returns the number of priority levels for the given list of base URLs after exclusion.
     */
    fun getPriorityCountAfterExclusion(baseUrls: List<BaseUrl>): Int {
        return applyExclusions(baseUrls).map { it.priority }.distinct().size
    }

    /** Resets the state. */
    fun reset() {
        excludedServiceLocations.clear()
        excludedPriorities.clear()
        selectionsTaken.clear()
    }

    private fun applyExclusions(baseUrls: List<BaseUrl>): List<BaseUrl> {
        val nowMs = SystemClock.elapsedRealtime()
        removeExpiredExclusions(nowMs, excludedServiceLocations)
        removeExpiredExclusions(nowMs, excludedPriorities)

        return baseUrls.filter { baseUrl ->
            baseUrl.serviceLocation !in excludedServiceLocations &&
                    baseUrl.priority !in excludedPriorities
        }
    }

    private fun selectWeighted(candidates: List<BaseUrl>): BaseUrl {
        val totalWeight = candidates.sumOf { it.weight }
        var randomChoice = random.nextInt(totalWeight)
        var accumulatedWeight = 0

        for (baseUrl in candidates) {
            accumulatedWeight += baseUrl.weight
            if (randomChoice < accumulatedWeight) {
                return baseUrl
            }
        }
        return candidates.last()
    }

    companion object {
        private fun <T> addExclusion(
            toExclude: T,
            excludeUntilMs: Long,
            currentExclusions: MutableMap<T, Long>
        ) {
            val maxExclusion = maxOf(
                excludeUntilMs,
                currentExclusions[toExclude] ?: Long.MIN_VALUE
            )
            currentExclusions[toExclude] = maxExclusion
        }

        private fun <T> removeExpiredExclusions(nowMs: Long, exclusions: MutableMap<T, Long>) {
            exclusions.entries.removeAll { it.value <= nowMs }
        }

        fun getPriorityCount(baseUrls: List<BaseUrl>): Int {
            return baseUrls.map { it.priority }.distinct().size
        }
    }
}
