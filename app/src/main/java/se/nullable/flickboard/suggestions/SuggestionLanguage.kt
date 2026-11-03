package se.nullable.flickboard.suggestions

import se.nullable.flickboard.ui.Labeled

enum class SuggestionLanguage(override val label: String, val code: String) : Labeled {
    Polish("Polski", "pl"),
    English("English", "en"),
}
