package com.example.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class QuranMealSettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allPreviousSourcesAndBothAdditionalSourcesCanBeSelectedWithoutResettingPreferences() {
        val original = setOf("tr.diyanet", "tr.yazir", "tr.vakfi", "tr.custom-saved")
        val selected = mutableStateOf(original)
        compose.setContent {
            MyApplicationTheme {
                QuranMealSettingsSubScreen(
                    activeEditions = selected.value,
                    onToggleEdition = { identifier ->
                        selected.value = if (identifier in selected.value) selected.value - identifier else selected.value + identifier
                    },
                    onBack = {}
                )
            }
        }
        compose.runOnIdle { assertEquals(original, selected.value) }
        File("build/quran-reader").mkdirs()
        compose.onRoot().captureRoboImage("build/quran-reader/meal-sources.png")
        for ((identifier, title) in QuranReadingEditions.choices) {
            compose.onNodeWithTag("quran-meal-choices").performScrollToNode(hasTestTag("quran-edition-$identifier"))
            compose.onNodeWithText(title).assertIsDisplayed()
        }
        compose.onNodeWithTag("quran-meal-choices").performScrollToNode(hasTestTag("quran-edition-tr.rowwad"))
        compose.onNodeWithTag("quran-edition-tr.rowwad").performClick()
        compose.runOnIdle { assertEquals(original + "tr.rowwad", selected.value) }
        compose.onNodeWithTag("quran-meal-choices").performScrollToNode(hasTestTag("quran-edition-tr.diyanet"))
        compose.onNodeWithTag("quran-edition-tr.diyanet").performClick()
        compose.runOnIdle {
            assertFalse("tr.diyanet" in selected.value)
            assertTrue(selected.value.containsAll(setOf("tr.yazir", "tr.vakfi", "tr.rowwad", "tr.custom-saved")))
        }
    }

    @Test
    fun labelsAndArabicSelectionKeepTheirActualSourceIdentity() {
        assertEquals("Yaşar Nuri Öztürk", QuranReadingEditions.name("tr.ozturk"))
        assertEquals("Suat Yıldırım", QuranReadingEditions.name("tr.suat"))
        assertEquals("tr.yildirim", QuranReadingEditions.assetIdentifier("tr.suat"))
        assertEquals(listOf("tr.yazir", "tr.ozturk"), QuranReadingEditions.orderedMeals(setOf("tr.ozturk", "tr.yazir", "ar.alafasy", "tr.transliteration")))
        assertEquals("tr.custom-saved", QuranReadingEditions.name("tr.custom-saved"))
    }
}
