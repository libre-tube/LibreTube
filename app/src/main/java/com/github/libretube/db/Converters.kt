package com.github.libretube.db

import androidx.room.TypeConverter
import com.github.libretube.api.JsonHelper
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate
import kotlinx.serialization.encodeToString

object Converters {
    @TypeConverter
    fun localDateToString(localDate: LocalDate?) = localDate?.toString()

    @TypeConverter
    fun stringToLocalDate(string: String?) = string?.toLocalDate()

    @TypeConverter
    fun pathToString(path: Path?) = path?.toString()

    @TypeConverter
    fun stringToPath(string: String?) = string?.let { Paths.get(it) }

    @TypeConverter
    fun stringListToJson(value: List<String>) = JsonHelper.json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String) = JsonHelper.json.decodeFromString<List<String>>(value)
}
