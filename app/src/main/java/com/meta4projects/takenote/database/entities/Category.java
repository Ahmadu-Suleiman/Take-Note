package com.meta4projects.takenote.database.entities;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.meta4projects.takenote.database.NoteDatabase;

import java.util.ArrayList;

@Entity(tableName = "categories")
public class Category {

    @Ignore
    private final ArrayList<Note> notesInCategory;
    @ColumnInfo(name = "category_id")
    @PrimaryKey(autoGenerate = true)
    private int categoryId;
    @ColumnInfo(name = "category_name")
    private String name;

    public Category(String name) {
        this.name = name;
        this.notesInCategory = new ArrayList<>();
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Note> getNotesInCategory() {
        return notesInCategory;
    }

// --Commented out by Inspection START (1/7/2021 9:29 PM):
//    public void setNotesInCategory(ArrayList<Note> notesInCategory) {
//        this.notesInCategory = notesInCategory;
//    }
// --Commented out by Inspection STOP (1/7/2021 9:29 PM)

    /**
     * called from a thread in order to update the notes in this category
     * at runtime
     *
     * @param context used to access dao methods for querying the category table
     */
    public void update(Context context) {
        notesInCategory.clear();
        notesInCategory.addAll(NoteDatabase.getINSTANCE(context).noteDao().getNotesInCategory(name));
    }
}
