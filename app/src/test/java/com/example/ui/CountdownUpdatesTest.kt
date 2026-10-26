package com.example.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CountdownUpdatesTest {
    @Test fun notificationFailureRetriesWithoutLosingTheUpdateLoopAndSuccessResetsBackoff() = runTest {
        var attempts = 0
        val failures = mutableListOf<Exception>()
        val job = backgroundScope.launch {
            runResilientCountdownUpdates(failures::add) {
                attempts++
                if (attempts in listOf(1, 2, 4)) throw IllegalStateException("Notification service unavailable")
                30_000L
            }
        }
        runCurrent()
        assertEquals(1, attempts)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(2, attempts)
        advanceTimeBy(10_000); runCurrent()
        assertEquals(3, attempts)
        advanceTimeBy(30_000); runCurrent()
        assertEquals(4, attempts)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(5, attempts)
        assertEquals(3, failures.size)
        job.cancel()
        advanceTimeBy(120_000); runCurrent()
        assertEquals(5, attempts)
    }

    @Test fun cancellationIsNotReportedOrRetried() = runTest {
        var attempts = 0
        val job = launch {
            runResilientCountdownUpdates({ fail("Cancellation is not a failure") }) {
                attempts++
                throw CancellationException("Service destroyed")
            }
        }
        job.join()
        advanceTimeBy(120_000); runCurrent()
        assertTrue(job.isCancelled)
        assertEquals(1, attempts)
    }
}
