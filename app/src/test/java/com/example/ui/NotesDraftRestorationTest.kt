package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class NotesDraftRestorationTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun reset() { context.getSharedPreferences("user_notes_prefs", Context.MODE_PRIVATE).edit().clear().commit() }

    private fun openNotebook(): StateRestorationTester {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MyApplicationTheme {
                LazyColumn(Modifier.fillMaxSize()) { item { ToolsNotesSection() } }
            }
        }
        return restoration
    }

    @Test fun newEditorAndItsUnsavedDraftAndCursorReturnAfterStateRestoration() {
        val restoration = openNotebook()
        compose.onNodeWithText("Yeni Not").performClick()
        compose.onNodeWithTag("note-editor-title").performTextInput("Kaydedilmemiş başlık")
        compose.onNodeWithTag("note-editor-body").performTextInput("Birinci ikinci")
        compose.onNodeWithTag("note-editor-body").performTextInputSelection(TextRange(7))
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("note-editor-title").assertTextEquals("Kaydedilmemiş başlık")
        compose.onNodeWithTag("note-editor-body").assertTextEquals("Birinci ikinci")
        assertEquals(TextRange(7), compose.onNodeWithTag("note-editor-body").fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        compose.onNodeWithTag("note-format-Sesle yaz").assertIsDisplayed()
        compose.onNodeWithTag("note-editor-save").performClick()
        compose.runOnIdle {
            val saved = NotesStore.loadNotes(context).single()
            assertEquals("Kaydedilmemiş başlık", saved.title)
            assertEquals("Birinci ikinci", saved.content)
        }
    }

    @Test fun existingNoteIdentityMetadataAndModifiedDraftSurviveCallerAndEditorRecreation() {
        val original = UserNote(id = "same-note-id", title = "Asıl not", content = "Eski metin", category = "Özel",
            createdAt = 123, updatedAt = 456, isImported = true, isPinned = true, paperStyle = "DOTTED", useSerifFont = false, fontSizeSp = 22)
        NotesStore.saveCategories(context, listOf("Özel"))
        NotesStore.saveNotes(context, listOf(original))
        val restoration = openNotebook()
        compose.onNodeWithContentDescription("Tam Ekran").performClick()
        compose.onNodeWithTag("notes-folder-Tümü").performClick()
        compose.onNodeWithText("Asıl not").performScrollTo().performClick()
        compose.onNodeWithTag("note-editor-title").performTextReplacement("Yeni başlık")
        compose.onNodeWithTag("note-editor-body").performTextInput(" ve taslak")
        compose.onNodeWithTag("note-editor-body").performTextInputSelection(TextRange(2, 5))
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("note-editor-title").assertTextEquals("Yeni başlık")
        compose.onNodeWithTag("note-editor-body").assertTextEquals("Eski metin ve taslak")
        assertEquals(TextRange(2, 5), compose.onNodeWithTag("note-editor-body").fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        compose.onNodeWithTag("note-editor-save").performClick()
        compose.runOnIdle {
            val saved = NotesStore.loadNotes(context).single()
            assertEquals(original.copy(title = "Yeni başlık", content = "Eski metin ve taslak", updatedAt = saved.updatedAt), saved)
        }
    }
    @Test fun actualNotesScreenRestoresItsNewEditorAndDraftSelection() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MyApplicationTheme { NotesScreen(onBack = {}) } }
        compose.onNodeWithText("Yeni Not").performClick()
        compose.onNodeWithTag("note-editor-title").performTextInput("Gerçek modül taslağı")
        compose.onNodeWithTag("note-editor-body").performTextInput("Korunan sesli taslak")
        compose.onNodeWithTag("note-editor-body").performTextInputSelection(TextRange(4, 8))
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("note-editor-title").assertTextEquals("Gerçek modül taslağı")
        compose.onNodeWithTag("note-editor-body").assertTextEquals("Korunan sesli taslak")
        assertEquals(TextRange(4, 8), compose.onNodeWithTag("note-editor-body").fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        compose.onNodeWithTag("note-format-Sesle yaz").assertIsDisplayed()
        compose.onNodeWithTag("note-editor-save").performClick()
        compose.runOnIdle {
            assertEquals("Korunan sesli taslak", NotesStore.loadNotes(context).single().content)
        }
    }

    @Test fun actualNotesScreenRestoresExistingNoteIdentityAndUnsavedEdits() {
        val original = UserNote(id = "real-notes-id", title = "Gerçek not", content = "İlk metin", category = "Özel", createdAt = 13, updatedAt = 17, isImported = true, isPinned = true)
        NotesStore.saveCategories(context, listOf("Özel"))
        NotesStore.saveNotes(context, listOf(original))
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MyApplicationTheme { NotesScreen(onBack = {}) } }
        compose.onNodeWithTag("notes-folder-Tümü").performClick()
        compose.onNodeWithText("Gerçek not").performScrollTo().performClick()
        compose.onNodeWithTag("note-editor-body").performTextInput(" ve taslak")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("note-editor-body").assertTextEquals("İlk metin ve taslak")
        compose.onNodeWithTag("note-editor-save").performClick()
        compose.runOnIdle {
            val saved = NotesStore.loadNotes(context).single()
            assertEquals(original.copy(content = "İlk metin ve taslak", updatedAt = saved.updatedAt), saved)
        }
    }

    @Test fun allDiscoverSubscreensUseDistinctStableStringIdsAndRestoreToTheirRoute() {
        val routes = listOf(
            DiscoverSubScreen.Main, DiscoverSubScreen.Notes, DiscoverSubScreen.QuranMiracles,
            DiscoverSubScreen.QazaTracker, DiscoverSubScreen.Surahs, DiscoverSubScreen.EsmaUlHusna,
            DiscoverSubScreen.AlarmSettings, DiscoverSubScreen.ZakatCalc, DiscoverSubScreen.QuranIndex,
            DiscoverSubScreen.HadithLibrary, DiscoverSubScreen.QuranReading, DiscoverSubScreen.Steps,
            DiscoverSubScreen.NearestMosque, DiscoverSubScreen.PrayerLearning, DiscoverSubScreen.AppSettings
        )
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = value is String }
        val saved = routes.map { route -> with(DiscoverSubScreenSaver) { scope.save(route) } }
        assertEquals("notes", saved[1])
        assertEquals(routes.size, saved.distinct().size)
        assertEquals(routes, saved.map { DiscoverSubScreenSaver.restore(it!!) })
        assertEquals(DiscoverSubScreen.Main, DiscoverSubScreenSaver.restore("unknown-old-route"))
    }

}
