package com.example.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Retain the last good notification if a platform/cache operation fails temporarily. */
internal suspend fun runResilientCountdownUpdates(
    onFailure: (Exception) -> Unit,
    update: suspend () -> Long
) {
    var retryMillis = 5_000L
    while (currentCoroutineContext().isActive) {
        val waitMillis = try {
            update().coerceAtLeast(1L).also { retryMillis = 5_000L }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onFailure(error)
            retryMillis.also { retryMillis = (retryMillis * 2).coerceAtMost(60_000L) }
        }
        delay(waitMillis)
    }
}
