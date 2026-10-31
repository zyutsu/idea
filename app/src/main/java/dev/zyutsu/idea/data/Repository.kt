
package dev.zyutsu.idea.data

import android.content.Context
import dev.zyutsu.idea.core.Day
import dev.zyutsu.idea.core.Fuzzy
import dev.zyutsu.idea.core.Markdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

data class NoteCard(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAt: Long,
    val pinned: Boolean,
    val favorite: Boolean,
    val tags: List<String>,
    val outCount: Int,
    val inCount: Int
)

data class GraphNode(
    val id: String,
    val title: String,
    val tags: List<String>,
    val degree: Int,
    val missing: Boolean = false
)

data class GraphEdge(val from: String, val to: String)

data class Graph(val nodes: List<GraphNode>, val edges: List<GraphEdge>)

data class Suggestion(val title: String, val id: String?, val kind: Kind) {
    enum class Kind { NOTE, TAG, CREATE }
}

data class BacklinkGroup(val note: NoteCard, val snippet: String, val kind: NoteRelation)

enum class NoteRelation { OUTGOING, BACKLINK, MENTION }

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    private val notes = db.notes()
    private val links = db.links()
    private val tags = db.tags()
    private val checkins = db.checkins()
    private val syncDao = db.sync()

    val allNotes: Flow<List<Note>> = notes.observeAll()
    val noteCount: Flow<Int> = notes.observeCount()
    val tagCounts: Flow<List<TagCount>> = tags.observeWithCounts()
    val favorites: Flow<List<Note>> = notes.observeFavorites()
    val recent: Flow<List<Note>> = notes.observeRecent(12)

    fun observeCheckins(days: List<String>): Flow<List<CheckIn>> = checkins.observeDays(days)

    fun observeSyncState(): Flow<SyncState?> = syncDao.observe()

    suspend fun noteById(id: String): Note? = withContext(Dispatchers.IO) { notes.getById(id) }

    suspend fun observeNoteIdsForTag(tag: String): Flow<List<String>> =
        withContext(Dispatchers.IO) { tags.observeNoteIdsFor(tag) }

    suspend fun search(query: String): List<Pair<Note, Int>> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val term = if (q.startsWith("#")) q.substring(1) else q
        val wantTagOnly = q.startsWith("#")
        val candidates = notes.observeSearch(term).first()
        val scored = ArrayList<Pair<Note, Int>>(candidates.size)
        for (n in candidates) {
            val titleScore = Fuzzy.score(n.title, term) * 3
            val bodyScore = Fuzzy.score(n.body, term)
            val tagNames = tagNamesFor(n.id)
            val tagScore = tagNames.maxOfOrNull { Fuzzy.score(it, term) }?.times(2) ?: 0
            var score = maxOf(titleScore, bodyScore, tagScore)
            if (n.title.contains(term, ignoreCase = true)) score += 50
            if (n.favorite) score += 8
            if (wantTagOnly && tagScore == 0) score = 0
            if (score > 0) scored += n to score
        }
        scored.sortByDescending { it.second }
        scored.take(80)
    }

    suspend fun tagNamesFor(noteId: String): List<String> = withContext(Dispatchers.IO) {
        tags.tagNamesFor(noteId)
    }

    suspend fun cardsFor(list: List<Note>): List<NoteCard> = withContext(Dispatchers.IO) {
        list.map { card(it) }
    }

    suspend fun card(note: Note): NoteCard {
        val out = links.outlinksFor(note.id).size
        val back = links.backlinksFor(note.id).size
        return NoteCard(
            id = note.id,
            title = note.title,
            preview = Markdown.preview(note.body),
            updatedAt = note.updatedAt,
            pinned = note.pinned,
            favorite = note.favorite,
            tags = tagNamesFor(note.id),
            outCount = out,
            inCount = back
        )
    }

    /** Create or update a note, re-deriving title, wiki links and tags. Returns the note id. */
    suspend fun save(id: String?, body: String, source: String = "local"): String =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = id?.let { notes.getById(it) }
            val noteId = existing?.id ?: id ?: UUID.randomUUID().toString()
            val title = Markdown.titleFrom(body)
            val note = existing?.copy(
                title = title,
                body = body,
                updatedAt = now,
                deleted = false,
                source = source
            ) ?: Note(
                id = noteId,
                title = title,
                body = body,
                createdAt = now,
                updatedAt = now,
                source = source
            )
            notes.upsert(note)
            index(note)
            noteId
        }

    private suspend fun index(note: Note) {
        links.clearFor(note.id)
        val titles = notes.getAllOnce().asSequence()
            .filter { it.id != note.id && !it.deleted }
            .associateBy { it.title.lowercase(Locale.ROOT) }
        for (target in Markdown.wikiLinks(note.body)) {
            val hit = titles[target]
            links.insert(Link(fromId = note.id, toId = hit?.id, toTitle = hit?.title ?: target))
        }
        tags.clearFor(note.id)
        for (t in Markdown.tags(note.body)) {
            tags.upsertTag(TagEntity(t, colorIndex = Math.floorMod(t.hashCode(), 6)))
            tags.link(NoteTag(note.id, t))
        }
    }

    suspend fun reindexAll() = withContext(Dispatchers.IO) {
        notes.getAllOnce().forEach { index(it) }
    }

    suspend fun relations(id: String): Triple<List<Link>, List<Link>, List<String>> =
        withContext(Dispatchers.IO) {
            val note = notes.getById(id) ?: return@withContext Triple(emptyList(), emptyList(), emptyList())
            val out = links.outlinksFor(id)
            val back = links.backlinksFor(id).filter { it.fromId != id }.distinctBy { it.fromId }
            val unresolved = out.filter { it.toId == null }.map { it.toTitle }
            Triple(out, back, unresolved)
        }


    suspend fun graph(): Graph = withContext(Dispatchers.IO) {
        val live = notes.getAllOnce().filter { !it.deleted }
        val byTitle = live.associateBy { it.title.lowercase(Locale.ROOT) }
        val allLinks = links.getAllOnce()
        val degree = HashMap<String, Int>()
        val edges = LinkedHashSet<GraphEdge>()
        val ghosts = LinkedHashMap<String, String>()
        for (l in allLinks) {
            val targetId = l.toId ?: byTitle[l.toTitle.lowercase(Locale.ROOT)]?.id
            if (targetId == null) {
                val key = l.toTitle.lowercase(Locale.ROOT)
                if (!ghosts.containsKey(key)) ghosts[key] = l.toTitle
                continue
            }
            if (targetId == l.fromId) continue
            edges += GraphEdge(l.fromId, targetId)
            degree[l.fromId] = (degree[l.fromId] ?: 0) + 1
            degree[targetId] = (degree[targetId] ?: 0) + 1
        }
        val nodes = live.map { GraphNode(it.id, it.title, tagNamesFor(it.id), degree[it.id] ?: 0) } +
            ghosts.map { (key, title) -> GraphNode(GHOST + key, title, emptyList(), 0, missing = true) }
        val ghostEdges = allLinks
            .filter { it.toId == null && ghosts.containsKey(it.toTitle.lowercase(Locale.ROOT)) }
            .map { GraphEdge(it.fromId, GHOST + it.toTitle.lowercase(Locale.ROOT)) }
            .distinct()
        Graph(nodes, (edges + ghostEdges).toList())
    }

    suspend fun setPinned(id: String, value: Boolean) = editFlags(id) { copy(pinned = value) }

    suspend fun setFavorite(id: String, value: Boolean) = editFlags(id) { copy(favorite = value) }

    private suspend fun editFlags(id: String, block: Note.() -> Note) = withContext(Dispatchers.IO) {
        val n = notes.getById(id) ?: return@withContext
        notes.upsert(n.block().copy(updatedAt = System.currentTimeMillis()))
        index(notes.getById(id)!!)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        notes.softDelete(id, System.currentTimeMillis())
    }

    suspend fun restore(id: String) = withContext(Dispatchers.IO) {
        notes.restore(id, System.currentTimeMillis())
    }

    suspend fun createBlank(title: String): String = withContext(Dispatchers.IO) {
        val body = if (title.startsWith("#")) title else "# " + title.trim().ifEmpty { "Без названия" }
        save(null, body)
    }

    suspend fun quickCapture(text: String): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext ""
        save(null, trimmed)
    }

    suspend fun toggleCheckIn(day: String): Boolean = withContext(Dispatchers.IO) {
        val current = checkins.observeDays(listOf(day)).first().firstOrNull { it.day == day }
        val next = !(current?.done ?: false)
        checkins.upsert(CheckIn(day, next, System.currentTimeMillis()))
        next
    }

    suspend fun setCheckIn(day: String, done: Boolean) = withContext(Dispatchers.IO) {
        checkins.upsert(CheckIn(day, done, System.currentTimeMillis()))
    }

    suspend fun suggestions(query: String, mode: SuggestMode): List<Suggestion> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase(Locale.ROOT)
        when (mode) {
            SuggestMode.LINK -> {
                val notesHits = notes.getAllOnce().filter { !it.deleted }
                    .map { Suggestion(it.title, it.id, Suggestion.Kind.NOTE) }
                    .filter { q.isEmpty() || Fuzzy.match(it.title, q) }
                    .sortedByDescending { Fuzzy.score(it.title, q) }
                    .take(8)
                val existing = notesHits.map { it.title.lowercase(Locale.ROOT) }.toSet()
                val out = ArrayList(notesHits)
                if (q.length >= 2 && !existing.contains(q)) {
                    out += Suggestion(query.trim(), null, Suggestion.Kind.CREATE)
                }
                out
            }
            SuggestMode.TAG -> {
                val known = tags.observeWithCounts().first().map { Suggestion(it.name, null, Suggestion.Kind.TAG) }
                val hits = known.filter { q.isEmpty() || Fuzzy.match(it.title, q) }
                    .sortedByDescending { Fuzzy.score(it.title, q) }
                    .take(8)
                if (q.isNotEmpty() && hits.none { it.title.equals(q, true) }) {
                    hits + Suggestion(query.trim(), null, Suggestion.Kind.CREATE)
                } else hits
            }
        }
    }

    suspend fun snapshot(): Vault = withContext(Dispatchers.IO) {
        vaultOf(notes.getAllOnce(), checkins.getAllOnce(), tags.getAllOnce(), System.currentTimeMillis())
    }

    suspend fun merge(vault: Vault): Int = withContext(Dispatchers.IO) {
        var changed = 0
        for (incoming in vault.toLocalNotes()) {
            val local = notes.getById(incoming.id)
            if (local == null || local.updatedAt < incoming.updatedAt) {
                notes.upsert(incoming)
                changed++
            }
        }
        for (c in vault.toLocalCheckIns()) {
            val list = checkins.observeDays(listOf(c.day)).first()
            val local = list.firstOrNull { it.day == c.day }
            if (local == null || local.updatedAt < c.updatedAt) checkins.upsert(c)
        }
        for (t in vault.toLocalTags()) tags.upsertTag(t)
        reindexAll()
        changed
    }

    suspend fun serverUrl(): String = withContext(Dispatchers.IO) { syncDao.get()?.serverUrl ?: "" }

    suspend fun deviceName(): String = withContext(Dispatchers.IO) { syncDao.get()?.deviceName ?: "" }

    suspend fun saveSyncSettings(url: String, device: String) = withContext(Dispatchers.IO) {
        val prev = syncDao.get()
        syncDao.save(
            (prev ?: SyncState()).copy(
                id = 0,
                serverUrl = url.trim().trimEnd('/'),
                deviceName = device.trim().ifEmpty { android.os.Build.MODEL.replace(' ', '-') }
            )
        )
    }

    suspend fun markSyncResult(result: String) = withContext(Dispatchers.IO) {
        val prev = syncDao.get() ?: SyncState()
        syncDao.save(prev.copy(id = 0, lastSyncAt = System.currentTimeMillis(), lastResult = result))
    }

    companion object {
        const val TAG_PALETTE = 6
        const val GHOST = "ghost:"
    }
}

enum class SuggestMode { LINK, TAG }

