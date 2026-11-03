package se.nullable.flickboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionEditTest {
    @Test fun correctionReplacesTypedWordAndAddsSpace() {
        assertEquals(SuggestionEdit(4, 0, "góra "), SuggestionEdit.create("to gura", "", "góra"))
    }

    @Test fun completionReplacesSuffixInMiddleOfWord() {
        assertEquals(SuggestionEdit(2, 3, "góra "), SuggestionEdit.create("to gu", "ra jest", "góra"))
    }

    @Test fun existingSpaceIsConsumedRatherThanDuplicated() {
        assertEquals(SuggestionEdit(4, 1, "góra "), SuggestionEdit.create("gura", " jest", "góra"))
    }

    @Test fun punctuationStaysAttachedToCorrectedWord() {
        assertEquals(SuggestionEdit(4, 0, "góra"), SuggestionEdit.create("gura", ",", "góra"))
    }

    @Test fun nextWordDoesNotDeletePreviousWord() {
        assertEquals(SuggestionEdit(0, 0, "domu "), SuggestionEdit.create("wracam do ", "", "domu"))
    }
}
