package se.nullable.flickboard

import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import se.nullable.flickboard.suggestions.SuggestionLanguage
import se.nullable.flickboard.suggestions.SuggestionRepository
import se.nullable.flickboard.suggestions.SuggestionSession

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SuggestionEditorCompatibilityTest {
    private val context = RuntimeEnvironment.getApplication()
    private val repository = SuggestionRepository(context)

    @After fun cleanup() {
        runBlocking { repository.clear() }
    }

    private fun connection(beforeUnavailable: Boolean = false, afterUnavailable: Boolean = false,
                           extractedAvailable: Boolean = false, text: String = "gura"): BaseInputConnection {
        val buffer = SpannableStringBuilder(text)
        Selection.setSelection(buffer, buffer.length)
        return object : BaseInputConnection(View(context), true) {
            override fun getEditable(): Editable = buffer
            override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence? =
                if (beforeUnavailable) null else super.getTextBeforeCursor(length, flags)
            override fun getTextAfterCursor(length: Int, flags: Int): CharSequence? =
                if (afterUnavailable) null else super.getTextAfterCursor(length, flags)
            override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? =
                if (!extractedAvailable) null else ExtractedText().also {
                    it.text = buffer.toString()
                    it.selectionStart = Selection.getSelectionStart(buffer)
                    it.selectionEnd = Selection.getSelectionEnd(buffer)
                    it.partialStartOffset = -1
                    it.partialEndOffset = -1
                }
        }
    }

    private fun TestScope.session(input: BaseInputConnection, inputType: Int, options: Int = 0): SuggestionSession =
        SuggestionSession(input, EditorInfo().also { it.inputType = inputType; it.imeOptions = options }, repository, backgroundScope)
            .also { it.configure(true, false, SuggestionLanguage.Polish) }

    private suspend fun TestScope.awaitCorrection(session: SuggestionSession) {
        backgroundScope.coroutineContext.job.children.toList().joinAll()
        assertTrue("Expected góra in suggestions, got ${session.words.value}", "góra" in session.words.value)
    }

    @Test fun searchFieldWithNoSuggestionsFlagOffersAndInsertsCorrection() = runTest {
        val input = connection()
        val session = session(input, InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_FILTER or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        assertTrue(session.visible.value)
        awaitCorrection(session)
        session.accept("góra")
        assertEquals("góra ", input.editable.toString())
    }

    @Test fun browserAddressBarCanCompleteTextSearchQueries() = runTest {
        val session = session(connection(), InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        assertTrue(session.visible.value)
        awaitCorrection(session)
    }

    @Test fun editorWithoutTextAfterCursorStillOffersAndInsertsCorrection() = runTest {
        val input = connection(afterUnavailable = true)
        val session = session(input, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT)
        awaitCorrection(session)
        session.accept("góra")
        assertEquals("góra ", input.editable.toString())
    }

    @Test fun extractedTextCanSupplyMissingSurroundingText() = runTest {
        val input = connection(beforeUnavailable = true, afterUnavailable = true, extractedAvailable = true)
        val session = session(input, InputType.TYPE_CLASS_TEXT)
        awaitCorrection(session)
        session.accept("góra")
        assertEquals("góra ", input.editable.toString())
    }

    @Test fun passwordFlagStillSuppressesSuggestions() = runTest {
        val session = session(connection(), InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        assertFalse(session.visible.value)
        assertTrue(session.words.value.isEmpty())
    }
    @Test fun tappingCurrentWordSavesItWithAutomaticLearningDisabled() = runTest {
        val input = connection(text = "FlickZZboard")
        val session = session(input, InputType.TYPE_CLASS_TEXT)
        backgroundScope.coroutineContext.job.children.toList().joinAll()
        assertEquals("FlickZZboard", session.words.value.first())
        assertTrue(repository.savedWords(SuggestionLanguage.Polish).isEmpty())
        session.accept(session.words.value.first())
        backgroundScope.coroutineContext.job.children.toList().joinAll()
        assertEquals("FlickZZboard ", input.editable.toString())
        assertEquals(listOf("FlickZZboard"), repository.savedWords(SuggestionLanguage.Polish))
        assertTrue(repository.savedWords(SuggestionLanguage.English).isEmpty())
        assertEquals(listOf("FlickZZboard"), SuggestionRepository(context).savedWords(SuggestionLanguage.Polish))
        val persisted = kotlinx.serialization.json.Json.decodeFromString<se.nullable.flickboard.suggestions.LearnedWords>(
            java.io.File(context.noBackupFilesDir, "learned-words-pl.json").readText())
        assertEquals("FlickZZboard", persisted.savedWords["flickzzboard"])
    }

    @Test fun privateEditorDoesNotSaveCurrentWord() = runTest {
        val session = session(connection(text = "FlickZZboard"), InputType.TYPE_CLASS_TEXT,
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        backgroundScope.coroutineContext.job.children.toList().joinAll()
        session.accept(session.words.value.first())
        backgroundScope.coroutineContext.job.children.toList().joinAll()
        assertTrue(repository.savedWords(SuggestionLanguage.Polish).isEmpty())
    }

    @Test fun currentWordIncludesSuffixWhenEditingInMiddle() = runTest {
        val input = connection(text = "FlickZZboard")
        Selection.setSelection(input.editable, 5)
        val session = session(input, InputType.TYPE_CLASS_TEXT)
        backgroundScope.coroutineContext.job.children.toList().joinAll()
        assertEquals("FlickZZboard", session.words.value.first())
        session.accept(session.words.value.first())
        backgroundScope.coroutineContext.job.children.toList().joinAll()
        assertEquals("FlickZZboard ", input.editable.toString())
    }

}
