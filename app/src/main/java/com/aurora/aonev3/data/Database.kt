package com.aurora.aonev3.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aurora.aonev3.App
import com.aurora.aonev3.data.templates.IdentitiesDao
import com.aurora.aonev3.data.templates.Identity

@Database(entities = [
    Identity::class
], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun identitiesDao(): IdentitiesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    App.context,
                    AppDatabase::class.java,
                    "aone_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }

    }
}
