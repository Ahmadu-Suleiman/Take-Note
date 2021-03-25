package com.meta4projects.takenote.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.meta4projects.takenote.database.daos.CategoryDao;
import com.meta4projects.takenote.database.daos.NoteDao;
import com.meta4projects.takenote.database.entities.Category;
import com.meta4projects.takenote.database.entities.Note;

@Database(entities = {Note.class, Category.class}, version = 1, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    private static volatile NoteDatabase INSTANCE;

    public static NoteDatabase getINSTANCE(final Context context) {
        if (INSTANCE == null) {
            synchronized (NoteDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = getDataBase(context);
                }
            }
        }

        return INSTANCE;
    }

    private static NoteDatabase getDataBase(final Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                NoteDatabase.class, "note_database").build();
    }

    public abstract NoteDao noteDao();

    public abstract CategoryDao categoryDao();
}
