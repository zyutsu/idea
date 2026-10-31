
package dev.zyutsu.idea.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.data.NoteCard
import dev.zyutsu.idea.ui.theme.Palette

@Composable
fun TagPill(
    name: String,
    color: Color,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) color else color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = if (selected) 1f else 0.35f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "#" + name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xFF120A0C) else color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NoteRow(
    card: NoteCard,
    palette: Palette,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(palette.surface, palette.surface.copy(alpha = 0.55f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.055f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (card.pinned) {
                Icon(Icons.Rounded.PushPin, null, tint = palette.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                card.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (card.favorite) {
                Icon(Icons.Outlined.Star, null, tint = palette.accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
            }
        }
        if (card.preview.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                card.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (card.tags.isNotEmpty()) {
                card.tags.take(3).forEachIndexed { i, t ->
                    TagPill(t, palette.tagColors[(i + t.hashCode()) % palette.tagColors.size])
                    Spacer(Modifier.width(6.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            if (card.outCount + card.inCount > 0) {
                Icon(Icons.Outlined.Link, null, tint = palette.textDim, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    (card.outCount + card.inCount).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textDim
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                dev.zyutsu.idea.core.Day.time(card.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textDim.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
fun CheckInDot(
    done: Boolean,
    accent: Color,
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (done) 1f else 0.72f, tween(220), label = "dot")
    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
        Spacer(Modifier.height(4.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp).clickable(onClick = onClick)) {
            Box(
                Modifier
                    .size((30 * scale).dp)
                    .clip(CircleShape)
                    .background(if (done) accent else Color.White.copy(alpha = 0.08f))
                    .border(1.dp, if (done) accent else Color.White.copy(alpha = 0.16f), CircleShape)
            )
            if (done) Icon(Icons.Rounded.Check, null, tint = Color(0xFF12070A), modifier = Modifier.size(17.dp))
        }
        Text(sub, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.35f))
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null, color: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.55f), modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun EmptyState(title: String, hint: String, accent: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)).border(1.dp, accent.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                Text("✦", color = accent, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(6.dp))
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.4f))
        }
    }
}

