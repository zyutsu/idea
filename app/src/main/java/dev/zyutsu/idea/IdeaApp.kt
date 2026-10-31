
package dev.zyutsu.idea

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.data.SyncEngine
import dev.zyutsu.idea.ui.widget.IdeaWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IdeaApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
        SyncEngine.schedule(this)
        seedIfFirstRun()
    }

    /** Copies demo notes from assets on the very first launch so the graph and links are alive. */
    private fun seedIfFirstRun() {
        val prefs = getSharedPreferences("idea_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val files = assets.list(SEED_DIR).orEmpty()
                if (files.isNotEmpty()) {
                    for (name in files) {
                        val text = assets.open(SEED_DIR + "/" + name).bufferedReader().use { it.readText() }
                        repository.save(null, text, "seed")
                    }
                    repository.reindexAll()
                }
            } catch (_: Exception) {
            } finally {
                prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            }
        }
    }

    companion object {
        fun from(context: Context): IdeaApp = context.applicationContext as IdeaApp
        private const val SEED_DIR = "seed"
        private const val KEY_SEEDED = "seeded"
    }
}

