
package dev.zyutsu.idea

import android.app.Activity
import android.os.Bundle
import dev.zyutsu.idea.core.Day
import dev.zyutsu.idea.data.Repository
import dev.zyutsu.idea.ui.widget.IdeaWidget
import dev.zyutsu.idea.ui.widget.refreshWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Headless trampoline: widget check-in taps toggle the day and repaint the widget. */
class ToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val day = intent?.getStringExtra(EXTRA_DAY) ?: Day.today()
        val repo = Repository(applicationContext)
        CoroutineScope(Dispatchers.Main).launch {
            repo.toggleCheckIn(day)
            IdeaWidget().let { refreshWidgets(this@ToggleActivity) }
            finish()
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val EXTRA_DAY = "day"
    }
}

