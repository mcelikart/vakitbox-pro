package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SunriseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            SunriseAlarmPreNotification.ACTION_PRE_NOTIFY -> SunriseAlarmPreNotification.receive(context, intent)
            SunriseAlarmScheduler.ACTION_FIRE -> {
                val delivery = SunriseAlarmScheduler.receive(context, intent) ?: return
                try {
                    ContextCompat.startForegroundService(context, SunriseAlarmRingingService.startIntent(context, delivery))
                } catch (_: IllegalStateException) {
                    SunriseAlarmRingingService.postStartFailureNotification(context)
                } catch (_: SecurityException) {
                    SunriseAlarmRingingService.postStartFailureNotification(context)
                }
                SunriseAlarmScheduler.refresh(context)
                armRefresh(context)
                SunriseAlarmDataRefresh.request(context)
            }
            ACTION_DISMISS -> SunriseAlarmScheduler.dismiss(context, intent.getStringExtra(EXTRA_TOKEN) ?: return)
            ACTION_SNOOZE -> {
                if (!SunriseAlarmScheduler.snooze(context, intent.getStringExtra(EXTRA_TOKEN) ?: return)) {
                    Toast.makeText(context, "Alarm ertelenemedi. Alarm izinlerini kontrol edin.", Toast.LENGTH_LONG).show()
                }
            }
            ACTION_REFRESH, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_UNLOCKED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                // A reboot never resumes a pre-reboot ringing session.
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    val state = SunriseAlarmRingingState(context)
                    state.list().forEach { state.remove(it.delivery.token) }
                    SunriseAlarmRingingService.clearNotificationIfIdle(context)
                }
                SunriseAlarmAudio.recoverVolumeIfIdle(context)
                SunriseAlarmScheduler.refresh(context)
                armRefresh(context)
                val result = goAsync()
                scope.launch {
                    try { SunriseAlarmDataRefresh.refreshIfNeeded(context.applicationContext) }
                    finally { result.finish() }
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.alarm.REFRESH_SUNRISE_ALARMS"
        const val ACTION_DISMISS = "com.example.alarm.DISMISS_SUNRISE_ALARM"
        const val ACTION_SNOOZE = "com.example.alarm.SNOOZE_SUNRISE_ALARM"
        const val EXTRA_TOKEN = "sunrise_alarm_token"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun armRefresh(context: Context, now: Long = System.currentTimeMillis()) {
            val manager = context.getSystemService(AlarmManager::class.java) ?: return
            val pending = PendingIntent.getBroadcast(context, 73_002,
                Intent(context, SunriseAlarmReceiver::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (SunriseAlarmStore(context).list().none { it.enabled }) {
                manager.cancel(pending)
                return
            }
            val nextDay = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Maintenance has no exact-time requirement and does not create an alarm-clock icon.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextDay.timeInMillis, pending)
        }
    }
}
