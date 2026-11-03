package se.nullable.flickboard.suggestions

import android.text.InputType
import org.junit.Assert.*
import org.junit.Test

class SuggestionPolicyTest {
    @Test fun ordinaryTextSupportsSuggestions() {
        assertTrue(supportsSuggestions(InputType.TYPE_CLASS_TEXT))
        assertTrue(supportsSuggestions(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
    }

    @Test fun searchAndWebEditorsSupportSuggestionsEvenWhenAppDisablesItsOwnCompletion() {
        for (variation in listOf(
            InputType.TYPE_TEXT_VARIATION_NORMAL,
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT,
            InputType.TYPE_TEXT_VARIATION_FILTER,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE,
        )) assertTrue(supportsSuggestions(InputType.TYPE_CLASS_TEXT or variation or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS))
    }

    @Test fun sensitiveOrNonTextFieldsNeverUseSuggestions() {
        for (variation in listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        )) assertFalse(supportsSuggestions(InputType.TYPE_CLASS_TEXT or variation))
        assertFalse(supportsSuggestions(InputType.TYPE_CLASS_NUMBER))
        assertFalse(supportsSuggestions(InputType.TYPE_CLASS_PHONE))
        assertFalse(supportsSuggestions(InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS))
    }
}
