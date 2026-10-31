
package dev.zyutsu.idea.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index("updatedAt"), Index("title")]
)
data class Note(
    @PrimaryKey val id: String,
    var title: String,
    var body: String,
    var createdAt: Long,
    var updatedAt: Long,
    var pinned: Boolean = false,
    var favorite: Boolean = false,
    var deleted: Boolean = false,
    var source: String = "local"
)

@Entity(
    tableName = "links",
    indices = [Index("fromId"), Index("toTitle")]
)
data class Link(
    @PrimaryKey(autoGenerate = true) val lid: Long = 0,
    val fromId: String,
    val toId: String?,
    val toTitle: String
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    var colorIndex: Int = 0
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagName"],
    indices = [Index("tagName")]
)
data class NoteTag(
    val noteId: String,
    val tagName: String
)

@Entity(tableName = "checkins")
data class CheckIn(
    @PrimaryKey val day: String,
    var done: Boolean,
    var updatedAt: Long
)

@Entity(tableName = "sync_state")
data class SyncState(
    @PrimaryKey val id: Int = 0,
    var serverUrl: String = "",
    var deviceName: String = "",
    var lastSyncAt: Long = 0,
    var lastResult: String = ""
)

