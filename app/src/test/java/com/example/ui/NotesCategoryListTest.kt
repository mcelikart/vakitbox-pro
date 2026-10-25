package com.example.ui

import android.content.Context
import android.Manifest
import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.navBarBottomPadding
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
class NotesCategoryListTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() { context.getSharedPreferences("user_notes_prefs", Context.MODE_PRIVATE).edit().clear().commit() }

    private fun openNotes(largeFont: Boolean = false): StateRestorationTester {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MyApplicationTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, if (largeFont) 1.5f else 1f)) {
                    NotesScreen(onBack = {})
                }
            }
        }
        return restoration
    }

    @Test fun actualNotebookStartsAtFoldersAndOpeningOneReplacesTheFolderListWithItsNotes() {
        NotesStore.saveCategories(context, listOf("Özel", "Diğer"))
        NotesStore.saveNotes(context, listOf(UserNote(id = "a", title = "Özel notum", content = "Metin", category = "Özel"),
            UserNote(id = "b", title = "Diğer notum", content = "Başka", category = "Diğer")))
        val restoration = openNotes()
        compose.onNodeWithTag("notes-folder-Tümü").assertIsDisplayed()
        compose.onNodeWithTag("notes-folder-Kategorisiz").assertIsDisplayed()
        compose.onNodeWithTag("note-item-a").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Özel kategorisini sil").assertCountEquals(0)
        compose.onNodeWithTag("notes-folder-Özel").performClick()
        compose.onNodeWithTag("notes-folders").assertDoesNotExist()
        compose.onNodeWithText("Özel notum").assertIsDisplayed()
        compose.onNodeWithText("Diğer notum").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Özel notum").assertIsDisplayed()
        compose.onNodeWithTag("notes-back-to-folders").performClick()
        compose.onNodeWithTag("notes-folder-Diğer").assertIsDisplayed()
        File("build/notes-scenes").mkdirs()
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/notes-scenes/folders.png")
    }

    @Test fun quickFolderAddAndSeparateManagementKeepNotesWhenAFolderIsDeleted() {
        NotesStore.saveCategories(context, listOf("Özel", "Diğer"))
        NotesStore.saveNotes(context, listOf(UserNote(id = "a", title = "Korunacak", content = "Metin", category = "Özel")))
        openNotes()
        compose.onNodeWithTag("notes-add-folder").performClick()
        compose.onNodeWithTag("notes-new-folder-name").performTextInput("Yeni klasör")
        compose.onNodeWithTag("notes-save-folder").performClick()
        compose.onNodeWithTag("notes-folder-Yeni klasör").assertExists()
        compose.onNodeWithTag("notes-overflow").performClick()
        compose.onAllNodesWithText("Klasörleri yönet").onFirst().performClick()
        compose.onAllNodesWithContentDescription("Sil").onFirst().performClick()
        compose.onNodeWithText("“Özel” silinsin mi? Notlar silinmez, Kategorisiz'e taşınır.").assertIsDisplayed()
        compose.onNodeWithText("Sil").performClick()
        compose.onNodeWithText("Kapat").performClick()
        compose.runOnIdle {
            assertEquals("Metin", NotesStore.loadNotes(context).single().content)
            assertEquals(NotesStore.UNCATEGORIZED, NotesStore.loadNotes(context).single().category)
        }
        compose.onNodeWithTag("notes-folder-Kategorisiz").performClick()
        compose.onNodeWithText("Korunacak").assertIsDisplayed()
    }

    @Test fun creatingANoteInsideAFolderKeepsThatFolderWithoutChangingExistingNotes() {
        NotesStore.saveCategories(context, listOf("Özel"))
        openNotes()
        compose.onNodeWithTag("notes-folder-Özel").performClick()
        compose.onNodeWithTag("notes-new-note").performClick()
        compose.onNodeWithTag("note-editor-title").performTextInput("Klasördeki not")
        compose.onNodeWithTag("note-editor-body").performTextInput("Yeni içerik")
        compose.onNodeWithTag("note-editor-save").performClick()
        compose.onNodeWithText("Klasördeki not").assertIsDisplayed()
        compose.runOnIdle { assertEquals("Özel", NotesStore.loadNotes(context).single().category) }
    }

    @Test fun foldersAndFinalNoteCanScrollClearOfTheMeasuredNavigationIslandAtLargeFont() {
        val categories = List(12) { "Klasör ${it + 1}" }
        compose.setContent {
            MyApplicationTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, 1.5f)) {
                    FloatingNavigationScaffold(contentWindowInsets = WindowInsets(bottom = 48.dp), imeInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = { Box(Modifier.fillMaxWidth().height(128.dp)) }) {
                        LazyColumn(Modifier.testTag("notes-test-list"), contentPadding = PaddingValues(bottom = navBarBottomPadding(extra = 16.dp))) {
                            item { NotesFolderList(categories, emptyList(), {}, {}, {}) }
                            item { Text("Son not", Modifier.fillMaxWidth().height(80.dp).testTag("notes-final-note")) }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("notes-test-list").performScrollToIndex(1)
        compose.onNodeWithTag("notes-test-list").performTouchInput { swipeUp() }
        val final = compose.onNodeWithTag("notes-final-note").getUnclippedBoundsInRoot()
        val bar = compose.onNodeWithTag("app-bottom-navigation").getUnclippedBoundsInRoot()
        assertTrue("final note $final must clear navigation $bar", final.bottom <= bar.top + 0.5.dp)
        File("build/notes-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/notes-scenes/folders-large-font.png")
    }

    @Test fun editorKeepsSaveAndInlineVoiceWritingAccessibleOnANarrowScreenWithLargeText() {
        var saved: UserNote? = null
        compose.setContent {
            MyApplicationTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, 1.5f)) {
                    FullScreenStrawPaperNoteEditor(initialNote = UserNote(title = "Notum", content = "Korunan metin"), onDismiss = {}, onSave = { saved = it })
                }
            }
        }
        compose.onNodeWithTag("note-format-Sesle yaz").assertIsDisplayed()
        compose.onNodeWithTag("note-editor-save").assertIsDisplayed()
        compose.onNodeWithTag("note-editor-body").performTextInput(" eklenen")
        File("build/notes-scenes").mkdirs()
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/notes-scenes/editor-large-font.png")
        compose.onNodeWithTag("note-editor-save").performClick()
        compose.runOnIdle { assertEquals("Korunan metin eklenen", saved?.content) }
    }
    @Test fun fixtureNotebookScreenshotsShowOnlyDemonstrationFoldersAndNotes() {
        NotesStore.saveCategories(context, listOf("Kişisel", "Okuma", "Günlük"))
        NotesStore.saveNotes(context, listOf(
            UserNote(id = "fixture-1", title = "Bugünün planı", content = "Sabah yürüyüşü ve yirmi dakika kitap okuma.", category = "Kişisel", isPinned = true),
            UserNote(id = "fixture-2", title = "Haftalık hedefler", content = "Her gün küçük bir adım.", category = "Kişisel"),
            UserNote(id = "fixture-3", title = "Kitap notları", content = "Okurken aklıma gelen düşünceler.", category = "Okuma"),
            UserNote(id = "fixture-4", title = "Bugünden bir hatıra", content = "Günün sonunda kısa bir not.", category = "Günlük")
        ))
        openNotes()
        File("build/notes-scenes").mkdirs()
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/notes-scenes/notebook-folders-fixture.png")
        compose.onNodeWithTag("notes-folder-Kişisel").performClick()
        compose.onNodeWithText("Bugünün planı").assertIsDisplayed()
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/notes-scenes/notebook-folder-content-fixture.png")
    }

    @Test fun inlineVoiceFixtureShowsPartialTextInTheActualEditorWithoutAnExternalActivity() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.RECORD_AUDIO)
        var events: NoteSpeechEvents? = null
        var released = false
        val factory = NoteSpeechRecognizerFactory { _, callbacks ->
            events = callbacks
            object : NoteSpeechRecognizer {
                override val isOnDevice = true
                override fun startListening(intent: Intent) { callbacks.onReady(); callbacks.onPartial("Bugün yirmi dakika kitap okuyacağım") }
                override fun stopListening() { callbacks.onResults("Bugün yirmi dakika kitap okuyacağım.") }
                override fun cancel() = Unit
                override fun destroy() { released = true }
            }
        }
        var saved: UserNote? = null
        compose.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalNoteSpeechRecognizerFactory provides factory) {
                    FullScreenStrawPaperNoteEditor(initialNote = UserNote(title = "Sesli not", content = "Günün planı: ", category = "Günlük"),
                        onDismiss = {}, onSave = { saved = it })
                }
            }
        }
        compose.onNodeWithTag("note-format-Sesle yaz").performClick()
        compose.onNodeWithText("Durdur").assertIsDisplayed()
        compose.onNodeWithTag("note-voice-status").assertTextEquals("Dinleniyor…")
        compose.onNodeWithTag("note-editor-body").assertTextEquals("Günün planı: Bugün yirmi dakika kitap okuyacağım")
        File("build/notes-scenes").mkdirs()
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/notes-scenes/notebook-voice-active-fixture.png")
        compose.onNodeWithTag("note-format-Sesle yaz").performClick()
        compose.onNodeWithTag("note-editor-body").assertTextEquals("Günün planı: Bugün yirmi dakika kitap okuyacağım.")
        compose.onNodeWithTag("note-editor-save").performClick()
        compose.runOnIdle {
            assertEquals("Günün planı: Bugün yirmi dakika kitap okuyacağım.", saved?.content)
            assertTrue(released)
        }
        compose.runOnIdle { events?.onPartial("geç gelen ses") }
        compose.onNodeWithTag("note-editor-body").assertTextEquals("Günün planı: Bugün yirmi dakika kitap okuyacağım.")
    }

    @Test fun reservedOrDuplicateFolderRenameKeepsTheActualOpenFolderAndItsNotes() {
        NotesStore.saveCategories(context, listOf("Özel", "Diğer"))
        NotesStore.saveNotes(context, listOf(UserNote(id = "rename-a", title = "Bu klasörde kalır", content = "Korunur", category = "Özel")))
        openNotes()
        compose.onNodeWithTag("notes-folder-Özel").performClick()
        compose.onNodeWithTag("notes-overflow").performClick()
        compose.onNodeWithText("Klasörleri yönet").performClick()
        compose.onAllNodesWithContentDescription("Düzenle").onFirst().performClick()
        for (invalid in listOf("tümü", "Diğer", "")) {
            compose.onNodeWithTag("notes-rename-folder-name").performTextReplacement(invalid)
            compose.onNodeWithTag("notes-rename-folder-save").assertIsNotEnabled()
        }
        compose.onNodeWithContentDescription("İptal").performClick()
        compose.onNodeWithText("Kapat").performClick()
        compose.onNodeWithText("Not Defteri / Özel").assertIsDisplayed()
        compose.onNodeWithText("Bu klasörde kalır").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf("Özel", "Diğer"), NotesStore.loadCategories(context))
            assertEquals("Özel", NotesStore.loadNotes(context).single().category)
        }
    }

}
