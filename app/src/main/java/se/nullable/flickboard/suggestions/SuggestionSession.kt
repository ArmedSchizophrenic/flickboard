package se.nullable.flickboard.suggestions

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Reads editor state on the main thread; never learns text merely by moving the cursor. */
class SuggestionSession(
    private val connection: InputConnection,
    editor: EditorInfo?,
    private val repository: SuggestionRepository,
    private val scope: CoroutineScope,
) {
    val words = mutableStateOf<List<String>>(emptyList())
    val visible = mutableStateOf(false)
    private val allowed = editor != null && supportsSuggestions(editor.inputType)
    private val learningAllowed = editor != null &&
        editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0
    private var enabled = false
    private var learn = false
    private var language = SuggestionLanguage.Polish
    private var refreshJob: Job? = null
    private var displayedAt: Snapshot? = null
    private var requestedAt: Snapshot? = null
    private var closed = false

    private data class Snapshot(val before: String, val after: String)
    private fun snapshot(): Snapshot? {
        if (!enabled || closed || !connection.getSelectedText(0).isNullOrEmpty()) return null
        val before = connection.getTextBeforeCursor(256, 0)?.toString() ?: return null
        val after = connection.getTextAfterCursor(64, 0)?.toString() ?: return null
        return Snapshot(before, after)
    }

    fun configure(enabled: Boolean, learning: Boolean, language: SuggestionLanguage) {
        val active = enabled && allowed
        if (this.enabled == active && this.learn == learning && this.language == language) return
        this.enabled = active
        visible.value = active
        this.learn = learning
        this.language = language
        displayedAt = null
        requestedAt = null
        refresh()
    }

    fun refresh() {
        val current = snapshot()
        if (current != null && (current == displayedAt ||
                (current == requestedAt && refreshJob?.isActive == true))) return
        refreshJob?.cancel()
        displayedAt = null
        words.value = emptyList()
        requestedAt = current
        val snapshot = current ?: return
        val selectedLanguage = language
        refreshJob = scope.launch {
            delay(35)
            val suggestions = repository.suggest(selectedLanguage, WordContext.from(snapshot.before))
            if (snapshot() == snapshot && language == selectedLanguage) {
                displayedAt = snapshot
                words.value = suggestions
            }
        }
    }

    /** Called only for a delimiter the user actually types, before committing it. */
    fun prepareTyping(text: String): () -> Unit {
        if (text.firstOrNull()?.let(::isWordCharacter) != false) return {}
        val context = snapshot()?.before?.let(WordContext::from) ?: return {}
        return { learn(context.prefix, context.previous) }
    }

    private fun learn(word: String, previous: List<String>) {
        if (!enabled || !learn || !learningAllowed || word.isEmpty()) return
        val selectedLanguage = language
        scope.launch { repository.learn(selectedLanguage, word, previous) }
    }

    fun accept(word: String) {
        val at = displayedAt ?: return
        if (word !in words.value || snapshot() != at) {
            refresh()
            return
        }
        val context = WordContext.from(at.before)
        val edit = SuggestionEdit.create(at.before, at.after, word)
        connection.beginBatchEdit()
        try {
            if (connection.deleteSurroundingText(edit.before, edit.after)) {
                if (connection.commitText(edit.text, 1)) {
                    learn(word, context.previous)
                }
            }
        } finally {
            connection.endBatchEdit()
        }
        refresh()
    }

    fun close() {
        closed = true
        refreshJob?.cancel()
        words.value = emptyList()
    }
}

internal fun supportsSuggestions(inputType: Int): Boolean {
    if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
    if (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false
    return inputType and InputType.TYPE_MASK_VARIATION !in setOf(
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_URI,
        InputType.TYPE_TEXT_VARIATION_FILTER,
    )
}
