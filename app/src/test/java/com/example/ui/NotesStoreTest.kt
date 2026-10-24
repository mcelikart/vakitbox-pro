package com.example.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class NotesStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun reset() { context.getSharedPreferences("user_notes_prefs", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun deletingCategoryKeepsEveryNoteAndMovesOnlyAffectedNotesToUncategorized() {
        NotesStore.saveCategories(context, listOf("Özel", "Diğer"))
        val affected = UserNote(id = "a", title = "Başlık", content = "İçerik", category = "Özel", createdAt = 11, updatedAt = 22, isPinned = true, paperStyle = "DOTTED", fontSizeSp = 23)
        val other = UserNote(id = "b", title = "Başka", content = "Aynen kalır", category = "Diğer", createdAt = 33, updatedAt = 44)
        NotesStore.saveNotes(context, listOf(affected, other))
        val categories = NotesStore.deleteCategory(context, "Özel")
        assertFalse("Özel" in categories)
        assertTrue(NotesStore.UNCATEGORIZED in categories)
        assertEquals(2, NotesStore.loadNotes(context).size)
        assertEquals(affected.copy(category = NotesStore.UNCATEGORIZED), NotesStore.loadNotes(context).single { it.id == "a" })
        assertEquals(other, NotesStore.loadNotes(context).single { it.id == "b" })
    }

    @Test fun deletingTheLastCategoryPreservesItsNotesAndTheFallbackCannotBeDeleted() {
        NotesStore.saveCategories(context, listOf("Tek"))
        NotesStore.saveNotes(context, listOf(UserNote(title = "Not", content = "Kaybolmaz", category = "Tek")))
        assertEquals(listOf(NotesStore.UNCATEGORIZED), NotesStore.deleteCategory(context, "Tek"))
        assertEquals(listOf(NotesStore.UNCATEGORIZED), NotesStore.deleteCategory(context, NotesStore.UNCATEGORIZED))
        assertEquals("Kaybolmaz", NotesStore.loadNotes(context).single().content)
    }

    @Test fun addingCategoriesRetainsExistingSelectionsAndRejectsDuplicateAndReservedNames() {
        NotesStore.saveCategories(context, listOf("Özel", "Diğer"))
        assertEquals(listOf("Özel", "Diğer", "Yeni"), NotesStore.addCategory(context, " Yeni "))
        assertEquals(listOf("Özel", "Diğer", "Yeni"), NotesStore.addCategory(context, "yeni"))
        assertEquals(listOf("Özel", "Diğer", "Yeni"), NotesStore.addCategory(context, "Tümü"))
    }
    @Test fun invalidRenameLeavesCategoriesAndTheirNotesUnchanged() {
        val original = listOf("Özel", "Diğer")
        NotesStore.saveCategories(context, original)
        val note = UserNote(id = "stable", title = "Başlık", content = "Korunan içerik", category = "Özel", createdAt = 7, updatedAt = 9, isPinned = true)
        NotesStore.saveNotes(context, listOf(note))
        for (invalid in listOf("", "  ", "Tümü", " tümü ", "Diğer", "diğer")) {
            assertEquals(original, NotesStore.renameCategory(context, "Özel", invalid))
            assertEquals(original, NotesStore.loadCategories(context))
            assertEquals(note, NotesStore.loadNotes(context).single())
        }
    }

    @Test fun validAndCaseOnlyRenamesKeepEveryNoteFieldAndTheCategoryOrder() {
        NotesStore.saveCategories(context, listOf("Özel", "Diğer"))
        val note = UserNote(id = "stable", title = "Başlık", content = "Korunan içerik", category = "Özel", createdAt = 7, updatedAt = 9, isPinned = true)
        NotesStore.saveNotes(context, listOf(note))
        assertEquals(listOf("Yeni", "Diğer"), NotesStore.renameCategory(context, "Özel", " Yeni "))
        assertEquals(note.copy(category = "Yeni"), NotesStore.loadNotes(context).single())
        assertEquals(listOf("yeni", "Diğer"), NotesStore.renameCategory(context, "Yeni", "yeni"))
        assertEquals(note.copy(category = "yeni"), NotesStore.loadNotes(context).single())
    }

}
