package com.github.libretube.db

import androidx.room.Room
import com.github.libretube.LibreTubeApp
import com.github.libretube.constants.DATABASE_NAME

object DatabaseHolder {
    val Database by lazy {
        Room.databaseBuilder(LibreTubeApp.instance, AppDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
}
