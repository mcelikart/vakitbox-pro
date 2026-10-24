package com.example.ui

/** Stable source identifiers also used by existing saved user selections. */
internal object QuranReadingEditions {
    const val ARABIC = "ar.alafasy"
    const val PRONUNCIATION = "tr.transliteration"
    val choices = listOf(
        "tr.diyanet" to "Diyanet İşleri Meali",
        "tr.yazir" to "Elmalılı Hamdi Yazır",
        "tr.ozturk" to "Yaşar Nuri Öztürk",
        "tr.yildirim" to "Suat Yıldırım",
        "tr.vakfi" to "Türkiye Diyanet Vakfı",
        "tr.golpinarli" to "Abdülbaki Gölpınarlı",
        "tr.ates" to "Süleyman Ateş",
        "en.sahih" to "Saheeh International (English)",
        OfflineQuranReader.EDITION_ID to "Rowwad Türkçe Meali",
        ARABIC to "Arapça Orijinal Metin"
    )
    private val names = choices.toMap()
    private val aliases = mapOf("tr.suat" to "tr.yildirim")

    fun name(identifier: String): String = if (identifier == PRONUNCIATION) "Türkçe Okunuş" else
        names[identifier] ?: aliases[identifier]?.let { names[it] } ?: identifier

    fun assetIdentifier(identifier: String): String = aliases[identifier] ?: identifier

    fun orderedMeals(selected: Set<String>): List<String> =
        (choices.map { it.first }.filter { it in selected } + selected.filter { it !in names }.sorted())
            .filter { it != ARABIC && it != PRONUNCIATION }

    fun sourceLabel(identifier: String): String = when (identifier) {
        OfflineQuranReader.EDITION_ID -> "QuranEnc · Rowwad 1.0.4"
        ARABIC -> "Tanzil · Uthmani 1.1"
        else -> name(identifier)
    }
}
