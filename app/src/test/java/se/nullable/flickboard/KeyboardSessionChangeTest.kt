package se.nullable.flickboard

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.nullable.flickboard.model.Action
import se.nullable.flickboard.model.Direction
import se.nullable.flickboard.model.KeyM
import se.nullable.flickboard.model.ModifierState
import se.nullable.flickboard.model.TextDirection
import se.nullable.flickboard.model.layouts.EN_MESSAGEASE
import se.nullable.flickboard.ui.FlickBoardParent
import se.nullable.flickboard.ui.Key
import se.nullable.flickboard.ui.Keyboard
import se.nullable.flickboard.ui.OnAction

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardSessionChangeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun resizedKeyDispatchesTapsAndFlicksToNewEditor() {
        val oldEditor = mutableListOf<Action>()
        val newEditor = mutableListOf<Action>()
        val handler = mutableStateOf(OnAction { action, _, _ -> oldEditor.add(action); true })
        val width = mutableStateOf(240.dp)
        val key = KeyM(actions = mapOf(Direction.CENTER to Action.Text("a"), Direction.RIGHT to Action.Text("b")))
        compose.setContent {
            FlickBoardParent {
                Key(key, handler.value, modifierState = null,
                    modifier = Modifier.width(width.value).testTag("key"),
                    layoutTextDirection = TextDirection.LeftToRight)
            }
        }
        compose.onNodeWithTag("key").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(listOf(Action.Text("a")), oldEditor)
            // Returning from settings changes the editor while retaining the same key.
            width.value = 160.dp
            handler.value = OnAction { action, _, _ -> newEditor.add(action); true }
        }
        compose.onNodeWithTag("key").performTouchInput { click() }
        compose.onNodeWithTag("key").performTouchInput {
            swipe(center, centerRight, durationMillis = 150)
        }
        compose.runOnIdle {
            assertEquals(listOf(Action.Text("a")), oldEditor)
            assertEquals(listOf(Action.Text("a"), Action.Text("b")), newEditor)
        }
    }

    @Test fun newEditorReceivesModifierStateWithoutAnotherKeyPress() {
        val oldEditor = mutableListOf<ModifierState>()
        val newEditor = mutableListOf<ModifierState>()
        val listener = mutableStateOf<(ModifierState) -> Unit>({ oldEditor.add(it) })
        compose.setContent {
            FlickBoardParent {
                Keyboard(EN_MESSAGEASE, onAction = OnAction { _, _, _ -> true },
                    onModifierStateUpdated = listener.value)
            }
        }
        compose.runOnIdle {
            assertEquals(listOf(ModifierState()), oldEditor)
            listener.value = { newEditor.add(it) }
        }
        compose.runOnIdle { assertEquals(listOf(ModifierState()), newEditor) }
    }
}
