package se.nullable.flickboard

import android.text.Editable
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.InputType
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import se.nullable.flickboard.suggestions.SuggestionLanguage
import se.nullable.flickboard.suggestions.SuggestionRepository
import se.nullable.flickboard.suggestions.SuggestionSession

class SuggestionSessionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val repository = SuggestionRepository.get(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)

    @After fun cleanup() {
        scope.cancel()
        runBlocking { repository.clear() }
    }

    private fun connection(text: String, cursor: Int = text.length): BaseInputConnection {
        val buffer = SpannableStringBuilder(text)
        Selection.setSelection(buffer, cursor)
        return object : BaseInputConnection(View(context), true) {
            override fun getEditable(): Editable = buffer
        }
    }

    private fun session(connection: BaseInputConnection, inputType: Int = InputType.TYPE_CLASS_TEXT,
                        options: Int = 0, learning: Boolean = false): SuggestionSession {
        return SuggestionSession(connection, EditorInfo().also {
            it.inputType = inputType
            it.imeOptions = options
        }, repository, scope).also { it.configure(true, learning, SuggestionLanguage.Polish) }
    }

    private fun awaitSuggestions(session: SuggestionSession) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            var ready = false
            main { ready = session.words.value.isNotEmpty() }
            if (ready) return
            Thread.sleep(30)
        }
        fail("Suggestions did not become available")
    }

    @Test fun acceptsCorrectionInsideWordAndLeavesCursorAfterSpace() {
        lateinit var session: SuggestionSession
        lateinit var input: BaseInputConnection
        main {
            input = connection("to gura jest", 5)
            session = session(input)
        }
        awaitSuggestions(session)
        main {
            // At "gu|ra", choose a completion offered by the live model.
            val word = session.words.value.first()
            session.accept(word)
            assertEquals("to $word jest", input.editable.toString())
            assertEquals(3 + word.length + 1, Selection.getSelectionStart(input.editable))
        }
    }

    @Test fun rejectsStaleSuggestionAfterCursorMoves() {
        lateinit var session: SuggestionSession
        lateinit var input: BaseInputConnection
        main { input = connection("gura"); session = session(input) }
        awaitSuggestions(session)
        main {
            val word = session.words.value.first()
            Selection.setSelection(input.editable, 0)
            session.accept(word)
            assertEquals("gura", input.editable.toString())
        }
    }

    @Test fun passwordsNeverReadOrLearnWords() {
        main {
            val session = session(connection("supersecret"),
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, learning = true)
            session.prepareTyping(" ").invoke()
            assertFalse(session.visible.value)
            assertTrue(session.words.value.isEmpty())
        }
    }

    @Test fun noPersonalizedLearningFlagPreventsSaving() {
        runBlocking { repository.clear() }
        main {
            val input = connection("niespotykanywyraz")
            val session = session(input, options = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, learning = true)
            val committed = session.prepareTyping(" ")
            input.commitText(" ", 1)
            committed()
        }
        val words = runBlocking { repository.suggest(SuggestionLanguage.Polish,
            se.nullable.flickboard.suggestions.WordContext.from("niespotykanywyraz")) }
        assertFalse("niespotykanywyraz" in words)
    }
}
