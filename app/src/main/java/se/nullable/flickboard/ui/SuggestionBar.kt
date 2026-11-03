package se.nullable.flickboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import se.nullable.flickboard.ui.theme.LocalKeyboardTheme

@Composable
fun SuggestionBar(words: List<String>, onSelect: (String) -> Unit) {
    val theme = LocalKeyboardTheme.current
    Surface(color = theme.keySurfaceColour, contentColor = theme.keyIndicatorColour) {
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            words.take(3).forEach { word ->
                Box(
                    Modifier.weight(1f).height(48.dp)
                        .clickable(role = Role.Button) { onSelect(word) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(word, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
