
package dev.zyutsu.idea.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zyutsu.idea.data.GraphEdge
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.ui.theme.LocalPalette
import dev.zyutsu.idea.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private class SimNode(
    val id: String,
    val title: String,
    val degree: Int,
    val missing: Boolean,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
) {
    val radius: Float get() = if (missing) 5.5f else (7f + degree * 2.2f).coerceAtMost(19f)
}

/** Force-directed, pannable and zoomable map of every note and wiki link. */
@Composable
fun GraphRoute(repo: Repository, onOpenNote: (String) -> Unit, onBack: () -> Unit) {
    val palette = LocalPalette.current
    val nodes = remember { mutableStateOf<List<SimNode>>(emptyList()) }
    val edges = remember { mutableStateOf<List<Pair<SimNode, SimNode>>>(emptyList()) }
    var version by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<SimNode?>(null) }

    LaunchedEffect(Unit) {
        val graph = repo.graph()
        val count = graph.nodes.size.coerceAtLeast(1)
        val sim = graph.nodes.mapIndexed { i, node ->
            val angle = (2.0 * Math.PI * i) / count
            val ring = 95f + (i % 5) * 24f
            SimNode(node.id, node.title, node.degree, node.missing, (cos(angle) * ring).toFloat(), (sin(angle) * ring).toFloat())
        }
        val index = sim.associateBy { it.id }
        nodes.value = sim
        edges.value = graph.edges.mapNotNull { e: GraphEdge ->
            val a = index[e.from]
            val b = index[e.to]
            if (a != null && b != null && a !== b) a to b else null
        }
        version++
    }

    LaunchedEffect(version) {
        var tick = 0
        while (tick < 1400) {
            val list = nodes.value
            if (list.isNotEmpty()) {
                step(list, edges.value)
                version++
            }
            tick++
            delay(30)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.surfaceHigh.copy(alpha = 0.8f), palette.bg)))
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(version) {
                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.35f, 4.5f)
                        pan = pan + gesturePan
                    }
                }
                .pointerInput(version) {
                    detectTapGestures { tap ->
                        val cx = size.width / 2f + pan.x / zoom
                        val cy = size.height / 2f + pan.y / zoom
                        val gx = (tap.x - cx) / zoom
                        val gy = (tap.y - cy) / zoom
                        val hit = nodes.value.firstOrNull { n ->
                            val dx = n.x - gx
                            val dy = n.y - gy
                            sqrt(dx * dx + dy * dy) <= n.radius + 10f
                        }
                        selected = hit
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            for ((a, b) in edges.value) {
                drawLine(
                    color = palette.accent.copy(alpha = 0.22f),
                    start = Offset(cx + a.x, cy + a.y),
                    end = Offset(cx + b.x, cy + b.y),
                    strokeWidth = 1.3f
                )
            }
            for (n in nodes.value) {
                val pos = Offset(cx + n.x, cy + n.y)
                val base = if (n.missing) palette.textDim else palette.accent
                if (n == selected) {
                    drawCircle(color = base.copy(alpha = 0.16f), radius = n.radius + 14f, center = pos)
                    drawCircle(color = base.copy(alpha = 0.5f), radius = n.radius + 14f, center = pos, style = Stroke(width = 1f))
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(base.copy(alpha = if (n.missing) 0.55f else 0.95f), base.copy(alpha = if (n.missing) 0.22f else 0.5f)),
                        center = pos,
                        radius = n.radius * 2.1f
                    ),
                    radius = n.radius,
                    center = pos
                )
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(palette, Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Граф связей", style = MaterialTheme.typography.titleMedium, color = palette.text)
                    Text(
                        nodes.value.count { !it.missing }.toString() + " узлов · " + edges.value.size + " связей",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textDim
                    )
                }
                RoundIconButton(palette, Icons.Rounded.Refresh, "Центрировать") { zoom = 1f; pan = Offset.Zero }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                RoundIconButton(palette, Icons.Rounded.Remove, "Меньше") { zoom = (zoom * 0.82f).coerceIn(0.35f, 4.5f) }
                Spacer(Modifier.width(8.dp))
                RoundIconButton(palette, Icons.Rounded.Add, "Больше") { zoom = (zoom * 1.22f).coerceIn(0.35f, 4.5f) }
                Spacer(Modifier.width(14.dp))
                Text(
                    (zoom * 100).toInt().toString() + "%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textDim,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }

        selected?.let { node ->
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surfaceHigh.copy(alpha = 0.97f))
                    .border(1.dp, palette.accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                    .clickable(enabled = !node.missing) { onOpenNote(node.id) }
                    .padding(16.dp)
            ) {
                Text(node.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        node.missing -> "Заметки ещё нет — создай её из любой ссылки"
                        node.degree > 0 -> node.degree.toString() + " связей · нажми, чтобы открыть"
                        else -> "Изолированная заметка · нажми, чтобы открыть"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textDim
                )
            }
        }

        if (nodes.value.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Добавь [[ссылку]] в заметку —\nграф построится сам",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textDim
                )
            }
        }
    }
}

private fun step(list: List<SimNode>, links: List<Pair<SimNode, SimNode>>) {
    for (i in list.indices) {
        val a = list[i]
        var fx = 0f
        var fy = 0f
        for (j in list.indices) {
            if (i == j) continue
            val b = list[j]
            var dx = a.x - b.x
            var dy = a.y - b.y
            var d2 = dx * dx + dy * dy
            if (d2 < 0.01f) {
                dx = Math.random().toFloat() - 0.5f
                dy = Math.random().toFloat() - 0.5f
                d2 = 0.01f
            }
            val d = sqrt(d2)
            val repel = 2400f / d2
            fx += (dx / d) * repel
            fy += (dy / d) * repel
        }
        a.vx = (a.vx + fx) * 0.82f
        a.vy = (a.vy + fy) * 0.82f
    }
    for ((a, b) in links) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val pull = (d - 98f) * 0.035f
        val ux = dx / d
        val uy = dy / d
        a.vx += ux * pull
        a.vy += uy * pull
        b.vx -= ux * pull
        b.vy -= uy * pull
    }
    for (c in list) {
        c.vx -= c.x * 0.012f
        c.vy -= c.y * 0.012f
        c.x += c.vx.coerceIn(-14f, 14f)
        c.y += c.vy.coerceIn(-14f, 14f)
    }
}

@Composable
private fun RoundIconButton(palette: Palette, icon: ImageVector, desc: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        Modifier
            .size(42.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = palette.text.copy(alpha = 0.85f), modifier = Modifier.size(19.dp))
    }
}

