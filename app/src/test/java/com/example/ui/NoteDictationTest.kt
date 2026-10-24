package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class NoteDictationTest {
    @Test fun requestUsesThePhoneSpeechActivityInTurkishAndPrefersOfflineRecognition() {
        val request = noteDictationIntent()
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, request.action)
        assertEquals("tr-TR", request.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, request.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
        assertTrue(request.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
    }

    @Test fun recognisedTextIsInsertedWithoutReplacingTheExistingSelectionOrNote() {
        val original = TextFieldValue("Birinci ikinci", TextRange(0, 7))
        val result = insertNoteDictation(original, "sesli not")
        assertEquals("Birinci sesli not ikinci", result.text)
        assertEquals(TextRange(17), result.selection)
    }

    @Test fun cancelledOrEmptyRecognitionLeavesTheOriginalNoteAndSelectionIntact() {
        val original = TextFieldValue("Notum aynı kalır", TextRange(5))
        val data = Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("istenmeyen"))
        assertNull(recognizedNoteDictation(Activity.RESULT_CANCELED, data))
        assertEquals(original, insertNoteDictation(original, recognizedNoteDictation(Activity.RESULT_CANCELED, data)))
        assertEquals(original, insertNoteDictation(original, "   "))
        assertNull(recognizedNoteDictation(Activity.RESULT_OK, null))
    }

    @Test fun resultKeepsSpeechPunctuationAndInsertsBeforeExistingPunctuationCleanly() {
        val data = Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("", "  yeni söz  "))
        assertEquals("yeni söz", recognizedNoteDictation(Activity.RESULT_OK, data))
        assertEquals("Not yeni söz.", insertNoteDictation(TextFieldValue("Not.", TextRange(3)), "yeni söz").text)
    }
}
