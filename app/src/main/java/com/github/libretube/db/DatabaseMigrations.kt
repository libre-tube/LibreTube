package com.github.libretube.db

import android.os.Build
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE 'localPlaylist' ADD COLUMN 'description' TEXT DEFAULT NULL")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE 'playlistBookmark' ADD COLUMN 'videos' INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE 'subscriptionGroups' ADD COLUMN 'index' INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE 'downloadItem' ADD COLUMN 'language' TEXT DEFAULT NULL")
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE 'watchHistoryItem' ADD COLUMN 'isShort' INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE 'downloadChapters' (" +
                    "id INTEGER PRIMARY KEY NOT NULL, " +
                    "videoId TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "start INTEGER NOT NULL, " +
                    "thumbnailUrl TEXT NOT NULL" +
                    ")"
            )
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE 'downloadSponsorBlockSegment' (" +
                    "uuid TEXT PRIMARY KEY NOT NULL, " +
                    "videoId TEXT NOT NULL, " +
                    "actionType TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "description TEXT, " +
                    "locked INTEGER NOT NULL, " +
                    "startTime REAL NOT NULL, " +
                    "endTime REAL NOT NULL, " +
                    "videoDuration REAL NOT NULL, " +
                    "votes INTEGER NOT NULL, " +
                    "CONSTRAINT parentDownload FOREIGN KEY (videoId) REFERENCES download (videoId) ON DELETE CASCADE" +
                    ")"
            )
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE 'download' ADD COLUMN 'uploaderUrl' TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE 'download' ADD COLUMN 'views' INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE 'download' ADD COLUMN 'likes' INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE 'download' ADD COLUMN 'dislikes' INTEGER NOT NULL DEFAULT -1")
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // `ALTER TABLE ... DROP COLUMN` requires SQLite >= 3.35 (Android 12+).
            // On older devices the table must be rebuilt to drop the `url` column.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                db.execSQL("ALTER TABLE 'downloadItem' DROP COLUMN 'url'")
            } else {
                val rebuildSql = """
                    CREATE TABLE IF NOT EXISTS downloadItem_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        type TEXT NOT NULL,
                        videoId TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        path TEXT NOT NULL,
                        format TEXT ,
                        quality TEXT ,
                        language TEXT ,
                        downloadSize INTEGER NOT NULL,
                        FOREIGN KEY(videoId) REFERENCES download(videoId) ON DELETE CASCADE
                    )
                """.trimIndent()
                db.execSQL(rebuildSql)
                db.execSQL(
                    """
                    INSERT INTO downloadItem_new (
                        id, type, videoId, fileName, path, format, quality, language, downloadSize
                    )
                    SELECT
                        id, type, videoId, fileName, path, format, quality, language, downloadSize
                    FROM downloadItem
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE downloadItem")
                db.execSQL("ALTER TABLE downloadItem_new RENAME TO downloadItem")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_downloadItem_path ON downloadItem(path)"
                )
            }
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_17_18,
        MIGRATION_21_22,
        MIGRATION_22_23,
        MIGRATION_23_24
    )
}
