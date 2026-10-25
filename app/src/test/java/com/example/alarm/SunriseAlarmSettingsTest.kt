package com.example.alarm

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class SunriseAlarmSettingsTest {
    private lateinit var context: Context
    private val now = 1_800_000_000_000L
    private val delivery = SunriseAlarmDelivery("alarm", "token", now, now + 1_800_000L, "2027-01-15")

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        listOf(SunriseAlarmSettingsStore.PREFERENCES, SunriseAlarmRingingState.PREFERENCES,
            SunriseAlarmVolumeLease.PREFERENCES, SunriseAlarmStore.PREFERENCES).forEach {
            context.getSharedPreferences(it, 0).edit().clear().commit()
        }
    }

    @Test fun settingsSurviveRecreationWithoutDeletingExistingAlarms() {
        val alarm = SunriseAlarm(id = "saved", minutesBeforeSunrise = 45)
        SunriseAlarmStore(context).save(alarm)
        val settings = SunriseAlarmSettings("content://media/internal/audio/media/7", 30, 83, true, 15, 20, true, AlarmVolumeKeyAction.DISMISS)
        SunriseAlarmSettingsStore(context).save(settings)
        assertEquals(settings, SunriseAlarmSettingsStore(context).read())
        assertEquals(alarm, SunriseAlarmStore(context).find("saved"))
    }

    @Test fun corruptedOrFutureValuesFallBackToAudibleSafeDefaults() {
        val json = JSONObject().put("ringMinutes", 999).put("snoozeMinutes", -1)
            .put("preNotificationMinutes", -1).put("volumePercent", 0)
            .put("volumeKeyAction", "FUTURE").put("ringtoneUri", "https://invalid.example/tone")
        context.getSharedPreferences(SunriseAlarmSettingsStore.PREFERENCES, 0).edit()
            .putString(SunriseAlarmSettingsStore.SETTINGS, json.toString()).commit()
        assertEquals(SunriseAlarmSettings(), SunriseAlarmSettingsStore(context).read())
    }

    @Test fun ringingSnapshotsDurationAndSnoozeAndDoesNotExtendOnSettingsChangeOrProcessRecovery() {
        val settings = SunriseAlarmSettings(ringMinutes = 30, snoozeMinutes = 15, gradualVolume = true)
        SunriseAlarmSettingsStore(context).save(settings)
        val record = SunriseAlarmRingingState(context).enqueue(delivery, "Sabah", now)
        SunriseAlarmSettingsStore(context).save(settings.copy(ringMinutes = 1, snoozeMinutes = 5))
        assertEquals(now + 30 * 60_000L, record.expiresAtMillis)
        assertEquals(settings, record.settings)
        assertEquals(record, SunriseAlarmRingingState(context).enqueue(delivery, "Tekrar", now + 60_000L))
    }

    @Test fun oldRingingRecordsKeepTheirOriginalDeadlineAndGainDefaultControls() {
        SunriseAlarmRingingState(context).enqueue(delivery, "Sabah", now)
        val prefs = context.getSharedPreferences(SunriseAlarmRingingState.PREFERENCES, 0)
        val raw = org.json.JSONArray(prefs.getString(SunriseAlarmRingingState.RECORDS, "[]"))
        raw.getJSONObject(0).remove("settings")
        prefs.edit().putString(SunriseAlarmRingingState.RECORDS, raw.toString()).commit()
        val record = SunriseAlarmRingingState(context).list().single()
        assertEquals(now + 10 * 60_000L, record.expiresAtMillis)
        assertEquals(SunriseAlarmSettings(), record.settings)
    }

    @Test fun preferredAlarmVolumeIsTemporaryAndAUserChangeSurvivesServiceRestart() {
        val audio = context.getSystemService(AudioManager::class.java)
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0)
        SunriseAlarmVolumeLease(context).acquire(100)
        assertEquals(maximum, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        SunriseAlarmVolumeLease(context).release()
        assertEquals(1, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        SunriseAlarmVolumeLease(context).acquire(100)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 2, 0)
        SunriseAlarmVolumeLease(context).acquire(100)
        SunriseAlarmVolumeLease(context).release()
        assertEquals(2, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test fun alreadyFaceDownPhoneDoesNotSnoozeAndOneDeliberateFlipOnlyFiresOnce() {
        val detector = SunriseAlarmFlipDetector()
        assertFalse(detector.update(-9.8f, 0))
        assertFalse(detector.update(-9.8f, 1_000))
        assertFalse(detector.update(9.8f, 2_000))
        assertFalse(detector.update(-9.8f, 2_100))
        assertFalse(detector.update(-9.8f, 2_500))
        assertTrue(detector.update(-9.8f, 2_800))
        assertFalse(detector.update(-9.8f, 4_000))
    }
}
