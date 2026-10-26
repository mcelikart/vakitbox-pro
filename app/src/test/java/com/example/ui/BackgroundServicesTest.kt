package com.example.ui

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.PendingIntent
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
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
import org.robolectric.shadows.ShadowActivityManager.ApplicationExitInfoBuilder
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackgroundServicesTest {
    private lateinit var app: Application
    private lateinit var jobs: JobScheduler
    private lateinit var alarms: AlarmManager

    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        jobs = app.getSystemService(JobScheduler::class.java)
        alarms = app.getSystemService(AlarmManager::class.java)
        jobs.cancelAll()
        alarms.cancelAll()
        listOf("background_service_recovery", "countdown_service_prefs", "prayer_daily_times", "alarm_prefs")
            .forEach { app.getSharedPreferences(it, 0).edit().clear().commit() }
        BackgroundServices.onCountdownStopped()
        while (shadowOf(app).nextStartedService != null) { /* Drain earlier starts. */ }
    }

    @After fun cleanup() {
        BackgroundServices.onCountdownStopped()
        jobs.cancelAll()
        alarms.cancelAll()
    }

    @Test fun repeatedArmingKeepsOnePersistedJobAndItsOriginalSchedule() {
        BackgroundServices.armWatchdog(app)
        val original = checkNotNull(jobs.getPendingJob(BackgroundServices.RECOVERY_JOB_ID))
        repeat(5) { BackgroundServices.armWatchdog(app) }
        assertEquals(1, jobs.allPendingJobs.size)
        assertSame(original, jobs.getPendingJob(BackgroundServices.RECOVERY_JOB_ID))
        assertTrue(original.isPersisted)
        assertTrue(original.isPeriodic)
        assertEquals(15 * 60_000L, original.intervalMillis)
        assertEquals(BackgroundRecoveryJobService::class.java.name, original.service.className)
        assertTrue(shadowOf(alarms).scheduledAlarms.isEmpty())
    }

    @Test fun watchdogDoesNotPostponeShortRelativeRecoveryOrRequireExactAlarmPermission() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val scheduledAt = SystemClock.elapsedRealtime()
        BackgroundServices.scheduleRecovery(app)
        val recovery = shadowOf(alarms).scheduledAlarms.single()
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, recovery.type)
        assertEquals(scheduledAt + 30_000L, recovery.triggerAtTime)
        assertTrue(recovery.isAllowWhileIdle)
        assertNotEquals(ShadowAlarmManager.WINDOW_EXACT, recovery.windowLengthMs)
        repeat(5) { BackgroundServices.armWatchdog(app) }
        val stillPending = shadowOf(alarms).scheduledAlarms.single()
        assertEquals(recovery.triggerAtTime, stillPending.triggerAtTime)
        assertEquals(recovery.operation, stillPending.operation)
    }

    @Test fun successfulPromotionCancelsShortRecoveryButKeepsDurableMaintenance() {
        BackgroundServices.scheduleRecovery(app)
        BackgroundServices.onCountdownStarted(app)
        assertTrue(BackgroundServices.hasHealthyCountdown())
        assertTrue(shadowOf(alarms).scheduledAlarms.isEmpty())
        assertNotNull(jobs.getPendingJob(BackgroundServices.RECOVERY_JOB_ID))
    }

    @Test fun deniedStartPreservesFreshDataAndDoesNotCreateImmediateRetryLoop() {
        val denied = object : ContextWrapper(app) {
            override fun startForegroundService(service: Intent): ComponentName? {
                throw IllegalStateException("Background foreground-service start denied")
            }
        }
        val update = Intent().putExtra("LOCATION_NAME", "Ankara")
            .putExtra("NEXT_PRAYER_NAME", "Öğle")
            .putExtra("NEXT_PRAYER_TIME_MILLIS", 1_900_000_000_000L)
            .putExtra("CURRENT_PRAYER_INDEX", 1)
            .putExtra("ALL_PRAYERS", "İmsak,05:30|Güneş,07:00|Öğle,12:30")
            .putExtra("ALL_PRAYERS_DATE", "2026-10-27")
        assertFalse(BackgroundServices.startCountdown(denied, update, userInitiated = true))
        val stored = app.getSharedPreferences("countdown_service_prefs", 0)
        assertEquals("Ankara", stored.getString("LOCATION_NAME", null))
        assertEquals("Öğle", stored.getString("NEXT_PRAYER_NAME", null))
        assertEquals(1_900_000_000_000L, stored.getLong("NEXT_PRAYER_TIME_MILLIS", 0L))
        assertEquals("2026-10-27", stored.getString("ALL_PRAYERS_DATE", null))
        assertEquals(1, stored.getInt("CURRENT_PRAYER_INDEX", -1))
        assertNotNull(jobs.getPendingJob(BackgroundServices.RECOVERY_JOB_ID))
        assertTrue(shadowOf(alarms).scheduledAlarms.isEmpty())
        assertNull(shadowOf(app).nextStartedService)
    }

    @Test fun healthyCountdownIsNotRestartedByMaintenance() {
        BackgroundServices.onCountdownStarted(app)
        val controller = Robolectric.buildService(BackgroundRecoveryJobService::class.java).create()
        try {
            assertFalse(controller.get().onStartJob(null))
            assertNull(shadowOf(app).nextStartedService)
            assertTrue(BackgroundServices.hasHealthyCountdown())
        } finally { controller.destroy() }
    }

    @Test fun persistedMaintenanceRecoversColdProcessWithoutDestroyCallback() {
        // No CountdownService was created and no onDestroy callback is needed.
        BackgroundServices.armWatchdog(app)
        val controller = Robolectric.buildService(BackgroundRecoveryJobService::class.java).create()
        try {
            assertFalse(controller.get().onStartJob(null))
            assertEquals(CountdownService::class.java.name, shadowOf(app).nextStartedService.component?.className)
            assertNotNull(jobs.getPendingJob(BackgroundServices.RECOVERY_JOB_ID))
            assertFalse(controller.get().onStopJob(null))
        } finally { controller.destroy() }
    }

    @Test fun staleHeartbeatRecoversWhileFreshHeartbeatKeepsExistingLoop() {
        BackgroundServices.onCountdownStarted(app)
        ShadowSystemClock.advanceBy(Duration.ofMinutes(4))
        assertFalse(BackgroundServices.hasHealthyCountdown())
        BackgroundServices.performMaintenance(app)
        assertEquals(CountdownService::class.java.name, shadowOf(app).nextStartedService.component?.className)
        BackgroundServices.onCountdownHeartbeat(app)
        BackgroundServices.performMaintenance(app)
        assertNull(shadowOf(app).nextStartedService)
    }

    @Test fun schedulerFailureCannotStopAnAlreadyPromotedCountdown() {
        shadowOf(jobs).failOnJob(BackgroundServices.RECOVERY_JOB_ID)
        BackgroundServices.onCountdownStarted(app)
        assertTrue(BackgroundServices.hasHealthyCountdown())
        assertNull(jobs.getPendingJob(BackgroundServices.RECOVERY_JOB_ID))
    }

    @Test fun maintenanceHonorsUserStopUntilVisibleAppInteraction() {
        val activityManager = shadowOf(app.getSystemService(ActivityManager::class.java))
        val stoppedAt = System.currentTimeMillis() - 1L
        activityManager.addApplicationExitInfo(ApplicationExitInfoBuilder.newBuilder()
            .setProcessName(app.packageName).setPid(123)
            .setReason(ApplicationExitInfo.REASON_USER_REQUESTED).setTimestamp(stoppedAt).build())
        BackgroundServices.performMaintenance(app)
        BackgroundServices.scheduleRecovery(app)
        assertFalse(BackgroundServices.automaticRecoveryAllowed(app))
        assertNull(shadowOf(app).nextStartedService)
        assertTrue(shadowOf(alarms).scheduledAlarms.isEmpty())
        // Maintenance-process exits must not erase a previously observed user stop.
        repeat(40) { index ->
            activityManager.addApplicationExitInfo(ApplicationExitInfoBuilder.newBuilder()
                .setProcessName(app.packageName).setPid(200 + index)
                .setReason(ApplicationExitInfo.REASON_LOW_MEMORY)
                .setTimestamp(stoppedAt + index + 1L).build())
        }
        assertFalse(BackgroundServices.automaticRecoveryAllowed(app))
        assertTrue(BackgroundServices.startCountdown(app, userInitiated = true))
        assertTrue(BackgroundServices.automaticRecoveryAllowed(app))
        assertEquals(CountdownService::class.java.name, shadowOf(app).nextStartedService.component?.className)
    }

    @Test fun migrationRemovesOnlyLegacyExactWatchdog() {
        val legacy = PendingIntent.getBroadcast(app, 9999,
            Intent(app, PrayerNotificationReceiver::class.java).setAction(PrayerNotificationReceiver.ACTION_RESTART),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val unrelated = PendingIntent.getBroadcast(app, 4444,
            Intent(app, PrayerNotificationReceiver::class.java).setAction("unrelated_test_alarm"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 900_000L, legacy)
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1_200_000L, unrelated)
        BackgroundServices.armWatchdog(app)
        assertEquals(unrelated, shadowOf(alarms).scheduledAlarms.single().operation)
        assertTrue(shadowOf(legacy).isCanceled)
    }
}
