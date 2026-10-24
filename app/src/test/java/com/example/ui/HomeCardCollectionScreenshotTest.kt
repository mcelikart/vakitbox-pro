package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.Density
import com.example.alarm.HomeStepsAndAlarmRow
import com.example.data.CurrentWeather
import com.example.data.Timings
import com.example.data.WeatherResponse
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.drawCustomBackground
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Visual checks of the real home widgets at narrow width and large text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h2200dp-xhdpi", sdk = [36])
class HomeCardCollectionScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureHomeCardsAtNarrowWidthAndLargeText() {
        var theme by mutableStateOf(AppTheme.CLASSIC_EMERALD)
        var fontScale by mutableStateOf(1f)
        var dark by mutableStateOf(false)
        val timings = Timings("05:30", "06:45", "12:46", "16:12", "18:52", "18:52", "20:15", "05:20", "00:10")
        File("build/home-cards").mkdirs()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MyApplicationTheme(darkTheme = dark, appTheme = theme) {
                    Column(
                        Modifier.fillMaxSize().drawCustomBackground(theme = theme, isDark = dark)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ThemePrayerScene(timings, "02:28:01", "Güneş" to "Öğle", .4f)
                        HomeStepsAndAlarmRow(onAlarmClick = {}) { HomeStepsMiniWidget({}, it) }
                        WeatherSection(WeatherResponse(CurrentWeather(20.0, 19.0, 45, 7.0, 0), null, null), "Ankara")
                        HomeRandomMiracleCard {}
                        DailyQuoteCard()
                        DetailedTimingsCard(timings)
                    }
                }
            }
        }
        for (nextTheme in listOf(AppTheme.CLASSIC_EMERALD, AppTheme.KUBBELI_SAFIR)) {
            for (scale in listOf(1f, 1.5f)) {
                compose.runOnIdle { theme = nextTheme; fontScale = scale }
                compose.waitForIdle()
                compose.onRoot().captureRoboImage("build/home-cards/${nextTheme.name}-$scale.png")
                for (label in listOf("Sabah Namazı Alarmı", "ADIMSAYAR", "Hedef 6.000")) {
                    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    val node = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
                    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                    // A wrap-content Text can keep a paragraph wider than its
                    // glyphs. Check actual line bounds, allowing pixel rounding.
                    assertTrue("$nextTheme/$scale: clipped $label ${layouts.map { "${it.size} lines=${(0 until it.lineCount).map(it::getLineRight)} height=${it.didOverflowHeight}" }}",
                        layouts.isNotEmpty() && layouts.all {
                            !it.didOverflowHeight && !it.multiParagraph.didExceedMaxLines &&
                                (0 until it.lineCount).all { line ->
                                    it.getLineLeft(line) >= -1f && it.getLineRight(line) <= it.size.width + 1f
                                }
                        })
                }
            }
        }
        compose.runOnIdle { dark = true }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/home-cards/KUBBELI_SAFIR-dark-large.png")
    }
}
