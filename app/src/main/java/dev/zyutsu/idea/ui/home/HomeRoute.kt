
package dev.zyutsu.idea.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.core.Day
import dev.zyutsu.idea.data.NoteCard
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.data.TagCount
import dev.zyutsu.idea.ui.common.CheckInDot
import dev.zyutsu.idea.ui.common.NoteRow
import dev.zyutsu.idea.ui.common.SectionHeader
import dev.zyutsu.idea.ui.common.TagPill
import dev.zyutsu.idea.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    repo: Repository,
    onOpenNote: (String) -> Unit,
    onOpenGraph: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = dev.zyutsu.idea.ui.theme.LocalPalette.current
    val scope = rememberCoroutineScope()

    val notes by repo.allNotes.collectAsState(emptyList())
    val tagCounts by repo.tagCounts.collectAsState(emptyList())
    val favorites by repo.favorites.collectAsState(emptyList())
    val weekDays = remember { Day.last(7) }
    val checkins by repo.observeCheckins(weekDays).collectAsState(emptyList())
    var cards by remember { mutableStateOf<List<NoteCard>>(emptyList()) }
    var favCards by remember { mutableStateOf<List<NoteCard>>(emptyList()) }
    var filterTag by remember { mutableStateOf<String?>(null) }
    var showCapture by remember { mutableStateOf(false) }

    LaunchedEffect(favorites) {
        favCards = repo.cardsFor(favorites)
    }

    LaunchedEffect(notes, filterTag) {
        val visible = ArrayList<dev.zyutsu.idea.data.Note>(notes.size)
        for (n in notes) {
            if (filterTag == null || repo.tagNamesFor(n.id).contains(filterTag)) visible.add(n)
        }
        cards = repo.cardsFor(visible)
    }

    val doneByDay = checkins.associate { it.day to it.done }
    val streak = Day.streak(doneByDay.filterValues { it }.keys.toSet())

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.surfaceHigh.copy(alpha = 0.85f), palette.bg, palette.bg)))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Idea", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = palette.text)
                        Text(
                            notes.size.toString() + " записей · " + streak + " дн. подряд",
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                            color = palette.textDim
                        )
                    }
                    IconBox(palette, Icons.Outlined.AutoGraph, "Граф", onOpenGraph)
                    Spacer(Modifier.width(8.dp))
                    IconBox(palette, Icons.Outlined.Settings, "Настройки", onOpenSettings)
                }
            }

            item {
                CheckInCard(
                    days = weekDays,
                    doneByDay = doneByDay,
                    streak = streak,
                    palette = palette,
                    onToggle = { day -> scope.launch { repo.toggleCheckIn(day) } }
                )
            }

            item {
                SearchField(palette = palette, onClick = onOpenSearch)
            }

            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Идеи", color = palette.text)
                    Spacer(Modifier.weight(1f))
                }
            }

            if (tagCounts.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        item {
                            TagPill(
                                name = "все",
                                color = if (filterTag == null) palette.accent else palette.textDim,
                                selected = filterTag == null,
                                onClick = { filterTag = null }
                            )
                        }
                        items(tagCounts) { tc ->
                            TagPill(
                                name = tc.name,
                                color = palette.tagColors[Math.floorMod(tc.name.hashCode(), palette.tagColors.size)],
                                selected = filterTag == tc.name,
                                onClick = { filterTag = if (filterTag == tc.name) null else tc.name }
                            )
                        }
                    }
                }
            }

            if (favCards.isNotEmpty() && filterTag == null) {
                item { SectionHeader("Избранное", color = palette.textDim) }
                items(favCards, key = { "fav-" + it.id }) { card ->
                    NoteRow(card = card, palette = palette, onClick = { onOpenNote(card.id) })
                }
                item { SectionHeader("Все заметки", color = palette.textDim) }
            }

            if (cards.isEmpty()) {
                item {
                    dev.zyutsu.idea.ui.common.EmptyState(
                        title = if (filterTag != null) "Пусто в этом теге" else "Заметок пока нет",
                        hint = "Нажми «Записать идею» — сохранится за один тап",
                        accent = palette.accent,
                        modifier = Modifier.height(220.dp)
                    )
                }
            } else {
                items(cards, key = { it.id }) { card ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                    ) {
                        NoteRow(card = card, palette = palette, onClick = { onOpenNote(card.id) })
                    }
                }
            }
        }

        CaptureFab(
            palette = palette,
            onClick = { showCapture = true },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        AnimatedVisibility(
            visible = showCapture,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp, start = 18.dp, end = 18.dp)
        ) {
            QuickCaptureSheet(
                palette = palette,
                onSave = { text ->
                    scope.launch {
                        val id = repo.quickCapture(text)
                        showCapture = false
                        if (id.isNotEmpty()) onOpenNote(id)
                    }
                },
                onDismiss = { showCapture = false }
            )
        }
    }
}

@Composable
private fun IconBox(palette: Palette, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(42.dp).clip(shape).background(palette.surface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = palette.text.copy(alpha = 0.8f), modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun CheckInCard(days: List<String>, doneByDay: Map<String, Boolean>, streak: Int, palette: Palette, onToggle: (String) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val doneCount = days.count { doneByDay[it] == true }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(palette.surface)
            .border(1.dp, palette.accent.copy(alpha = 0.18f), shape)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("План дня", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = palette.text)
                Text(
                    if (doneByDay[Day.today()] == true) "Отмечено — день закрыт" else "Отметь, что план выполнен",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = palette.textDim
                )
            }
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(palette.accent.copy(alpha = 0.16f))
                    .border(1.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(streak.toString() + " 🔥", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = palette.accent)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { d ->
                CheckInDot(
                    done = doneByDay[d] == true,
                    accent = palette.accent,
                    label = Day.weekdayInitial(d),
                    sub = Day.dayOfMonth(d),
                    onClick = { onToggle(d) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            doneCount.toString() + " из " + days.size + " за неделю",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = palette.textDim.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SearchField(palette: Palette, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(palette.surface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = palette.textDim, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text("Поиск по заметкам, тегам, связям", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = palette.textDim)
    }
}

@Composable
private fun CaptureFab(palette: Palette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(end = 20.dp, bottom = 26.dp)
            .size(60.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(palette.accent, palette.accentSoft)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.AutoAwesome, "Записать идею", tint = Color(0xFF12070A), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun QuickCaptureSheet(palette: Palette, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(palette.surfaceHigh)
            .border(1.dp, palette.accent.copy(alpha = 0.25f), shape)
            .padding(14.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Мысль, идея, цитата…  #тег  [[связь]]", color = palette.textDim, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = palette.accent,
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text
            ),
            minLines = 2,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSave(text) }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enter + Отправить сохранит сразу", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = palette.textDim, modifier = Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(palette.accent.copy(alpha = 0.15f))
                    .clickable { onDismiss() }.padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("Отмена", color = palette.textDim, style = androidx.compose.material3.MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(palette.accent)
                    .clickable(enabled = text.isNotBlank()) { onSave(text) }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("Сохранить", color = Color(0xFF12070A), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
        }
    }
}

