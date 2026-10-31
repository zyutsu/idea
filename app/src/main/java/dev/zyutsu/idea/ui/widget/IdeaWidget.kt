
package dev.zyutsu.idea.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.zyutsu.idea.CaptureActivity
import dev.zyutsu.idea.ToggleActivity
import dev.zyutsu.idea.IdeaApp
import dev.zyutsu.idea.core.Day
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Home-screen widget: one-tap capture plus the daily plan check-in row.
 * A dot turns into a check mark when the day is marked in the app.
 */
class IdeaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = IdeaApp.from(context).repository
        val days = Day.last(7)
        val done = withContext(Dispatchers.IO) { repo.observeCheckins(days).first() }.associate { it.day to it.done }
        val today = Day.today()
        val todayDone = done[today] == true
        val streak = Day.streak(done.filterValues { it }.keys.toSet())
        val recent = withContext(Dispatchers.IO) { repo.cardsFor(repo.recent.first().take(1)) }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF140B0D))
                    .padding(16.dp)
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text("Idea", style = TextStyle(color = ColorProvider(Color(0xFFE7DEE0)), fontWeight = FontWeight.Bold, fontSize = 17.sp))
                        Text(
                            if (streak > 0) streak.toString() + " дн. подряд" else "новый день — новый план",
                            style = TextStyle(color = ColorProvider(Color(0xFF9C8E92)), fontSize = 11.sp)
                        )
                    }
                    Box(
                        modifier = GlanceModifier
                            .size(38.dp)
                            .background(Color(0xFFC42759))
                            .cornerRadius(19.dp)
                            .clickable(actionStartActivity<CaptureActivity>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", style = TextStyle(color = ColorProvider(Color(0xFF12070A)), fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(GlanceModifier.height(12.dp))

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    days.forEach { d ->
                        val isDone = done[d] == true
                        Box(
                            modifier = GlanceModifier.size(32.dp).padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .size(26.dp)
                                    .background(if (isDone) Color(0xFFC42759) else Color(0x1FFFFFFF))
                                    .cornerRadius(13.dp)
                                    .clickable(actionStartActivity<ToggleActivity>(actionParametersOf(KEY_DAY to d))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Text("✓", style = TextStyle(color = ColorProvider(Color(0xFF12070A)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                                } else {
                                    Text(
                                        Day.dayOfMonth(d),
                                        style = TextStyle(color = ColorProvider(Color(0xFF8C7F83)), fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(GlanceModifier.height(8.dp))
                Text(
                    if (todayDone) "План на сегодня выполнен ✓" else "Отметь, что план на сегодня выполнен",
                    style = TextStyle(
                        color = ColorProvider(if (todayDone) Color(0xFFE06A8B) else Color(0xFF9C8E92)),
                        fontSize = 11.sp
                    )
                )
                recent.firstOrNull()?.let { card ->
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        card.title,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(Color(0xFFC9BFC2)), fontSize = 12.sp)
                    )
                }
            }
        }
    }

    companion object {
        val KEY_DAY: ActionParameters.Key<String> = ActionParameters.Key("day")
    }
}

class IdeaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IdeaWidget()
}

suspend fun refreshWidgets(context: Context) {
    IdeaWidget().updateAll(context)
}

