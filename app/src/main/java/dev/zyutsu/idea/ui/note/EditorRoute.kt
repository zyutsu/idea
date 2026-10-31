
package dev.zyutsu.idea.ui.note

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.core.Day
import dev.zyutsu.idea.core.Markdown
import dev.zyutsu.idea.data.Note
import dev.zyutsu.idea.data.NoteCard
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.data.Suggestion
import dev.zyutsu.idea.data.SuggestMode
import dev.zyutsu.idea.ui.common.TagPill
import dev.zyutsu.idea.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class SaveState { SAVED, SAVING, IDLE }

@Composable
fun EditorRoute(
    repo: Repository,
    noteId: String,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenTag: (String) -> Unit
) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var note by remember { mutableStateOf<Note?>(null) }
    var docId by remember { mutableStateOf(noteId) }
    var text by remember { mutableStateOf("") }
    var caret by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var saveState by remember { mutableStateOf(SaveState.IDLE) }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var suggestMode by remember { mutableStateOf(SuggestMode.LINK) }
    var showMenu by remember { mutableStateOf(false) }
    var backlinks by remember { mutableStateOf<List<NoteCard>>(emptyList()) }
    var outlinks by remember { mutableStateOf<List<NoteCard>>(emptyList()) }
    var ghosts by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastSaved by remember { mutableLongStateOf(0L) }

    LaunchedEffect(noteId) {
        val n = repo.noteById(noteId)
        note = n
        docId = noteId
        text = n?.body ?: ""
        caret = text.length
        loaded = true
        if (n != null) refreshRelations(repo, n.id) { b, o, g -> backlinks = b; outlinks = o; ghosts = g }
    }

    // debounced autosave
    LaunchedEffect(text, loaded) {
        if (!loaded) return@LaunchedEffect
        if (text.isBlank() && note == null) return@LaunchedEffect
        if (text == note?.body) { saveState = SaveState.IDLE; return@LaunchedEffect }
        saveState = SaveState.SAVING
        delay(420)
        val id = repo.save(docId.ifEmpty { null }, text)
        docId = id
        note = repo.noteById(id)
        saveState = SaveState.SAVED
        lastSaved = System.currentTimeMillis()
        refreshRelations(repo, id) { b, o, g -> backlinks = b; outlinks = o; ghosts = g }
    }

    // autocomplete trigger detection
    LaunchedEffect(caret, text) {
        if (caret <= 0 || caret > text.length) { suggestions = emptyList(); return@LaunchedEffect }
        val before = text.substring(0, caret)
        val wiki = Regex("\\[\\[([^\\]]{0,60})$").find(before)
        val tag = Regex("(^|[\\s(\\[,.:;!?])#([\\p{L}\\p{N}][\\p{L}\\p{N}_/-]{0,40})$").find(before)
        when {
            wiki != null -> {
                suggestMode = SuggestMode.LINK
                suggestions = repo.suggestions(wiki.groupValues[1], SuggestMode.LINK)
            }
            tag != null -> {
                suggestMode = SuggestMode.TAG
                suggestions = repo.suggestions(tag.groupValues[2], SuggestMode.TAG)
            }
            else -> {
                suggestions = emptyList()
            }
        }
    }

    fun applySuggestion(s: Suggestion) {
        val before = text.substring(0, caret.coerceAtMost(text.length))
        val after = text.substring(caret.coerceAtMost(text.length))
        val updated = StringBuilder()
        var newCaret = caret
        when {
            Regex("\\[\\[[^\\]]*$").containsMatchIn(before) -> {
                val cut = before.lastIndexOf("[[")
                val head = before.substring(0, cut)
                val inserted = if (s.kind == Suggestion.Kind.CREATE) "[[" + s.title + "]]" else "[[" + s.title + "]]"
                updated.append(head).append(inserted).append(after)
                newCaret = (head.length + inserted.length)
            }
            Regex("#[\\p{L}\\p{N}_/-]*$").containsMatchIn(before) -> {
                val cut = before.lastIndexOf("#")
                val head = before.substring(0, cut)
                val inserted = "#" + s.title
                updated.append(head).append(inserted).append(" ").append(after)
                newCaret = head.length + inserted.length + 1
            }
            else -> {
                updated.append(before).append(s.title).append(after)
                newCaret = before.length + s.title.length
            }
        }
        text = updated.toString()
        caret = newCaret
        suggestions = emptyList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.surfaceHigh.copy(alpha = 0.6f), palette.bg)))
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", palette) { focus.clearFocus(); onBack() }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    note?.title?.ifEmpty { "Новая заметка" } ?: "Новая заметка",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val label = when (saveState) {
                    SaveState.SAVING -> "сохраняю…"
                    SaveState.SAVED -> "сохранено " + (if (lastSaved > 0) Day.time(lastSaved) else "")
                    SaveState.IDLE -> if (note != null) "измените текст — сохранится само" else "автосохранение включено"
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textDim.copy(alpha = 0.8f))
            }
            note?.let { n ->
                IconBtn(if (n.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, "Избранное", palette, active = n.favorite) {
                    scope.launch { repo.setFavorite(n.id, !n.favorite); note = repo.noteById(n.id) }
                }
                Spacer(Modifier.width(2.dp))
                IconBtn(Icons.Rounded.PushPin, "Закрепить", palette, active = n.pinned) {
                    scope.launch { repo.setPinned(n.id, !n.pinned); note = repo.noteById(n.id) }
                }
                Spacer(Modifier.width(2.dp))
                IconBtn(Icons.Rounded.DeleteOutline, "Удалить", palette) { showMenu = true }
            }
        }

        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = TextFieldValue(text, TextRange(caret.coerceIn(0, text.length))),
                    onValueChange = { v ->
                        text = v.text
                        caret = v.selection.end.coerceIn(0, v.text.length)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default, keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    "Пишите свободно…\n\n[[связь]] — ссылка на заметку\n#тег — категория\n# Заголовок — название",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textDim.copy(alpha = 0.55f)
                                )
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(20.dp))
                RelationsPanel(
                    repo = repo,
                    outlinks = outlinks,
                    backlinks = backlinks,
                    ghosts = ghosts,
                    onOpenNote = onOpenNote,
                    onOpenTag = onOpenTag,
                    onCreateGhost = { title ->
                        scope.launch {
                            val id = repo.createBlank(title)
                            onOpenNote(id)
                        }
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                SuggestionList(suggestions = suggestions, palette = palette, mode = suggestMode) { applySuggestion(it) }
            }
        }
    }

    if (showMenu && note != null) {
        ConfirmSheet(
            title = "Удалить заметку?",
            subtitle = note!!.title,
            palette = palette,
            onConfirm = {
                scope.launch {
                    repo.delete(note!!.id)
                    showMenu = false
                    onBack()
                }
            },
            onDismiss = { showMenu = false }
        )
    }
}


@Composable
private fun IconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    palette: dev.zyutsu.idea.ui.theme.Palette,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(38.dp).clip(CircleShape)
            .background(if (active) palette.accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = if (active) palette.accent else palette.text.copy(alpha = 0.75f), modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SuggestionList(suggestions: List<Suggestion>, palette: dev.zyutsu.idea.ui.theme.Palette, mode: SuggestMode, onPick: (Suggestion) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(palette.surfaceHigh.copy(alpha = 0.98f))
            .border(1.dp, palette.accent.copy(alpha = 0.22f), shape).padding(vertical = 6.dp)
    ) {
        Text(
            if (mode == SuggestMode.LINK) "Связать с заметкой" else "Тег",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textDim,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        suggestions.forEach { s ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(s) }.padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (s.kind) {
                        Suggestion.Kind.NOTE -> Icons.AutoMirrored.Outlined.Notes
                        Suggestion.Kind.TAG -> Icons.Outlined.Link
                        Suggestion.Kind.CREATE -> Icons.Rounded.PushPin
                    },
                    null,
                    tint = if (s.kind == Suggestion.Kind.CREATE) palette.accent else palette.textDim,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    s.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (s.kind == Suggestion.Kind.CREATE) {
                    Text("создать", style = MaterialTheme.typography.labelSmall, color = palette.accent)
                }
            }
        }
    }
}

private suspend fun refreshRelations(
    repo: Repository,
    id: String,
    set: (List<NoteCard>, List<NoteCard>, List<String>) -> Unit
) {
    val (out, back, unresolved) = repo.relations(id)
    val outCards = repo.cardsFor(out.mapNotNull { l -> l.toId?.let { repo.noteById(it) } })
    val backCards = repo.cardsFor(back.mapNotNull { repo.noteById(it.fromId) })
    set(backCards, outCards, unresolved)
}

@Composable
private fun RelationsPanel(
    repo: Repository,
    outlinks: List<NoteCard>,
    backlinks: List<NoteCard>,
    ghosts: List<String>,
    onOpenNote: (String) -> Unit,
    onOpenTag: (String) -> Unit,
    onCreateGhost: (String) -> Unit
) {
    val palette = LocalPalette.current
    if (outlinks.isEmpty() && backlinks.isEmpty() && ghosts.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        if (outlinks.isNotEmpty()) {
            BlockTitle("Ссылается", palette)
            outlinks.forEach { c -> RelationRow(c.title, c.preview, palette) { onOpenNote(c.id) } }
            Spacer(Modifier.height(14.dp))
        }
        if (backlinks.isNotEmpty()) {
            BlockTitle("Ссылаются на эту", palette)
            backlinks.forEach { c -> RelationRow(c.title, c.preview, palette) { onOpenNote(c.id) } }
            Spacer(Modifier.height(14.dp))
        }
        if (ghosts.isNotEmpty()) {
            BlockTitle("Пока без заметки", palette)
            ghosts.forEach { g ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(palette.accent.copy(alpha = 0.06f))
                        .clickable { onCreateGhost(g) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(g, style = MaterialTheme.typography.bodyMedium, color = palette.text.copy(alpha = 0.8f), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("создать", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun BlockTitle(text: String, palette: dev.zyutsu.idea.ui.theme.Palette) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.width(14.dp).height(2.dp).background(palette.accent.copy(alpha = 0.7f)))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = palette.textDim)
    }
}

@Composable
private fun RelationRow(title: String, preview: String, palette: dev.zyutsu.idea.ui.theme.Palette, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(palette.surface.copy(alpha = 0.7f))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (preview.isNotBlank()) {
            Text(preview, style = MaterialTheme.typography.bodySmall, color = palette.textDim.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ConfirmSheet(title: String, subtitle: String, palette: dev.zyutsu.idea.ui.theme.Palette, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(palette.surfaceHigh).padding(20.dp)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.text)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            Row {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(palette.surface).clickable(onClick = onDismiss).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("Отмена", color = palette.textDim, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(palette.accent).clickable(onClick = onConfirm).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Text("Удалить", color = Color(0xFF12070A), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

