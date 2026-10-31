
package dev.zyutsu.idea.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Note::class, Link::class, TagEntity::class, NoteTag::class, CheckIn::class, SyncState::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
    abstract fun links(): LinkDao
    abstract fun tags(): TagDao
    abstract fun checkins(): CheckInDao
    abstract fun sync(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "idea.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

