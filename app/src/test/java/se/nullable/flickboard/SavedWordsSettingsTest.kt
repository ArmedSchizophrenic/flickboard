package se.nullable.flickboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import se.nullable.flickboard.suggestions.SuggestionLanguage
import se.nullable.flickboard.suggestions.SuggestionRepository
import se.nullable.flickboard.ui.AppSettingsProvider
import se.nullable.flickboard.ui.SavedWordsSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavedWordsSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val repository get() = SuggestionRepository.get(RuntimeEnvironment.getApplication())

    @After fun cleanup() {
        runBlocking { repository.clear() }
    }

    private fun awaitWord(word: String) {
        compose.waitUntil(5000) { compose.onAllNodesWithText(word).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun listsSavedWordsAndDeletesOnlyFromSelectedLanguage() {
        runBlocking {
            repository.clear()
            repository.addWord(SuggestionLanguage.Polish, "FlickZZboard")
            repository.addWord(SuggestionLanguage.English, "WorkZZword")
        }
        compose.setContent {
            AppSettingsProvider { SavedWordsSettings(Modifier.fillMaxSize()) }
        }
        awaitWord("WorkZZword")
        compose.onNodeWithText("Polski").performClick()
        awaitWord("FlickZZboard")
        compose.onNodeWithContentDescription("Delete FlickZZboard").performClick()
        awaitWord("No saved words.")
        runBlocking {
            assertTrue(repository.savedWords(SuggestionLanguage.Polish).isEmpty())
            assertEquals(listOf("WorkZZword"), repository.savedWords(SuggestionLanguage.English))
            repository.addWord(SuggestionLanguage.Polish, "NoweZZslowo")
        }
        awaitWord("NoweZZslowo")
        compose.onNodeWithText("English").performClick()
        awaitWord("WorkZZword")
    }
}
