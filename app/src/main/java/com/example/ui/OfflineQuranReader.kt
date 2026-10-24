package com.example.ui

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException

/** Reads the packaged text independently of the search model and any network. */
internal class OfflineQuranReader(context: Context) {
    private val assets = context.applicationContext.assets
    private data class Text(val number: Int, val arabic: String, val translation: String, val footnotes: String)
    private val translations = mutableMapOf<String, List<String>?>()
    private val surahOffsets = QuranData.surahMetadata.associate { metadata ->
        metadata.id to QuranData.surahMetadata.takeWhile { it.id != metadata.id }.sumOf { it.verseCount }
    }

    private val textsBySurah by lazy {
        val source = assets.open("quran_search/verses.json").bufferedReader().use { JSONArray(it.readText()) }
        val notes = assets.open("quran_search/footnotes.json").bufferedReader().use { JSONObject(it.readText()) }
        val surahs = linkedMapOf<Int, MutableList<Text>>()
        for (index in 0 until source.length()) {
            val row = source.getJSONObject(index)
            val surahId = row.getInt("surahId")
            val number = row.getInt("verseNumber")
            surahs.getOrPut(surahId) { mutableListOf() }.add(
                Text(number, row.getString("arabic"), row.getString("translation"), notes.optString("$surahId:$number"))
            )
        }
        require(source.length() == 6236 && surahs.size == 114) { "Incomplete Quran text assets" }
        QuranData.surahMetadata.forEach { meta ->
            val verses = surahs.getValue(meta.id)
            require(verses.map { it.number } == (1..meta.verseCount).toList()) { "Incomplete surah ${meta.id}" }
            require(verses.all { it.arabic.isNotBlank() && it.translation.isNotBlank() })
        }
        surahs
    }

    private fun translation(identifier: String): List<String>? = synchronized(translations) {
        val assetId = QuranReadingEditions.assetIdentifier(identifier)
        if (translations.containsKey(assetId)) return@synchronized translations[assetId]
        val text = try {
            val source = assets.open("quran_reading/$assetId.json").bufferedReader().use { JSONArray(it.readText()) }
            require(source.length() == 6236) { "Incomplete Quran edition $assetId" }
            List(source.length()) { index -> source.getString(index) }.also { verses ->
                require(verses.all { it.isNotBlank() }) { "Empty ayah in Quran edition $assetId" }
            }
        } catch (_: FileNotFoundException) {
            null
        }
        translations[assetId] = text
        text
    }

    suspend fun loadSurah(surah: Surah, editions: List<String> = listOf(EDITION_ID)): List<SurahVerse> = withContext(Dispatchers.IO) {
        ensureActive()
        val selected = editions.distinct().filter { it != QuranReadingEditions.ARABIC && it != QuranReadingEditions.PRONUNCIATION }
        val selectedTranslations = selected.filter { it != EDITION_ID }.associateWith { translation(it) }
        val packagedPronunciation = translation(QuranReadingEditions.PRONUNCIATION)
        val pronunciationFallback = surah.verses.associate { it.number to it.pronunciation }
        val offset = surahOffsets.getValue(surah.id)
        val result = textsBySurah.getValue(surah.id).map { verse ->
            val meals = buildMap {
                selected.forEach { identifier ->
                    val text = if (identifier == EDITION_ID) verse.translation else selectedTranslations[identifier]?.get(offset + verse.number - 1)
                    if (text != null) put(identifier, text)
                }
            }
            val primaryEdition = selected.firstOrNull { it in meals }
            val notes = if (EDITION_ID in meals && verse.footnotes.isNotBlank()) mapOf(EDITION_ID to verse.footnotes) else emptyMap()
            SurahVerse(
                number = verse.number,
                arabic = verse.arabic,
                pronunciation = packagedPronunciation?.get(offset + verse.number - 1)
                    ?: pronunciationFallback[verse.number].orEmpty(),
                diyanetMeal = meals["tr.diyanet"].orEmpty(),
                elmaliliMeal = meals["tr.yazir"].orEmpty(),
                vakfiMeal = meals["tr.vakfi"].orEmpty(),
                diyanetTefsir = "",
                bilmenTefsir = "",
                selectedMeal = primaryEdition?.let(meals::get).orEmpty(),
                mealsMap = meals,
                footnotes = primaryEdition?.let(notes::get).orEmpty(),
                footnotesMap = notes
            )
        }
        ensureActive()
        result
    }

    companion object {
        const val EDITION_ID = "tr.rowwad"
    }
}
