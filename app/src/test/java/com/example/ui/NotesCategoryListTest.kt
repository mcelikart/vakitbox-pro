package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
class NotesCategoryListTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun reset() { context.getSharedPreferences("user_notes_prefs", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun categoriesAreVisibleInTheNoteListAndCanBeAddedOrDeletedWithoutLosingNotes() {
        NotesStore.saveCategories(context, listOf("Özel", "Diğer"))
        NotesStore.saveNotes(context, listOf(UserNote(title = "Korunacak", content = "Metin", category = "Özel")))
        val categories = mutableStateOf(NotesStore.loadCategories(context))
        val notes = mutableStateOf(NotesStore.loadNotes(context))
        val selected = mutableStateOf("Tümü")
        compose.setContent {
            MyApplicationTheme {
                LazyColumn {
                    item {
                        NotesCategoryList(categories.value, notes.value, selected.value,
                            onSelectCategory = { selected.value = it },
                            onAddCategory = { categories.value = NotesStore.addCategory(context, it) },
                            onDeleteCategory = {
                                categories.value = NotesStore.deleteCategory(context, it)
                                notes.value = NotesStore.loadNotes(context)
                            }, onManageCategories = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("notes-category-Özel").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Özel", selected.value) }
        compose.onNodeWithTag("notes-delete-category-Özel").performClick()
        compose.onNodeWithText("“Özel” silinsin mi? Notlar silinmez, Kategorisiz'e taşınır.").assertIsDisplayed()
        compose.onNodeWithText("Sil").performClick()
        compose.onNodeWithTag("notes-category-Kategorisiz").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("Tümü", selected.value)
            assertEquals("Metin", notes.value.single().content)
            assertEquals(NotesStore.UNCATEGORIZED, notes.value.single().category)
        }
        compose.onNodeWithTag("notes-add-category").performClick()
        compose.onNodeWithTag("notes-new-category-name").performTextInput("Yeni kategori")
        compose.onNodeWithTag("notes-save-category").performClick()
        compose.onNodeWithTag("notes-category-Yeni kategori").assertExists()
        compose.runOnIdle { assertEquals("Yeni kategori", selected.value) }
        File("build/notes-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/notes-scenes/categories.png")
    }

    @Test fun finalNoteCanScrollAboveTheMeasuredNavigationIslandWithLargeText() {
        val categories = List(12) { "Kategori ${it + 1}" }
        compose.setContent {
            MyApplicationTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, 1.5f)) {
                    FloatingNavigationScaffold(
                        contentWindowInsets = WindowInsets(bottom = 48.dp),
                        imeInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = { Box(Modifier.fillMaxWidth().height(128.dp)) }
                    ) {
                        LazyColumn(Modifier.testTag("notes-test-list"), contentPadding = PaddingValues(bottom = navBarBottomPadding(extra = 16.dp))) {
                            item {
                                NotesCategoryList(categories, emptyList(), "Tümü", {}, {}, {}, {})
                            }
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
        compose.onRoot().captureRoboImage("build/notes-scenes/categories-large-font.png")
    }

    @Test fun editorKeepsSaveAndVoiceWritingAccessibleOnANarrowScreenWithLargeText() {
        var saved: UserNote? = null
        compose.setContent {
            MyApplicationTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(base.density, 1.5f)) {
                    FullScreenStrawPaperNoteEditor(
                        initialNote = UserNote(title = "Notum", content = "Korunan metin"),
                        onDismiss = {}, onSave = { saved = it }
                    )
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
}
