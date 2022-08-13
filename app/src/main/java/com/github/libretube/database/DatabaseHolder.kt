package com.github.libretube.database

import android.content.Context
import androidx.room.Room
import com.github.libretube.DATABASE_NAME

object DatabaseHolder {
    lateinit var database: AppDatabase

    fun initializeDatabase(context: Context) {
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
