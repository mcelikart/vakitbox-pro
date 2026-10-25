package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Looper
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppRepository
import com.example.data.dataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class SunriseAlarmReceiverTest {
    private lateinit var context: Context
    private lateinit var manager: AlarmManager
    private lateinit var originalZone: TimeZone
    private lateinit var store: SunriseAlarmStore
    // The receiver intentionally reads the real wall clock. Arm a just-due occurrence using
    // the scheduler's explicit planning time, rather than mocking a bootstrap JVM clock.
    private val trigger = System.currentTimeMillis() / 60_000L * 60_000L
    private val now = trigger - 60_000L
    private val sunrise = trigger + 30 * 60_000L
    private var nextSunrise = 0L
    private fun at(value: String) = Instant.parse(value).toEpochMilli()
    private fun date(timestamp: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
    private fun time(timestamp: Long) = SimpleDateFormat("HH:mm", Locale.US).format(Date(timestamp))

    @Before fun setup() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext<Application>()
        manager = context.getSystemService(AlarmManager::class.java)
        listOf(SunriseAlarmStore.PREFERENCES, SunriseAlarmRingingState.PREFERENCES, SunriseAlarmVolumeLease.PREFERENCES, SunriseAlarmSettingsStore.PREFERENCES,
            "sunrise_alarm_runtime", "prayer_daily_times", "countdown_service_prefs").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        SunriseAlarmSettingsStore(context).save(SunriseAlarmSettings(preNotificationMinutes = 0))
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        if (Build.VERSION.SDK_INT >= 33) shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // Receiver data maintenance is allowed to run, but these tests must never request the network.
        runBlocking { context.dataStore.edit { it.remove(AppRepository.LATITUDE_KEY); it.remove(AppRepository.LONGITUDE_KEY) } }
        nextSunrise = Calendar.getInstance().apply { timeInMillis = sunrise; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).edit()
            .putString(date(sunrise), "Güneş,${time(sunrise)}")
            .putString(date(nextSunrise), "Güneş,${time(nextSunrise)}").commit()
        store = SunriseAlarmStore(context)
    }

    @After fun restore() {
        TimeZone.setDefault(originalZone)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test fun validFireStartsForegroundRingingAndRearmsNextDayButDuplicateDoesNotStartAgain() {
        store.save(SunriseAlarm(id = "morning"))
        SunriseAlarmScheduler.refresh(context, now)
        val delivery = SunriseAlarmScheduler.pending(context).single()
        val fire = SunriseAlarmScheduler.fireIntent(context, delivery)
        SunriseAlarmReceiver().onReceive(context, fire)
        val start = requireNotNull(shadowOf(context as Application).nextStartedService)
        assertEquals(SunriseAlarmRingingService::class.java.name, start.component?.className)
        assertEquals(SunriseAlarmRingingService.ACTION_RING, start.action)
        assertEquals(delivery.token, start.getStringExtra(SunriseAlarmRingingService.EXTRA_TOKEN))
        assertEquals(delivery, SunriseAlarmRingingState(context).list().single().delivery)
        val next = SunriseAlarmScheduler.pending(context).single()
        assertEquals(date(nextSunrise), next.sourceDate)
        assertEquals(nextSunrise - 30 * 60_000L, next.triggerAtMillis)
        assertNotEquals(delivery.token, next.token)
        SunriseAlarmReceiver().onReceive(context, fire)
        assertNull(shadowOf(context as Application).nextStartedService)
        assertEquals(1, SunriseAlarmRingingState(context).list().size)
    }

    @Test fun queuedFireForEditedOrDeletedAlarmCannotStartForegroundRinging() {
        val alarm = SunriseAlarm(id = "morning")
        store.save(alarm)
        SunriseAlarmScheduler.refresh(context, now)
        val old = SunriseAlarmScheduler.pending(context).single()
        store.save(alarm.copy(minutesBeforeSunrise = 45))
        SunriseAlarmReceiver().onReceive(context, SunriseAlarmScheduler.fireIntent(context, old))
        store.delete(alarm.id)
        SunriseAlarmReceiver().onReceive(context, SunriseAlarmScheduler.fireIntent(context, old))
        assertNull(shadowOf(context as Application).nextStartedService)
        assertTrue(SunriseAlarmRingingState(context).list().isEmpty())
    }

    @Test fun explicitManifestBootBroadcastClearsOldRingingAndVolumeLeaseWithoutStartingAlarmService() {
        val alarm = SunriseAlarm(id = "preboot")
        store.save(alarm)
        val delivery = SunriseAlarmDelivery(alarm.id, "preboot-token", now, sunrise, date(sunrise))
        SunriseAlarmRingingState(context).enqueue(delivery, alarm.label, now)
        SunriseAlarmRingingService.postStartFailureNotification(context)
        val audio = context.getSystemService(AudioManager::class.java)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        SunriseAlarmVolumeLease(context, audio).acquire()
        assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0)
        store.save(alarm.copy(enabled = false))

        context.sendBroadcast(Intent(context, SunriseAlarmReceiver::class.java).setAction(Intent.ACTION_BOOT_COMPLETED))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(SunriseAlarmRingingState(context).list().isEmpty())
        assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        assertNull(shadowOf(context.getSystemService(NotificationManager::class.java))
            .getNotification(SunriseAlarmRingingService.NOTIFICATION_ID))
        // Other manifest receivers may restore the permanent countdown on BOOT_COMPLETED.
        val startedServices = generateSequence { shadowOf(context as Application).nextStartedService }.toList()
        assertTrue("Reboot must not resume ringing: $startedServices", startedServices.none {
            it.component?.className == SunriseAlarmRingingService::class.java.name
        })
        assertTrue(SunriseAlarmScheduler.pending(context).isEmpty())
    }

    @Test fun dailyMaintenanceUsesNextLocalMidnightPlusFiveMinutesAndNoAlarmClockIcon() {
        store.save(SunriseAlarm(id = "morning"))
        val beforeMidnight = at("2027-01-31T23:59:00Z")
        SunriseAlarmReceiver.armRefresh(context, beforeMidnight)
        SunriseAlarmReceiver.armRefresh(context, beforeMidnight)
        val maintenance = shadowOf(manager).scheduledAlarms.single()
        assertEquals(at("2027-02-01T00:05:00Z"), maintenance.triggerAtTime)
        assertTrue(maintenance.isAllowWhileIdle)
        assertNull(maintenance.alarmClockInfo)
        assertEquals(SunriseAlarmReceiver.ACTION_REFRESH, shadowOf(requireNotNull(maintenance.operation)).savedIntent.action)
    }

    @Test fun dailyMaintenanceFollowsLocalDateAfterTimezoneChangeAndCancelsWhenNoAlarmIsEnabled() {
        val alarm = SunriseAlarm(id = "morning")
        store.save(alarm)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
        SunriseAlarmReceiver.armRefresh(context, at("2027-01-31T20:50:00Z"))
        assertEquals(at("2027-01-31T21:05:00Z"), shadowOf(manager).scheduledAlarms.single().triggerAtTime)
        store.save(alarm.copy(enabled = false))
        SunriseAlarmReceiver.armRefresh(context, now)
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
    }
}
