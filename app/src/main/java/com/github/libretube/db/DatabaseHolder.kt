package com.github.libretube.db

import androidx.room.Room
import com.github.libretube.LibreTubeApp

object DatabaseHolder {
    private const val DATABASE_NAME = "LibreTubeDatabase"

    val Database by lazy {
        Room.databaseBuilder(LibreTubeApp.instance, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()
    }
}
