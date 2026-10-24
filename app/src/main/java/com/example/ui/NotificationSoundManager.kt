package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object NotificationSoundManager {

    val SOUND_OPTIONS = listOf(
        SoundOption(0, "Sessiz (Yalnızca Titreşim)", "Ses çalınmaz"),
        SoundOption(1, "Varsayılan Sistem Sesi", "Cihazın standart bildirim tonu"),
        SoundOption(2, "Manevi Takbir Melodisi", "Sıcak makam ezan takbiri tonu"),
        SoundOption(3, "Saray Ney Sesi", "Derin ve dinlendirici ney üflemesi"),
        SoundOption(4, "Osmanlı Kanun & Tambur", "Geleneksel kanun tınlaması"),
        SoundOption(5, "Kubbeli Cami Zili", "Kristal berraklığında kubbe çanı"),
        SoundOption(6, "Tasavvuf Su Damlası", "Yumuşak su ve kristal tınısı")
    )

    data class SoundOption(val id: Int, val title: String, val subtitle: String)

    private val scope = CoroutineScope(Dispatchers.Default)

    // --- Direct In-Memory PCM Audio Engine for Zero-Latency Dhikr Clicks ---
    private val cachedBeadSamples = ConcurrentHashMap<Int, ShortArray>()
    @Volatile private var isDhikrSoundsInit = false
    private var fallbackToneGen: ToneGenerator? = null

    @Synchronized
    private fun getToneGen(): ToneGenerator? {
        if (fallbackToneGen == null) {
            try {
                fallbackToneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            } catch (_: Exception) {}
        }
        return fallbackToneGen
    }

    /**
     * Pre-generates synthesized Dhikr sound effects into memory for fast zero-latency playback.
     */
    fun initDhikrSounds(context: Context) {
        if (isDhikrSoundsInit) return
        isDhikrSoundsInit = true
        scope.launch(Dispatchers.Default) {
            try {
                cachedBeadSamples[0] = generateWoodenBeadSamples()
                cachedBeadSamples[1] = generateStoneBeadSamples()
                cachedBeadSamples[2] = generateKukaBeadSamples()
                cachedBeadSamples[3] = generateBoneBeadSamples()
            } catch (_: Exception) {}
        }
    }

    /**
     * Plays Tesbih / Dhikr bead click sound instantly using direct AudioTrack PCM playback.
     */
    fun playDhikrClick(context: Context? = null, soundType: Int = 0) {
        scope.launch {
            try {
                if (context != null && !isDhikrSoundsInit) {
                    initDhikrSounds(context)
                }

                val samples = cachedBeadSamples[soundType] ?: when (soundType) {
                    0 -> generateWoodenBeadSamples().also { cachedBeadSamples[0] = it }
                    1 -> generateStoneBeadSamples().also { cachedBeadSamples[1] = it }
                    2 -> generateKukaBeadSamples().also { cachedBeadSamples[2] = it }
                    3 -> generateBoneBeadSamples().also { cachedBeadSamples[3] = it }
                    else -> generateWoodenBeadSamples()
                }

                if (!playPcmTrack(samples, 44100, AudioAttributes.USAGE_MEDIA)) {
                    getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
                }
            } catch (e: Exception) {
                try {
                    getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Plays a notification or preview sound based on [soundId].
     */
    fun playSound(context: Context, soundId: Int) {
        scope.launch {
            try {
                when (soundId) {
                    0 -> { /* Silent */ }
                    1 -> playSystemNotificationSound(context)
                    2 -> playTakbirSound(context)
                    3 -> playNeySound(context)
                    4 -> playKanunSound(context)
                    5 -> playDomeBellSound(context)
                    6 -> playWaterDropSound(context)
                    else -> playSystemNotificationSound(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- System Ringtone ---
    private fun playSystemNotificationSound(context: Context) {
        try {
            var uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
            } else {
                getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            } catch (_: Exception) {}
        }
    }

    // --- Synthesized Dhikr Waveform Generators ---

    fun generateWoodenBeadSamples(sampleRate: Int = 44100): ShortArray {
        val durationMs = 35
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 260.0)
            val freq = 850.0 * exp(-t * 180.0)
            val sine1 = sin(2.0 * PI * freq * t)
            val sine2 = sin(2.0 * PI * 380.0 * t) * 0.4
            val woodNoise = (Math.random() * 2.0 - 1.0) * 0.35 * exp(-t * 350.0)
            val valDouble = (sine1 * 0.6 + sine2 * 0.3 + woodNoise) * env * 32767.0 * 0.95
            samples[i] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    fun generateStoneBeadSamples(sampleRate: Int = 44100): ShortArray {
        // Taş / Oltu / Akik Tesbih - Sharp, crisp, mineral stone impact
        val durationMs = 28
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 310.0)
            val freq = 1750.0 * exp(-t * 220.0)
            val sine1 = sin(2.0 * PI * freq * t)
            val sine2 = sin(2.0 * PI * 3400.0 * t) * 0.25
            val stoneClick = (Math.random() * 2.0 - 1.0) * 0.3 * exp(-t * 400.0)
            val valDouble = (sine1 * 0.65 + sine2 * 0.2 + stoneClick) * env * 32767.0 * 0.95
            samples[i] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    fun generateKukaBeadSamples(sampleRate: Int = 44100): ShortArray {
        // Kuka & Kehribar Tesbih - Crisp, organic hollow bead click
        val durationMs = 32
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 220.0)
            val freq = 1150.0 * exp(-t * 140.0)
            val sine1 = sin(2.0 * PI * freq * t)
            val sine2 = sin(2.0 * PI * 620.0 * t) * 0.45
            val kukaNoise = (Math.random() * 2.0 - 1.0) * 0.25 * exp(-t * 300.0)
            val valDouble = (sine1 * 0.55 + sine2 * 0.35 + kukaNoise) * env * 32767.0 * 0.9
            samples[i] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    fun generateBoneBeadSamples(sampleRate: Int = 44100): ShortArray {
        // Kemik / Fildişi Tesbih - Deep, heavy, solid bead impact
        val durationMs = 38
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 180.0)
            val freq = 580.0 * exp(-t * 90.0)
            val sine1 = sin(2.0 * PI * freq * t)
            val sine2 = sin(2.0 * PI * 290.0 * t) * 0.5
            val boneNoise = (Math.random() * 2.0 - 1.0) * 0.2 * exp(-t * 250.0)
            val valDouble = (sine1 * 0.6 + sine2 * 0.35 + boneNoise) * env * 32767.0 * 0.95
            samples[i] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun createWavHeader(pcmSamples: ShortArray, sampleRate: Int): ByteArray {
        val totalAudioLen = pcmSamples.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte(); header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavBytes = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, wavBytes, 0, 44)
        for (i in pcmSamples.indices) {
            val sample = pcmSamples[i].toInt()
            wavBytes[44 + i * 2] = (sample and 0xff).toByte()
            wavBytes[44 + i * 2 + 1] = ((sample shr 8) and 0xff).toByte()
        }
        return wavBytes
    }

    // --- Synthesized Notification / Alarm Tones ---

    private fun playTakbirSound(context: Context) {
        val sampleRate = 44100
        val notes = listOf(
            Pair(523.25, 300), // C5
            Pair(659.25, 300), // E5
            Pair(783.99, 400), // G5
            Pair(1046.50, 700) // C6
        )
        val totalMs = notes.sumOf { it.second } + 400
        val numSamples = (sampleRate * totalMs) / 1000
        val samples = ShortArray(numSamples)

        var currentSample = 0
        for (note in notes) {
            val noteFreq = note.first
            val noteDurationMs = note.second
            val noteSamples = (sampleRate * noteDurationMs) / 1000

            for (i in 0 until noteSamples) {
                if (currentSample >= numSamples) break
                val t = i.toDouble() / sampleRate
                val env = sin(PI * (i.toDouble() / noteSamples))
                val sine1 = sin(2.0 * PI * noteFreq * t)
                val sine2 = sin(2.0 * PI * (noteFreq * 2) * t) * 0.3
                val valDouble = (sine1 + sine2) * env * 32767.0 * 0.7
                samples[currentSample++] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
            }
        }

        while (currentSample < numSamples) {
            val t = (currentSample - (numSamples - (sampleRate * 400) / 1000)).toDouble() / sampleRate
            val env = exp(-t * 10.0)
            val sine = sin(2.0 * PI * 1046.50 * t) * 0.2
            samples[currentSample++] = (sine * env * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }

        playWavWithMediaPlayer(context, samples, sampleRate, "takbir_v1")
    }

    private fun playNeySound(context: Context) {
        val sampleRate = 44100
        val notes = listOf(
            Pair(392.00, 350),
            Pair(440.00, 350),
            Pair(493.88, 400),
            Pair(587.33, 800)
        )
        val totalMs = notes.sumOf { it.second }
        val numSamples = (sampleRate * totalMs) / 1000
        val samples = ShortArray(numSamples)

        var currentSample = 0
        for (note in notes) {
            val noteFreq = note.first
            val noteSamples = (sampleRate * note.second) / 1000

            for (i in 0 until noteSamples) {
                if (currentSample >= numSamples) break
                val t = i.toDouble() / sampleRate
                val env = sin(PI * (i.toDouble() / noteSamples))
                val vibrato = sin(2.0 * PI * 5.5 * t) * 4.0
                val freqWithVibrato = noteFreq + vibrato
                val sine = sin(2.0 * PI * freqWithVibrato * t)
                val breathNoise = (Math.random() * 2.0 - 1.0) * 0.08
                val valDouble = (sine * 0.85 + breathNoise) * env * 32767.0 * 0.75
                samples[currentSample++] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        playWavWithMediaPlayer(context, samples, sampleRate, "ney_v1")
    }

    private fun playKanunSound(context: Context) {
        val sampleRate = 44100
        val notes = listOf(
            Pair(587.33, 200),
            Pair(739.99, 200),
            Pair(880.00, 600)
        )
        val totalMs = notes.sumOf { it.second }
        val numSamples = (sampleRate * totalMs) / 1000
        val samples = ShortArray(numSamples)

        var currentSample = 0
        for (note in notes) {
            val noteFreq = note.first
            val noteSamples = (sampleRate * note.second) / 1000

            for (i in 0 until noteSamples) {
                if (currentSample >= numSamples) break
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 8.0)
                val sine1 = sin(2.0 * PI * noteFreq * t)
                val sine2 = sin(2.0 * PI * (noteFreq * 3) * t) * 0.2
                val valDouble = (sine1 + sine2) * env * 32767.0 * 0.7
                samples[currentSample++] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        playWavWithMediaPlayer(context, samples, sampleRate, "kanun_v1")
    }

    private fun playDomeBellSound(context: Context) {
        val sampleRate = 44100
        val durationMs = 1200
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 3.5)
            val sine1 = sin(2.0 * PI * 1046.50 * t)
            val sine2 = sin(2.0 * PI * 2093.00 * t) * 0.4
            val sine3 = sin(2.0 * PI * 3135.50 * t) * 0.2
            val valDouble = (sine1 + sine2 + sine3) * env * 32767.0 * 0.6
            samples[i] = valDouble.toInt().coerceIn(-32768, 32767).toShort()
        }
        playWavWithMediaPlayer(context, samples, sampleRate, "domebell_v1")
    }

    private fun playWaterDropSound(context: Context) {
        val sampleRate = 44100
        val durationMs = 800
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)

        val drop1Samples = (sampleRate * 350) / 1000
        for (i in 0 until numSamples) {
            val valDouble = if (i < drop1Samples) {
                val t1 = i.toDouble() / sampleRate
                val env = exp(-t1 * 15.0)
                val freq = 500.0 + (800.0 * (t1 / 0.35))
                sin(2.0 * PI * freq * t1) * env * 0.7
            } else {
                val t2 = (i - drop1Samples).toDouble() / sampleRate
                val env = exp(-t2 * 10.0)
                val freq = 650.0 + (1000.0 * (t2 / 0.45))
                sin(2.0 * PI * freq * t2) * env * 0.8
            }
            samples[i] = (valDouble * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        playWavWithMediaPlayer(context, samples, sampleRate, "waterdrop_v1")
    }

    private fun playWavWithMediaPlayer(
        context: Context,
        samples: ShortArray,
        sampleRate: Int,
        tag: String
    ) {
        playPcmTrack(samples, sampleRate)
    }

    private fun playPcmTrack(
        samples: ShortArray,
        sampleRate: Int,
        audioUsage: Int = AudioAttributes.USAGE_NOTIFICATION_RINGTONE
    ): Boolean {
        var activeTrack: AudioTrack? = null
        return try {
            val bufferSizeInBytes = samples.size * Short.SIZE_BYTES
            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(audioUsage)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    if (audioUsage == AudioAttributes.USAGE_MEDIA) AudioManager.STREAM_MUSIC
                    else AudioManager.STREAM_NOTIFICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STATIC
                )
            }
            activeTrack = track

            // MODE_STATIC starts in STATE_NO_STATIC_DATA. It becomes initialized
            // only after PCM has been written; rejecting it before that replaced
            // all four bead materials with the same fallback beep.
            if (track.state == AudioTrack.STATE_UNINITIALIZED) {
                track.release()
                false
            } else {
                val written = track.write(samples, 0, samples.size)
                if (written != samples.size || track.state != AudioTrack.STATE_INITIALIZED) {
                    track.release()
                    false
                } else {
                    track.setVolume(0.9f)
                    track.play()
                    val durationMs = ((samples.size.toDouble() / sampleRate) * 1000).toLong() + 120
                    scope.launch {
                        kotlinx.coroutines.delay(durationMs)
                        runCatching {
                            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                            track.release()
                        }
                    }
                    true
                }
            }
        } catch (_: Exception) {
            runCatching { activeTrack?.release() }
            false
        }
    }
}

