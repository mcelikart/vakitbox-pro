package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.R
import java.util.Date

/** Started by a delivered user alarm, never by a periodic worker or boot broadcast. */
class SunriseAlarmRingingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var state: SunriseAlarmRingingState
    private lateinit var audio: SunriseAlarmAudio
    private lateinit var flipGesture: SunriseAlarmFlipGesture
    private var audioToken: String? = null
    private var foreground = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeDeadline = 0L
    private val elapsedDeadlines = mutableMapOf<String, Long>()
    private val expiry = Runnable { reconcile() }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        handler.post { reconcile() }
    }

    override fun onCreate() {
        super.onCreate()
        state = SunriseAlarmRingingState(this)
        audio = SunriseAlarmAudio(this)
        flipGesture = SunriseAlarmFlipGesture(this) {
            state.list().firstOrNull { it.expiresAtMillis > System.currentTimeMillis() }?.let {
                if (!SunriseAlarmScheduler.snooze(this, it.delivery.token)) {
                    Toast.makeText(this, "Alarm ertelenemedi. Alarm iznini kontrol edin.", Toast.LENGTH_LONG).show()
                }
            }
        }
        state.registerListener(listener)
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = intent?.getStringExtra(EXTRA_TOKEN)
        when (intent?.action) {
            ACTION_DISMISS -> if (token != null) SunriseAlarmScheduler.dismiss(this, token)
            ACTION_SNOOZE -> if (token != null && !SunriseAlarmScheduler.snooze(this, token)) {
                Toast.makeText(this, "Alarm ertelenemedi. Alarm iznini kontrol edin.", Toast.LENGTH_LONG).show()
            }
        }
        // ACTION_RING references the receiver's durable, validated queue. It cannot resurrect a
        // dismissed token, and a null sticky restart resumes only unexpired records.
        return if (reconcile()) START_STICKY else START_NOT_STICKY
    }

    private fun reconcile(): Boolean {
        handler.removeCallbacks(expiry)
        val now = System.currentTimeMillis()
        val elapsedNow = SystemClock.elapsedRealtime()
        state.list().filter { record ->
            val elapsedDeadline = elapsedDeadlines.getOrPut(record.delivery.token) {
                elapsedNow + (record.expiresAtMillis - now).coerceIn(0L, SunriseAlarmRingingState.MAX_RING_DURATION_MILLIS)
            }
            record.expiresAtMillis <= now || elapsedDeadline <= elapsedNow ||
                SunriseAlarmStore(this).find(record.delivery.alarmId)?.enabled != true
        }.forEach { SunriseAlarmScheduler.dismiss(this, it.delivery.token) }
        val records = state.list().filter { it.expiresAtMillis > now }
        elapsedDeadlines.keys.retainAll(records.map { it.delivery.token }.toSet())
        if (records.isEmpty()) {
            audio.stop()
            audioToken = null
            flipGesture.stop()
            releaseWakeLock()
            if (foreground) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                foreground = false
            }
            stopSelf()
            return false
        }
        val notification = buildNotification(this, records)
        if (!foreground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foreground = true
        } else {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
        }
        val first = records.first()
        if (audioToken != first.delivery.token) {
            audio.stop()
            flipGesture.stop()
            // Android 15 permits alarm audio focus only after foreground entry.
            audio.start(first.settings)
            audioToken = first.delivery.token
        }
        if (first.settings.flipToSnooze) flipGesture.start() else flipGesture.stop()
        val lastDeadline = records.maxOf { elapsedDeadlines.getValue(it.delivery.token) }
        keepCpuAwake(lastDeadline, elapsedNow)
        val delay = records.minOf { record ->
            minOf(record.expiresAtMillis - now, elapsedDeadlines.getValue(record.delivery.token) - elapsedNow)
        }
        handler.postDelayed(expiry, delay.coerceAtLeast(1L))
        return true
    }

    private fun keepCpuAwake(deadline: Long, now: Long) {
        if (wakeLock?.isHeld == true && wakeDeadline >= deadline) return
        releaseWakeLock()
        val manager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EzanVakti:SunriseAlarm").apply {
            setReferenceCounted(false)
            acquire((deadline - now + 5_000L).coerceIn(1_000L, SunriseAlarmRingingState.MAX_RING_DURATION_MILLIS + 5_000L))
        }
        wakeDeadline = deadline
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) runCatching { wakeLock?.release() }
        wakeLock = null
        wakeDeadline = 0L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        state.unregisterListener(listener)
        handler.removeCallbacksAndMessages(null)
        audio.stop()
        flipGesture.stop()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 83001
        const val CHANNEL_ID = "sunrise_alarm_ringing_v1"
        const val SNOOZE_MINUTES = 10
        private const val ALARM_GROUP = "com.example.sunrise.alarm.ringing"
        internal const val ACTION_RING = "com.example.alarm.RING"
        internal const val ACTION_DISMISS = "com.example.alarm.DISMISS"
        internal const val ACTION_SNOOZE = "com.example.alarm.SNOOZE"
        internal const val ACTION_SYNC = "com.example.alarm.SYNC"
        internal const val EXTRA_TOKEN = "delivery_token"

        fun startIntent(context: Context, delivery: SunriseAlarmDelivery): Intent =
            actionIntent(context, ACTION_RING, delivery.token)

        fun stopIntent(context: Context, token: String): Intent = actionIntent(context, ACTION_DISMISS, token)

        fun syncIntent(context: Context): Intent = Intent(context, SunriseAlarmRingingService::class.java).setAction(ACTION_SYNC)

        /** Provides visible controls and a visible-activity retry if an OEM blocks the receiver's start. */
        fun postStartFailureNotification(context: Context) {
            val now = System.currentTimeMillis()
            val records = SunriseAlarmRingingState(context).list().filter {
                it.expiresAtMillis > now && SunriseAlarmStore(context).find(it.delivery.alarmId)?.enabled == true
            }
            if (records.isEmpty()) {
                clearNotificationIfIdle(context)
                return
            }
            createChannel(context)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, buildNotification(context, records))
        }

        fun clearNotificationIfIdle(context: Context) {
            if (SunriseAlarmRingingState(context).list().none { it.expiresAtMillis > System.currentTimeMillis() }) {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
            }
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(CHANNEL_ID, "Çalan sabah namazı alarmları", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sabah namazı alarmını kapatma ve erteleme"
                setSound(null, null) // The player uses the alarm stream, avoiding a second notification sound.
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        private fun buildNotification(context: Context, records: List<SunriseAlarmRingRecord>): Notification {
            val current = records.first()
            val open = PendingIntent.getActivity(
                context, 83002, SunriseAlarmRingingActivity.openIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val sunrise = android.text.format.DateFormat.getTimeFormat(context).format(Date(current.delivery.sunriseAtMillis))
            val text = if (records.size > 1) "Güneş $sunrise · ${records.size} alarm çalıyor" else "Güneş $sunrise"
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(current.label)
                .setContentText(text)
                .setWhen(current.delivery.triggerAtMillis)
                .setShowWhen(true)
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setDefaults(0)
                .setTimeoutAfter((records.maxOf { it.expiresAtMillis } - System.currentTimeMillis()).coerceAtLeast(1L))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setGroup(ALARM_GROUP)
                .setGroupSummary(false)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(0, "${current.settings.snoozeMinutes} dk ertele", notificationAction(context, SunriseAlarmReceiver.ACTION_SNOOZE, current.delivery.token))
                .addAction(0, "Kapat", notificationAction(context, SunriseAlarmReceiver.ACTION_DISMISS, current.delivery.token))
                .build().apply { flags = flags or Notification.FLAG_NO_CLEAR }
        }

        private fun notificationAction(context: Context, action: String, token: String): PendingIntent =
            PendingIntent.getBroadcast(context, 0, Intent(context, SunriseAlarmReceiver::class.java).apply {
                this.action = action
                data = Uri.Builder().scheme("ezan-alarm").authority("ringing-action").appendPath(action).appendPath(token).build()
                putExtra(SunriseAlarmReceiver.EXTRA_TOKEN, token)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        internal fun actionIntent(context: Context, action: String, token: String): Intent =
            Intent(context, SunriseAlarmRingingService::class.java).apply {
                this.action = action
                data = Uri.Builder().scheme("ezan-alarm").authority("ringing").appendPath(action).appendPath(token).build()
                putExtra(EXTRA_TOKEN, token)
            }
    }
}
