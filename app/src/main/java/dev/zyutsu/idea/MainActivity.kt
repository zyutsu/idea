
package dev.zyutsu.idea

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.zyutsu.idea.ui.IdeaApplication
import dev.zyutsu.idea.ui.theme.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val capture = intent?.getBooleanExtra(EXTRA_CAPTURE, false) == true
        if (capture) {
            startActivity(Intent(this, CaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        setContent {
            val store = remember { LocalSettings.get(this) }
            val palette by store.palette.collectAsState(initial = Palette.EMBER)
            var showCapture by remember { mutableStateOf(false) }
            IdeaApplication(palette = palette, openCapture = showCapture, onCaptureDismiss = { showCapture = false })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_CAPTURE, false)) {
            startActivity(
                Intent(this, CaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    companion object {
        const val EXTRA_CAPTURE = "capture"
    }
}

