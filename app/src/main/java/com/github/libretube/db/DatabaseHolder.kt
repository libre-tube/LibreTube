package com.github.libretube.db

import android.content.Context
import androidx.room.Room
import com.github.libretube.DATABASE_NAME

object DatabaseHolder {
    lateinit var db: AppDatabase

    fun initializeDatabase(context: Context) {
        db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .build()
    }
}
