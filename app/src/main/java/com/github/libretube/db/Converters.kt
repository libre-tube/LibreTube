package com.github.libretube.db

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

object Converters {
    @TypeConverter
    fun localDateToString(localDate: LocalDate?) = localDate?.toString()

    @TypeConverter
    fun stringToLocalDate(string: String?) = string?.toLocalDate()
}
