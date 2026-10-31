
package dev.zyutsu.idea.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.ui.theme.LocalPalette
import kotlinx.coroutines.launch

/** One-tap idea capture: type, hit send, done. */
@Composable
fun CaptureDialog(repo: Repository, onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue("", TextRange(0))) }

    LaunchedEffect(Unit) { focus.requestFocus() }

    fun save() {
        if (value.text.isBlank()) { onDismiss(); return }
        scope.launch {
            val id = repo.quickCapture(value.text)
            keyboard?.hide()
            onDismiss()
            if (id.isNotEmpty()) onOpen(id)
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = true, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceHigh)
                .border(1.dp, palette.accent.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .imePadding()
                .padding(18.dp)
        ) {
            Text("Быстрая идея", style = MaterialTheme.typography.titleMedium, color = palette.text)
            Spacer(Modifier.height(3.dp))
            Text("Сохранится отдельной заметкой · #тег и [[связь]] работают", style = MaterialTheme.typography.labelSmall, color = palette.textDim)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.height(110.dp)) {
                if (value.text.isEmpty()) {
                    Text(
                        "Запишите мысль…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textDim.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(palette.surface).clickable(onClick = onDismiss)
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Text("Отмена", style = MaterialTheme.typography.labelLarge, color = palette.textDim)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(palette.accent)
                        .clickable(enabled = value.text.isNotBlank()) { save() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Сохранить", style = MaterialTheme.typography.labelLarge, color = Color(0xFF12070A))
                }
            }
        }
    }
}

