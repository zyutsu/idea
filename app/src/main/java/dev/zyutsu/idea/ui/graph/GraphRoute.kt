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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zyutsu.idea.data.GraphEdge
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.ui.theme.LocalPalette
import dev.zyutsu.idea.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
    val radius: Float get() = if (missing) 7f else (9f + degree * 2.4f).coerceAtMost(22f)
}

/**
 * Force-directed map of every note and wiki link.
 *
 * Zoom and pan are applied to the drawn projection, so gestures actually move the graph;
 * the simulation writes into plain fields and only bumps [frame], which the canvas reads in
 * its draw phase. That keeps gesture detectors alive across frames.
 */
@Composable
fun GraphRoute(repo: Repository, onOpenNote: (String) -> Unit, onBack: () -> Unit) {
    val palette = LocalPalette.current
    val textMeasurer = rememberTextMeasurer()
    val nodes = remember { mutableListOf<SimNode>() }
    val edges = remember { mutableListOf<Pair<SimNode, SimNode>>() }
    var frame by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var layoutTick by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<SimNode?>(null) }

    LaunchedEffect(Unit) {
        val graph = repo.graph()
        val count = graph.nodes.size.coerceAtLeast(1)
        nodes.clear()
        edges.clear()
        val sim = graph.nodes.mapIndexed { i, node ->
            val angle = (2.0 * Math.PI * i) / count
            val ring = 120f + (i % 5) * 26f
            SimNode(
                id = node.id,
                title = node.title,
                degree = node.degree,
                missing = node.missing,
                x = (cos(angle) * ring).toFloat() + Random.nextFloat() * 6f,
                y = (sin(angle) * ring).toFloat() + Random.nextFloat() * 6f
            )
        }
        val index = sim.associateBy { it.id }
        nodes.addAll(sim)
        edges.addAll(graph.edges.mapNotNull { e: GraphEdge ->
            val a = index[e.from]
            val b = index[e.to]
            if (a != null && b != null && a !== b) a to b else null
        })
        ready = true
        frame++
    }

    // One simulation pass per load/layout request; the loop is cancelled if we relayout.
    LaunchedEffect(ready, layoutTick) {
        if (!ready) return@LaunchedEffect
        var tick = 0
        while (tick < 900) {
            step(nodes, edges)
            frame++
            tick++
            kotlinx.coroutines.yield()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.surfaceHigh.copy(alpha = 0.8f), palette.bg)))
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, gesturePan, gestureZoom, _ ->
                        val z = (zoom * gestureZoom).coerceIn(0.3f, 5f)
                        // keep the world point under the fingers fixed while zooming
                        val worldX = (centroid.x - size.width / 2f - pan.x) / zoom
                        val worldY = (centroid.y - size.height / 2f - pan.y) / zoom
                        pan = Offset(centroid.x - size.width / 2f - worldX * z, centroid.y - size.height / 2f - worldY * z)
                        zoom = z
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val gx = (tap.x - size.width / 2f - pan.x) / zoom
                        val gy = (tap.y - size.height / 2f - pan.y) / zoom
                        val tolerance = 14f / zoom
                        selected = nodes.firstOrNull { n ->
                            val dx = n.x - gx
                            val dy = n.y - gy
                            sqrt(dx * dx + dy * dy) <= n.radius + tolerance
                        }
                    }
                }
        ) {
            // Reading frame in the draw phase makes every simulation step repaint.
            if (frame >= 0) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                fun toScreen(n: SimNode) = Offset(cx + n.x * zoom + pan.x, cy + n.y * zoom + pan.y)

                for ((a, b) in edges) {
                    drawLine(
                        color = palette.accent.copy(alpha = 0.24f),
                        start = toScreen(a),
                        end = toScreen(b),
                        strokeWidth = 1.3f * zoom
                    )
                }
                for (n in nodes) {
                    val pos = toScreen(n)
                    val r = n.radius * zoom
                    val base = if (n.missing) palette.textDim else palette.accent
                    if (n === selected) {
                        drawCircle(color = base.copy(alpha = 0.15f), radius = r + 14f * zoom, center = pos)
                        drawCircle(
                            color = base.copy(alpha = 0.55f),
                            radius = r + 14f * zoom,
                            center = pos,
                            style = Stroke(width = 1f * zoom)
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                base.copy(alpha = if (n.missing) 0.5f else 0.95f),
                                base.copy(alpha = if (n.missing) 0.18f else 0.45f)
                            ),
                            center = pos,
                            radius = r * 1.9f
                        ),
                        radius = r,
                        center = pos
                    )
                    if (n.missing) {
                        drawCircle(
                            color = Color.Transparent,
                            radius = r,
                            center = pos,
                            style = Stroke(width = 1f * zoom, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f * zoom, 5f * zoom)))
                        )
                    }
                }
                // Labels only when they can be read.
                if (zoom > 0.72f) {
                    val style = TextStyle(color = palette.text.copy(alpha = 0.82f), fontSize = (10f * zoom).sp)
                    for (n in nodes) {
                        val pos = toScreen(n)
                        if (pos.x !in -80f..(size.width + 80f) || pos.y !in -40f..(size.height + 60f)) continue
                        val label = if (n.title.length > 18) n.title.take(17) + "\u2026" else n.title
                        val layout = textMeasurer.measure(label, style)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(pos.x - layout.size.width / 2f, pos.y + n.radius * zoom + 6f * zoom)
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(palette, Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Граф связей", style = MaterialTheme.typography.titleMedium, color = palette.text)
                    Text(
                        nodes.count { !it.missing }.toString() + " узлов · " + edges.size + " связей",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textDim
                    )
                }
                RoundIconButton(palette, Icons.Rounded.Refresh, "Разложить заново") {
                    zoom = 1f
                    pan = Offset.Zero
                    selected = null
                    layoutTick++
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                RoundIconButton(palette, Icons.Rounded.Remove, "Меньше") { zoom = (zoom * 0.82f).coerceIn(0.3f, 5f) }
                Spacer(Modifier.width(8.dp))
                RoundIconButton(palette, Icons.Rounded.Add, "Больше") { zoom = (zoom * 1.22f).coerceIn(0.3f, 5f) }
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
                Text(node.title, style = MaterialTheme.typography.titleMedium, color = palette.text, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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

        if (ready && nodes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 120.dp), contentAlignment = Alignment.Center) {
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
    val n = list.size
    if (n == 0) return
    for (i in 0 until n) {
        val a = list[i]
        var fx = 0f
        var fy = 0f
        for (j in 0 until n) {
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
            val repel = 3200f / d2
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
        val pull = (d - 120f) * 0.035f
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
        c.vx = c.vx.coerceIn(-13f, 13f)
        c.vy = c.vy.coerceIn(-13f, 13f)
        c.x += c.vx
        c.y += c.vy
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

