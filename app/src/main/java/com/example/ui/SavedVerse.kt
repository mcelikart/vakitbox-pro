package com.example.ui

data class SavedVerse(
    val surahId: Int,
    val surahName: String,
    val verseNumber: Int,
    val arabic: String,
    val pronunciation: String,
    val diyanetMeal: String,
    val elmaliliMeal: String = "",
    val vakfiMeal: String = "",
    val savedAtTimestamp: Long = System.currentTimeMillis(),
    val selectedMeal: String = "",
    val mealSource: String = "",
    val footnotes: String = ""
)
