package com.github.libretube.handler

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class ImportHandler : AbstractCoroutineContextElement(ImportHandler) {


    private var paused = MutableStateFlow(false)
    private var cancel = MutableStateFlow(false)

    val isPaused: Boolean
        get() = paused.value

    val isCancelled: Boolean
        get() = cancel.value

    fun pause() {
        paused.value = true
    }

    fun resume() {
        paused.value = false
    }

    suspend fun awaitResumed() {
        paused.first { !it }
    }

    fun cancel() {
        cancel.value = true
    }

    companion object : CoroutineContext.Key<ImportHandler> {

        suspend fun current() = checkNotNull(currentCoroutineContext()[this]) {
            "PausingHandle not found in current context"
        }
    }
}