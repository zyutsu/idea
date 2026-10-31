
package dev.zyutsu.idea.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.core.Day
import dev.zyutsu.idea.core.Fuzzy
import dev.zyutsu.idea.core.Markdown
import dev.zyutsu.idea.data.Note
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.data.TagCount
import dev.zyutsu.idea.ui.common.TagPill
import dev.zyutsu.idea.ui.theme.LocalPalette
import dev.zyutsu.idea.ui.theme.Palette

private data class Hit(val note: Note, val score: Int, val snippet: String)

@Composable
fun SearchRoute(repo: Repository, onOpenNote: (String) -> Unit, onOpenTag: (String) -> Unit, onBack: () -> Unit) {
    val palette = LocalPalette.current
    var query by remember { mutableStateOf(TextFieldValue("", TextRange(0))) }
    var hits by remember { mutableStateOf<List<Hit>>(emptyList()) }
    var tagHits by remember { mutableStateOf<List<TagCount>>(emptyList()) }
    val focus = remember { FocusRequester() }
    val allTags = repo.tagCounts.let { remember { mutableStateOf<List<TagCount>>(emptyList()) } }

    LaunchedEffect(Unit) {
        repo.tagCounts.collect { allTags.value = it }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    LaunchedEffect(query.text) {
        val q = query.text.trim()
        if (q.isEmpty()) {
            hits = emptyList()
            tagHits = emptyList()
            return@LaunchedEffect
        }
        val scored = repo.search(q)
        hits = scored.map { (n, s) -> Hit(n, s, Markdown.snippet(n.body, q.removePrefix("#"))) }
        tagHits = allTags.value.filter { Fuzzy.match(it.name, q.removePrefix("#")) }.take(8)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.surfaceHigh.copy(alpha = 0.7f), palette.bg)))
            .imePadding()
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
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(palette.surface)
                    .border(1.dp, palette.accent.copy(alpha = if (query.text.isEmpty()) 0.06f else 0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = palette.textDim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(9.dp))
                Box(Modifier.weight(1f)) {
                    if (query.text.isEmpty()) {
                        Text("Искать по тексту, названию, #тегам…", style = MaterialTheme.typography.bodyMedium, color = palette.textDim)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.text),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                }
                if (query.text.isNotEmpty()) {
                    Icon(Icons.Rounded.Close, "Очистить", tint = palette.textDim, modifier = Modifier.size(16.dp).clickable { query = TextFieldValue("", TextRange(0)) })
                }
            }
        }

        if (query.text.isBlank()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.Center) {
                Text("Мгновенный поиск", style = MaterialTheme.typography.titleLarge, color = palette.text)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Fuzzy-поиск по названиям, тексту, тегам и связям. Начните вводить — результаты появятся сразу.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textDim
                )
                Spacer(Modifier.height(18.dp))
                if (allTags.value.isNotEmpty()) {
                    Text("Популярные теги", style = MaterialTheme.typography.labelLarge, color = palette.textDim)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        allTags.value.take(6).forEach { tc ->
                            TagPill(tc.name, palette.tagColors[Math.floorMod(tc.name.hashCode(), palette.tagColors.size)], onClick = { onOpenTag(tc.name) })
                        }
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (tagHits.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(top = 4.dp)) {
                            Text("Теги", style = MaterialTheme.typography.labelLarge, color = palette.textDim)
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                tagHits.forEach { tc ->
                                    TagPill(tc.name, palette.tagColors[Math.floorMod(tc.name.hashCode(), palette.tagColors.size)], onClick = { onOpenTag(tc.name) })
                                }
                            }
                        }
                    }
                }
                items(hits, key = { it.note.id }) { hit ->
                    ResultRow(hit, palette) { onOpenNote(hit.note.id) }
                }
                if (hits.isEmpty() && tagHits.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Text("Ничего не найдено по «" + query.text + "»", style = MaterialTheme.typography.bodyMedium, color = palette.textDim)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(hit: Hit, palette: Palette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(palette.surface.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), shape).clickable(onClick = onClick).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hit.note.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(Day.time(hit.note.updatedAt), style = MaterialTheme.typography.labelSmall, color = palette.textDim.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(5.dp))
        Text(hit.snippet, style = MaterialTheme.typography.bodyMedium, color = palette.textDim, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

