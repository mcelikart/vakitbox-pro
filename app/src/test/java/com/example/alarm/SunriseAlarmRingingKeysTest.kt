package com.example.alarm

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowAudioManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class SunriseAlarmRingingKeysTest {
    private lateinit var context: Context
    private lateinit var zone: TimeZone

    @Before fun setup() {
        zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext()
        listOf(SunriseAlarmSettingsStore.PREFERENCES, SunriseAlarmStore.PREFERENCES,
            SunriseAlarmRingingState.PREFERENCES, "sunrise_alarm_runtime", "prayer_daily_times", "countdown_service_prefs").forEach {
            context.getSharedPreferences(it, 0).edit().clear().commit()
        }
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        if (Build.VERSION.SDK_INT >= 33) shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After fun cleanup() { TimeZone.setDefault(zone) }

    private fun ring(action: AlarmVolumeKeyAction) {
        val trigger = System.currentTimeMillis() / 60_000L * 60_000L
        val sunrise = trigger + 30 * 60_000L
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(sunrise))
        val clock = SimpleDateFormat("HH:mm", Locale.US).format(Date(sunrise))
        context.getSharedPreferences("prayer_daily_times", 0).edit().putString(date, "Güneş,$clock").commit()
        SunriseAlarmSettingsStore(context).save(SunriseAlarmSettings(preNotificationMinutes = 0,
            snoozeMinutes = 15, volumeKeyAction = action))
        SunriseAlarmStore(context).save(SunriseAlarm(id = "morning"))
        SunriseAlarmScheduler.refresh(context, trigger - 60_000L)
        val delivery = SunriseAlarmScheduler.pending(context).single()
        assertNotNull(SunriseAlarmScheduler.receive(context, SunriseAlarmScheduler.fireIntent(context, delivery), trigger))
    }

    @Test fun heldVolumeKeySnoozesOnceUsingConfiguredDuration() {
        ring(AlarmVolumeKeyAction.SNOOZE)
        val controller = Robolectric.buildActivity(SunriseAlarmRingingActivity::class.java).create()
        try {
            val activity = controller.get()
            val before = System.currentTimeMillis()
            assertTrue(activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)))
            assertTrue(activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 1)))
            val pending = SunriseAlarmScheduler.pending(context).single()
            assertTrue(pending.fromSnooze)
            assertTrue(pending.triggerAtMillis >= before + 15 * 60_000L)
            assertTrue(pending.triggerAtMillis <= System.currentTimeMillis() + 15 * 60_000L)
            assertTrue(SunriseAlarmRingingState(context).list().isEmpty())
            assertTrue(activity.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN)))
        } finally { controller.destroy() }
    }

    @Test fun dismissKeyIgnoresRepeatsUntilFirstDownAndNoneLeavesAlarmUnchanged() {
        ring(AlarmVolumeKeyAction.DISMISS)
        val controller = Robolectric.buildActivity(SunriseAlarmRingingActivity::class.java).create()
        try {
            val activity = controller.get()
            activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 1))
            assertEquals(1, SunriseAlarmRingingState(context).list().size)
            activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP))
            assertTrue(SunriseAlarmRingingState(context).list().isEmpty())
        } finally { controller.destroy() }
        ring(AlarmVolumeKeyAction.NONE)
        val noAction = Robolectric.buildActivity(SunriseAlarmRingingActivity::class.java).create()
        try {
            noAction.get().onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP))
            assertEquals(1, SunriseAlarmRingingState(context).list().size)
        } finally { noAction.destroy() }
    }

    @Test
    @Config(shadows = [TargetedAlarmKeyAudioManagerShadow::class])
    fun volumeModeChangesAlarmStreamOnlyAndKeepsAlarmRinging() {
        ring(AlarmVolumeKeyAction.VOLUME)
        val audio = context.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 1, 0)
        val music = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val controller = Robolectric.buildActivity(SunriseAlarmRingingActivity::class.java).create()
        try {
            controller.get().onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP))
            val volumeCall = Shadow.extract<TargetedAlarmKeyAudioManagerShadow>(audio)
            assertEquals(Triple(AudioManager.STREAM_ALARM, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI), volumeCall.lastAdjustment)
            assertEquals(2, audio.getStreamVolume(AudioManager.STREAM_ALARM))
            assertEquals(music, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertEquals(1, SunriseAlarmRingingState(context).list().size)
        } finally { controller.destroy() }
    }
}

/** Robolectric 4.16.1's adjustStreamVolume calls its all-stream setStreamVolume helper.
 * Keep the real API boundary observable while modelling Android's requested-stream behavior. */
@Implements(AudioManager::class)
class TargetedAlarmKeyAudioManagerShadow : ShadowAudioManager() {
    @RealObject private lateinit var manager: AudioManager
    var lastAdjustment: Triple<Int, Int, Int>? = null
        private set

    @Implementation
    public override fun adjustStreamVolume(streamType: Int, direction: Int, flags: Int) {
        lastAdjustment = Triple(streamType, direction, flags)
        val change = when (direction) {
            AudioManager.ADJUST_RAISE -> 1
            AudioManager.ADJUST_LOWER -> -1
            else -> 0
        }
        manager.setStreamVolume(streamType,
            (manager.getStreamVolume(streamType) + change).coerceIn(0, manager.getStreamMaxVolume(streamType)), flags)
    }
}
