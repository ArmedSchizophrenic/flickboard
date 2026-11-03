package se.nullable.flickboard

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.SpannableStringBuilder
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import se.nullable.flickboard.model.TextDirection
import se.nullable.flickboard.model.layouts.SPACE
import se.nullable.flickboard.suggestions.SuggestionRepository
import se.nullable.flickboard.suggestions.SuggestionSession
import se.nullable.flickboard.ui.FlickBoardParent
import se.nullable.flickboard.ui.Key
import se.nullable.flickboard.ui.LocalAppSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpaceLanguageGestureTest {
    @get:Rule val compose = createComposeRule()

    @Test fun downSwipeChangesOnlySuggestionLanguageWhileTapStillTypesSpace() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("space-language-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val text = SpannableStringBuilder()
        val snackbar = SnackbarHostState()
        Selection.setSelection(text, 0)
        val connection = object : BaseInputConnection(View(context), true) {
            override fun getEditable(): Editable = text
        }
        compose.setContent {
            FlickBoardParent(prefs) {
                val settings = LocalAppSettings.current
                val scope = rememberCoroutineScope()
                val session = remember {
                    val editor = EditorInfo().also { it.inputType = InputType.TYPE_CLASS_TEXT }
                    InputSession(connection, editor,
                        SuggestionSession(connection, editor, SuggestionRepository.get(context), scope))
                }
                val handler = session.actionHandler(snackbar, {})
                Column {
                    Text("${settings.activeLetterLayer.name}:${settings.currentSuggestionLanguage.code}")
                    Key(SPACE, handler, modifierState = null,
                        modifier = Modifier.width(280.dp).testTag("space"),
                        layoutTextDirection = TextDirection.LeftToRight)
                }
            }
        }
        compose.onNodeWithText("English:en").assertExists()
        assertTrue(compose.onAllNodesWithText("EN").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("PL/EN").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag("space").performTouchInput {
            swipe(center, Offset(center.x, height - 2f), durationMillis = 150)
        }
        compose.onNodeWithText("English:pl").assertExists()
        assertTrue(compose.onAllNodesWithText("PL").fetchSemanticsNodes().isNotEmpty())
        compose.runOnIdle {
            assertFalse(prefs.getBoolean("followLetterLayout", true))
            assertEquals("Polish", prefs.getString("suggestionLanguage", null))
            assertEquals("", text.toString())
            assertNull(snackbar.currentSnackbarData)
        }
        compose.onNodeWithTag("space").performTouchInput {
            swipe(center, Offset(center.x, height - 2f), durationMillis = 150)
        }
        compose.onNodeWithText("English:en").assertExists()
        assertTrue(compose.onAllNodesWithText("EN").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("space").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(" ", text.toString())
            assertNull(snackbar.currentSnackbarData)
        }
    }
}
