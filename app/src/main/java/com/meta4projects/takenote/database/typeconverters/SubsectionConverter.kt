package com.meta4projects.takenote.database.typeconverters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meta4projects.takenote.database.Lists

class SubsectionConverter {
    @TypeConverter
    fun fromSubsection(subsections: Lists.Subsections?): String? {
        if (subsections == null) return null
        val gson = Gson()
        val type = object : TypeToken<Lists.Subsections?>() {}.type
        return gson.toJson(subsections, type)
    }

    @TypeConverter
    fun toSubsection(value: String?): Lists.Subsections? {
        if (value == null) return null
        val gson = Gson()
        val type = object : TypeToken<Lists.Subsections?>() {}.type
        return gson.fromJson(value, type)
    }
}