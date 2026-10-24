package com.example.ui

import java.util.Locale

internal object QuranAudioSupport {
    fun arabicVerseUrls(surahId: Int, verseNumber: Int): List<String> {
        val fileName = String.format(Locale.US, "%03d%03d.mp3", surahId, verseNumber)
        return listOf(
            "https://verses.quran.foundation/Alafasy/mp3/$fileName",
            "https://everyayah.com/data/Alafasy_128kbps/$fileName"
        )
    }

    fun mealText(verse: SurahVerse, preferredEditions: List<String>): String {
        val selected = preferredEditions
            .asSequence()
            .mapNotNull { verse.mealsMap[it] }
            .firstOrNull { it.isNotBlank() }
            ?: verse.selectedMeal.takeIf { it.isNotBlank() }
            ?: verse.diyanetMeal

        return selected.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Text handed to the Turkish narrator.
     *
     * The bug this fixes: meal editions include `ar.alafasy` ("Arapça Orijinal
     * Metin"), which lives in the same `mealsMap` as the Turkish translations.
     * The old lookup took the first non-blank entry in the user's active
     * edition order, so tapping "Türkçe meal okut" could feed Arabic script to
     * the Turkish voice — which read it aloud as unintelligible Latin gibberish.
     *
     * Three guards, in order:
     *  1. Only editions whose id declares a Latin-script language are eligible.
     *  2. Any candidate that actually contains Arabic codepoints is rejected,
     *     regardless of what its edition id claims.
     *  3. The Diyanet fallback is used only if it passes the same script test.
     */
    fun mealSpeechText(verse: SurahVerse, preferredEditions: List<String>): String {
        val latinEditions = preferredEditions.filter { isLatinEdition(it) }
        val fromEditions = latinEditions
            .asSequence()
            .mapNotNull { verse.mealsMap[it] }
            .firstOrNull { it.isNotBlank() && isSpeakable(it) }

        val raw = fromEditions
            ?: verse.diyanetMeal.takeIf { isSpeakable(it) }
            ?: verse.selectedMeal.takeIf { isSpeakable(it) }
            ?: verse.mealsMap["tr.diyanet"]?.takeIf { isSpeakable(it) }
            ?: ""

        return polishForSpeech(raw)
    }

    /** A visible publisher tab must never narrate a different publisher's text. */
    fun mealSpeechTextForEdition(verse: SurahVerse, edition: String): String {
        if (!isLatinEdition(edition)) return ""
        val text = verse.mealsMap[edition]?.takeIf { isSpeakable(it) } ?: return ""
        return polishForSpeech(text)
    }

    /** Turkish and English editions speak on a Turkish engine; Arabic ones do not. */
    private fun isLatinEdition(key: String): Boolean {
        val lang = key.substringBefore('.').lowercase(Locale.ROOT)
        return lang == "tr" || lang == "en"
    }

    private val ARABIC_RANGE = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

    fun isSpeakable(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        if (ARABIC_RANGE.containsMatchIn(text)) return false
        return text.any { it.isLetter() }
    }

    /**
     * Cleans translation prose for narration and gives the engine the
     * punctuation it needs to place natural phrase breaks.
     *
     * - Strips citation parentheses and verse-number brackets that Turkish
     *   engines otherwise spell out letter by letter.
     * - Expands the abbreviations that appear in meal text, so "Hz." becomes
     *   "Hazreti" instead of being read as "be y ze".
     * - Converts em/en dashes and slashes into commas, which is what a reader
     *   would pause at.
     * - Guarantees a terminal full stop, otherwise the engine's final phrase
     *   runs into the next utterance without falling intonation.
     */
    private fun polishForSpeech(raw: String): String {
        var text = raw
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)

        text = text
            // (bakınız: ...) / (bk. ...) parentheticals
            .replace(Regex("\\((?:bk\\.?|bakınız|bkz\\.?)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
            // trailing verse/citation numbers: [12] or (12)
            .replace(Regex("\\[\\s*\\d+[a-zA-Z]?\\s*]"), " ")
            .replace(Regex("\\(\\s*\\d+[a-zA-Z]?\\s*\\)"), " ")
            // honorific abbreviations
            .replace(Regex("\\bHz\\.?"), "Hazreti")
            .replace(Regex("\\bHz\\.(?=\\s)"), "Hazreti")
            .replace(Regex("\\bs\\.a\\.v\\.?", RegexOption.IGNORE_CASE), "sallallahu aleyhi ve sellem")
            .replace(Regex("\\ba\\.s\\.?(?=\\s)", RegexOption.IGNORE_CASE), "aleyhisselam")
            .replace(Regex("\\br\\.a\\.?(?=\\s)", RegexOption.IGNORE_CASE), "radiyallahu anh")
            .replace(Regex("\\bk\\.a\\.?(?=\\s)", RegexOption.IGNORE_CASE), "kellimallahu aleyhi")
            .replace(Regex("\\br\\.?a\\.?(?=\\s)", RegexOption.IGNORE_CASE), "radiyallahu anh")
            .replace(Regex("\\bc\\.?a\\.?(?=\\s)", RegexOption.IGNORE_CASE), "cemilullah")
            .replace(Regex("\\bvb\\.?(?=\\s)"), "ve benzeri")
            .replace(Regex("\\bvs\\.?(?=\\s)"), "ve savaş")
            // Quranic citation shorthand
            .replace(Regex("\\b sûre\\b", RegexOption.IGNORE_CASE), " Suresi ")
            .replace(Regex("\\b âyet\\b", RegexOption.IGNORE_CASE), " ayeti ")
            // punctuation -> spoken pause
            .replace('—', ',')
            .replace('–', ',')
            .replace('/', ',')
            .replace(Regex("\\.{3,}"), "…")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,.;:!?…])"), "$1")
            .replace(Regex("([,.;:!?…])(?=\\S)"), "$1 ")
            .trim()

        // A verse must end in terminal punctuation, otherwise the engine merges
        // the last phrase into the next narration without dropping its voice.
        if (text.isNotEmpty() && text.last() !in ".!?…") text += "."
        return text
    }
}
