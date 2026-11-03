package se.nullable.flickboard.suggestions

/** Replaces the whole word at the cursor and leaves one space for the next word. */
internal data class SuggestionEdit(val before: Int, val after: Int, val text: String) {
    companion object {
        fun create(beforeCursor: String, afterCursor: String, word: String): SuggestionEdit {
            val prefixLength = beforeCursor.takeLastWhile(::isWordCharacter).length
            val suffixLength = afterCursor.takeWhile(::isWordCharacter).length
            val remaining = afterCursor.drop(suffixLength)
            val consumeSpace = if (remaining.startsWith(" ")) 1 else 0
            val appendSpace = remaining.isEmpty() || remaining.first().isWhitespace()
            return SuggestionEdit(prefixLength, suffixLength + consumeSpace,
                word + if (appendSpace) " " else "")
        }
    }
}
