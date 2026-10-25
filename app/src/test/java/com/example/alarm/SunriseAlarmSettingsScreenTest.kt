package com.example.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.FloatingNavigationScaffold
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
class SunriseAlarmSettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private var latest = SunriseAlarmSettings()
    private var tonePickerCount = 0

    private fun open(dark: Boolean = false, fontScale: Float = 1f) {
        compose.setContent {
            MyApplicationTheme(darkTheme = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    var settings by remember { mutableStateOf(SunriseAlarmSettings()) }
                    FloatingNavigationScaffold(
                        showBottomBar = true,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0), imeInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = { Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                            Box(Modifier.fillMaxWidth().height(120.dp).testTag("settings-test-nav"))
                        } }
                    ) {
                        SunriseAlarmSettingsContent(settings, "Güzel Gün", 60, true, {}, {
                            settings = it
                            latest = it
                        }, { tonePickerCount++ })
                    }
                }
            }
        }
    }

    @Test fun settingsChoicesUpdateRealConfigurationAndRingtoneOpensPicker() {
        open()
        compose.onNodeWithTag("alarm-setting-tone").performClick()
        assertEquals(1, tonePickerCount)
        compose.onNodeWithTag("alarm-setting-duration").performClick()
        compose.onNodeWithText("30 dk").performClick()
        assertEquals(30, latest.ringMinutes)
        compose.onNodeWithTag("alarm-setting-gradual").performClick()
        assertTrue(latest.gradualVolume)
        compose.onNodeWithTag("sunrise-settings-list").performScrollToNode(hasTestTag("alarm-setting-snooze"))
        compose.onNodeWithTag("alarm-setting-snooze").performClick()
        compose.onNodeWithText("15 dk").performClick()
        assertEquals(15, latest.snoozeMinutes)
    }

    @Test fun narrowLargeTextSettingsClearSharedNavigationAndKeyChoiceCanBeReached() {
        open(fontScale = 1.7f)
        File("build/sunrise-alarm-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/sunrise-alarm-scenes/settings-large-text-top.png")
        compose.onNodeWithTag("sunrise-settings-list").performScrollToNode(hasTestTag("alarm-power-off-note"))
        compose.onNodeWithTag("sunrise-settings-list").performTouchInput { swipeUp() }
        val note = compose.onNodeWithTag("alarm-power-off-note").assertIsDisplayed().getUnclippedBoundsInRoot()
        val navigation = compose.onNodeWithTag("settings-test-nav").getUnclippedBoundsInRoot()
        assertTrue(note.bottom <= navigation.top + .5.dp)
        compose.onRoot().captureRoboImage("build/sunrise-alarm-scenes/settings-large-text-bottom.png")
        compose.onNodeWithTag("alarm-setting-keys").performClick()
        compose.onNodeWithText("Kapat").performClick()
        assertEquals(AlarmVolumeKeyAction.DISMISS, latest.volumeKeyAction)
    }

    @Test fun darkSettingsKeepVolumeAndCoreControlsReadable() {
        open(dark = true)
        compose.onNodeWithTag("alarm-setting-volume").assertIsDisplayed()
        compose.onNodeWithText("Güzel Gün").assertIsDisplayed()
        File("build/sunrise-alarm-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/sunrise-alarm-scenes/settings-dark.png")
    }
}
