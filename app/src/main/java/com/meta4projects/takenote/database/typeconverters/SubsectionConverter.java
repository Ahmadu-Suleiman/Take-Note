package com.meta4projects.takenote.database.typeconverters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.meta4projects.takenote.models.Subsection;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class SubsectionConverter {

    @TypeConverter
    public String fromSubsection(ArrayList<Subsection> subsections) {
        if (subsections == null) {
            return null;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Subsection>>() {
        }.getType();
        return gson.toJson(subsections, type);
    }

    @TypeConverter
    public ArrayList<Subsection> toSubsection(String value) {
        if (value == null) {
            return null;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Subsection>>() {
        }.getType();
        return gson.fromJson(value, type);
    }
}
