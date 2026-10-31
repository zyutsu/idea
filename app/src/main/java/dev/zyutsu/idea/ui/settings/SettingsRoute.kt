
package dev.zyutsu.idea.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.LocalSettings
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.data.SyncEngine
import dev.zyutsu.idea.ui.theme.LocalPalette
import dev.zyutsu.idea.ui.theme.Palette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(repo: Repository, onBack: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val settings = remember { LocalSettings.get(context) }
    val scope = rememberCoroutineScope()

    val syncState by repo.observeSyncState().collectAsState(null)
    var url by remember { mutableStateOf(syncState?.serverUrl ?: "") }
    var device by remember { mutableStateOf(syncState?.deviceName ?: "") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(syncState?.lastResult ?: "") }

    LaunchedEffect(syncState) {
        syncState?.let {
            if (url.isEmpty()) url = it.serverUrl
            if (device.isEmpty()) device = it.deviceName
            status = it.lastResult
        }
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
            Text("Настройки", style = MaterialTheme.typography.titleMedium, color = palette.text)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Card(palette, "Тема") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Palette.entries.forEach { p ->
                        val selected = p.id == palette.id
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(p.bg)
                                .border(1.dp, if (selected) p.accent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .clickable { settings.setPalette(p) }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                p.tagColors.take(3).forEach { c ->
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(c))
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(p.label, style = MaterialTheme.typography.labelMedium, color = p.text)
                            if (selected) {
                                Spacer(Modifier.height(4.dp))
                                Icon(Icons.Rounded.Check, null, tint = p.accent, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            } }

            item { Card(palette, "Синхронизация между устройствами") {
                Text(
                    "Введите адрес вашего хранилища (свой сервер или локальная папка, доступная по сети). Приложение работает офлайн и синхронизируется само, когда появится сеть.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textDim
                )
                Spacer(Modifier.height(12.dp))
                Field(palette, "https://example.com/idea", "Сервер", url) { url = it }
                Spacer(Modifier.height(8.dp))
                Field(palette, "Pixel-8", "Имя устройства", device) { device = it }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Solid(palette, "Сохранить") {
                        scope.launch {
                            repo.saveSyncSettings(url, device)
                            Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Outline(palette, "Синхронизировать") {
                        if (busy) return@Outline
                        busy = true
                        scope.launch {
                            repo.saveSyncSettings(url, device)
                            val result = SyncEngine(context).syncNow()
                            status = when (result) {
                                is SyncEngine.Result.Success -> "Готово: отправлено " + result.pushed + ", получено " + result.pulled
                                is SyncEngine.Result.Skipped -> "Сервер не настроен"
                                is SyncEngine.Result.Failed -> "Ошибка сети. Данные не потеряны — повторим автоматически"
                            }
                            busy = false
                        }
                    }
                }
                if (status.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(status, style = MaterialTheme.typography.labelSmall, color = palette.textDim)
                }
                syncState?.lastSyncAt?.let { t ->
                    if (t > 0) {
                        Spacer(Modifier.height(3.dp))
                        Text("Последняя синхронизация: " + dev.zyutsu.idea.core.Day.time(t), style = MaterialTheme.typography.labelSmall, color = palette.textDim.copy(alpha = 0.7f))
                    }
                }
            } }

            item { Card(palette, "Быстрый захват") {
                Text(
                    "Долгое нажатие на иконку приложения → «Записать идею». Виджет «Idea» на главном экране добавляет мысль и отмечает план дня в один тап.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textDim
                )
            } }

            item { Card(palette, "О приложении") {
                Text("Idea · v1.0.0", style = MaterialTheme.typography.bodyMedium, color = palette.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Заметки, связи [[wiki]], #теги, граф и fuzzy-поиск. Всё хранится локально и работает без интернета.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textDim
                )
            } }
        }
    }
}

@Composable
private fun Card(palette: Palette, title: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(palette.surface.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape).padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.text)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun Field(palette: Palette, placeholder: String, label: String, value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textDim)
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier.fillMaxWidth().clip(shape).background(palette.bg.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape).padding(horizontal = 13.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = palette.textDim.copy(alpha = 0.6f))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.text),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = if (label == "Сервер") KeyboardType.Uri else KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Solid(palette: Palette, text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(palette.accent).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = Color(0xFF12070A)) }
}

@Composable
private fun Outline(palette: Palette, text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(palette.surfaceHigh)
            .border(1.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp)
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = palette.text) }
}

