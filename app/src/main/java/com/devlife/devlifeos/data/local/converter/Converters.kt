package com.devlife.devlifeos.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database
 */
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringSet(value: Set<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringSet(value: String): Set<Int> {
        val type = object : TypeToken<Set<Int>>() {}.type
        return gson.fromJson(value, type) ?: emptySet()
    }
}
