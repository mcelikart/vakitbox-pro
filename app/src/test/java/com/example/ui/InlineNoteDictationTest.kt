package com.example.ui

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class InlineNoteDictationTest {
    @Test fun permissionDeniedDoesNotCreateOrStartARecognizer() {
        val fixture = Fixture(permission = false)
        fixture.controller.start()
        assertFalse(fixture.controller.isActive)
        assertFalse(fixture.controller.isListening)
        assertTrue(fixture.controller.status.orEmpty().contains("mikrofon"))
        assertTrue(fixture.factory.engines.isEmpty())
        fixture.permission = true
        fixture.controller.start()
        assertEquals(1, fixture.factory.engines.size)
        assertTrue(fixture.controller.isActive)
    }

    @Test fun inlinePartialsAreRevisedAndIdenticalFinalIsWrittenOnlyOnce() {
        val fixture = Fixture()
        fixture.controller.start()
        val first = fixture.factory.engines.single()
        assertEquals(1, first.startCalls)
        assertTrue(fixture.factory.preferences.single())
        assertTrue(first.intent!!.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        first.events.onReady()
        assertTrue(fixture.controller.isListening)
        first.events.onPartial("sesli")
        first.events.onPartial("sesli not")
        first.events.onResults("sesli not")
        assertEquals("Önceki sesli not", fixture.value.text)
        assertEquals(2, fixture.updates.size)
        assertTrue(first.destroyed)
        first.events.onResults("ikinci kez gelmemeli")
        first.events.onPartial("geç gelen")
        assertEquals("Önceki sesli not", fixture.value.text)
    }

    @Test fun synchronousResultDuringCreationDisposesReturnedEngineWithoutStartingIt() {
        val fixture = Fixture()
        fixture.factory.duringCreate = { it.events.onResults("erken sonuç") }
        fixture.controller.start()
        val stale = fixture.factory.engines.single()
        assertEquals("Önceki erken sonuç", fixture.value.text)
        assertEquals(0, stale.startCalls)
        assertTrue(stale.destroyed)
        assertEquals(1, fixture.scheduler.pendingCount)
        fixture.factory.duringCreate = null
        fixture.scheduler.runNext()
        assertEquals(1, fixture.factory.engines.last().startCalls)
        stale.events.onPartial("eski motor")
        assertEquals("Önceki erken sonuç", fixture.value.text)
        fixture.controller.release()
    }

    @Test fun synchronousErrorDuringCreationCannotStartTheReturnedEngine() {
        val fixture = Fixture()
        fixture.factory.duringCreate = { it.events.onError(SpeechRecognizer.ERROR_AUDIO) }
        fixture.controller.start()
        val stale = fixture.factory.engines.single()
        assertEquals(0, stale.startCalls)
        assertTrue(stale.destroyed)
        assertFalse(fixture.controller.isActive)
        assertEquals(0, fixture.scheduler.pendingCount)
        stale.events.onReady()
        assertFalse(fixture.controller.isListening)
        assertTrue(fixture.controller.status.orEmpty().contains("Mikrofon"))
    }

    @Test fun synchronousEditorDismissalDuringCreationDisposesAndNeverStartsTheEngine() {
        val fixture = Fixture()
        fixture.factory.duringCreate = { fixture.controller.release() }
        fixture.controller.start()
        val stale = fixture.factory.engines.single()
        assertEquals(0, stale.startCalls)
        assertTrue(stale.destroyed)
        assertFalse(fixture.controller.isActive)
        stale.events.onPartial("kapanınca gelmemeli")
        fixture.scheduler.runAll()
        assertEquals("Önceki", fixture.value.text)
        assertEquals(0, fixture.scheduler.pendingCount)
        fixture.controller.start()
        assertEquals(1, fixture.factory.engines.size)
    }

    @Test fun silenceRestartsListeningAndEachUtteranceAppendsOnlyItsOwnWords() {
        val fixture = Fixture()
        fixture.controller.start()
        val first = fixture.factory.engines.last()
        first.events.onResults("ilk cümle")
        assertTrue(fixture.controller.isActive)
        assertFalse(fixture.controller.isListening)
        fixture.scheduler.runNext()
        val second = fixture.factory.engines.last()
        assertEquals(2, fixture.factory.engines.size)
        second.events.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        fixture.scheduler.runNext()
        val third = fixture.factory.engines.last()
        third.events.onPartial("ikinci")
        third.events.onResults("ikinci cümle")
        assertEquals("Önceki ilk cümle ikinci cümle", fixture.value.text)
    }

    @Test fun immediateTimeoutAndNoMatchBackOffAtTheCapUntilTheUserStops() {
        val fixture = Fixture()
        fixture.controller.start()
        repeat(10) { index ->
            val engine = fixture.factory.engines.last()
            engine.events.onReady()
            engine.events.onError(if (index % 2 == 0) SpeechRecognizer.ERROR_SPEECH_TIMEOUT else SpeechRecognizer.ERROR_NO_MATCH)
            assertTrue(engine.destroyed)
            assertTrue(fixture.controller.isActive)
            assertEquals(1, fixture.scheduler.pendingCount)
            if (index < 9) fixture.scheduler.runNext()
        }
        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L) + List(6) { 8_000L }, fixture.scheduler.scheduledDelays)
        assertEquals(10, fixture.factory.engines.size)
        assertTrue(fixture.controller.isActive)
        fixture.controller.stop()
        assertFalse(fixture.controller.isActive)
        assertFalse(fixture.controller.isListening)
        fixture.scheduler.runAll()
        assertEquals(0, fixture.scheduler.pendingCount)
        assertEquals(10, fixture.factory.engines.size)
        assertEquals("Önceki", fixture.value.text)
        fixture.controller.start()
        fixture.factory.engines.last().events.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        assertTrue(fixture.controller.isActive)
        assertEquals(500L, fixture.scheduler.scheduledDelays.last())
        fixture.controller.release()
    }

    @Test fun normalLengthSilenceAndEmptyResultsKeepContinuousModeAndResetFastFailureBackoff() {
        val fixture = Fixture()
        fixture.controller.start()
        repeat(2) {
            fixture.factory.engines.last().events.onError(SpeechRecognizer.ERROR_NO_MATCH)
            fixture.scheduler.runNext()
        }
        repeat(6) { index ->
            val engine = fixture.factory.engines.last()
            engine.events.onReady()
            fixture.elapsedMillis += 1_500
            when (index % 3) {
                0 -> engine.events.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                1 -> engine.events.onError(SpeechRecognizer.ERROR_NO_MATCH)
                else -> engine.events.onResults("   ")
            }
            assertTrue(fixture.controller.isActive)
            assertEquals(250L, fixture.scheduler.scheduledDelays.last())
            fixture.scheduler.runNext()
        }
        assertEquals(listOf(500L, 1_000L) + List(6) { 250L }, fixture.scheduler.scheduledDelays)
        fixture.factory.engines.last().events.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(500L, fixture.scheduler.scheduledDelays.last())
        fixture.controller.stop()
        fixture.scheduler.runAll()
        assertFalse(fixture.controller.isActive)
        assertEquals(0, fixture.scheduler.pendingCount)
    }

    @Test fun actualWordsResetTheSilenceBackoffAndRemainInTheNoteOnNoMatch() {
        val fixture = Fixture()
        fixture.controller.start()
        repeat(2) {
            fixture.factory.engines.last().events.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
            fixture.scheduler.runNext()
        }
        val spoken = fixture.factory.engines.last()
        spoken.events.onPartial("devam eden söz")
        spoken.events.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals("Önceki devam eden söz", fixture.value.text)
        assertEquals(250L, fixture.scheduler.scheduledDelays.last())
        fixture.scheduler.runNext()
        fixture.factory.engines.last().events.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(500L, fixture.scheduler.scheduledDelays.last())
        fixture.scheduler.runNext()
        fixture.factory.engines.last().events.onResults("son cümle")
        assertEquals(250L, fixture.scheduler.scheduledDelays.last())
        fixture.scheduler.runNext()
        fixture.factory.engines.last().events.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(500L, fixture.scheduler.scheduledDelays.last())
        assertEquals("Önceki devam eden söz son cümle", fixture.value.text)
        fixture.controller.release()
    }

    @Test fun emptyFinalResultsAlsoUseBoundedSilenceBackoff() {
        val fixture = Fixture()
        fixture.controller.start()
        repeat(10) { index ->
            fixture.factory.engines.last().events.onResults("   ")
            assertTrue(fixture.controller.isActive)
            if (index < 9) fixture.scheduler.runNext()
        }
        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L) + List(6) { 8_000L }, fixture.scheduler.scheduledDelays)
        assertEquals(10, fixture.factory.engines.size)
        fixture.controller.stop()
        fixture.scheduler.runAll()
        assertFalse(fixture.controller.isActive)
        assertEquals(0, fixture.scheduler.pendingCount)
    }

    @Test fun stopRequestsFinalPreservesPartialAndNeverRestarts() {
        val fixture = Fixture()
        fixture.controller.start()
        val first = fixture.factory.engines.single()
        first.events.onPartial("yarım söz")
        fixture.controller.stop()
        assertFalse(fixture.controller.isActive)
        assertFalse(fixture.controller.isListening)
        assertEquals(1, first.stopCalls)
        assertEquals("Önceki yarım söz", fixture.value.text)
        first.events.onResults("tam söz.")
        assertEquals("Önceki tam söz.", fixture.value.text)
        fixture.scheduler.runAll()
        assertEquals(1, fixture.factory.engines.size)
        assertTrue(first.destroyed)
    }

    @Test fun stopDeadlineReleasesAnUnresponsiveProviderAndKeepsDisplayedWords() {
        val fixture = Fixture()
        fixture.controller.start()
        val engine = fixture.factory.engines.single()
        engine.events.onPartial("kaybolmaz")
        fixture.controller.stop()
        fixture.scheduler.runAll()
        assertTrue(engine.destroyed)
        assertTrue(engine.cancelCalls > 0)
        engine.events.onResults("geç sonuç")
        assertEquals("Önceki kaybolmaz", fixture.value.text)
        assertFalse(fixture.controller.isActive)
    }

    @Test fun synchronousFinalDuringStopDoesNotLeaveAnExtraDeadline() {
        val fixture = Fixture()
        fixture.controller.start()
        val engine = fixture.factory.engines.single()
        engine.onStop = { engine.events.onResults("son söz") }
        fixture.controller.stop()
        assertEquals("Önceki son söz", fixture.value.text)
        assertTrue(engine.destroyed)
        assertEquals(0, fixture.scheduler.pendingCount)
        assertFalse(fixture.controller.isActive)
    }

    @Test fun lifecycleStopReleasesMicrophonePreservesWordsAndRejectsLateCallbacks() {
        val fixture = Fixture()
        val owner = TestLifecycleOwner()
        owner.lifecycle.addObserver(noteDictationLifecycleObserver(fixture.controller))
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        fixture.controller.start()
        val engine = fixture.factory.engines.single()
        engine.events.onReady()
        engine.events.onPartial("korunacak")
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertFalse(fixture.controller.isActive)
        assertTrue(engine.destroyed)
        engine.events.onPartial("arka planda eklenmemeli")
        engine.events.onResults("arka planda eklenmemeli")
        fixture.scheduler.runAll()
        assertEquals("Önceki korunacak", fixture.value.text)
        assertEquals(1, fixture.factory.engines.size)
        fixture.controller.start()
        assertEquals(2, fixture.factory.engines.size)
    }

    @Test fun releaseCancelsScheduledRestartAndCannotBeRestarted() {
        val fixture = Fixture()
        fixture.controller.start()
        val engine = fixture.factory.engines.single()
        engine.events.onResults("bitir")
        fixture.controller.release()
        fixture.scheduler.runAll()
        fixture.controller.start()
        assertFalse(fixture.controller.isActive)
        assertEquals(1, fixture.factory.engines.size)
        assertEquals("Önceki bitir", fixture.value.text)
    }

    @Test fun unavailableOnDeviceLanguageFallsBackToTheSystemRecognizerOnce() {
        val fixture = Fixture()
        fixture.factory.onDevice = true
        fixture.controller.start()
        val first = fixture.factory.engines.single()
        assertTrue(first.intent!!.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
        first.events.onError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        fixture.scheduler.runNext()
        val fallback = fixture.factory.engines.last()
        assertEquals(listOf(true, false), fixture.factory.preferences)
        assertFalse(fallback.isOnDevice)
        assertFalse(fallback.intent!!.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
        fallback.events.onResults("telefon tanıdı")
        assertEquals("Önceki telefon tanıdı", fixture.value.text)
    }

    @Test fun missingProviderAndPermanentErrorsStopRatherThanLoopForever() {
        val unavailable = Fixture()
        unavailable.factory.available = false
        unavailable.controller.start()
        assertFalse(unavailable.controller.isActive)
        assertNotNull(unavailable.controller.status)
        val broken = Fixture()
        broken.controller.start()
        val engine = broken.factory.engines.single()
        engine.events.onPartial("eldeki söz")
        engine.events.onError(SpeechRecognizer.ERROR_AUDIO)
        broken.scheduler.runAll()
        assertFalse(broken.controller.isActive)
        assertEquals(1, broken.factory.engines.size)
        assertEquals("Önceki eldeki söz", broken.value.text)
        assertTrue(engine.destroyed)
    }

    @Test fun repeatedBusyErrorsHaveABoundedRetryCount() {
        val fixture = Fixture()
        fixture.controller.start()
        repeat(3) {
            fixture.factory.engines.last().events.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            fixture.scheduler.runAll()
        }
        assertEquals(3, fixture.factory.engines.size)
        assertFalse(fixture.controller.isActive)
    }

    @Test fun rapidCallbacksDoNotDependOnComposeRecomposingBetweenPartials() {
        var visible = TextFieldValue("Not", TextRange(3))
        val delayedUpdates = mutableListOf<TextFieldValue>()
        val factory = FakeFactory()
        val controller = NoteDictationController(
            getValue = { visible }, onValueChange = { delayedUpdates.add(it) },
            recognizerFactory = factory, scheduler = FakeScheduler()
        )
        controller.start()
        factory.engines.single().events.onPartial("ilk")
        factory.engines.single().events.onPartial("ilk söz")
        factory.engines.single().events.onResults("ilk söz.")
        visible = delayedUpdates.last()
        assertEquals("Not ilk söz.", visible.text)
        controller.release()
    }

    private class TestLifecycleOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }

    private class Fixture(var permission: Boolean = true) {
        var elapsedMillis = 0L
        var value = TextFieldValue("Önceki", TextRange(6))
        val updates = mutableListOf<TextFieldValue>()
        val factory = FakeFactory()
        val scheduler = FakeScheduler()
        val controller = NoteDictationController(
            getValue = { value },
            onValueChange = { value = it; updates.add(it) },
            recognizerFactory = factory,
            scheduler = scheduler,
            hasMicrophonePermission = { permission },
            monotonicClock = { elapsedMillis }
        )
    }

    private class FakeFactory : NoteSpeechRecognizerFactory {
        var available = true
        var onDevice = false
        var duringCreate: ((FakeRecognizer) -> Unit)? = null
        val preferences = mutableListOf<Boolean>()
        val engines = mutableListOf<FakeRecognizer>()
        override fun create(preferOnDevice: Boolean, events: NoteSpeechEvents): NoteSpeechRecognizer? {
            preferences.add(preferOnDevice)
            if (!available) return null
            return FakeRecognizer(events, onDevice && preferOnDevice).also {
                engines.add(it)
                duringCreate?.invoke(it)
            }
        }
    }

    private class FakeRecognizer(val events: NoteSpeechEvents, override val isOnDevice: Boolean) : NoteSpeechRecognizer {
        var intent: Intent? = null
        var startCalls = 0
        var stopCalls = 0
        var cancelCalls = 0
        var destroyed = false
        var onStop: (() -> Unit)? = null
        override fun startListening(intent: Intent) { this.intent = intent; startCalls++ }
        override fun stopListening() { stopCalls++; onStop?.invoke() }
        override fun cancel() { cancelCalls++ }
        override fun destroy() { destroyed = true }
    }

    private class FakeScheduler : NoteDictationScheduler {
        private class Task(val action: () -> Unit) { var cancelled = false }
        private val tasks = mutableListOf<Task>()
        val scheduledDelays = mutableListOf<Long>()
        val pendingCount get() = tasks.count { !it.cancelled }
        override fun schedule(delayMillis: Long, action: () -> Unit): () -> Unit {
            scheduledDelays.add(delayMillis)
            val task = Task(action)
            tasks.add(task)
            return { task.cancelled = true }
        }
        fun runNext() {
            while (tasks.isNotEmpty()) {
                val next = tasks.removeAt(0)
                if (!next.cancelled) { next.action(); return }
            }
        }
        fun runAll() {
            repeat(25) {
                if (tasks.none { !it.cancelled }) { tasks.clear(); return }
                runNext()
            }
            error("Recognition restart loop did not settle")
        }
    }
}
