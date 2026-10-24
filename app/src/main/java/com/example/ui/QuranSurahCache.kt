package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Small screen-session cache of parsed, bundled surahs. Reader text is loaded
 * from local assets, independently of network services and the search model.
 */
class QuranSurahCache(private val maxEntries: Int = 6) {
    private data class Key(val surahId: Int, val editions: List<String>)

    private val entries = object : LinkedHashMap<Key, List<SurahVerse>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, List<SurahVerse>>?) =
            size > maxEntries
    }

    @Synchronized
    fun get(surahId: Int, editions: List<String>): List<SurahVerse>? = entries[Key(surahId, editions.toList())]

    @Synchronized
    fun put(surahId: Int, editions: List<String>, verses: List<SurahVerse>) {
        entries[Key(surahId, editions.toList())] = verses
    }

    @Synchronized
    fun clear() = entries.clear()
}

@Composable
internal fun rememberQuranSurahCache(): QuranSurahCache = remember { QuranSurahCache() }
