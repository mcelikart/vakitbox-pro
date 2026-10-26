package com.example.ui

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.PowerManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class BackgroundSettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Application
    private lateinit var notifications: NotificationManager
    private lateinit var power: PowerManager

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notifications = context.getSystemService(NotificationManager::class.java)
        power = context.getSystemService(PowerManager::class.java)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(notifications).setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        context.getSharedPreferences("background_protection", 0).edit().clear().commit()
    }

    @Test fun returningFromSystemSettingsRefreshesBatteryExactAlarmAndChannelState() {
        val owner = Owner()
        compose.runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MyApplicationTheme { BackgroundSettingsScreen(onBack = {}) }
            }
        }
        compose.onNodeWithText("Pil optimizasyonu etkin").assertExists()
        compose.onNodeWithText("İzin gerekli").assertExists()
        File("build/background-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/background-scenes/background-settings.png")
        compose.runOnUiThread {
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            shadowOf(power).setIgnoringBatteryOptimizations(context.packageName, true)
            ShadowAlarmManager.setCanScheduleExactAlarms(true)
            notifications.createNotificationChannel(NotificationChannel(CountdownService.CHANNEL_ID, "Geri sayım", NotificationManager.IMPORTANCE_NONE))
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        compose.onNodeWithText("Pil optimizasyonundan muaf").assertExists()
        compose.onNodeWithText("İzin verildi").assertExists()
        compose.onNodeWithText("Geri sayım kanalı kapalı").assertExists()
        compose.onNodeWithText("Telefon ayarlarından kontrol edin").assertExists()
    }

    @Test fun dialogDoneOnlyClosesItWithoutStartingTwoSystemPermissionFlows() {
        var dismissed = 0
        var permissionRequests = 0
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        compose.setContent {
            MyApplicationTheme {
                BackgroundProtectionDialog(onDismiss = { dismissed++ }, onRequestPermissions = { permissionRequests++ })
            }
        }
        compose.onNodeWithTag("background-protection-done").performScrollTo().performClick()
        assertEquals(1, dismissed)
        assertEquals(0, permissionRequests)
        assertEquals(null, shadowOf(context).nextStartedActivity)
        compose.onNodeWithText("Sistem arka planda kapatmayacaktır.", substring = true).assertDoesNotExist()
    }

    @Test fun upgradeGuidanceWaitsForInitialSetupAndStaysDismissedAfterRecreation() {
        val canPrompt = mutableStateOf(false)
        val mounted = mutableStateOf(true)
        compose.setContent {
            MyApplicationTheme { if (mounted.value) BackgroundProtectionPrompt(canPrompt.value) }
        }
        compose.onNodeWithTag("background-protection-done").assertDoesNotExist()
        compose.runOnUiThread { canPrompt.value = true }
        compose.onNodeWithTag("background-protection-done").performScrollTo().performClick()
        compose.onNodeWithTag("background-protection-done").assertDoesNotExist()
        compose.runOnUiThread { mounted.value = false }
        compose.waitForIdle()
        compose.runOnUiThread { mounted.value = true }
        compose.onNodeWithTag("background-protection-done").assertDoesNotExist()
        assertEquals(true, context.getSharedPreferences("background_protection", 0).getBoolean("background_guidance_v1_seen", false))
        assertEquals(null, shadowOf(context).nextStartedActivity)
    }

    @Test fun completePermissionsDoNotShowUpgradeGuidance() {
        shadowOf(power).setIgnoringBatteryOptimizations(context.packageName, true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        compose.setContent { MyApplicationTheme { BackgroundProtectionPrompt(canPrompt = true) } }
        compose.onNodeWithTag("background-protection-done").assertDoesNotExist()
        assertEquals(null, shadowOf(context).nextStartedActivity)
    }

    private class Owner : LifecycleOwner { override val lifecycle = LifecycleRegistry(this) }
}
