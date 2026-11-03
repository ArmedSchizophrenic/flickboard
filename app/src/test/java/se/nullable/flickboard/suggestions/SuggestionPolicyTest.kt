package se.nullable.flickboard.suggestions

import android.text.InputType
import org.junit.Assert.*
import org.junit.Test

class SuggestionPolicyTest {
    @Test fun ordinaryTextSupportsSuggestions() {
        assertTrue(supportsSuggestions(InputType.TYPE_CLASS_TEXT))
        assertTrue(supportsSuggestions(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
    }

    @Test fun sensitiveOrNonTextFieldsNeverUseSuggestions() {
        for (variation in listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
        )) assertFalse(supportsSuggestions(InputType.TYPE_CLASS_TEXT or variation))
        assertFalse(supportsSuggestions(InputType.TYPE_CLASS_NUMBER))
        assertFalse(supportsSuggestions(InputType.TYPE_CLASS_PHONE))
        assertFalse(supportsSuggestions(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS))
    }
}
