
package dev.zyutsu.idea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.zyutsu.idea.ui.CaptureDialog
import dev.zyutsu.idea.ui.theme.IdeaTheme
import dev.zyutsu.idea.ui.theme.Palette

/**
 * Transparent one-tap capture entry: launched from the widget, a launcher shortcut
 * or a home-screen icon. Saves without entering the main app.
 */
class CaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = LocalSettings.get(this)
        setContent {
            val store = remember { settings }
            val palette by store.palette.collectAsState(initial = Palette.EMBER)
            var open by remember { mutableStateOf(true) }
            IdeaTheme(palette = palette) {
                if (open) {
                    val repo = remember { (application as IdeaApp).repository }
                    CaptureDialog(
                        repo = repo,
                        onDismiss = { finish() },
                        onOpen = {
                            startActivity(
                                android.content.Intent(this, MainActivity::class.java)
                                    .putExtra(MainActivity.EXTRA_OPEN_NOTE, it)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            )
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

