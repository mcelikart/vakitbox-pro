package com.example.ui

import android.Manifest
import android.app.Application
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackgroundAccessTest {
    private lateinit var context: Application
    private lateinit var notifications: NotificationManager

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notifications = context.getSystemService(NotificationManager::class.java)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(notifications).setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test fun explicitBackgroundRestrictionIsNotHiddenByBatteryExemption() {
        shadowOf(context.getSystemService(PowerManager::class.java)).setIgnoringBatteryOptimizations(context.packageName, true)
        shadowOf(context.getSystemService(ActivityManager::class.java)).setBackgroundRestricted(true)
        val state = BackgroundAccess.read(context)
        assertTrue(state.batteryExempt)
        assertTrue(state.backgroundRestricted)
        assertEquals("Arka plan kullanımı kısıtlı", state.batteryStatus)
        requestIgnoreBatteryOptimizations(context)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, shadowOf(context).nextStartedActivity.action)
    }

    @Test fun runtimeAndGlobalNotificationDenialsAreBothReported() {
        assertTrue(BackgroundAccess.read(context).notifications)
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(BackgroundAccess.read(context).notifications)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(notifications).setNotificationsEnabled(false)
        assertFalse(BackgroundAccess.read(context).notifications)
    }

    @Test fun blockedCountdownChannelIsVisibleEvenWhenAppNotificationsAreAllowed() {
        notifications.createNotificationChannel(NotificationChannel(CountdownService.CHANNEL_ID, "Geri sayım", NotificationManager.IMPORTANCE_NONE))
        val state = BackgroundAccess.read(context)
        assertTrue(state.notifications)
        assertFalse(state.countdownChannel)
        assertEquals("Geri sayım kanalı kapalı", state.countdownStatus)
        val intent = BackgroundAccess.countdownIntent(context)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(CountdownService.CHANNEL_ID, intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test fun globalBlockTakesPriorityOverChannelSettings() {
        notifications.createNotificationChannel(NotificationChannel(CountdownService.CHANNEL_ID, "Geri sayım", NotificationManager.IMPORTANCE_NONE))
        shadowOf(notifications).setNotificationsEnabled(false)
        assertEquals("Önce uygulama bildirimlerini açın", BackgroundAccess.read(context).countdownStatus)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, BackgroundAccess.countdownIntent(context).action)
    }

    @Test fun settingsOpenSafelyWhenOemPanelIsMissingAndAppDetailsIsProtected() {
        val attempts = mutableListOf<Intent>()
        val restrictedContext = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                attempts += intent
                when {
                    intent.component != null -> throw ActivityNotFoundException()
                    intent.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS -> throw SecurityException()
                }
            }
        }
        assertTrue(BackgroundAccess.open(restrictedContext, Intent().setClassName("missing", "missing.Panel")))
        assertEquals(3, attempts.size)
        assertEquals(Settings.ACTION_SETTINGS, attempts.last().action)
        assertTrue(attempts.all { it.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0 })
    }

    @Test fun unavailableSettingsDoNotCrash() {
        val restrictedContext = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException() }
        }
        assertFalse(BackgroundAccess.open(restrictedContext, BackgroundAccess.exactAlarmIntent(context)))
    }

    @Test fun vivoGuidanceTargetsManufacturerSettingsWithAppDetailsFallback() {
        val intents = BackgroundAccess.autoStartIntents(context, "vivo")
        assertEquals("com.vivo.permissionmanager", intents.first().component?.packageName)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intents.last().action)
        assertEquals("package:${context.packageName}", intents.last().data.toString())
        assertEquals(1, BackgroundAccess.autoStartIntents(context, "unknown").size)
    }

    @Test @Config(sdk = [24]) fun androidSevenUsesAppDetailsAndStillReportsGlobalNotificationBlock() {
        assertTrue(BackgroundAccess.read(context).exactAlarms)
        assertTrue(BackgroundAccess.read(context).countdownChannel)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, BackgroundAccess.exactAlarmIntent(context).action)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, BackgroundAccess.countdownIntent(context).action)
        shadowOf(notifications).setNotificationsEnabled(false)
        assertFalse(BackgroundAccess.read(context).notifications)
    }
}
