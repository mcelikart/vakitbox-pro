package com.example.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class OfflineQuranReaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val surahs = QuranData.buildCompleteSurahList(emptyList())

    @Test
    fun all114SurahsLoadCompleteTextVerbatimFromThePackagedAssets() = runBlocking {
        val reader = OfflineQuranReader(context)
        val source = context.assets.open("quran_search/verses.json").bufferedReader().use { JSONArray(it.readText()) }
        val notes = context.assets.open("quran_search/footnotes.json").bufferedReader().use { JSONObject(it.readText()) }
        val pronunciation = context.assets.open("quran_reading/tr.transliteration.json").bufferedReader().use { JSONArray(it.readText()) }
        assertEquals(6236, pronunciation.length())
        var sourceIndex = 0
        for (surah in surahs) {
            val verses = reader.loadSurah(surah)
            assertEquals(surah.verseCount, verses.size)
            assertEquals((1..surah.verseCount).toList(), verses.map { it.number })
            for (verse in verses) {
                val original = source.getJSONObject(sourceIndex++)
                assertEquals(original.getString("arabic"), verse.arabic)
                assertEquals(pronunciation.getString(sourceIndex - 1), verse.pronunciation)
                assertEquals(original.getString("translation"), verse.selectedMeal)
                assertEquals(mapOf(OfflineQuranReader.EDITION_ID to verse.selectedMeal), verse.mealsMap)
                assertEquals(notes.optString("${surah.id}:${verse.number}"), verse.footnotes)
                // Rowwad must never be presented as a different publisher.
                assertTrue(verse.diyanetMeal.isEmpty() && verse.elmaliliMeal.isEmpty() && verse.vakfiMeal.isEmpty())
            }
        }
        assertEquals(6236, sourceIndex)
    }

    @Test
    fun completeSourcePronunciationTakesPriorityAndTheLocalMealWorksForReadingAndSpeech() = runBlocking {
        val reader = OfflineQuranReader(context)
        val fatiha = surahs.first().copy(
            verses = listOf(SurahVerse(1, "", "Bismillâhirrahmânirrahîm.", "", "", "", "", ""))
        )
        val verses = reader.loadSurah(fatiha)
        val source = context.assets.open("quran_reading/tr.transliteration.json").bufferedReader().use { JSONArray(it.readText()) }
        assertEquals(source.getString(0), verses.first().pronunciation)
        assertEquals(List(7) { source.getString(it) }, verses.map { it.pronunciation })
        assertTrue(verses.all { it.pronunciation.isNotBlank() })
        val verse = verses[1]
        assertEquals(verse.selectedMeal, QuranAudioSupport.mealText(verse, listOf(OfflineQuranReader.EDITION_ID)))
        assertEquals(verse.selectedMeal, QuranAudioSupport.mealSpeechText(verse, listOf(OfflineQuranReader.EDITION_ID)))
        val grouped = groupSurahVerses(verses, OfflineQuranReader.EDITION_ID)
        assertEquals(7, grouped.size)
        assertTrue(grouped.all { it.mealText.isNotBlank() })
        assertEquals(verse.selectedMeal, groupSurahVerses(verses, null)[1].mealText)
    }
    @Test
    fun allSelectedMealsKeepTheirOwnPackagedTextAcrossEverySurahBoundary() = runBlocking {
        val identifiers = QuranReadingEditions.choices.map { it.first }
            .filter { it != QuranReadingEditions.ARABIC && it != OfflineQuranReader.EDITION_ID }
        val sources = identifiers.associateWith { identifier ->
            context.assets.open("quran_reading/$identifier.json").bufferedReader().use { JSONArray(it.readText()) }
                .also { assertEquals(6236, it.length()) }
        }
        val reader = OfflineQuranReader(context)
        var globalIndex = 0
        for (surah in surahs) {
            val verses = reader.loadSurah(surah, identifiers)
            for (verse in verses) {
                assertEquals(identifiers.toSet(), verse.mealsMap.keys)
                identifiers.forEach { identifier ->
                    assertEquals("$identifier ${surah.id}:${verse.number}", sources.getValue(identifier).getString(globalIndex), verse.mealsMap[identifier])
                }
                assertEquals(verse.mealsMap["tr.diyanet"], verse.diyanetMeal)
                assertEquals(verse.mealsMap["tr.yazir"], verse.elmaliliMeal)
                assertEquals(verse.mealsMap["tr.vakfi"], verse.vakfiMeal)
                assertTrue("Rowwad notes must not leak into another publisher", verse.footnotes.isEmpty() && verse.footnotesMap.isEmpty())
                globalIndex++
            }
        }
        assertEquals(6236, globalIndex)
    }

    @Test
    fun unavailableEditionDoesNotBorrowAnotherTranslationAndArabicIsNotAnEmptyMeal() = runBlocking {
        val reader = OfflineQuranReader(context)
        val verses = reader.loadSurah(surahs.first(), listOf("tr.unavailable", OfflineQuranReader.EDITION_ID))
        assertTrue(verses.all { "tr.unavailable" !in it.mealsMap && OfflineQuranReader.EDITION_ID in it.mealsMap })
        assertTrue(groupSurahVerses(verses, "tr.unavailable").all { it.mealText.isEmpty() })
        val arabic = reader.loadSurah(surahs.first(), listOf(QuranReadingEditions.ARABIC))
        assertTrue(arabic.all { it.arabic.isNotBlank() && it.mealsMap.isEmpty() && it.selectedMeal.isEmpty() })
        assertTrue(QuranReadingEditions.orderedMeals(setOf(QuranReadingEditions.ARABIC)).isEmpty())
        val aliases = reader.loadSurah(surahs.first(), listOf("tr.suat"))
        val yildirim = reader.loadSurah(surahs.first(), listOf("tr.yildirim"))
        assertEquals(yildirim.map { it.selectedMeal }, aliases.map { it.selectedMeal })
    }

    @Test
    fun cacheKeepsSelectionPayloadsSeparateAndDoesNotRetainMutableKeys() = runBlocking {
        val reader = OfflineQuranReader(context)
        val rowwad = reader.loadSurah(surahs.first())
        val diyanet = reader.loadSurah(surahs.first(), listOf("tr.diyanet"))
        val cache = QuranSurahCache()
        val key = mutableListOf(OfflineQuranReader.EDITION_ID)
        cache.put(1, key, rowwad)
        key[0] = "tr.diyanet"
        cache.put(1, key, diyanet)
        assertEquals(rowwad, cache.get(1, listOf(OfflineQuranReader.EDITION_ID)))
        assertEquals(diyanet, cache.get(1, listOf("tr.diyanet")))
    }

}
