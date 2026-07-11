package com.avtar.cabbilling.data.local

import androidx.room.TypeConverter

/**
 * Persists a [List] of car names as a single delimited column.
 *
 * Uses a newline as the delimiter. Car names are entered through single-line
 * text fields, so a name can never itself contain a newline and corrupt the
 * encoding. Blank fragments are dropped on the way back in.
 */
class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String =
        list.orEmpty().joinToString(separator = "\n")

    @TypeConverter
    fun toStringList(data: String?): List<String> =
        if (data.isNullOrEmpty()) emptyList()
        else data.split("\n").filter { it.isNotBlank() }
}
