package com.example.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.BuildConfig
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class AppAboutScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun aboutShowsThePackagedVersionAndBundledSources() {
        compose.setContent { MyApplicationTheme { AboutSubScreen(onBack = {}) } }
        compose.onNodeWithText("Vakit Pro").assertIsDisplayed()
        compose.onNodeWithText("Sürüm ${BuildConfig.VERSION_NAME} • Yapı ${BuildConfig.VERSION_CODE}").assertIsDisplayed()
        compose.onNodeWithText("Kur’an metni: Tanzil · Seçilebilir mealler ve Türkçe okunuş: çevrimdışı").performScrollTo().assertIsDisplayed()
        File("build/quran-index-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/quran-index-scenes/about.png")
        compose.onNodeWithText("Kaynaklar ve lisanslar").performClick()
        compose.onNodeWithText("intfloat/multilingual-e5-small", substring = true).assertExists()
        compose.onNodeWithText("Diyanet İşleri", substring = true).assertExists()
        compose.onNodeWithText("https://tanzil.net/trans/", substring = true).assertExists()
        compose.onNodeWithText("Kapat").performClick()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
    fun versionAndSourcesRemainReachableWithLargeText() {
        compose.setContent {
            MyApplicationTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                    AboutSubScreen(onBack = {})
                }
            }
        }
        compose.onNodeWithText("Sürüm ${BuildConfig.VERSION_NAME} • Yapı ${BuildConfig.VERSION_CODE}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Kaynaklar ve lisanslar").performScrollTo().assertIsDisplayed()
    }
}
