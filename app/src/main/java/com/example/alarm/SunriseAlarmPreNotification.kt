package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Date

/** A separate, silent notice. It never starts playback or changes the permanent countdown. */
internal object SunriseAlarmPreNotification {
    const val ACTION_PRE_NOTIFY = "com.example.alarm.PRE_NOTIFY_SUNRISE"
    const val NOTIFICATION_ID = 83003
    const val CHANNEL_ID = "sunrise_alarm_upcoming_v1"
    private const val REQUEST = 73003

    fun sync(context: Context, deliveries: List<SunriseAlarmDelivery>, now: Long) {
        val lead = SunriseAlarmSettingsStore(context).read().preNotificationMinutes * 60_000L
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        deliveries.forEach { delivery ->
            // Cancel before rearming so changing lead time cannot leave the old broadcast queued.
            pending(context, delivery, PendingIntent.FLAG_NO_CREATE)?.let(manager::cancel)
            if (lead == 0L || delivery.fromSnooze || delivery.triggerAtMillis <= now) {
                notifications(context).cancel(delivery.token, NOTIFICATION_ID)
            } else {
                val at = delivery.triggerAtMillis - lead
                if (at > now) {
                    notifications(context).cancel(delivery.token, NOTIFICATION_ID)
                    runCatching {
                        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at,
                            requireNotNull(pending(context, delivery, PendingIntent.FLAG_UPDATE_CURRENT)))
                    }
                }
            }
        }
    }

    fun cancel(context: Context, delivery: SunriseAlarmDelivery) {
        val manager = context.getSystemService(AlarmManager::class.java)
        pending(context, delivery, PendingIntent.FLAG_NO_CREATE)?.let { manager?.cancel(it) }
        notifications(context).cancel(delivery.token, NOTIFICATION_ID)
    }

    fun receive(context: Context, intent: Intent, now: Long = System.currentTimeMillis()) {
        if (intent.action != ACTION_PRE_NOTIFY) return
        val delivery = SunriseAlarmScheduler.decodeDelivery(intent) ?: return
        val lead = SunriseAlarmSettingsStore(context).read().preNotificationMinutes * 60_000L
        if (lead == 0L || delivery.fromSnooze || now < delivery.triggerAtMillis - lead || now >= delivery.triggerAtMillis ||
            !SunriseAlarmScheduler.canShowPreNotification(context, delivery)) return
        val alarm = SunriseAlarmStore(context).find(delivery.alarmId) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifications(context).createNotificationChannel(NotificationChannel(CHANNEL_ID,
                "Yaklaşan sabah namazı alarmları", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
            })
        }
        val clock = android.text.format.DateFormat.getTimeFormat(context)
        val open = PendingIntent.getActivity(context, 83004,
            Intent(context, MainActivity::class.java).setAction(SunriseAlarmScheduler.ACTION_OPEN_ALARMS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notifications(context).notify(delivery.token, NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(alarm.label)
                .setContentText("Alarm ${clock.format(Date(delivery.triggerAtMillis))} · Güneş ${clock.format(Date(delivery.sunriseAtMillis))}")
                .setContentIntent(open)
                .setWhen(delivery.triggerAtMillis)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setTimeoutAfter(delivery.triggerAtMillis - now)
                .build())
    }

    internal fun intent(context: Context, delivery: SunriseAlarmDelivery): Intent =
        SunriseAlarmScheduler.fireIntent(context, delivery).setAction(ACTION_PRE_NOTIFY)

    private fun pending(context: Context, delivery: SunriseAlarmDelivery, flag: Int) =
        PendingIntent.getBroadcast(context, REQUEST, intent(context, delivery), flag or PendingIntent.FLAG_IMMUTABLE)

    private fun notifications(context: Context) = context.getSystemService(NotificationManager::class.java)
}
