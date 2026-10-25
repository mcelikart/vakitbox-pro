package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Allows a deterministic microphone fixture without opening a real device microphone. */
internal val LocalNoteSpeechRecognizerFactory = staticCompositionLocalOf<NoteSpeechRecognizerFactory?> { null }

/** The editor owns and saves its TextFieldValue; the microphone never owns the draft. */
@Composable
internal fun rememberNoteDictationController(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
): NoteDictationController {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestValue = rememberUpdatedState(value)
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    val factoryOverride = LocalNoteSpeechRecognizerFactory.current
    val controller = remember(context, factoryOverride) {
        NoteDictationController(
            getValue = { latestValue.value },
            onValueChange = { latestOnValueChange.value(it) },
            recognizerFactory = factoryOverride ?: AndroidNoteSpeechRecognizerFactory(context),
            hasMicrophonePermission = {
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    DisposableEffect(controller, lifecycleOwner) {
        val observer = noteDictationLifecycleObserver(controller)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.cancel()
        }
    }
    DisposableEffect(controller) { onDispose { controller.release() } }
    return controller
}

internal fun noteDictationLifecycleObserver(controller: NoteDictationController) =
    LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) controller.cancel() }

internal interface NoteSpeechEvents {
    fun onReady()
    fun onSpeechEnded()
    fun onPartial(text: String?)
    fun onResults(text: String?)
    fun onError(error: Int)
}

internal interface NoteSpeechRecognizer {
    val isOnDevice: Boolean
    fun startListening(intent: Intent)
    fun stopListening()
    fun cancel()
    fun destroy()
}

internal fun interface NoteSpeechRecognizerFactory {
    fun create(preferOnDevice: Boolean, events: NoteSpeechEvents): NoteSpeechRecognizer?
}

internal fun interface NoteDictationScheduler {
    /** Returns an idempotent cancellation action. */
    fun schedule(delayMillis: Long, action: () -> Unit): () -> Unit
}

private class MainThreadNoteDictationScheduler : NoteDictationScheduler {
    private val handler = Handler(Looper.getMainLooper())
    override fun schedule(delayMillis: Long, action: () -> Unit): () -> Unit {
        val runnable = Runnable(action)
        handler.postDelayed(runnable, delayMillis)
        return { handler.removeCallbacks(runnable) }
    }
}

/** All entry points and recognizer callbacks run on the main thread. */
internal class NoteDictationController(
    private val getValue: () -> TextFieldValue,
    private val onValueChange: (TextFieldValue) -> Unit,
    private val recognizerFactory: NoteSpeechRecognizerFactory,
    private val scheduler: NoteDictationScheduler = MainThreadNoteDictationScheduler(),
    private val hasMicrophonePermission: () -> Boolean = { true },
    private val monotonicClock: () -> Long = { SystemClock.elapsedRealtime() }
) {
    var isActive by mutableStateOf(false)
        private set
    var isListening by mutableStateOf(false)
        private set
    var status by mutableStateOf<String?>(null)
        private set

    private val buffer = NoteDictationSessionBuffer()
    private var recognizer: NoteSpeechRecognizer? = null
    private var generation = 0L
    private var released = false
    private var awaitingResult = false
    private var preferOnDevice = true
    private var busyRetries = 0
    private var silenceRetries = 0
    private var utteranceHasWords = false
    private var utteranceStartedAt = 0L
    private var cancelRestart: (() -> Unit)? = null
    private var cancelDeadline: (() -> Unit)? = null
    private var lastObservedValue = getValue()
    private var editorValue = lastObservedValue

    /** Called by the UI only after RECORD_AUDIO permission is granted. */
    fun start() {
        if (released || isActive) return
        clearPendingActions()
        disposeRecognizer()
        if (!hasMicrophonePermission()) {
            isListening = false
            status = "Sesle yazmak için mikrofon izni gerekiyor"
            return
        }
        preferOnDevice = true
        busyRetries = 0
        silenceRetries = 0
        isActive = true
        startUtterance()
    }

    /** Stop recording immediately, keeping partial words and accepting the pending final result. */
    fun stop() {
        if (released) return
        isActive = false
        isListening = false
        cancelRestart?.invoke()
        cancelRestart = null
        if (!awaitingResult || recognizer == null) {
            closeUtterance()
            status = "Sesle yazma durduruldu"
            return
        }
        status = "Yazı tamamlanıyor…"
        try {
            val token = generation
            recognizer?.stopListening()
            // A local/fake provider may return its final result inside stopListening().
            if (!accepts(token)) return
            cancelDeadline?.invoke()
            cancelDeadline = scheduler.schedule(1_500) {
                if (token == generation) {
                    closeUtterance()
                    status = "Sesle yazma durduruldu"
                }
            }
        } catch (_: Exception) {
            closeUtterance()
            status = "Sesle yazma durduruldu"
        }
    }

    /** Lifecycle/editor dismissal: keep displayed words and ignore every late provider callback. */
    fun cancel() {
        isActive = false
        isListening = false
        clearPendingActions()
        closeUtterance()
        status = null
    }

    fun release() {
        cancel()
        released = true
    }

    private fun currentValue(): TextFieldValue {
        val observed = getValue()
        // Two provider callbacks may arrive before Compose applies the first state update.
        if (observed != lastObservedValue) {
            lastObservedValue = observed
            editorValue = observed
        }
        return editorValue
    }

    private fun publish(value: TextFieldValue) {
        if (value != editorValue) {
            editorValue = value
            onValueChange(value)
        }
    }

    private fun startUtterance() {
        if (!isActive || released) return
        cancelRestart = null
        val token = ++generation
        buffer.begin(currentValue())
        utteranceHasWords = false
        utteranceStartedAt = monotonicClock()
        awaitingResult = true
        isListening = false
        status = "Mikrofon açılıyor…"
        val events = object : NoteSpeechEvents {
            override fun onReady() {
                if (!accepts(token)) return
                isListening = isActive
                status = if (isActive) "Dinleniyor…" else "Yazı tamamlanıyor…"
            }

            override fun onSpeechEnded() {
                if (!accepts(token)) return
                isListening = false
                status = if (isActive) "Yazıya dönüştürülüyor…" else "Yazı tamamlanıyor…"
                // A broken provider must not hold the microphone session indefinitely.
                cancelDeadline?.invoke()
                cancelDeadline = scheduler.schedule(if (isActive) 8_000 else 1_500) {
                    if (accepts(token)) finishUtterance(null)
                }
            }

            override fun onPartial(text: String?) {
                if (!accepts(token)) return
                if (!text.isNullOrBlank()) {
                    utteranceHasWords = true
                    silenceRetries = 0
                    busyRetries = 0
                }
                publish(buffer.partial(currentValue(), text))
            }

            override fun onResults(text: String?) {
                if (accepts(token)) finishUtterance(text)
            }

            override fun onError(error: Int) {
                if (accepts(token)) handleError(error)
            }
        }
        try {
            val activeRecognizer = recognizerFactory.create(preferOnDevice, events)
            // Creation may synchronously invoke a terminal callback or dismiss the editor.
            // Never attach/start the returned engine after its utterance has been cancelled.
            if (!accepts(token) || !isActive) {
                releaseRecognizer(activeRecognizer)
                return
            }
            if (activeRecognizer == null) {
                isActive = false
                closeUtterance()
                status = "Bu cihazda sesle yazma kullanılamıyor"
            } else {
                recognizer = activeRecognizer
                activeRecognizer.startListening(noteDictationIntent(activeRecognizer.isOnDevice))
            }
        } catch (_: SecurityException) {
            if (!accepts(token)) return
            isActive = false
            closeUtterance()
            status = "Sesle yazmak için mikrofon izni gerekiyor"
        } catch (_: Exception) {
            if (!accepts(token)) return
            isActive = false
            closeUtterance()
            status = "Mikrofon başlatılamadı. Tekrar deneyin"
        }
    }

    private fun accepts(token: Long) = !released && awaitingResult && token == generation

    private fun finishUtterance(text: String?) {
        val hadWords = utteranceHasWords || !text.isNullOrBlank()
        publish(buffer.finish(currentValue(), text))
        closeUtterance()
        if (!isActive) {
            status = "Sesle yazma durduruldu"
        } else if (hadWords) {
            silenceRetries = 0
            busyRetries = 0
            scheduleRestart(250)
        } else {
            scheduleSilenceRetry()
        }
    }

    private fun handleError(error: Int) {
        val wasOnDevice = recognizer?.isOnDevice == true
        val hadWords = utteranceHasWords
        closeUtterance()
        if (!isActive) {
            status = "Sesle yazma durduruldu"
            return
        }
        when {
            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH ->
                if (hadWords) scheduleRestart(250) else scheduleSilenceRetry()
            wasOnDevice && preferOnDevice && error in setOf(
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                SpeechRecognizer.ERROR_CLIENT
            ) -> {
                preferOnDevice = false
                scheduleRestart(300)
            }
            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY && busyRetries++ < 2 ->
                scheduleRestart(700)
            else -> {
                isActive = false
                status = when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni gerekiyor"
                    SpeechRecognizer.ERROR_AUDIO -> "Mikrofon kullanılamıyor. Tekrar deneyin"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                        "Ses tanıma bağlantısı kurulamadı. Tekrar deneyin"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                        "Türkçe ses tanıma bu cihazda hazır değil"
                    else -> "Sesle yazma durdu. Tekrar deneyin"
                }
            }
        }
    }

    private fun scheduleSilenceRetry() {
        if (monotonicClock() - utteranceStartedAt >= 1_000) {
            // An ordinary listening timeout is just a pause between phrases.
            silenceRetries = 0
            scheduleRestart(250)
        } else {
            // A provider returning immediately must not spin, but continuous mode stays on.
            val delay = SILENCE_RESTART_DELAYS[silenceRetries]
            silenceRetries = (silenceRetries + 1).coerceAtMost(SILENCE_RESTART_DELAYS.lastIndex)
            scheduleRestart(delay, "Ses bekleniyor…")
        }
    }

    private fun scheduleRestart(delayMillis: Long, message: String = "Dinleme devam ediyor…") {
        if (!isActive || released) return
        isListening = false
        status = message
        cancelRestart?.invoke()
        cancelRestart = scheduler.schedule(delayMillis) { startUtterance() }
    }

    private fun closeUtterance() {
        buffer.finish(currentValue())
        awaitingResult = false
        isListening = false
        cancelDeadline?.invoke()
        cancelDeadline = null
        disposeRecognizer()
    }

    private fun disposeRecognizer() {
        ++generation
        val old = recognizer
        recognizer = null
        releaseRecognizer(old)
    }

    private fun releaseRecognizer(old: NoteSpeechRecognizer?) {
        try { old?.cancel() } catch (_: Exception) { /* Provider may already have disconnected. */ }
        try { old?.destroy() } catch (_: Exception) { /* Never retain a failed provider. */ }
    }

    private fun clearPendingActions() {
        cancelRestart?.invoke()
        cancelRestart = null
        cancelDeadline?.invoke()
        cancelDeadline = null
    }

    private companion object {
        // Keep ordinary dictation continuous, but avoid a hot service loop when no words arrive.
        val SILENCE_RESTART_DELAYS = longArrayOf(500, 1_000, 2_000, 4_000, 8_000)
    }
}

private class AndroidNoteSpeechRecognizerFactory(private val context: Context) : NoteSpeechRecognizerFactory {
    override fun create(preferOnDevice: Boolean, events: NoteSpeechEvents): NoteSpeechRecognizer? {
        if (preferOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    return AndroidNoteSpeechRecognizer(SpeechRecognizer.createOnDeviceSpeechRecognizer(context), true, events)
                }
            } catch (_: Exception) { /* Some devices advertise an unavailable on-device provider. */ }
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
        return AndroidNoteSpeechRecognizer(SpeechRecognizer.createSpeechRecognizer(context), false, events)
    }
}

private class AndroidNoteSpeechRecognizer(
    private val delegate: SpeechRecognizer,
    override val isOnDevice: Boolean,
    events: NoteSpeechEvents
) : NoteSpeechRecognizer {
    init {
        delegate.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = events.onReady()
            override fun onBeginningOfSpeech() = events.onReady()
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = events.onSpeechEnded()
            override fun onError(error: Int) = events.onError(error)
            override fun onResults(results: Bundle?) = events.onResults(results.bestText())
            override fun onPartialResults(partialResults: Bundle?) = events.onPartial(partialResults.bestText())
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    override fun startListening(intent: Intent) = delegate.startListening(intent)
    override fun stopListening() = delegate.stopListening()
    override fun cancel() = delegate.cancel()
    override fun destroy() = delegate.destroy()
}

private fun Bundle?.bestText(): String? = this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    ?.firstOrNull { it.isNotBlank() }?.trim()
