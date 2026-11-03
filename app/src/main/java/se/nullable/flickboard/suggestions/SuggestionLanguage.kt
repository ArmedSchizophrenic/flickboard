package se.nullable.flickboard.suggestions

import se.nullable.flickboard.ui.Labeled
import se.nullable.flickboard.ui.LetterLayerOption

enum class SuggestionLanguage(override val label: String, val code: String) : Labeled {
    Polish("Polski", "pl"),
    English("English", "en");

    companion object {
        fun forLetterLayout(layout: LetterLayerOption): SuggestionLanguage? = when (layout) {
            LetterLayerOption.PolishRmitura -> Polish
            LetterLayerOption.English, LetterLayerOption.EnglishThumbKey,
            LetterLayerOption.GermanEnglish -> English
            else -> null
        }
    }
}
