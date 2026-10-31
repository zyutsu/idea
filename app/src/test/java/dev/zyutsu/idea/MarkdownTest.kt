
package dev.zyutsu.idea

import dev.zyutsu.idea.core.Day
import dev.zyutsu.idea.core.Fuzzy
import dev.zyutsu.idea.core.Markdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MarkdownTest {
    @Test
    fun titlePrefersHeading() {
        assertEquals("План недели", Markdown.titleFrom("# План недели" + "\n\n" + "текст"))
    }

    @Test
    fun titleFallsBackToFirstLine() {
        assertEquals("Идея про граф", Markdown.titleFrom("Идея про граф" + "\n" + "ещё строка"))
    }

    @Test
    fun emptyNoteHasFallbackTitle() {
        assertEquals("Без названия", Markdown.titleFrom("   "))
    }

    @Test
    fun wikiLinksAreParsedAndNormalized() {
        val body = "Смотри [[Вторая заметка]] и [[Третья|алиас]]"
        assertEquals(listOf("вторая заметка", "третья"), Markdown.wikiLinks(body))
    }

    @Test
    fun duplicateLinksCollapse() {
        assertEquals(listOf("одна"), Markdown.wikiLinks("[[Одна]] [[одна]]"))
    }

    @Test
    fun codeBlocksDoNotCreateLinks() {
        val fence = "\u0060\u0060\u0060"
        assertTrue(Markdown.wikiLinks(fence + "\n[[скрытая]]\n" + fence).isEmpty())
    }

    @Test
    fun hashtagsAreParsed() {
        val tags = Markdown.tags("Мысль #работа и ещё #идеи/дизайн, плюс #2024")
        assertEquals(listOf("работа", "идеи/дизайн"), tags)
    }

    @Test
    fun hashInsideWordIsNotATag() {
        assertTrue(Markdown.tags("a#b").isEmpty())
    }

    @Test
    fun previewStripsMarkdown() {
        val p = Markdown.preview("# Заголовок" + "\n" + "**жирный** текст", 100)
        assertTrue(p.contains("Заголовок"))
        assertFalse(p.contains("**"))
    }

    @Test
    fun fuzzyMatchesSubsequences() {
        assertTrue(Fuzzy.match("Вторая заметка", "втзм"))
        assertTrue(Fuzzy.score("Вторая заметка", "вторая") > Fuzzy.score("Вторая заметка", "втз"))
        assertEquals(0, Fuzzy.score("Граф", "zzz"))
    }

    @Test
    fun streakCountsConsecutiveDays() {
        val days = setOf("2026-10-29", "2026-10-30", "2026-10-31")
        assertEquals(3, Day.streak(days, "2026-10-31"))
    }

    @Test
    fun streakAllowsTodayUnmarked() {
        val days = setOf("2026-10-29", "2026-10-30")
        assertEquals(2, Day.streak(days, "2026-10-31"))
    }

    @Test
    fun lastDaysAreOrdered() {
        val days = Day.last(3, LocalDate.of(2026, 10, 31))
        assertEquals(listOf("2026-10-29", "2026-10-30", "2026-10-31"), days)
    }
}

