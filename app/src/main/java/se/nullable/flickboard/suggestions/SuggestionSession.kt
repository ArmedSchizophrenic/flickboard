package se.nullable.flickboard.suggestions

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
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
    private val personalizationAllowed = editor != null &&
        editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0
    private val learningAllowed = personalizationAllowed && editor != null &&
        editor.inputType and InputType.TYPE_MASK_VARIATION != InputType.TYPE_TEXT_VARIATION_URI
    private var enabled = false
    private var learn = false
    private var language = SuggestionLanguage.Polish
    private var refreshJob: Job? = null
    private var displayedAt: Snapshot? = null
    private var requestedAt: Snapshot? = null
    private var closed = false
    private var modelRevision = -1L

    private data class Snapshot(val before: String, val after: String)
    private fun snapshot(): Snapshot? {
        if (!enabled || closed || !connection.getSelectedText(0).isNullOrEmpty()) return null
        val before = connection.getTextBeforeCursor(256, 0)?.toString()
        val after = connection.getTextAfterCursor(64, 0)?.toString()
        if (before != null && after != null) return Snapshot(before, after)
        // Web/custom editors may omit one surrounding-text query while still
        // supporting extracted text. An unavailable suffix must not hide all
        // suggestions when the prefix is available.
        val extracted = connection.getExtractedText(ExtractedTextRequest().also {
            it.hintMaxChars = 512
            it.hintMaxLines = 8
        }, 0)
        val text = extracted?.text?.toString()
        if (extracted != null && text != null && extracted.partialStartOffset < 0 &&
            extracted.selectionStart in 0..text.length &&
            extracted.selectionEnd in 0..text.length) {
            if (extracted.selectionStart != extracted.selectionEnd) return null
            val cursor = extracted.selectionStart
            return Snapshot(before ?: text.take(cursor).takeLast(256),
                after ?: text.drop(cursor).take(64))
        }
        return Snapshot(before ?: return null, after ?: "")
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
        if (current != null && modelRevision == repository.revision.value && (current == displayedAt ||
                (current == requestedAt && refreshJob?.isActive == true))) return
        refreshJob?.cancel()
        displayedAt = null
        words.value = emptyList()
        requestedAt = current
        modelRevision = repository.revision.value
        val snapshot = current ?: return
        val selectedLanguage = language
        refreshJob = scope.launch {
            delay(35)
            val context = WordContext.from(snapshot.before)
            val suggestions = repository.suggest(selectedLanguage, context,
                context.prefix + snapshot.after.takeWhile(::isWordCharacter))
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
                    val currentWord = context.prefix + at.after.takeWhile(::isWordCharacter)
                    if (context.prefix.isNotEmpty() && word == currentWord && personalizationAllowed) {
                        val selectedLanguage = language
                        scope.launch { repository.addWord(selectedLanguage, word) }
                    }
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
    // Search boxes and browser address bars often set NO_SUGGESTIONS or FILTER.
    // The user's keyboard preference controls suggestions in ordinary text;
    // password/address sensitivity is handled separately below.
    return inputType and InputType.TYPE_MASK_VARIATION !in setOf(
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
    )
}
