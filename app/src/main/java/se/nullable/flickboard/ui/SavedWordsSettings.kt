package se.nullable.flickboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.nullable.flickboard.R
import se.nullable.flickboard.suggestions.SuggestionLanguage
import se.nullable.flickboard.suggestions.SuggestionRepository

@Composable
fun SavedWordsSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { SuggestionRepository.get(context) }
    val scope = rememberCoroutineScope()
    val activeLanguage = LocalAppSettings.current.currentSuggestionLanguage
    var language by remember { mutableStateOf(activeLanguage) }
    var words by remember { mutableStateOf<List<String>?>(null) }
    val revision by repository.revision.collectAsState()
    LaunchedEffect(language, revision) {
        words = null
        words = repository.savedWords(language)
    }
    Column(modifier.padding(16.dp)) {
        Text("Tap the word you are typing in the first suggestion slot to save it here.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionLanguage.entries.forEach { option ->
                TextButton(onClick = { language = option }, enabled = language != option) {
                    Text(option.label)
                }
            }
        }
        Text("${language.label} — personal dictionary", Modifier.padding(vertical = 8.dp))
        val displayedWords = words
        when {
            displayedWords == null -> Text("Loading…")
            displayedWords.isEmpty() -> Text("No saved words.")
            else -> LazyColumn(Modifier.weight(1f)) {
                items(displayedWords, key = { it }) { word ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(word, Modifier.weight(1f))
                        IconButton(onClick = {
                            val selectedLanguage = language
                            scope.launch { repository.removeWord(selectedLanguage, word) }
                        }) {
                            Icon(painterResource(R.drawable.baseline_clear_24), "Delete $word")
                        }
                    }
                }
            }
        }
    }
}
