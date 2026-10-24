package com.example.ui

import android.media.AudioAttributes
import android.os.Build
import android.media.AudioTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowAudioTrack
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36], shadows = [RecordingDhikrAudioTrack::class])
class NotificationSoundManagerTest {
    @Before fun resetPlaybacks() { RecordingDhikrAudioTrack.playbacks.clear() }

    @Test fun allFourMaterialsLoadTheirOwnStaticPcmBeforePlaying() {
        val durationsMs = listOf(35, 28, 32, 38)
        val playedSamples = mutableListOf<ShortArray>()

        for ((soundType, durationMs) in durationsMs.withIndex()) {
            NotificationSoundManager.playDhikrClick(soundType = soundType)
            val played = RecordingDhikrAudioTrack.playbacks.poll(3, TimeUnit.SECONDS)
            assertNotNull("Material $soundType must play its own PCM instead of the common fallback beep", played)
            played!!
            // This is Android's actual static-track state transition: the native
            // write is substituted, while AudioTrack.write updates its own state.
            assertEquals(AudioTrack.STATE_NO_STATIC_DATA, played.stateBeforeWrite)
            assertEquals(AudioTrack.STATE_INITIALIZED, played.stateAtPlay)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertEquals(AudioAttributes.USAGE_MEDIA, played.usage)
            }
            assertEquals(44100 * durationMs / 1000, played.samples.size)
            assertTrue("Material $soundType must contain audible samples", played.samples.any { kotlin.math.abs(it.toInt()) > 1000 })
            playedSamples += played.samples
        }

        assertEquals("The four material presets must retain distinct waveforms", 4, playedSamples.map { it.contentHashCode() }.toSet().size)
    }
}

/** Only substitutes native audio I/O; Android's static-buffer lifecycle stays real. */
@Implements(AudioTrack::class)
class RecordingDhikrAudioTrack : ShadowAudioTrack() {
    @RealObject private lateinit var realTrack: AudioTrack
    private var loadedSamples = ShortArray(0)
    private var stateBeforeWrite = AudioTrack.STATE_UNINITIALIZED

    @Implementation
    @Suppress("UNUSED_PARAMETER", "FunctionName")
    protected fun native_write_short(data: ShortArray, offset: Int, size: Int, format: Int, blocking: Boolean): Int {
        stateBeforeWrite = realTrack.state
        loadedSamples = data.copyOfRange(offset, offset + size)
        return size
    }

    @Implementation
    public override fun play() {
        super.play()
        // AudioTrack exposes its attributes on newer Android versions; on
        // API 24 only the static-buffer lifecycle and PCM output are observed.
        val usage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) realTrack.audioAttributes.usage else null
        playbacks.offer(Playback(loadedSamples, stateBeforeWrite, realTrack.state, usage))
    }

    data class Playback(val samples: ShortArray, val stateBeforeWrite: Int, val stateAtPlay: Int, val usage: Int?)

    companion object { val playbacks = LinkedBlockingQueue<Playback>() }
}
