package com.github.libretube.db

import android.content.Context
import androidx.room.Room
import com.github.libretube.constants.DATABASE_NAME

class DatabaseHolder {
    fun initializeDatabase(context: Context) {
        Database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    companion object {
        lateinit var Database: AppDatabase
    }
}
