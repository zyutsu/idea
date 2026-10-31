
package dev.zyutsu.idea

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import dev.zyutsu.idea.ui.theme.Palette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LocalSettings(context: Context) {
    private val prefs = context.getSharedPreferences("idea_settings", Context.MODE_PRIVATE)

    private val _palette = MutableStateFlow(read())
    val palette: Flow<Palette> = _palette

    private fun read(): Palette =
        Palette.entries.firstOrNull { it.id == prefs.getString(KEY_PALETTE, null) } ?: Palette.EMBER

    fun setPalette(p: Palette) {
        prefs.edit().putString(KEY_PALETTE, p.id).apply()
        _palette.value = p
    }

    var lastNoteId: String?
        get() = prefs.getString(KEY_LAST, null)
        set(value) = prefs.edit().putString(KEY_LAST, value).apply()

    companion object {
        private const val KEY_PALETTE = "palette"
        private const val KEY_LAST = "lastNote"

        @Volatile
        private var instance: LocalSettings? = null

        fun get(context: Context): LocalSettings =
            instance ?: synchronized(this) {
                instance ?: LocalSettings(context.applicationContext).also { instance = it }
            }
    }
}

val LocalSettingsKey = staticCompositionLocalOf<LocalSettings?> { null }

