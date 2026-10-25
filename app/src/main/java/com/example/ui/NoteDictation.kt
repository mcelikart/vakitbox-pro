package com.example.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** An inline recognition request; this intent is never launched as an external activity. */
internal fun noteDictationIntent(preferOffline: Boolean = true): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
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

/**
 * One recognition utterance. Partials and the final replace the same inserted range.
 * Already displayed words survive stop, errors and cancellation. If the user edits the text
 * during a phrase, that edit wins; the next utterance starts at the new cursor.
 */
internal class NoteDictationSessionBuffer {
    private var baseline = TextFieldValue()
    private var rendered = TextFieldValue()
    private var hypothesis = ""
    private var open = false

    fun begin(current: TextFieldValue) {
        baseline = current
        rendered = current
        hypothesis = ""
        open = true
    }

    fun partial(current: TextFieldValue, spoken: String?): TextFieldValue {
        if (!open) return current
        if (current.text != rendered.text) {
            open = false
            return current
        }
        val text = spoken?.trim().orEmpty()
        if (text.isEmpty() || text == hypothesis) return current
        hypothesis = text
        return insertNoteDictation(baseline, text).also { rendered = it }
    }

    fun finish(current: TextFieldValue, spoken: String? = null): TextFieldValue {
        val result = partial(current, spoken)
        open = false
        return result
    }
}
