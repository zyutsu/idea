
package dev.zyutsu.idea.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Day {
    private val iso = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val human = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
    private val clock = DateTimeFormatter.ofPattern("HH:mm")

    fun today(): String = LocalDate.now().format(iso)

    fun key(date: LocalDate): String = date.format(iso)

    fun parse(key: String): LocalDate? = try { LocalDate.parse(key, iso) } catch (e: Exception) { null }

    /** Last n ISO day keys ending with [end] (oldest first). */
    fun last(n: Int, end: LocalDate = LocalDate.now()): List<String> =
        (0 until n).map { key(end.minusDays((n - 1 - it).toLong())) }

    fun weekdayInitial(key: String): String {
        val d = parse(key) ?: return "?"
        return d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("ru")).replace(".", "").uppercase(Locale.ROOT)
    }

    fun dayOfMonth(key: String): String = parse(key)?.dayOfMonth?.toString() ?: "?"

    fun pretty(key: String): String = parse(key)?.format(human) ?: key

    fun time(ts: Long): String {
        val zdt = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
        val d = zdt.toLocalDate()
        val today = LocalDate.now()
        val hhmm = clock.format(zdt)
        return when {
            d == today -> "сегодня $hhmm"
            d == today.minusDays(1) -> "вчера $hhmm"
            d.year == today.year -> d.format(human)
            else -> d.toString()
        }
    }

    /** Consecutive completed days ending today (or yesterday if today is not yet marked). */
    fun streak(days: Set<String>, today: String = today()): Int {
        var cursor = parse(today) ?: return 0
        if (!days.contains(today)) cursor = cursor.minusDays(1)
        var n = 0
        while (days.contains(key(cursor))) {
            n++
            cursor = cursor.minusDays(1)
        }
        return n
    }
}

