package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class SunriseAlarmRingingServiceTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private lateinit var audio: AudioManager

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        listOf(SunriseAlarmStore.PREFERENCES, SunriseAlarmRingingState.PREFERENCES, SunriseAlarmVolumeLease.PREFERENCES, SunriseAlarmSettingsStore.PREFERENCES,
            "sunrise_alarm_scheduler").forEach { context.getSharedPreferences(it, 0).edit().clear().commit() }
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("kept_countdown", "Sayaç", NotificationManager.IMPORTANCE_DEFAULT))
        }
        manager.notify(1001, NotificationCompat.Builder(context, "kept_countdown")
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Sayaç").setOngoing(true).build())
    }

    private fun queue(id: String): SunriseAlarmDelivery {
        SunriseAlarmStore(context).save(SunriseAlarm(id = id, label = "Alarm $id"))
        val now = System.currentTimeMillis()
        val delivery = SunriseAlarmDelivery(id, "token-$id", now, now + 30 * 60_000L, "2027-01-15")
        SunriseAlarmRingingState(context).enqueue(delivery, "Alarm $id", now)
        return delivery
    }

    @Test fun ringingUsesFullScreenHighAlarmNotificationWithNoGroupSuppressionAndNoCountdownCollision() {
        val delivery = queue("a")
        val controller = Robolectric.buildService(SunriseAlarmRingingService::class.java).create()
        try {
            controller.get().onStartCommand(SunriseAlarmRingingService.startIntent(context, delivery), 0, 1)
            val notification = shadowOf(controller.get()).lastForegroundNotification
            assertEquals(83001, shadowOf(controller.get()).lastForegroundNotificationId)
            assertEquals(Notification.CATEGORY_ALARM, notification.category)
            assertNotNull(notification.fullScreenIntent)
            assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertNotNull(shadowOf(manager).getNotification(1001))
            assertEquals(AudioAttributes.USAGE_ALARM, SunriseAlarmAudio.ALARM_ATTRIBUTES.usage)
            assertEquals(2, notification.actions.size)
            assertEquals(SunriseAlarmReceiver.ACTION_SNOOZE,
                shadowOf(notification.actions[0].actionIntent).savedIntent.action)
            assertEquals("token-a", shadowOf(notification.actions[1].actionIntent).savedIntent
                .getStringExtra(SunriseAlarmReceiver.EXTRA_TOKEN))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assertEquals(Notification.GROUP_ALERT_ALL, notification.groupAlertBehavior)
                val channel = manager.getNotificationChannel(notification.channelId)
                assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
                assertNull(channel.sound)
                assertFalse(channel.shouldVibrate())
            }
        } finally { controller.destroy() }
        assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test fun overlappingAlarmsRemainAudibleUntilTheLastTokenIsDismissedAndCountdownIsPreserved() {
        val first = queue("a")
        queue("b")
        val controller = Robolectric.buildService(SunriseAlarmRingingService::class.java).create()
        try {
            controller.get().onStartCommand(SunriseAlarmRingingService.startIntent(context, first), 0, 1)
            assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0)
            SunriseAlarmScheduler.dismiss(context, "token-a")
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("token-b", SunriseAlarmRingingState(context).list().single().delivery.token)
            assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0)
            assertFalse(shadowOf(controller.get()).isStoppedBySelf)
            SunriseAlarmScheduler.dismiss(context, "token-b")
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(shadowOf(controller.get()).isStoppedBySelf)
            assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
            assertNotNull(shadowOf(manager).getNotification(1001))
        } finally { controller.destroy() }
    }

    @Test fun unansweredAlarmStopsAfterTenMinutesWithoutStoppingTheCountdown() {
        val delivery = queue("a")
        val controller = Robolectric.buildService(SunriseAlarmRingingService::class.java).create()
        try {
            controller.get().onStartCommand(SunriseAlarmRingingService.startIntent(context, delivery), 0, 1)
            shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.MINUTES)
            assertTrue(SunriseAlarmRingingState(context).list().isEmpty())
            assertTrue(shadowOf(controller.get()).isStoppedBySelf)
            assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
            assertNotNull(shadowOf(manager).getNotification(1001))
        } finally { controller.destroy() }
    }

    @Test fun staleStartIntentCannotResurrectAnAlreadyDismissedAlarm() {
        val delivery = queue("a")
        SunriseAlarmScheduler.dismiss(context, delivery.token)
        val controller = Robolectric.buildService(SunriseAlarmRingingService::class.java).create()
        try {
            assertEquals(android.app.Service.START_NOT_STICKY,
                controller.get().onStartCommand(SunriseAlarmRingingService.startIntent(context, delivery), 0, 1))
            assertNull(shadowOf(controller.get()).lastForegroundNotification)
            assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        } finally { controller.destroy() }
    }

    @Test fun blockedStartFallbackProvidesFullScreenAndBroadcastControlsWithoutStartingAService() {
        queue("a")
        SunriseAlarmRingingService.postStartFailureNotification(context)
        val notification = shadowOf(manager).getNotification(SunriseAlarmRingingService.NOTIFICATION_ID)
        assertNotNull(notification.fullScreenIntent)
        assertNull(shadowOf(context as Application).nextStartedService)
        val action = shadowOf(notification.actions[1].actionIntent).savedIntent
        assertEquals(SunriseAlarmReceiver::class.java.name, action.component?.className)
        SunriseAlarmReceiver().onReceive(context, action)
        assertTrue(SunriseAlarmRingingState(context).list().isEmpty())
        assertNotNull(shadowOf(manager).getNotification(1001))
    }

    @Test fun visibleAlarmActivityRetriesBlockedServiceOnlyForAnUnexpiredQueue() {
        queue("a")
        val controller = Robolectric.buildActivity(SunriseAlarmRingingActivity::class.java).create().start().resume()
        try {
            val retry = requireNotNull(shadowOf(context as Application).nextStartedService)
            assertEquals(SunriseAlarmRingingService::class.java.name, retry.component?.className)
            assertEquals(SunriseAlarmRingingService.ACTION_SYNC, retry.action)
        } finally { controller.pause().stop().destroy() }
    }
}
