package se.nullable.flickboard

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import se.nullable.flickboard.suggestions.SuggestionLanguage
import se.nullable.flickboard.ui.AppSettings
import se.nullable.flickboard.ui.AppSettingsProvider
import se.nullable.flickboard.ui.LetterLayerOption
import se.nullable.flickboard.ui.LocalAppSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SuggestionLayoutChangeTest {
    @get:Rule val compose = createComposeRule()

    private fun settings(layouts: List<LetterLayerOption>, activeIndex: Int = 0): () -> AppSettings {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("layout-language-test", Context.MODE_PRIVATE)
        prefs.edit().clear().putInt("activeLetterLayerIndex", activeIndex).commit()
        lateinit var settings: AppSettings
        compose.setContent {
            AppSettingsProvider(prefs) {
                val current = LocalAppSettings.current
                val layout = current.activeLetterLayer
                val language = current.currentSuggestionLanguage
                SideEffect { settings = current }
                Text("${layout.name}:${language.code}")
            }
        }
        compose.runOnIdle { settings.letterLayers.currentValue = layouts }
        compose.waitForIdle()
        return { settings }
    }

    @Test fun switchingActiveLayoutChangesLanguageWithoutChangingManualPreference() {
        val settings = settings(listOf(LetterLayerOption.PolishRmitura, LetterLayerOption.English, LetterLayerOption.EnglishThumbKey))
        compose.onNodeWithText("PolishRmitura:pl").assertExists()
        compose.runOnIdle { settings().activeLetterLayerIndex.currentValue = 1 }
        compose.onNodeWithText("English:en").assertExists()
        compose.runOnIdle { settings().activeLetterLayerIndex.currentValue = 2 }
        compose.onNodeWithText("EnglishThumbKey:en").assertExists()
        compose.runOnIdle {
            assertEquals(SuggestionLanguage.Polish, settings().suggestionLanguage.currentValue)
            settings().activeLetterLayerIndex.currentValue = 0
        }
        compose.onNodeWithText("PolishRmitura:pl").assertExists()
    }

    @Test fun replacingLayoutListAndInvalidIndexUseTheDisplayedLayout() {
        val settings = settings(listOf(LetterLayerOption.English), activeIndex = 5)
        compose.onNodeWithText("English:en").assertExists()
        compose.runOnIdle { settings().letterLayers.currentValue = listOf(LetterLayerOption.PolishRmitura) }
        compose.onNodeWithText("PolishRmitura:pl").assertExists()
    }

    @Test fun manualOverrideAndUnsupportedLayoutUseSavedLanguage() {
        val settings = settings(listOf(LetterLayerOption.English, LetterLayerOption.French))
        compose.onNodeWithText("English:en").assertExists()
        compose.runOnIdle { settings().followLetterLayout.currentValue = false }
        compose.onNodeWithText("English:pl").assertExists()
        compose.runOnIdle {
            settings().followLetterLayout.currentValue = true
            settings().activeLetterLayerIndex.currentValue = 1
        }
        compose.onNodeWithText("French:pl").assertExists()
        compose.runOnIdle { settings().suggestionLanguage.currentValue = SuggestionLanguage.English }
        compose.onNodeWithText("French:en").assertExists()
    }
}
