
package dev.zyutsu.idea.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WireNote(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val deleted: Boolean = false,
    val source: String = "local"
)

@Serializable
data class WireCheckIn(
    val day: String,
    val done: Boolean,
    val updatedAt: Long
)

@Serializable
data class Vault(
    @SerialName("notes") val notes: List<WireNote> = emptyList(),
    @SerialName("checkins") val checkins: List<WireCheckIn> = emptyList(),
    @SerialName("tags") val tags: List<WireTag> = emptyList(),
    @SerialName("exportedAt") val exportedAt: Long = 0,
    @SerialName("device") val device: String = ""
)

fun vaultOf(
    notes: List<Note>,
    checkins: List<CheckIn>,
    tags: List<TagEntity>,
    exportedAt: Long,
    device: String = ""
): Vault = Vault(
    notes = notes.map {
        WireNote(it.id, it.title, it.body, it.createdAt, it.updatedAt, it.pinned, it.favorite, it.deleted, it.source)
    },
    checkins = checkins.map { WireCheckIn(it.day, it.done, it.updatedAt) },
    tags = tags.map { WireTag(it.name, it.colorIndex) },
    exportedAt = exportedAt,
    device = device
)

@Serializable
data class WireTag(val name: String, val colorIndex: Int = 0)

fun Vault.toLocalNotes(): List<Note> = notes.map {
    Note(it.id, it.title, it.body, it.createdAt, it.updatedAt, it.pinned, it.favorite, it.deleted, it.source)
}

fun Vault.toLocalCheckIns(): List<CheckIn> = checkins.map { CheckIn(it.day, it.done, it.updatedAt) }

fun Vault.toLocalTags(): List<TagEntity> = tags.map { TagEntity(it.name, it.colorIndex) }

