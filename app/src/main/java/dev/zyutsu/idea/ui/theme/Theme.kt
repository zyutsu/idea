
package dev.zyutsu.idea.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Shapes
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Palettes taken from the reference boards: burgundy ember, deep pine, dusk blue. */
enum class Palette(val id: String, val label: String, val accent: Color, val accentSoft: Color, val bg: Color, val surface: Color, val surfaceHigh: Color, val text: Color, val textDim: Color, val tagColors: List<Color>) {
    EMBER(
        id = "ember", label = "Полынь",
        accent = Color(0xFFC42759),
        accentSoft = Color(0xFF550416),
        bg = Color(0xFF120A0C),
        surface = Color(0xFF1C1113),
        surfaceHigh = Color(0xFF2C0107),
        text = Color(0xFFD8D6D7),
        textDim = Color(0xFF9A8F92),
        tagColors = listOf(Color(0xFFC42759), Color(0xFFE06A8B), Color(0xFFABABAB), Color(0xFF797878), Color(0xFF8E3A52), Color(0xFFD8D6D7))
    ),
    PINE(
        id = "pine", label = "Хвоя",
        accent = Color(0xFF8EB69B),
        accentSoft = Color(0xFF235347),
        bg = Color(0xFF051F20),
        surface = Color(0xFF0B2B26),
        surfaceHigh = Color(0xFF163832),
        text = Color(0xFFDAF1DE),
        textDim = Color(0xFF7FA896),
        tagColors = listOf(Color(0xFF8EB69B), Color(0xFFDAF1DE), Color(0xFF235347), Color(0xFF5E8C7A), Color(0xFFB4D3B8), Color(0xFF3F6B58))
    ),
    DUSK(
        id = "dusk", label = "Сумерки",
        accent = Color(0xFF86A8CF),
        accentSoft = Color(0xFF26425A),
        bg = Color(0xFF0A1420),
        surface = Color(0xFF111F30),
        surfaceHigh = Color(0xFF1B2C42),
        text = Color(0xFFE1CBD7),
        textDim = Color(0xFF8B93A6),
        tagColors = listOf(Color(0xFFC38EB4), Color(0xFF86A8CF), Color(0xFFE1CBD7), Color(0xFF26425A), Color(0xFF6F8FB5), Color(0xFFA57C9E))
    )
}

private fun scheme(p: Palette) = darkColorScheme(
    primary = p.accent,
    onPrimary = Color(0xFF14060A),
    secondary = p.accentSoft,
    onSecondary = p.text,
    background = p.bg,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.surfaceHigh,
    onSurfaceVariant = p.textDim,
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF)
)

val IdeaTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp)
)

val IdeaShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp)
)

val LocalPalette = compositionLocalOf { Palette.EMBER }

@Composable
fun IdeaTheme(palette: Palette = Palette.EMBER, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = false
        }
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme(palette),
            typography = IdeaTypography,
            shapes = IdeaShapes,
            content = content
        )
    }
}

