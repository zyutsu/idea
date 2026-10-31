
package dev.zyutsu.idea.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Upsert
    suspend fun upsert(note: Note)

    @Upsert
    suspend fun upsertAll(notes: List<Note>)

    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deleted = 0 AND favorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deleted = 0 AND (title LIKE '%' || :q || '%' OR body LIKE '%' || :q || '%') ORDER BY updatedAt DESC LIMIT 80")
    fun observeSearch(q: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): Note?

    @Query("SELECT * FROM notes WHERE deleted = 0")
    suspend fun getAllOnce(): List<Note>

    @Query("SELECT * FROM notes WHERE lower(title) = lower(:title) AND deleted = 0 LIMIT 1")
    suspend fun getByTitle(title: String): Note?

    @Query("UPDATE notes SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE notes SET deleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("DELETE FROM notes WHERE deleted = 1")
    suspend fun purgeDeleted()

    @Query("SELECT COUNT(*) FROM notes WHERE deleted = 0")
    fun observeCount(): Flow<Int>
}

@Dao
interface LinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: Link)

    @Query("DELETE FROM links WHERE fromId = :fromId")
    suspend fun clearFor(fromId: String)

    @Query("SELECT * FROM links")
    suspend fun getAllOnce(): List<Link>

    @Query("SELECT * FROM links WHERE toId = :id OR toTitle IN (SELECT title FROM notes WHERE id = :id)")
    suspend fun backlinksFor(id: String): List<Link>

    @Query("SELECT * FROM links WHERE fromId = :id")
    suspend fun outlinksFor(id: String): List<Link>

    @Query("SELECT * FROM links")
    fun observeAll(): Flow<List<Link>>
}

@Dao
interface TagDao {
    @Upsert
    suspend fun upsertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(rel: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearFor(noteId: String)

    @Query("SELECT name, COUNT(noteId) as cnt FROM tags JOIN note_tags ON tags.name = note_tags.tagName GROUP BY name HAVING cnt > 0 ORDER BY cnt DESC, name ASC")
    fun observeWithCounts(): Flow<List<TagCount>>

    @Query("SELECT noteId FROM note_tags WHERE tagName = :tag")
    fun observeNoteIdsFor(tag: String): Flow<List<String>>

    @Query("SELECT * FROM tags")
    suspend fun getAllOnce(): List<TagEntity>

    @Query("SELECT * FROM note_tags")
    suspend fun getAllLinksOnce(): List<NoteTag>

    @Query("SELECT tagName FROM note_tags WHERE noteId = :noteId ORDER BY tagName")
    suspend fun tagNamesFor(noteId: String): List<String>
}

data class TagCount(val name: String, val cnt: Int)

@Dao
interface CheckInDao {
    @Upsert
    suspend fun upsert(checkIn: CheckIn)

    @Query("SELECT * FROM checkins WHERE day IN (:days)")
    fun observeDays(days: List<String>): Flow<List<CheckIn>>

    @Query("SELECT * FROM checkins")
    suspend fun getAllOnce(): List<CheckIn>

    @Upsert
    suspend fun upsertAll(list: List<CheckIn>)
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_state WHERE id = 0")
    fun observe(): Flow<SyncState?>

    @Query("SELECT * FROM sync_state WHERE id = 0")
    suspend fun get(): SyncState?

    @Upsert
    suspend fun save(state: SyncState)
}

