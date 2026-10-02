package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FocusSessionEntity::class, RestrictionAttemptEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FocusLockDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: FocusLockDatabase? = null

        fun getDatabase(context: Context): FocusLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusLockDatabase::class.java,
                    "focus_lock_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
