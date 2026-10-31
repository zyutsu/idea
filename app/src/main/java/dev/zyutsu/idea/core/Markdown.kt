
package dev.zyutsu.idea.core

import java.util.Locale

/** Lightweight markdown / wiki-link / tag analysis shared by editor, repository and search. */
object Markdown {
    /** Backtick, kept as an escape so raw strings stay readable. */
    const val BT = "\u0060"

val WIKI: Regex = Regex("""\[\[([^\]]{1,200})]]""")
    val TAG: Regex = Regex("""(^|[\s(\[,.:;!?])(#([\p{L}\p{N}][\p{L}\p{N}_/-]{0,63}))""")
    val HEADING: Regex = Regex("""^\s{0,3}#{1,6}\s+(.+)$""", RegexOption.MULTILINE)
    val CODE_BLOCK: Regex = Regex(BT + BT + BT + "[^" + BT + "]*" + BT + BT + BT)
    val CODE_INLINE: Regex = Regex(BT + "[^" + BT + "]+")
    val NOISE: Regex = Regex("""[#*_>~\[\]()!]+""")
    val WS: Regex = Regex("""\s+""")

    fun clean(s: String): String = WS.replace(NOISE.replace(s, " "), " ").trim()

    fun titleFrom(body: String): String {
        val text = body.trim()
        if (text.isEmpty()) return "Без названия"
        HEADING.find(text)?.let { return clean(it.groupValues[1]).ifEmpty { "Без названия" } }
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return "Без названия"
        val wiki = WIKI.find(firstLine)
        val candidate =
            if (wiki != null && clean(wiki.groupValues[1]).length >= 3) clean(wiki.groupValues[1])
            else firstLine
        val cleaned = clean(candidate)
        return when {
            cleaned.isEmpty() -> "Без названия"
            cleaned.length > 64 -> cleaned.take(64).trimEnd() + "\u2026"
            else -> cleaned
        }
    }

    fun wikiLinks(body: String): List<String> =
        WIKI.findAll(stripCode(body))
            .map { it.groupValues[1].substringBefore('|').substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .map { it.lowercase(Locale.ROOT) }
            .distinct()
            .toList()

    fun tags(body: String): List<String> =
        TAG.findAll(stripCode(body))
            .map { it.groupValues[3].trimEnd('-', '_', '/').lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() && it.any { c -> !c.isDigit() } }
            .distinct()
            .toList()

    fun stripCode(body: String): String = CODE_INLINE.replace(CODE_BLOCK.replace(body, " "), " ")

    fun preview(body: String, limit: Int = 170): String {
        val unwikified = WIKI.replace(stripCode(body)) { m -> m.groupValues[1].substringBefore('|') }
        val stripped = clean(unwikified)
        return when {
            stripped.isEmpty() -> ""
            stripped.length > limit -> stripped.take(limit).trimEnd() + "\u2026"
            else -> stripped
        }
    }

    fun snippet(body: String, query: String, radius: Int = 48): String {
        val flat = WS.replace(body, " ")
        if (query.isBlank()) return preview(flat, radius * 2)
        val q = query.lowercase(Locale.ROOT).trim()
        val idx = flat.lowercase(Locale.ROOT).indexOf(q)
        if (idx < 0) return preview(flat, radius * 2)
        val start = (idx - radius).coerceAtLeast(0)
        val end = (idx + q.length + radius).coerceAtMost(flat.length)
        val prefix = if (start > 0) "\u2026" else ""
        val suffix = if (end < flat.length) "\u2026" else ""
        return prefix + flat.substring(start, end).trim() + suffix
    }
}

/** Subsequence fuzzy matcher: prefix and word-start hits beat contiguous runs, gaps cost points. */
object Fuzzy {
    fun score(text: String, query: String): Int {
        if (query.isEmpty()) return 1
        val t = text.lowercase(Locale.ROOT)
        val q = query.lowercase(Locale.ROOT)
        var qi = 0
        var total = 0
        var streak = 0
        var gaps = 0
        var prev = -2
        for (i in t.indices) {
            if (qi < q.length && t[i] == q[qi]) {
                streak = if (i == prev + 1) streak + 1 else 1
                if (qi > 0 && i != prev + 1) gaps++
                total += 3 + streak
                if (i == 0 || t[i - 1] == ' ' || t[i - 1] == '#' || t[i - 1] == '[') total += 6
                prev = i
                qi++
            }
        }
        if (qi < q.length) return 0
        var s = total - gaps * 2
        if (t.contains(q)) s += 40
        if (t.startsWith(q)) s += 60
        if (t == q) s += 120
        return s.coerceAtLeast(1)
    }

    fun match(text: String, query: String): Boolean = score(text, query) > 0
}

