package com.github.libretube.db

import androidx.room.TypeConverter
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

object Converters {
    @TypeConverter
    fun localDateToString(localDate: LocalDate?) = localDate?.toString()

    @TypeConverter
    fun stringToLocalDate(string: String?) = string?.toLocalDate()

    @TypeConverter
    fun pathToString(path: Path?) = path?.toString()

    @TypeConverter
    fun stringToPath(string: String?) = string?.let { Paths.get(it) }
}
