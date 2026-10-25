package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class SunriseAlarmPreNotificationTest {
    private lateinit var context: Context
    private lateinit var notifications: NotificationManager
    private lateinit var manager: AlarmManager
    private lateinit var zone: TimeZone
    private val now = Instant.parse("2027-01-31T05:00:00Z").toEpochMilli()
    private val trigger = Instant.parse("2027-01-31T06:45:00Z").toEpochMilli()

    @Before fun setup() {
        zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext()
        notifications = context.getSystemService(NotificationManager::class.java)
        manager = context.getSystemService(AlarmManager::class.java)
        listOf(SunriseAlarmSettingsStore.PREFERENCES, SunriseAlarmStore.PREFERENCES,
            SunriseAlarmRingingState.PREFERENCES, "sunrise_alarm_runtime", "prayer_daily_times", "countdown_service_prefs").forEach {
            context.getSharedPreferences(it, 0).edit().clear().commit()
        }
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(notifications).setNotificationsEnabled(true)
        if (Build.VERSION.SDK_INT >= 33) shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        context.getSharedPreferences("prayer_daily_times", 0).edit().putString("2027-01-31", "Güneş,07:15").commit()
        SunriseAlarmStore(context).save(SunriseAlarm(id = "morning"))
        if (Build.VERSION.SDK_INT >= 26) notifications.createNotificationChannel(NotificationChannel("countdown-test", "Sayaç", NotificationManager.IMPORTANCE_LOW))
        notifications.notify(1001, NotificationCompat.Builder(context, "countdown-test")
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Sayaç").setOngoing(true).build())
    }

    @After fun cleanup() { TimeZone.setDefault(zone) }

    private fun delivery(): SunriseAlarmDelivery {
        SunriseAlarmScheduler.refresh(context, now)
        return SunriseAlarmScheduler.pending(context).single()
    }

    @Test fun upcomingNoticeHasSeparateSilentIdentityAndNeverStartsRingingOrConsumesDelivery() {
        val delivery = delivery()
        val reminder = shadowOf(manager).scheduledAlarms.single {
            shadowOf(requireNotNull(it.operation)).savedIntent.action == SunriseAlarmPreNotification.ACTION_PRE_NOTIFY
        }
        assertEquals(trigger - 30 * 60_000L, reminder.triggerAtTime)
        SunriseAlarmPreNotification.receive(context, SunriseAlarmPreNotification.intent(context, delivery), trigger - 30 * 60_000L)
        val notice = notifications.activeNotifications.single { it.id == SunriseAlarmPreNotification.NOTIFICATION_ID }.notification
        assertNull(notice.fullScreenIntent)
        assertEquals(0, notice.defaults)
        assertTrue(SunriseAlarmRingingState(context).list().isEmpty())
        assertEquals(delivery, SunriseAlarmScheduler.pending(context).single())
        assertNotNull(shadowOf(notifications).getNotification(1001))
        if (Build.VERSION.SDK_INT >= 26) assertNull(notifications.getNotificationChannel(notice.channelId).sound)
    }

    @Test fun changingLeadTimeRearmsAndTurningItOffCancelsAlreadyVisibleNotice() {
        val delivery = delivery()
        SunriseAlarmSettingsStore(context).save(SunriseAlarmSettings(preNotificationMinutes = 15))
        SunriseAlarmScheduler.refresh(context, now)
        val pre = shadowOf(manager).scheduledAlarms.single { it.alarmClockInfo == null }
        assertEquals(trigger - 15 * 60_000L, pre.triggerAtTime)
        SunriseAlarmPreNotification.receive(context, SunriseAlarmPreNotification.intent(context, delivery), trigger - 15 * 60_000L)
        SunriseAlarmSettingsStore(context).save(SunriseAlarmSettings(preNotificationMinutes = 0))
        SunriseAlarmScheduler.refresh(context, trigger - 10 * 60_000L)
        assertTrue(notifications.activeNotifications.none { it.id == SunriseAlarmPreNotification.NOTIFICATION_ID })
        assertTrue(shadowOf(manager).scheduledAlarms.all { it.alarmClockInfo != null })
        SunriseAlarmPreNotification.receive(context, SunriseAlarmPreNotification.intent(context, delivery), trigger - 5 * 60_000L)
        assertTrue(notifications.activeNotifications.none { it.id == SunriseAlarmPreNotification.NOTIFICATION_ID })
    }

    @Test fun editedDisabledDeletedOrDifferentDatedSunriseCannotShowStaleNotice() {
        val old = delivery()
        SunriseAlarmStore(context).save(SunriseAlarm(id = "morning", minutesBeforeSunrise = 45))
        SunriseAlarmScheduler.refresh(context, now)
        SunriseAlarmPreNotification.receive(context, SunriseAlarmPreNotification.intent(context, old), trigger - 20 * 60_000L)
        assertTrue(notifications.activeNotifications.none { it.id == SunriseAlarmPreNotification.NOTIFICATION_ID })
        val current = SunriseAlarmScheduler.pending(context).single()
        context.getSharedPreferences("prayer_daily_times", 0).edit().putString("2027-01-31", "Güneş,07:20").commit()
        SunriseAlarmPreNotification.receive(context, SunriseAlarmPreNotification.intent(context, current), trigger - 20 * 60_000L)
        assertTrue(notifications.activeNotifications.none { it.id == SunriseAlarmPreNotification.NOTIFICATION_ID })
        SunriseAlarmStore(context).delete("morning")
        SunriseAlarmScheduler.refresh(context, now)
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
        assertNotNull(shadowOf(notifications).getNotification(1001))
    }

    @Test fun actualAlarmDeliveryRemovesPreNoticeAndUsesSnapshottedSnoozeDuration() {
        SunriseAlarmSettingsStore(context).save(SunriseAlarmSettings(snoozeMinutes = 15))
        val delivery = delivery()
        SunriseAlarmPreNotification.receive(context, SunriseAlarmPreNotification.intent(context, delivery), trigger - 20 * 60_000L)
        assertNotNull(SunriseAlarmScheduler.receive(context, SunriseAlarmScheduler.fireIntent(context, delivery), trigger))
        assertTrue(notifications.activeNotifications.none { it.id == SunriseAlarmPreNotification.NOTIFICATION_ID })
        SunriseAlarmSettingsStore(context).save(SunriseAlarmSettings(snoozeMinutes = 5))
        assertTrue(SunriseAlarmScheduler.snooze(context, delivery.token, now = trigger + 1_000L))
        assertEquals(trigger + 1_000L + 15 * 60_000L, SunriseAlarmScheduler.pending(context).single().triggerAtMillis)
        assertNotNull(shadowOf(notifications).getNotification(1001))
    }
}
