package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranReaderPageIndexTest {
    private val verses = listOf(verse(1, "Birinci"), verse(2, "İkinci ve üçüncü"), verse(3, "İkinci ve üçüncü"), verse(4, "Dördüncü"))

    @Test
    fun besmeleHeaderRemainsOnMealAndPronunciationButDoesNotDuplicateArabic() {
        for (surah in 1..114) {
            assertFalse(hasQuranBesmeleHeader(surah, isArabicPage = true))
            assertEquals(surah != 1 && surah != 9, hasQuranBesmeleHeader(surah, isArabicPage = false))
        }
    }

    @Test
    fun verseJumpAndFastScrollAccountForTheMealHeaderAndGroupedVerses() {
        val meal = QuranReaderPageIndex(groupSurahVerses(verses, null), hasBesmeleHeader = true)
        assertTrue(meal.isBesmeleItem(0))
        assertFalse(meal.isBesmeleItem(1))
        assertEquals(1, meal.verseNumberForItem(0))
        assertEquals(1, meal.verseNumberForItem(1))
        assertEquals(2, meal.verseNumberForItem(2))
        assertEquals(4, meal.verseNumberForItem(3))
        assertEquals(0, meal.itemIndexForVerse(1, atBesmele = true))
        assertEquals(1, meal.itemIndexForVerse(1))
        assertEquals(2, meal.itemIndexForVerse(3))
        assertEquals(3, meal.itemIndexForVerse(4))
        assertEquals(3, meal.itemIndexForGroup(meal.groups.lastIndex))
    }

    @Test
    fun switchingFromMealToArabicAndPronunciationPreservesTheSameAyah() {
        val meal = QuranReaderPageIndex(groupSurahVerses(verses, null), hasBesmeleHeader = true)
        val arabic = QuranReaderPageIndex(groupSurahVerses(verses, null, disableGrouping = true), hasBesmeleHeader = false)
        val pronunciation = QuranReaderPageIndex(groupSurahVerses(verses, null), hasBesmeleHeader = true)
        val readingVerse = meal.verseNumberForItem(3)
        assertEquals(4, readingVerse)
        assertEquals(3, arabic.itemIndexForVerse(readingVerse))
        assertEquals(3, pronunciation.itemIndexForVerse(readingVerse))
        assertEquals(0, arabic.itemIndexForVerse(1, atBesmele = true))
        assertFalse(arabic.isBesmeleItem(0))
        assertEquals(1, arabic.verseNumberForItem(0))
        assertEquals(3, arabic.itemIndexForGroup(arabic.groups.lastIndex))
    }

    private fun verse(number: Int, meal: String) = SurahVerse(number, "ع", "okunuş", "", "", "", "", "", selectedMeal = meal)
}
