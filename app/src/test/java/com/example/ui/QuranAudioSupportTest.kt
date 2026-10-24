package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranAudioSupportTest {
    private val verse = SurahVerse(
        number = 5,
        arabic = "test",
        pronunciation = "test",
        diyanetMeal = "Diyanet meal",
        elmaliliMeal = "Elmalili meal",
        vakfiMeal = "Vakif meal",
        diyanetTefsir = "",
        bilmenTefsir = "",
        mealsMap = mapOf("tr.yazir" to "<b>Seçili</b> meal")
    )

    @Test
    fun arabicUrlsAreVerseSpecificAndHaveFallback() {
        assertEquals(
            listOf(
                "https://verses.quran.foundation/Alafasy/mp3/002005.mp3",
                "https://everyayah.com/data/Alafasy_128kbps/002005.mp3"
            ),
            QuranAudioSupport.arabicVerseUrls(2, 5)
        )
    }

    @Test
    fun mealTextUsesPreferredEditionAndRemovesMarkup() {
        assertEquals("Seçili meal", QuranAudioSupport.mealText(verse, listOf("tr.yazir")))
    }

    @Test
    fun mealTextFallsBackToDiyanet() {
        assertEquals("Diyanet meal", QuranAudioSupport.mealText(verse, listOf("tr.unknown")))
    }

    @Test
    fun mealSpeechTextCleansFootnotesAndExpandsTurkishAbbreviations() {
        val spokenVerse = verse.copy(
            mealsMap = mapOf(
                "tr.diyanet" to "Hz. Musa [12] kavmine seslendi (bk. Bakara 2/3); sonra yürüdü."
            )
        )

        assertEquals(
            "Hazreti Musa kavmine seslendi; sonra yürüdü.",
            QuranAudioSupport.mealSpeechText(spokenVerse, listOf("tr.diyanet"))
        )
    }

    @Test
    fun visibleEditionNeverBorrowsTheDefaultOrAnotherPublishersMeal() {
        val selectedVerse = verse.copy(selectedMeal = "Başka yayıncının seçili meali")
        assertEquals("", QuranAudioSupport.mealSpeechTextForEdition(selectedVerse, "tr.unknown"))
        assertEquals("", QuranAudioSupport.mealSpeechTextForEdition(selectedVerse.copy(mealsMap = mapOf("tr.unknown" to " ")), "tr.unknown"))
        // The fallback is still available when no publisher tab is being displayed.
        assertEquals("Diyanet meal.", QuranAudioSupport.mealSpeechText(selectedVerse, listOf("tr.unknown")))
    }

    @Test
    fun visibleEditionUsesOnlyItsOwnTextAndKeepsSpeechCleanup() {
        val selectedVerse = verse.copy(
            mealsMap = mapOf(
                "tr.diyanet" to "Farklı yayıncının metni",
                "tr.rowwad" to "<b>Hz. Musa</b> [1] kavmine seslendi."
            )
        )
        assertEquals("Hazreti Musa kavmine seslendi.", QuranAudioSupport.mealSpeechTextForEdition(selectedVerse, "tr.rowwad"))
    }

    @Test
    fun visibleEditionRejectsArabicWithoutReadingTheTurkishFallback() {
        val selectedVerse = verse.copy(mealsMap = mapOf("tr.rowwad" to "بِسْمِ اللَّهِ", "ar.alafasy" to "Arapça olmayan etiketli metin"))
        assertEquals("", QuranAudioSupport.mealSpeechTextForEdition(selectedVerse, "tr.rowwad"))
        assertEquals("", QuranAudioSupport.mealSpeechTextForEdition(selectedVerse, "ar.alafasy"))
    }
}
