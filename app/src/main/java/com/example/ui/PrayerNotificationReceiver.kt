package com.example.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PrayerAlarmScheduler.ACTION_EXACT_PRAYER_ALARM,
            PrayerAlarmScheduler.ACTION_WARNING_PRAYER_ALARM -> deliver(context, intent)
            PrayerAlertExpiry.ACTION -> PrayerAlertExpiry.receive(context, intent)
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_UNLOCKED, Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                if (!BackgroundServices.automaticRecoveryAllowed(context)) return
                runCatching { PrayerAlarmScheduler.scheduleAllPrayerAlarms(context) }
                    .onFailure { android.util.Log.w("PrayerNotification", "Schedule refresh deferred", it) }
                BackgroundServices.startCountdown(context)
            }
            ACTION_RESTART, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                BackgroundServices.performMaintenance(context)
            }
            ACTION_COUNTDOWN_DISMISSED -> {
                BackgroundServices.startCountdown(context, userInitiated = true)
            }
            // Old installations may still have a pending auto-dismiss alarm. Ignore it.
        }
    }

    private fun deliver(context: Context, intent: Intent) {
        val name = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: return
        val time = intent.getLongExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME_MILLIS, 0L)
        val warning = intent.action == PrayerAlarmScheduler.ACTION_WARNING_PRAYER_ALARM
        if (name !in PrayerAlarmScheduler.PRAYER_NAMES ||
            !PrayerAlertIdentity.isTimely(time, warning, System.currentTimeMillis()) ||
            !PrayerAlarmScheduler.isCurrentSchedule(context, name, time)) return
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val prefix = if (warning) "key_10min" else "key_exact"
        if (!prefs.getBoolean("${prefix}_enabled_$name", true)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val vibration = prefs.getBoolean("key_vibration_enabled_$name", true)
        val patternId = prefs.getInt("key_vibration_pattern_$name", 0)
        val pattern = VibrationPatternManager.getPatternById(patternId)
        val channelId = "prayer_alert_v3_${PrayerAlarmScheduler.PRAYER_NAMES.indexOf(name)}_${warning}_${vibration}_$patternId"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId,
                if (warning) "$name 10 Dakika Önce" else "$name Vakti",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Kalıcı sayacın altında gösterilen geçici vakit uyarısı"
                setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                enableVibration(vibration)
                if (vibration) vibrationPattern = pattern
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            })
            if (manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE) return
        }

        // Persist one delivery per prayer/date/type, including across process restarts.
        val dedupe = context.getSharedPreferences("prayer_deliveries_v2", Context.MODE_PRIVATE)
        val key = PrayerAlertIdentity.key(name, time, warning)
        synchronized(PrayerNotificationReceiver::class.java) {
            if (dedupe.contains(key)) return
            val edit = dedupe.edit()
            dedupe.all.filterValues { it is Long && it < System.currentTimeMillis() - 7 * 86_400_000L }
                .keys.forEach { edit.remove(it) }
            if (!edit.putLong(key, time).commit()) return
        }

        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val expiresAt = time + PrayerAlertExpiry.AFTER_PRAYER_MILLIS
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (warning) "$name Vaktine 10 Dakika Kaldı" else "$name Vakti Girdi")
            .setContentText(if (warning) "Namaz vakti yaklaşıyor." else "Hayırlı ibadetler.")
            .setContentIntent(open)
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setGroup(PRAYER_GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setSortKey(if (warning) "11_warning" else "10_prayer")
            .setWhen(time)
            .setTimeoutAfter((expiresAt - System.currentTimeMillis()).coerceAtLeast(1L))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(if (vibration) pattern else longArrayOf(0L))
            .build()
        PrayerAlertExpiry.attach(notification, expiresAt)
        if (!warning) PrayerAlertExpiry.cancel(context, NOTIFICATION_ID_WARNING)
        val notificationId = if (warning) NOTIFICATION_ID_WARNING else NOTIFICATION_ID_EXACT
        manager.notify(notificationId, notification)
        PrayerAlertExpiry.schedule(context, notificationId, expiresAt)
        // Sound has a single owner; vibration is handled only by the notification channel.
        NotificationSoundManager.playSound(context.applicationContext,
            prefs.getInt("${prefix}_sound_$name", if (warning) 5 else 2))
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
        // Leave an existing countdown untouched. Recover it only if the system
        // removed the service; the recovery notification is always silent.
        if (manager.activeNotifications.none { it.id == 1001 }) {
            BackgroundServices.startCountdown(context)
        }
    }

    companion object {
        const val NOTIFICATION_ID_EXACT = 2001
        const val NOTIFICATION_ID_WARNING = 3001
        const val PRAYER_GROUP = "prayer_dismissible_alerts"
        const val ACTION_RESTART = "com.example.ACTION_RESTART_SERVICE"
        const val ACTION_COUNTDOWN_DISMISSED = "com.example.ACTION_NOTIFICATION_DISMISSED"
    }
}
