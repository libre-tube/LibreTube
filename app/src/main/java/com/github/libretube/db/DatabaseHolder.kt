package com.github.libretube.db

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.libretube.LibreTubeApp

object DatabaseHolder {
    private const val DATABASE_NAME = "LibreTubeDatabase"

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

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE 'subscriptionGroups' ADD COLUMN 'index' INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE 'downloadItem' ADD COLUMN 'language' TEXT DEFAULT NULL"
            )
        }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE 'watchHistoryItem' ADD COLUMN 'isShort' INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val Database by lazy {
        Room.databaseBuilder(LibreTubeApp.instance, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
            .fallbackToDestructiveMigration()
            .build()
    }
}
