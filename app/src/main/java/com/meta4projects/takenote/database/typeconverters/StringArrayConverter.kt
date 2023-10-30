package com.meta4projects.takenote.database.typeconverters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meta4projects.takenote.database.Lists

class StringArrayConverter {
    @TypeConverter
    fun fromStringArray(stringArray: Lists.Items?): String? {
        if (stringArray == null) return null
        val gson = Gson()
        val type = object : TypeToken<Lists.Items?>() {}.type
        return gson.toJson(stringArray, type)
    }

    @TypeConverter
    fun toStringArray(value: String?): Lists.Items? {
        if (value == null) return null
        val gson = Gson()
        val type = object : TypeToken<Lists.Items?>() {}.type
        return gson.fromJson(value, type)
    }
}