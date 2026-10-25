package com.example.ui

import android.speech.RecognizerIntent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class NoteDictationTest {
    @Test fun inlineRequestUsesTurkishPartialsAndExplicitOfflinePreference() {
        val request = noteDictationIntent()
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, request.action)
        assertEquals("tr-TR", request.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, request.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
        assertTrue(request.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
        assertTrue(request.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        assertEquals(false, noteDictationIntent(false).getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
    }

    @Test fun recognisedTextIsInsertedWithoutReplacingTheExistingSelectionOrNote() {
        val original = TextFieldValue("Birinci ikinci", TextRange(0, 7))
        val result = insertNoteDictation(original, "sesli not")
        assertEquals("Birinci sesli not ikinci", result.text)
        assertEquals(TextRange(17), result.selection)
    }

    @Test fun stoppingBeforeWordsArriveLeavesTheOriginalNoteAndSelectionIntact() {
        val original = TextFieldValue("Notum aynı kalır", TextRange(5))
        val session = NoteDictationSessionBuffer()
        session.begin(original)
        assertEquals(original, session.finish(original))
        assertEquals(original, session.partial(original, "geç gelen söz"))
        assertEquals(original, insertNoteDictation(original, "   "))
    }

    @Test fun resultKeepsSpeechPunctuationAndInsertsBeforeExistingPunctuationCleanly() {
        assertEquals("Not yeni söz.", insertNoteDictation(TextFieldValue("Not.", TextRange(3)), "yeni söz").text)
    }

    @Test fun revisedPartialsAndFinalReplaceOneInsertionRatherThanAppendingEachHypothesis() {
        val session = NoteDictationSessionBuffer()
        var value = TextFieldValue("Eski not", TextRange(8))
        session.begin(value)
        value = session.partial(value, "yeni")
        assertEquals("Eski not yeni", value.text)
        value = session.partial(value, "yeni söz")
        assertEquals("Eski not yeni söz", value.text)
        value = session.finish(value, "yeni söz.")
        assertEquals("Eski not yeni söz.", value.text)
        assertEquals(value, session.finish(value, "yeni söz."))
        assertEquals(value, session.partial(value, "geç gelen"))
    }

    @Test fun emptyFinalAndStopPreserveTheLastPartialAndNextUtteranceStartsFresh() {
        val session = NoteDictationSessionBuffer()
        var value = TextFieldValue("Başlık", TextRange(6))
        session.begin(value)
        value = session.partial(value, "ilk cümle")
        value = session.finish(value, " ")
        assertEquals("Başlık ilk cümle", value.text)
        session.begin(value)
        value = session.finish(value, "ikinci cümle")
        assertEquals("Başlık ilk cümle ikinci cümle", value.text)
    }

    @Test fun manualTextChangesDuringRecognitionAreNeverOverwrittenOrDuplicated() {
        val session = NoteDictationSessionBuffer()
        var value = TextFieldValue("Not", TextRange(3))
        session.begin(value)
        value = session.partial(value, "bir söz")
        val edited = TextFieldValue(value.text + " elle eklenen", TextRange(2))
        assertEquals(edited, session.partial(edited, "bir söz daha"))
        assertEquals(edited, session.finish(edited, "bir söz daha."))
    }

    @Test fun identicalFinalKeepsTheUsersCurrentCursorAndExistingSelectionIsNotDeleted() {
        val session = NoteDictationSessionBuffer()
        var value = TextFieldValue("Birinci ikinci", TextRange(0, 7))
        session.begin(value)
        value = session.partial(value, "sesli not")
        val movedCursor = value.copy(selection = TextRange(1))
        assertEquals("Birinci sesli not ikinci", value.text)
        assertEquals(movedCursor, session.finish(movedCursor, "sesli not"))
    }
}
