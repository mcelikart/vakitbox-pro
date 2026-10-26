package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.R
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class CountdownService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentJob: Job? = null
    private var refreshJob: Job? = null
    private var foregroundReady = false

    override fun onCreate() {
        super.onCreate()
        // Promote before cache/database/theme work: Android gives foreground
        // service starts only a short window to publish their notification.
        try {
            createNotificationChannel()
            val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val initial = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Vakitler güncelleniyor")
                .setContentIntent(open)
                .setDeleteIntent(countdownDeleteIntent())
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setGroup(COUNTDOWN_GROUP)
                .setGroupSummary(false)
                .setSortKey("00_countdown")
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build().apply { flags = flags or android.app.Notification.FLAG_NO_CLEAR }
            initial.publicVersion = initial.clone()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1001, initial)
            }
            foregroundReady = true
            BackgroundServices.onCountdownStarted(this)
        } catch (error: Exception) {
            android.util.Log.w("CountdownService", "Foreground start unavailable", error)
            BackgroundServices.armWatchdog(this)
            stopSelf()
        }
    }

    data class NextPrayerData(val name: String, val timeMillis: Long, val currentIndex: Int)

    private fun recalculateNextPrayer(allPrayers: String): NextPrayerData? {
        val now = System.currentTimeMillis()
        val displayNames = allPrayers.split("|").map { it.substringBefore(",") }
        for (offset in 0..1) {
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val prayers = com.example.data.PrayerTimeCache.read(this, day).split("|")
            var previousName = "Yatsı"
            for (entry in prayers) {
                val parts = entry.split(",")
                if (parts.size != 2 || parts[0] !in PrayerAlarmScheduler.PRAYER_NAMES) continue
                val time = parts[1].split(":")
                val hour = time.getOrNull(0)?.toIntOrNull() ?: continue
                val minute = time.getOrNull(1)?.toIntOrNull() ?: continue
                if (hour !in 0..23 || minute !in 0..59) continue
                val target = (day.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (target.timeInMillis > now) {
                    return NextPrayerData(parts[0], target.timeInMillis, displayNames.indexOf(previousName))
                }
                previousName = parts[0]
            }
        }
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!foregroundReady) return START_NOT_STICKY
        BackgroundServices.armWatchdog(this)
        // A normal step service is permitted once this process is foreground.
        // Also restore it when Android recreates the sticky service without an Activity.
        try {
            com.example.steps.StepCounterService.startIfEnabled(this)
        } catch (error: Exception) {
            android.util.Log.w("CountdownService", "Step tracking restore deferred", error)
        }
        runCatching { PrayerAlertExpiry.clearStaleAlerts(this) }
        val prefs = getSharedPreferences("countdown_service_prefs", Context.MODE_PRIVATE)

        var locationName = intent?.getStringExtra("LOCATION_NAME")
        var nextPrayerName = intent?.getStringExtra("NEXT_PRAYER_NAME")
        var nextPrayerTimeMillis = intent?.getLongExtra("NEXT_PRAYER_TIME_MILLIS", -1L) ?: -1L
        var allPrayersString = intent?.getStringExtra("ALL_PRAYERS")
        var currentIndex = intent?.getIntExtra("CURRENT_PRAYER_INDEX", -1) ?: -1

        // If intent has valid extras, save them to cache
        if (locationName != null || nextPrayerName != null || nextPrayerTimeMillis != -1L || allPrayersString != null) {
            val editor = prefs.edit()
            if (locationName != null) editor.putString("LOCATION_NAME", locationName)
            if (nextPrayerName != null) editor.putString("NEXT_PRAYER_NAME", nextPrayerName)
            if (nextPrayerTimeMillis != -1L) editor.putLong("NEXT_PRAYER_TIME_MILLIS", nextPrayerTimeMillis)
            if (allPrayersString != null) editor.putString("ALL_PRAYERS", allPrayersString)
            intent?.getStringExtra("ALL_PRAYERS_DATE")?.let { editor.putString("ALL_PRAYERS_DATE", it) }
            if (currentIndex != -1) editor.putInt("CURRENT_PRAYER_INDEX", currentIndex)
            editor.apply()
        } else {
            // Restore from cache if intent is null or empty (e.g. background service restart)
            locationName = prefs.getString("LOCATION_NAME", "Konum Bilinmiyor")
            nextPrayerName = prefs.getString("NEXT_PRAYER_NAME", "Namaz")
            nextPrayerTimeMillis = prefs.getLong("NEXT_PRAYER_TIME_MILLIS", System.currentTimeMillis())
            allPrayersString = prefs.getString("ALL_PRAYERS", "")
            currentIndex = prefs.getInt("CURRENT_PRAYER_INDEX", -1)
        }

        // Prefer the cached schedule for the actual calendar day after a restart.
        var finalAllPrayersString = com.example.data.PrayerTimeCache.read(this)
        if (finalAllPrayersString.isNotEmpty()) {
            val nextData = recalculateNextPrayer(finalAllPrayersString)
            if (nextData != null) {
                nextPrayerName = nextData.name
                nextPrayerTimeMillis = nextData.timeMillis
                currentIndex = nextData.currentIndex
                
                prefs.edit().apply {
                    putString("NEXT_PRAYER_NAME", nextPrayerName)
                    putLong("NEXT_PRAYER_TIME_MILLIS", nextPrayerTimeMillis)
                    putInt("CURRENT_PRAYER_INDEX", currentIndex)
                    apply()
                }
            }
        }

        val finalLocationName = locationName ?: "Konum Bilinmiyor"
        val finalNextPrayerName = nextPrayerName ?: "Namaz"
        performBackgroundLocationCheck()

        // Schedule exact alarms in System AlarmManager
        try {
            PrayerAlarmScheduler.scheduleAllPrayerAlarms(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        createNotificationChannel()
        
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        currentJob?.cancel()

        currentJob = serviceScope.launch {
            var activeNextName = finalNextPrayerName
            var activeNextTimeMillis = nextPrayerTimeMillis
            var activeCurrentIndex = currentIndex
            var lastBgLocCheckTime = System.currentTimeMillis()

            runResilientCountdownUpdates(onFailure = { error ->
                android.util.Log.w("CountdownService", "Countdown update will retry", error)
            }) {
                val now = System.currentTimeMillis()
                var diff = activeNextTimeMillis - now
                val updatedPrayers = com.example.data.PrayerTimeCache.read(this@CountdownService)
                if (updatedPrayers != finalAllPrayersString) {
                    finalAllPrayersString = updatedPrayers
                    recalculateNextPrayer(updatedPrayers)?.let {
                        activeNextName = it.name
                        activeNextTimeMillis = it.timeMillis
                        activeCurrentIndex = it.currentIndex
                        diff = activeNextTimeMillis - now
                    }
                    PrayerAlarmScheduler.scheduleAllPrayerAlarms(applicationContext)
                }
                
                // 15-minute background periodic location check
                if (now - lastBgLocCheckTime >= 15 * 60 * 1000L) {
                    lastBgLocCheckTime = now
                    performBackgroundLocationCheck()
                }
                
                // If the target prayer time has been reached, automatically transition to the next prayer!
                if (diff <= 0 && finalAllPrayersString.isNotEmpty()) {
                    val nextData = recalculateNextPrayer(finalAllPrayersString)
                    if (nextData != null) {
                        activeNextName = nextData.name
                        activeNextTimeMillis = nextData.timeMillis
                        activeCurrentIndex = nextData.currentIndex
                        
                        // Persist the new active state to cache so if the service restarts it starts from the correct prayer
                        prefs.edit().apply {
                            putString("NEXT_PRAYER_NAME", activeNextName)
                            putLong("NEXT_PRAYER_TIME_MILLIS", activeNextTimeMillis)
                            putInt("CURRENT_PRAYER_INDEX", activeCurrentIndex)
                            apply()
                        }

                        try {
                            PrayerAlarmScheduler.scheduleAllPrayerAlarms(applicationContext)
                        } catch (_: Exception) {}
                        
                        diff = activeNextTimeMillis - now
                    }
                }
                
                if (diff < 0) diff = 0
                
                val theme = selectedTheme()
                val themeColors = theme.spec.colors(theme.spec.prefersDark)
                val accentColor = themeColors.primary.toArgb()
                val primaryColor = themeColors.onSurface.toArgb()
                val secondaryColor = themeColors.onSurfaceVariant.toArgb()

                val collapsedViews = android.widget.RemoteViews(packageName, R.layout.notification_collapsed)
                val currentLocation = prefs.getString("LOCATION_NAME", finalLocationName) ?: finalLocationName
                collapsedViews.setTextViewText(R.id.tv_title, "$activeNextName vaktine kalan")
                collapsedViews.setTextViewText(R.id.tv_location, currentLocation)
                collapsedViews.setTextColor(R.id.tv_title, primaryColor)
                collapsedViews.setTextColor(R.id.tv_location, secondaryColor)
                collapsedViews.setTextColor(R.id.tv_text, accentColor)
                collapsedViews.setInt(R.id.notif_root_collapsed, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
                configureChronometer(collapsedViews, R.id.tv_text, diff)
                applyStepRow(collapsedViews, accentColor)

                val expandedViews = android.widget.RemoteViews(packageName, R.layout.notification_expanded)
                expandedViews.setTextViewText(R.id.tv_title, "$activeNextName vaktine kalan")
                expandedViews.setTextViewText(R.id.tv_location, currentLocation)
                expandedViews.setTextColor(R.id.tv_title, primaryColor)
                expandedViews.setTextColor(R.id.tv_location, secondaryColor)
                expandedViews.setTextColor(R.id.tv_countdown, accentColor)
                expandedViews.setInt(R.id.notif_root_expanded, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
                configureChronometer(expandedViews, R.id.tv_countdown, diff)
                applyStepRow(expandedViews, accentColor)
                
                if (finalAllPrayersString.isNotEmpty()) {
                    val prayersList = finalAllPrayersString.split("|")
                    val nameIds = intArrayOf(
                        R.id.tv_name_0, R.id.tv_name_1, R.id.tv_name_2, R.id.tv_name_3,
                        R.id.tv_name_4, R.id.tv_name_5, R.id.tv_name_6
                    )
                    val timeIds = intArrayOf(
                        R.id.tv_time_0, R.id.tv_time_1, R.id.tv_time_2, R.id.tv_time_3,
                        R.id.tv_time_4, R.id.tv_time_5, R.id.tv_time_6
                    )
                    for (i in 0 until minOf(prayersList.size, 7)) {
                        val parts = prayersList[i].split(",")
                        if (parts.size == 2) {
                            val name = parts[0]
                            val time = parts[1]
                            
                            val nameId = nameIds[i]
                            val timeId = timeIds[i]
                            
                            if (i == activeCurrentIndex) {
                                val spannableName = android.text.SpannableString(name)
                                spannableName.setSpan(android.text.style.ForegroundColorSpan(accentColor), 0, name.length, 0)
                                spannableName.setSpan(android.text.style.RelativeSizeSpan(1.2f), 0, name.length, 0)
                                spannableName.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, name.length, 0)
                                
                                val spannableTime = android.text.SpannableString(time)
                                spannableTime.setSpan(android.text.style.ForegroundColorSpan(accentColor), 0, time.length, 0)
                                spannableTime.setSpan(android.text.style.RelativeSizeSpan(1.2f), 0, time.length, 0)
                                spannableTime.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, time.length, 0)

                                expandedViews.setTextViewText(nameId, spannableName)
                                expandedViews.setTextViewText(timeId, spannableTime)
                            } else {
                                expandedViews.setTextColor(nameId, secondaryColor)
                                expandedViews.setTextColor(timeId, primaryColor)
                                expandedViews.setTextViewText(nameId, name)
                                expandedViews.setTextViewText(timeId, time)
                            }
                        }
                    }
                }

                val deletePendingIntent = countdownDeleteIntent()

                val notification = NotificationCompat.Builder(this@CountdownService, CHANNEL_ID)
                    .setSmallIcon(CountdownStatusIcon.create(this@CountdownService, diff))
                    .setContentTitle("$activeNextName vaktine kalan")
                    .setContentText(formatRemaining(diff))
                    .setSubText(currentLocation)
                    .setCustomContentView(collapsedViews)
                    .setCustomBigContentView(expandedViews)
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setShowWhen(true)
                    .setWhen(activeNextTimeMillis)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setLocalOnly(true)
                    .setDeleteIntent(deletePendingIntent)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setColor(accentColor)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .setGroup(COUNTDOWN_GROUP)
                    .setGroupSummary(false)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setSortKey("00_countdown")
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Shows on lockscreen and Always on Display
                    .setPublicVersion(publicCountdownNotification(
                        title = "$activeNextName vaktine kalan",
                        targetTimeMillis = if (diff > 0) now + diff else null,
                        contentIntent = pendingIntent,
                        accentColor = accentColor
                    ))
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .build()

                notification.flags = notification.flags or android.app.Notification.FLAG_NO_CLEAR or android.app.Notification.FLAG_ONGOING_EVENT

                // Serialize publication with lifecycle cancellation on the main thread.
                withContext(Dispatchers.Main.immediate) {
                    ensureActive()
                    getSystemService(NotificationManager::class.java)?.notify(1001, notification)
                    BackgroundServices.onCountdownHeartbeat(this@CountdownService)
                }

                // SystemUI renders Chronometer every second without waking this service every second.
                val untilPrayer = if (diff > 0L) diff + 50L else 30_000L
                minOf(30_000L, untilPrayer, CountdownStatusIcon.millisUntilNextMinuteChange(diff))
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleServiceRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        BackgroundServices.onCountdownStopped()
        scheduleServiceRestart()
    }

    private fun scheduleServiceRestart() {
        BackgroundServices.scheduleRecovery(applicationContext)
    }

    private fun countdownDeleteIntent(): PendingIntent = PendingIntent.getBroadcast(
        this, 0, Intent(this, PrayerNotificationReceiver::class.java)
            .setAction(PrayerNotificationReceiver.ACTION_COUNTDOWN_DISMISSED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun publicCountdownNotification(
        title: String,
        targetTimeMillis: Long?,
        contentIntent: PendingIntent?,
        accentColor: Int
    ): android.app.Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(CountdownStatusIcon.create(this,
            (targetTimeMillis ?: System.currentTimeMillis()) - System.currentTimeMillis()))
        .setContentTitle(title)
        .setContentText(if (targetTimeMillis == null) "Vakitler güncelleniyor" else formatRemaining(targetTimeMillis - System.currentTimeMillis()))
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(targetTimeMillis != null)
        .setWhen(targetTimeMillis ?: System.currentTimeMillis())
        .setUsesChronometer(targetTimeMillis != null)
        .setChronometerCountDown(targetTimeMillis != null)
        .setGroup(COUNTDOWN_GROUP)
        .setGroupSummary(false)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setSortKey("00_countdown")
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setColor(accentColor)
        .build()

    private fun configureChronometer(views: android.widget.RemoteViews, viewId: Int, diff: Long) {
        if (diff > 0) {
            views.setChronometer(viewId, SystemClock.elapsedRealtime() + diff, "%s", true)
            views.setChronometerCountDown(viewId, true)
        } else {
            views.setChronometer(viewId, SystemClock.elapsedRealtime(), null, false)
            views.setTextViewText(viewId, "00:00")
        }
    }

    /**
     * Folds the pedometer total into the countdown notification. The step
     * counter no longer posts a notification of its own, so this row is the
     * single surface where the daily total is visible: walking glyph first,
     * then a medium-large count that outweighs the two labels above it.
     */
    private fun applyStepRow(views: android.widget.RemoteViews, accentColor: Int) {
        val steps = try {
            com.example.steps.StepStore.get(this).day(com.example.steps.StepStore.dayKey())
        } catch (_: Throwable) {
            null
        }
        val enabled = com.example.steps.StepCounterService.enabled(this)
        if (!enabled || steps == null) {
            views.setViewVisibility(R.id.ll_steps, android.view.View.GONE)
            return
        }
        val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale("tr", "TR")).format(steps)
        views.setViewVisibility(R.id.ll_steps, android.view.View.VISIBLE)
        views.setTextViewText(R.id.tv_steps, formatted)
        views.setTextColor(R.id.tv_steps, accentColor)
        // The stock ic_steps drawable is a solid white fill, so tinting it to the
        // theme accent keeps the glyph legible on any surface. RemoteViews has no
        // setColorStateList overload that resolves against an ImageView target in
        // every supported API level, so the icon is swapped for a pre-tinted
        // bitmap instead.
        runCatching {
            views.setImageViewBitmap(R.id.iv_steps, tintedStepIcon(accentColor))
        }
    }

    private var tintedIconCache: Pair<Int, android.graphics.Bitmap>? = null

    private fun tintedStepIcon(tint: Int): android.graphics.Bitmap {
        tintedIconCache?.let { (color, bitmap) -> if (color == tint) return bitmap }
        val size = (17 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            style = android.graphics.Paint.Style.FILL
        }
        val path = android.graphics.Path().apply {
            // A single footprint: a sole with a slightly wider ball, drawn in a
            // unit square so the geometry is independent of the pixel size.
            // Simpler than a walking figure and it reads at notification scale.
            moveTo(0.50f, 0.04f)
            cubicTo(0.66f, 0.04f, 0.74f, 0.20f, 0.74f, 0.36f)
            cubicTo(0.74f, 0.48f, 0.70f, 0.56f, 0.68f, 0.66f)
            cubicTo(0.66f, 0.80f, 0.60f, 0.96f, 0.50f, 0.96f)
            cubicTo(0.40f, 0.96f, 0.34f, 0.80f, 0.32f, 0.66f)
            cubicTo(0.30f, 0.56f, 0.26f, 0.48f, 0.26f, 0.36f)
            cubicTo(0.26f, 0.20f, 0.34f, 0.04f, 0.50f, 0.04f)
            close()
        }
        // The path is authored in a unit square; without this the glyph would be
        // drawn at 1/68 of the bitmap and look like a stray dot next to the count.
        canvas.save()
        canvas.scale(size.toFloat(), size.toFloat())
        canvas.drawPath(path, paint)
        canvas.restore()
        tintedIconCache = tint to bitmap
        return bitmap
    }

    private fun selectedTheme(): AppTheme {
        val stored = getSharedPreferences("widget_prayer_prefs", Context.MODE_PRIVATE)
            .getString("theme", AppTheme.DEFAULT.name)
        return AppTheme.fromStored(stored)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            try {
                manager?.deleteNotificationChannel("prayer_countdown_channel_v6")
                manager?.deleteNotificationChannel("prayer_countdown_system_channel_v1")
                manager?.deleteNotificationChannel("prayer_countdown_priority_v2")
                manager?.deleteNotificationChannel("prayer_countdown_live_v3")
            } catch (_: Exception) {}

            val channel = NotificationChannel(
                CHANNEL_ID,
                "${getString(R.string.app_name)} Canlı Geri Sayım",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sonraki vakte kalan süreyi üst bildirim bölümünde sürekli gösterir"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC // Show on lockscreen and Always On Display
            }
            manager?.createNotificationChannel(channel)
        }
    }

    private fun performBackgroundLocationCheck() {
        if (refreshJob?.isActive == true) return
        refreshJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val repository = com.example.data.AppRepository(applicationContext)
                val savedLat = repository.latitudeFlow.firstOrNull() ?: return@launch
                val savedLng = repository.longitudeFlow.firstOrNull() ?: return@launch
                val savedCity = repository.cityFlow.firstOrNull() ?: ""
                val loc = repository.getCurrentLocation()
                val lat = loc?.latitude ?: savedLat
                val lng = loc?.longitude ?: savedLng
                val city = loc?.city?.takeIf { it.isNotBlank() } ?: savedCity
                val moved = kotlin.math.abs(lat - savedLat) > 0.003 || kotlin.math.abs(lng - savedLng) > 0.003
                if (moved) {
                    getSharedPreferences("prayer_daily_times", MODE_PRIVATE).edit().clear().apply()
                    repository.saveLocation(lat, lng, city)
                }
                val today = com.example.data.PrayerTimeCache.read(applicationContext)
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                if (moved || today.isEmpty()) repository.getTodayTimings(lat, lng)
                if (com.example.data.PrayerTimeCache.read(applicationContext, tomorrow).isEmpty()) {
                    repository.getMonthlyCalendar(lat, lng, tomorrow.get(Calendar.MONTH) + 1, tomorrow.get(Calendar.YEAR))
                }
                getSharedPreferences("countdown_service_prefs", MODE_PRIVATE).edit()
                    .putString("LOCATION_NAME", city).apply()
                PrayerAlarmScheduler.scheduleAllPrayerAlarms(applicationContext)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.w("CountdownService", "Prayer times refresh unavailable", error)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // A group summary is replaced by alert snippets on Android 7+.
        // Keep the running clock in a dedicated, non-summary group.
        internal const val COUNTDOWN_GROUP = "prayer_countdown_standalone"
        internal const val CHANNEL_ID = "prayer_countdown_live_v4_silent"

        internal fun formatRemaining(diffMillis: Long): String {
            val totalSeconds = (diffMillis.coerceAtLeast(0L) + 999L) / 1000L
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

}
