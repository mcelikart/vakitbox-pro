package com.example.alarm

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class SunriseAlarmRingingStateTest {
    private lateinit var context: Context
    private val now = 1_800_000_000_000L
    private fun delivery(id: String = "a", token: String = "token-a") =
        SunriseAlarmDelivery(id, token, now, now + 30 * 60_000L, "2027-01-15")

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        listOf(SunriseAlarmRingingState.PREFERENCES, SunriseAlarmVolumeLease.PREFERENCES, SunriseAlarmSettingsStore.PREFERENCES).forEach {
            context.getSharedPreferences(it, 0).edit().clear().commit()
        }
    }

    @Test fun deliveredQueueSurvivesProcessRecreationWithoutExtendingTheTenMinuteDeadline() {
        val record = SunriseAlarmRingingState(context).enqueue(delivery(), "Sabah", now)
        val duplicate = SunriseAlarmRingingState(context).enqueue(delivery(), "Başka etiket", now + 60_000L)
        assertEquals(record, duplicate)
        assertEquals(now + 10 * 60_000L, duplicate.expiresAtMillis)
        assertEquals(listOf(record), SunriseAlarmRingingState(context).list())
    }

    @Test fun stoppingOneOverlappingAlarmKeepsAllOtherTokensIncludingTheSameAlarmSnooze() {
        val state = SunriseAlarmRingingState(context)
        state.enqueue(delivery(), "İlk", now)
        state.enqueue(delivery("b", "token-b"), "İkinci", now)
        state.enqueue(delivery("a", "snooze-a").copy(fromSnooze = true), "İlk tekrar", now + 1)
        state.remove("token-a")
        assertEquals(setOf("token-b", "snooze-a"), state.list().map { it.delivery.token }.toSet())
        assertTrue(state.list().last().delivery.fromSnooze)
        state.removeForAlarm("a")
        assertEquals("b", state.list().single().delivery.alarmId)
    }

    @Test fun expiredRecordIsAvailableForServiceCleanupAndCannotResetItsDeadlineOnRestart() {
        val state = SunriseAlarmRingingState(context)
        val record = state.enqueue(delivery(), "", now)
        assertEquals(SunriseAlarm.DEFAULT_LABEL, record.label)
        assertEquals(record, SunriseAlarmRingingState(context).enqueue(delivery(), "", now + 11 * 60_000L))
        state.remove(record.delivery.token)
        assertTrue(state.list().isEmpty())
    }

    @Test fun malformedQueueEntryCannotRemoveAnotherValidAlarm() {
        val state = SunriseAlarmRingingState(context)
        state.enqueue(delivery(), "Sabah", now)
        val prefs = context.getSharedPreferences(SunriseAlarmRingingState.PREFERENCES, 0)
        val raw = requireNotNull(prefs.getString(SunriseAlarmRingingState.RECORDS, null))
        prefs.edit().putString(SunriseAlarmRingingState.RECORDS, raw.dropLast(1) + ", {\"alarmId\":\"broken\"}]").commit()
        assertEquals("token-a", state.list().single().delivery.token)
    }

    @Test fun mutedPhoneRaisesOnlyAlarmStreamThenRestoresAcrossLeaseObjectRecreation() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        val ring = audio.getStreamVolume(AudioManager.STREAM_RING)
        SunriseAlarmVolumeLease(context).acquire()
        assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0)
        assertEquals(AudioManager.RINGER_MODE_SILENT, audio.ringerMode)
        assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
        assertEquals(ring, audio.getStreamVolume(AudioManager.STREAM_RING))
        SunriseAlarmVolumeLease(context).acquire() // simulated process/service recovery
        SunriseAlarmVolumeLease(context).release()
        assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test fun changingAlarmVolumeWhileRingingPreservesTheUsersNewValue() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        val lease = SunriseAlarmVolumeLease(context)
        lease.acquire()
        val newVolume = (audio.getStreamVolume(AudioManager.STREAM_ALARM) - 1).coerceAtLeast(1)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0)
        lease.release()
        assertEquals(newVolume, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        assertTrue(context.getSharedPreferences(SunriseAlarmVolumeLease.PREFERENCES, 0).all.isEmpty())
    }

    @Test fun audibleAlarmVolumeIsNeverChanged() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0)
        val lease = SunriseAlarmVolumeLease(context)
        lease.acquire()
        assertEquals(2, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        lease.release()
        assertEquals(2, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test fun orphanRecoveryRestoresVolumeOnlyWhenNoUnexpiredAlarmIsQueued() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        SunriseAlarmVolumeLease(context).acquire()
        val state = SunriseAlarmRingingState(context)
        state.enqueue(delivery(), "Sabah", now)
        SunriseAlarmAudio.recoverVolumeIfIdle(context, now + 60_000L)
        assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0)
        SunriseAlarmAudio.recoverVolumeIfIdle(context, now + 11 * 60_000L)
        assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }
}
