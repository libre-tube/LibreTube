package com.github.libretube.db

import androidx.room.TypeConverter
import com.github.libretube.api.JsonHelper
import com.github.libretube.extensions.toLocalDateSafe
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import java.nio.file.Path
import kotlin.io.path.Path

object Converters {
    @TypeConverter
    fun localDateToString(localDate: LocalDate?) = localDate?.toString()

    @TypeConverter
    fun stringToLocalDate(string: String?) = string?.toLocalDateSafe()

    @TypeConverter
    fun pathToString(path: Path?) = path?.toString()

    @TypeConverter
    fun stringToPath(string: String?) = string?.let { Path(it) }

    @TypeConverter
    fun stringListToJson(value: List<String>) = JsonHelper.json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String) = JsonHelper.json.decodeFromString<List<String>>(value)
}
