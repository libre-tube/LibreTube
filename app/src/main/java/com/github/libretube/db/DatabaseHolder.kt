package com.github.libretube.db

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.libretube.LibreTubeApp
import com.github.libretube.constants.DATABASE_NAME

object DatabaseHolder {
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE 'localPlaylist' ADD COLUMN 'description' TEXT DEFAULT NULL"
            )
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE 'playlistBookmark' ADD COLUMN 'videos' INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val Database by lazy {
        Room.databaseBuilder(LibreTubeApp.instance, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_11_12, MIGRATION_12_13)
            .fallbackToDestructiveMigration()
            .build()
    }
}
