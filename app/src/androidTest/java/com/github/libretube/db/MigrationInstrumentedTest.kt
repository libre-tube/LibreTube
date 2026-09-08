package com.github.libretube.db

import android.content.Context
import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runtime Room migration validation against the exported schemas.
 *
 * These tests require a device or emulator (instrumentation) and run in CI.
 * On every modern device (SDK >= 31) the manual 23->24 migration takes the SQLite >= 3.35
 * path (`ALTER TABLE ... DROP COLUMN`); the pre-31 rebuild path can only be exercised on an
 * old emulator image and is kept in DatabaseMigrations by inspection.
 */
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {
    private val testDb = "migration-test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList<AutoMigrationSpec>(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate23To24dropsUrlAndKeepsRows() {
        helper.createDatabase(testDb, 23).apply {
            execSQL(
                "INSERT INTO downloadItem " +
                    "(id, type, videoId, fileName, path, url, format, quality, language, downloadSize) " +
                    "VALUES (1, 'video', 'vid1', 'f.mp4', '/p/f.mp4', 'http://x/y.mp4', 'mp4', '720p', 'en', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 24, true, DatabaseMigrations.MIGRATION_23_24)
        db.query("SELECT id, downloadSize FROM downloadItem WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(true, c.count == 1)
            assertEquals(1000, c.getInt(1))
        }
        db.query("PRAGMA table_info(downloadItem)").use { c ->
            val columns = mutableListOf<String>()
            while (c.moveToNext()) {
                columns.add(c.getString(1))
            }
            assertFalse("url" in columns)
        }
    }

    @Test
    fun migrate24To25addsCurrentDownloadPositionMillis() {
        helper.createDatabase(testDb, 24).apply {
            execSQL(
                "INSERT INTO downloadItem " +
                    "(id, type, videoId, fileName, path, format, downloadSize) " +
                    "VALUES (1, 'video', 'vid1', 'f.mp4', '/p/f.mp4', 'mp4', 1000)"
            )
            close()
        }

        // 24->25 is an AutoMigration; the helper applies it from the generated database class
        val db = helper.runMigrationsAndValidate(testDb, 25, true)
        db.query("SELECT currentDownloadPositionMillis FROM downloadItem WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(true, c.count == 1)
            assertEquals(true, c.isNull(0))
        }
    }

    @Test
    fun migrate25To26() {
        helper.createDatabase(testDb, 25).close()
        val db = helper.runMigrationsAndValidate(testDb, 26, true)
        db.query("SELECT COUNT(*) FROM downloadItem").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
    }

    @Test
    fun migrate23To26fullChain() {
        helper.createDatabase(testDb, 23).apply {
            execSQL(
                "INSERT INTO downloadItem " +
                    "(id, type, videoId, fileName, path, url, format, downloadSize) " +
                    "VALUES (1, 'video', 'vid1', 'f.mp4', '/p/f.mp4', 'http://x/y.mp4', 'mp4', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 26, true, DatabaseMigrations.MIGRATION_23_24)
        db.query("SELECT id, downloadSize FROM downloadItem WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(true, c.count == 1)
            assertEquals(1000, c.getInt(1))
        }
    }
}