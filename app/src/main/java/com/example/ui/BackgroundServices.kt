package com.example.ui

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.ApplicationExitInfo
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat

/** Bounded maintenance for the visible countdown, independent of activity lifetime. */
object BackgroundServices {
    internal const val RECOVERY_JOB_ID = 10_002
    internal const val RECOVERY_REQUEST_ID = 10_003
    internal const val MAINTENANCE_INTERVAL = 15 * 60_000L
    internal const val RECOVERY_DELAY = 30_000L
    private const val HEALTH_TIMEOUT = 3 * 60_000L
    private const val PREFS = "background_service_recovery"
    private const val LAST_USER_START = "last_user_start"
    private const val LAST_USER_STOP = "last_user_stop"
    private const val LEGACY_MIGRATED = "legacy_exact_watchdog_removed"
    private const val TAG = "BackgroundServices"

    @Volatile private var countdownRunning = false
    @Volatile private var lastHeartbeat = 0L

    /** True means Android accepted the request; onCountdownStarted confirms promotion. */
    fun startCountdown(context: Context, intent: Intent? = null, userInitiated: Boolean = false): Boolean {
        if (userInitiated) {
            preferences(context).edit().putLong(LAST_USER_START, System.currentTimeMillis()).apply()
        } else if (!automaticRecoveryAllowed(context)) {
            return false
        }
        // Keep fresh data even if Android defers this particular start request.
        intent?.let { saveCountdownData(context, it) }
        armWatchdog(context)
        val serviceIntent = Intent(context, CountdownService::class.java).apply {
            intent?.extras?.let { putExtras(it) }
        }
        return try {
            ContextCompat.startForegroundService(context, serviceIntent)
            true
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Countdown start deferred to an allowed foreground start", error)
            false
        } catch (error: SecurityException) {
            Log.w(TAG, "Countdown foreground permission unavailable", error)
            false
        }
    }

    fun onCountdownStarted(context: Context) {
        countdownRunning = true
        lastHeartbeat = SystemClock.elapsedRealtime()
        cancelShortRecovery(context)
        armWatchdog(context)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCountdownHeartbeat(context: Context) {
        // No scheduling or disk writes on the rendering path.
        countdownRunning = true
        lastHeartbeat = SystemClock.elapsedRealtime()
    }

    fun onCountdownStopped() {
        countdownRunning = false
        lastHeartbeat = 0L
    }

    internal fun hasHealthyCountdown(now: Long = SystemClock.elapsedRealtime()): Boolean =
        countdownRunning && now - lastHeartbeat in 0..HEALTH_TIMEOUT

    /** A single opportunistic retry; it is deliberately not an exact alarm. */
    fun scheduleRecovery(context: Context) {
        armWatchdog(context)
        if (!automaticRecoveryAllowed(context)) return
        try {
            val alarm = context.getSystemService(AlarmManager::class.java) ?: return
            alarm.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + RECOVERY_DELAY,
                recoveryPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
            )
        } catch (error: RuntimeException) {
            // The persisted maintenance job remains available; no immediate retry loop.
            Log.w(TAG, "Short countdown recovery could not be scheduled", error)
        }
    }

    /** A persisted job survives ordinary process death without an onDestroy callback. */
    fun armWatchdog(context: Context) {
        removeLegacyExactWatchdog(context)
        try {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            val service = ComponentName(context, BackgroundRecoveryJobService::class.java)
            val existing = scheduler.getPendingJob(RECOVERY_JOB_ID)
            if (existing?.service == service && existing.isPersisted && existing.isPeriodic &&
                existing.intervalMillis == MAINTENANCE_INTERVAL) return
            val job = JobInfo.Builder(RECOVERY_JOB_ID, service)
                .setPeriodic(MAINTENANCE_INTERVAL)
                .setPersisted(true)
                .build()
            if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) {
                Log.w(TAG, "Countdown maintenance scheduling was declined")
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Countdown maintenance unavailable", error)
        }
    }

    internal fun performMaintenance(context: Context) {
        if (!automaticRecoveryAllowed(context)) return
        try {
            PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Prayer schedule maintenance deferred", error)
        }
        if (!hasHealthyCountdown()) startCountdown(context)
    }

    /** Task Manager Stop is intentional. Only a later app interaction clears it. */
    internal fun automaticRecoveryAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        return try {
            val manager = context.getSystemService(ActivityManager::class.java) ?: return true
            val prefs = preferences(context)
            val lastUserStart = prefs.getLong(LAST_USER_START, 0L)
            val recordedStop = prefs.getLong(LAST_USER_STOP, 0L)
            if (recordedStop > lastUserStart) return false
            val lastUserExit = manager.getHistoricalProcessExitReasons(context.packageName, 0, 32)
                .filter { it.processName == context.packageName && it.reason == ApplicationExitInfo.REASON_USER_REQUESTED }
                .maxByOrNull { it.timestamp }
            val stoppedAt = lastUserExit?.timestamp ?: 0L
            if (stoppedAt > recordedStop) prefs.edit().putLong(LAST_USER_STOP, stoppedAt).apply()
            stoppedAt <= lastUserStart
        } catch (error: RuntimeException) {
            // Some vendor builds omit exit history. Android still enforces force-stop.
            Log.w(TAG, "Process exit history unavailable", error)
            true
        }
    }

    private fun saveCountdownData(context: Context, intent: Intent) {
        val edit = context.getSharedPreferences("countdown_service_prefs", Context.MODE_PRIVATE).edit()
        for (key in listOf("LOCATION_NAME", "NEXT_PRAYER_NAME", "ALL_PRAYERS", "ALL_PRAYERS_DATE")) {
            intent.getStringExtra(key)?.let { edit.putString(key, it) }
        }
        if (intent.hasExtra("NEXT_PRAYER_TIME_MILLIS")) {
            edit.putLong("NEXT_PRAYER_TIME_MILLIS", intent.getLongExtra("NEXT_PRAYER_TIME_MILLIS", 0L))
        }
        if (intent.hasExtra("CURRENT_PRAYER_INDEX")) {
            edit.putInt("CURRENT_PRAYER_INDEX", intent.getIntExtra("CURRENT_PRAYER_INDEX", -1))
        }
        edit.apply()
    }

    private fun removeLegacyExactWatchdog(context: Context) {
        try {
            val prefs = preferences(context)
            if (prefs.getBoolean(LEGACY_MIGRATED, false)) return
            val pending = PendingIntent.getBroadcast(context, 9999,
                Intent(context, PrayerNotificationReceiver::class.java)
                    .setAction(PrayerNotificationReceiver.ACTION_RESTART),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pending != null) {
                context.getSystemService(AlarmManager::class.java)?.cancel(pending)
                pending.cancel()
            }
            prefs.edit().putBoolean(LEGACY_MIGRATED, true).apply()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Legacy countdown watchdog cleanup deferred", error)
        }
    }

    private fun cancelShortRecovery(context: Context) {
        try {
            recoveryPendingIntent(context, PendingIntent.FLAG_NO_CREATE)?.let {
                context.getSystemService(AlarmManager::class.java)?.cancel(it)
                it.cancel()
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Completed recovery cleanup deferred", error)
        }
    }

    private fun recoveryPendingIntent(context: Context, flags: Int) = PendingIntent.getBroadcast(
        context, RECOVERY_REQUEST_ID,
        Intent(context, PrayerNotificationReceiver::class.java).setAction(PrayerNotificationReceiver.ACTION_RESTART),
        flags or PendingIntent.FLAG_IMMUTABLE
    )

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
