package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlin.math.ceil

/** Alarm audio is independent of the ringer/media streams and follows the phone's default alarm tone. */
class SunriseAlarmAudio(private val context: Context) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val handler = Handler(Looper.getMainLooper())
    private val volume = SunriseAlarmVolumeLease(context, manager)
    private var player: MediaPlayer? = null
    private var fallbackTone: ToneGenerator? = null
    private var request: AudioFocusRequest? = null
    private var running = false
    private var focused = false
    private var prepared = false
    private var candidates: List<Uri> = emptyList()
    private var candidateIndex = 0
    private var settings = SunriseAlarmSettings()
    private var startedElapsed = 0L
    private val volumeRamp = object : Runnable {
        override fun run() {
            if (!running || !settings.gradualVolume) return
            val gain = gradualGain()
            runCatching { player?.setVolume(gain, gain) }
            if (gain < 1f) handler.postDelayed(this, 500L)
        }
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (running) {
            focused = change == AudioManager.AUDIOFOCUS_GAIN
            if (focused) resumeSound() else {
                runCatching { if (player?.isPlaying == true) player?.pause() }
                fallbackTone?.stopTone()
                if (change == AudioManager.AUDIOFOCUS_LOSS) handler.postDelayed(retryFocus, 3_000L)
            }
        }
    }
    private val retryFocus = object : Runnable {
        override fun run() {
            if (!running || focused) return
            focused = acquireFocus()
            if (focused) resumeSound() else handler.postDelayed(this, 3_000L)
        }
    }
    private val fallbackPulse = object : Runnable {
        override fun run() {
            if (!running || !focused || fallbackTone == null) return
            if (settings.gradualVolume) {
                runCatching { fallbackTone?.release() }
                fallbackTone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, (gradualGain() * 100).toInt()) }.getOrNull()
            }
            fallbackTone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
            handler.postDelayed(this, 1_500L)
        }
    }

    /** The caller must have entered foreground before this call, including on Android 15+. */
    fun start(settings: SunriseAlarmSettings = SunriseAlarmSettings()) {
        if (running) return
        this.settings = settings.sanitized()
        startedElapsed = SystemClock.elapsedRealtime()
        running = true
        volume.acquire(this.settings.volumePercent)
        focused = acquireFocus()
        startVibration()
        candidates = toneCandidates()
        candidateIndex = 0
        prepareNextTone()
        if (settings.gradualVolume) volumeRamp.run()
        if (!focused) handler.postDelayed(retryFocus, 3_000L)
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.release() }
        player = null
        prepared = false
        runCatching { fallbackTone?.release() }
        fallbackTone = null
        runCatching { vibrator?.cancel() }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                request?.let(manager::abandonAudioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(focusListener)
            }
        }
        request = null
        focused = false
        volume.release()
    }

    private fun acquireFocus(): Boolean = runCatching {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = request ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(ALARM_ATTRIBUTES)
                .setOnAudioFocusChangeListener(focusListener, handler)
                .setWillPauseWhenDucked(true)
                .build().also { request = it }
            manager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(focusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
        result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }.getOrDefault(false)

    private fun toneCandidates(): List<Uri> = buildList {
        settings.ringtoneUri?.let(Uri::parse)?.let(::add)
        runCatching { RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM) }
            .getOrNull()?.let(::add)
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let(::add)
        // A deleted custom default must not leave the alarm silent. Try a system alarm before a beep.
        runCatching {
            val ringManager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
            ringManager.cursor.use { cursor ->
                if (cursor.moveToFirst()) ringManager.getRingtoneUri(0)?.let(::add)
            }
        }
    }.distinct()

    private fun prepareNextTone() {
        if (!running) return
        runCatching { player?.release() }
        player = null
        prepared = false
        val uri = candidates.getOrNull(candidateIndex++)
        if (uri == null) {
            startFallback()
            return
        }
        val next = runCatching { MediaPlayer() }.getOrElse {
            handler.post { prepareNextTone() }
            return
        }
        player = next
        try {
            next.setAudioAttributes(ALARM_ATTRIBUTES)
            next.isLooping = true
            val gain = gradualGain()
            next.setVolume(gain, gain)
            next.setOnPreparedListener {
                if (running && player === next) {
                    prepared = true
                    resumeSound()
                }
            }
            next.setOnErrorListener { _, _, _ ->
                if (running && player === next) handler.post { if (player === next) prepareNextTone() }
                true
            }
            next.setDataSource(context, uri)
            next.prepareAsync()
        } catch (exception: Exception) {
            Log.w(TAG, "Could not open default alarm tone; trying system fallback", exception)
            handler.post { if (player === next) prepareNextTone() }
        }
    }

    private fun resumeSound() {
        if (!running || !focused) return
        if (prepared) {
            runCatching { player?.start() }.onFailure { handler.post { prepareNextTone() } }
        }
        if (fallbackTone != null) {
            handler.removeCallbacks(fallbackPulse)
            fallbackPulse.run()
        }
    }

    private fun startFallback() {
        fallbackTone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, (gradualGain() * 100).toInt()) }.getOrNull()
        resumeSound()
    }

    private fun gradualGain(): Float = if (!settings.gradualVolume) 1f else
        (.08f + (SystemClock.elapsedRealtime() - startedElapsed).coerceAtLeast(0L) / 30_000f * .92f).coerceIn(.08f, 1f)

    @Suppress("DEPRECATION")
    private fun startVibration() = runCatching {
        if (vibrator?.hasVibrator() != true) return@runCatching
        val pattern = longArrayOf(0, 750, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), ALARM_ATTRIBUTES)
        } else {
            vibrator.vibrate(pattern, 0, ALARM_ATTRIBUTES)
        }
    }

    companion object {
        private const val TAG = "SunriseAlarmAudio"
        internal val ALARM_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /** Also called on app/scheduler recovery after an interrupted service or process death. */
        fun recoverVolumeIfIdle(context: Context, now: Long = System.currentTimeMillis()) {
            if (SunriseAlarmRingingState(context).list().any { it.expiresAtMillis > now }) return
            SunriseAlarmVolumeLease(context).release()
        }
    }
}

/** Applies an alarm-only volume temporarily; restores it unless changed by the user while ringing. */
internal class SunriseAlarmVolumeLease(
    context: Context,
    private val manager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun acquire(volumePercent: Int? = null) = synchronized(lock) {
        runCatching {
            val current = manager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (preferences.contains(APPLIED)) {
                // Preserve the original volume over a service/process restart, without another increase.
                if (current == preferences.getInt(APPLIED, -1)) return@synchronized
                preferences.edit().clear().commit()
                return@synchronized // A user volume change also wins over a process restart.
            }
            if (current != 0 && volumePercent == null) return@synchronized
            val maximum = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (maximum <= 0) return@synchronized
            val audible = ceil(maximum * ((volumePercent?.coerceIn(1, 100) ?: 60) / 100.0)).toInt().coerceIn(1, maximum)
            if (audible == current) return@synchronized
            // Commit the restoration lease before modifying system state.
            if (!preferences.edit().putInt(ORIGINAL, current).putInt(APPLIED, audible).commit()) return@synchronized
            manager.setStreamVolume(AudioManager.STREAM_ALARM, audible, 0)
            if (manager.getStreamVolume(AudioManager.STREAM_ALARM) != audible) preferences.edit().clear().commit()
        }.onFailure { Log.w("SunriseAlarmAudio", "Alarm stream volume could not be adjusted", it) }
        Unit
    }

    fun release() = synchronized(lock) {
        if (!preferences.contains(APPLIED)) return@synchronized
        runCatching {
            val applied = preferences.getInt(APPLIED, -1)
            val original = preferences.getInt(ORIGINAL, 0)
            if (manager.getStreamVolume(AudioManager.STREAM_ALARM) == applied) {
                manager.setStreamVolume(AudioManager.STREAM_ALARM, original, 0)
            }
            preferences.edit().clear().commit()
        }.onFailure { Log.w("SunriseAlarmAudio", "Original alarm stream volume could not be restored", it) }
        Unit
    }

    companion object {
        internal const val PREFERENCES = "sunrise_alarm_volume_lease"
        private const val ORIGINAL = "original"
        private const val APPLIED = "applied"
        private val lock = Any()
    }
}
