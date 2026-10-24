package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Uses the phone's speech input activity; no application API or service is required. */
internal fun noteDictationIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Notunuzu söyleyin")
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
}

internal fun recognizedNoteDictation(resultCode: Int, data: Intent?): String? {
    if (resultCode != Activity.RESULT_OK) return null
    return data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull { it.isNotBlank() }?.trim()
}

/** Inserts after the current selection, retaining every character of the existing note. */
internal fun insertNoteDictation(current: TextFieldValue, spoken: String?): TextFieldValue {
    val text = spoken?.trim().orEmpty()
    if (text.isEmpty()) return current
    val cursor = current.selection.max.coerceIn(0, current.text.length)
    val before = current.text.substring(0, cursor)
    val after = current.text.substring(cursor)
    val prefix = if (before.isNotEmpty() && !before.last().isWhitespace() && text.first() !in ".,;:!?…") " " else ""
    val suffix = if (after.isNotEmpty() && !after.first().isWhitespace() && after.first() !in ".,;:!?…") " " else ""
    val insertion = prefix + text + suffix
    return TextFieldValue(before + insertion + after, TextRange(cursor + insertion.length))
}
