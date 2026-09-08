package com.github.libretube.api

import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceFallbackManagerTest {

    @Test
    fun `excludes the failed instance`() {
        val candidates = InstanceFallbackManager.selectFallbackCandidates(
            apiUrls = listOf("a.com", "b.com", "c.com"),
            excludeApiUrl = "a.com",
            lastFailureTimes = emptyMap(),
            nowMs = 0L,
        )
        assertEquals(listOf("b.com", "c.com"), candidates)
    }

    @Test
    fun `preserves the user-configured order`() {
        val candidates = InstanceFallbackManager.selectFallbackCandidates(
            apiUrls = listOf("z.com", "a.com", "m.com"),
            excludeApiUrl = "none",
            lastFailureTimes = emptyMap(),
            nowMs = 0L,
        )
        assertEquals(listOf("z.com", "a.com", "m.com"), candidates)
    }

    @Test
    fun `excludes instances in cooldown`() {
        val nowMs = 1_000_000L
        val candidates = InstanceFallbackManager.selectFallbackCandidates(
            apiUrls = listOf("a.com", "b.com", "c.com"),
            excludeApiUrl = "none",
            // b.com failed 10s ago (within the 60s cooldown)
            lastFailureTimes = mapOf("b.com" to (nowMs - 10_000L)),
            nowMs = nowMs,
        )
        assertEquals(listOf("a.com", "c.com"), candidates)
    }

    @Test
    fun `instances are eligible again after the cooldown expires`() {
        val nowMs = 1_000_000L
        val candidates = InstanceFallbackManager.selectFallbackCandidates(
            apiUrls = listOf("a.com", "b.com"),
            excludeApiUrl = "none",
            // failed 61s ago, cooldown is only 60s
            lastFailureTimes = mapOf("b.com" to (nowMs - 61_000L)),
            nowMs = nowMs,
        )
        assertEquals(listOf("a.com", "b.com"), candidates)
    }

    @Test
    fun `caps the number of health-check candidates`() {
        val candidates = InstanceFallbackManager.selectFallbackCandidates(
            apiUrls = (1..5).map { "i$it.com" },
            excludeApiUrl = "none",
            lastFailureTimes = emptyMap(),
            nowMs = 0L,
            maxCandidates = 3,
        )
        assertEquals(3, candidates.size)
        assertEquals(listOf("i1.com", "i2.com", "i3.com"), candidates)
    }

    @Test
    fun `an instance that never failed is not in cooldown`() {
        assertEquals(false, InstanceFallbackManager.isInstanceInCooldown(lastFailureTime = 0L, nowMs = Long.MAX_VALUE))
    }

    @Test
    fun `a fresh failure is in cooldown`() {
        val nowMs = 5_000L
        assertEquals(true, InstanceFallbackManager.isInstanceInCooldown(lastFailureTime = 4_000L, nowMs = nowMs))
        assertEquals(false, InstanceFallbackManager.isInstanceInCooldown(lastFailureTime = 400L, nowMs = nowMs, cooldownMs = 1_000L))
    }
}