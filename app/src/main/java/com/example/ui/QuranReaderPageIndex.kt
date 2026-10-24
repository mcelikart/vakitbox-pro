package com.example.ui

/** The bundled Arabic text includes bismillah; meal/pronunciation need their existing header. */
internal fun hasQuranBesmeleHeader(surahId: Int, isArabicPage: Boolean): Boolean =
    surahId != 1 && surahId != 9 && !isArabicPage

/** Maps reader positions through grouped verses and an optional non-ayah header. */
internal class QuranReaderPageIndex(
    val groups: List<GroupedVerseItem>,
    val hasBesmeleHeader: Boolean
) {
    fun isBesmeleItem(itemIndex: Int): Boolean = hasBesmeleHeader && itemIndex == 0

    fun itemIndexForGroup(groupIndex: Int): Int = groupIndex + if (hasBesmeleHeader) 1 else 0

    fun verseNumberForItem(itemIndex: Int): Int {
        if (isBesmeleItem(itemIndex) || groups.isEmpty()) return 1
        val groupIndex = (itemIndex - if (hasBesmeleHeader) 1 else 0).coerceIn(0, groups.lastIndex)
        return groups[groupIndex].startVerseNumber
    }

    fun itemIndexForVerse(verseNumber: Int, atBesmele: Boolean = false): Int {
        if ((hasBesmeleHeader && atBesmele) || groups.isEmpty()) return 0
        val exact = groups.indexOfFirst { verseNumber in it.startVerseNumber..it.endVerseNumber }
        val groupIndex = if (exact >= 0) exact else {
            groups.indexOfFirst { it.startVerseNumber >= verseNumber }.takeIf { it >= 0 } ?: groups.lastIndex
        }
        return itemIndexForGroup(groupIndex)
    }
}
