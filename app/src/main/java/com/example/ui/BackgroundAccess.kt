package com.example.ui

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

internal data class BackgroundAccessState(
    val batteryExempt: Boolean,
    val backgroundRestricted: Boolean,
    val exactAlarms: Boolean,
    val notifications: Boolean,
    val countdownChannel: Boolean
) {
    val batteryStatus: String get() = when {
        backgroundRestricted -> "Arka plan kullanımı kısıtlı"
        batteryExempt -> "Pil optimizasyonundan muaf"
        else -> "Pil optimizasyonu etkin"
    }
    val countdownStatus: String get() = when {
        !notifications -> "Önce uygulama bildirimlerini açın"
        !countdownChannel -> "Geri sayım kanalı kapalı"
        else -> "Açık"
    }
}

internal object BackgroundAccess {
    fun read(context: Context): BackgroundAccessState = BackgroundAccessState(
        batteryExempt = isIgnoringBatteryOptimizations(context),
        backgroundRestricted = Build.VERSION.SDK_INT >= 28 && runCatching {
            context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
        }.getOrDefault(false),
        exactAlarms = Build.VERSION.SDK_INT < 31 || runCatching {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        }.getOrDefault(false),
        notifications = isNotificationPermissionGranted(context),
        countdownChannel = countdownChannelEnabled(context)
    )

    private fun countdownChannelEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 26) return true
        return runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return@runCatching false
            val channel = manager.getNotificationChannel(CountdownService.CHANNEL_ID) ?: return@runCatching true
            channel.importance != NotificationManager.IMPORTANCE_NONE &&
                (Build.VERSION.SDK_INT < 28 || channel.group == null ||
                    manager.getNotificationChannelGroup(channel.group)?.isBlocked != true)
        }.getOrDefault(false)
    }

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun exactAlarmIntent(context: Context): Intent = if (Build.VERSION.SDK_INT >= 31) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    } else appDetailsIntent(context)

    fun notificationsIntent(context: Context): Intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else appDetailsIntent(context)

    fun countdownIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT < 26 || !isNotificationPermissionGranted(context)) return notificationsIntent(context)
        val channelExists = context.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(CountdownService.CHANNEL_ID) != null
        return if (channelExists) Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CountdownService.CHANNEL_ID)
        else notificationsIntent(context)
    }

    /** OEM settings are not a public API. A successful launch does not imply permission. */
    fun autoStartIntents(context: Context, manufacturer: String = Build.MANUFACTURER): List<Intent> {
        val maker = manufacturer.lowercase(Locale.ROOT)
        val components = when {
            maker.contains("xiaomi") || maker.contains("redmi") || maker.contains("poco") -> listOf(
                "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
                "com.miui.securitycenter" to "com.miui.powercenter.PowerSettings"
            )
            maker.contains("samsung") -> listOf(
                "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
                "com.samsung.android.sm" to "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
            maker.contains("huawei") || maker.contains("honor") -> listOf(
                "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
                "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
            maker.contains("oppo") || maker.contains("realme") || maker.contains("oneplus") -> listOf(
                "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                "com.oplus.safecenter" to "com.oplus.safecenter.startupapp.StartupAppListActivity",
                "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity"
            )
            maker.contains("vivo") || maker.contains("iqoo") -> listOf(
                "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
            else -> emptyList()
        }
        return components.map { (pkg, activity) -> Intent().setClassName(pkg, activity) } + appDetailsIntent(context)
    }

    fun open(context: Context, vararg intents: Intent): Boolean {
        // Android package visibility can hide OEM panels from resolveActivity.
        for (intent in intents.toList() + appDetailsIntent(context) + Intent(Settings.ACTION_SETTINGS)) {
            try {
                context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: android.content.ActivityNotFoundException) {
                // The device may omit this panel.
            } catch (_: SecurityException) {
                // Some manufacturer panels are not exported to other apps.
            }
        }
        return false
    }
}

@Composable
internal fun rememberBackgroundAccess(): BackgroundAccessState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var access by remember(context) { mutableStateOf(BackgroundAccess.read(context)) }
    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) access = BackgroundAccess.read(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return access
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean = runCatching {
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
}.getOrDefault(false)

fun requestIgnoreBatteryOptimizations(context: Context) {
    val access = BackgroundAccess.read(context)
    when {
        access.backgroundRestricted -> BackgroundAccess.open(context, BackgroundAccess.appDetailsIntent(context))
        access.batteryExempt -> BackgroundAccess.open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        else -> BackgroundAccess.open(context,
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

fun openAutoStartSettings(context: Context) {
    BackgroundAccess.open(context, *BackgroundAccess.autoStartIntents(context).toTypedArray())
}

fun isNotificationPermissionGranted(context: Context): Boolean =
    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()
