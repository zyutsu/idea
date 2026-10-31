
package dev.zyutsu.idea.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.data.NoteCard
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.ui.common.EmptyState
import dev.zyutsu.idea.ui.common.NoteRow
import dev.zyutsu.idea.ui.theme.LocalPalette
import kotlinx.coroutines.flow.first

@Composable
fun TagRoute(repo: Repository, tag: String, onOpenNote: (String) -> Unit, onBack: () -> Unit) {
    val palette = LocalPalette.current
    val notes by repo.allNotes.collectAsState(emptyList())
    var cards by remember { mutableStateOf<List<NoteCard>>(emptyList()) }

    LaunchedEffect(tag, notes) {
        val ids = repo.observeNoteIdsForTag(tag).first().toSet()
        cards = repo.cardsFor(notes.filter { ids.contains(it.id) })
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.surfaceHigh.copy(alpha = 0.7f), palette.bg)))
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(palette.surface).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = palette.text, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.tagColors[Math.floorMod(tag.hashCode(), palette.tagColors.size)])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("#" + tag, style = MaterialTheme.typography.titleMedium, color = palette.text)
                }
                Text(cards.size.toString() + " записей", style = MaterialTheme.typography.labelSmall, color = palette.textDim)
            }
        }

        if (cards.isEmpty()) {
            EmptyState(
                title = "В теге #" + tag + " пусто",
                hint = "Добавьте #" + tag + " в любую заметку",
                accent = palette.accent,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cards, key = { it.id }) { c ->
                    NoteRow(card = c, palette = palette, onClick = { onOpenNote(c.id) })
                }
            }
        }
    }
}

