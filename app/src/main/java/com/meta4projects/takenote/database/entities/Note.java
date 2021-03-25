package com.meta4projects.takenote.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.meta4projects.takenote.database.typeconverters.StringArrayConverter;
import com.meta4projects.takenote.database.typeconverters.SubsectionConverter;
import com.meta4projects.takenote.models.Subsection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

@Entity(tableName = "notes")
public class Note implements Serializable {

    @ColumnInfo(name = "note_id")
    @PrimaryKey(autoGenerate = true)
    private int noteId;

    @ColumnInfo(name = "note_title")
    private String title;

    @ColumnInfo(name = "note_first_edittext_info")
    private String firstEdittextInfo;

    @ColumnInfo(name = "note_date_time")
    private long dateTime;

    @ColumnInfo(name = "note_image_path")
    private String imagePath;

    @ColumnInfo(name = "note_category_name")
    private String categoryName;

    @ColumnInfo(name = "note_color")
    private int color;

    @ColumnInfo(name = "note_in_trash")
    private boolean inTrash;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "note_viewtypes")
    private ArrayList<String> viewTypes;

    @TypeConverters(SubsectionConverter.class)
    @ColumnInfo(name = "note_subsections")
    private ArrayList<Subsection> subsections;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "note_edittext_info")
    private ArrayList<String> editTextInfo;

    public Note(String title, String firstEdittextInfo) {
        this.title = title;
        this.firstEdittextInfo = firstEdittextInfo;

        viewTypes = new ArrayList<>();
        subsections = new ArrayList<>();
        editTextInfo = new ArrayList<>();
    }

    @Ignore
    public Note() {
        viewTypes = new ArrayList<>();
        subsections = new ArrayList<>();
        editTextInfo = new ArrayList<>();
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstEdittextInfo() {
        return firstEdittextInfo;
    }

    public void setFirstEdittextInfo(String firstEdittextInfo) {
        this.firstEdittextInfo = firstEdittextInfo;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isInTrash() {
        return inTrash;
    }

    public void setInTrash(boolean inTrash) {
        this.inTrash = inTrash;
    }

    public ArrayList<String> getViewTypes() {
        return viewTypes;
    }

    public void setViewTypes(ArrayList<String> viewTypes) {
        this.viewTypes = viewTypes;
    }

    public ArrayList<Subsection> getSubsections() {
        return subsections;
    }

    public void setSubsections(ArrayList<Subsection> subsections) {
        this.subsections = subsections;
    }

    public ArrayList<String> getEditTextInfo() {
        return editTextInfo;
    }

    public void setEditTextInfo(ArrayList<String> editTextInfo) {
        this.editTextInfo = editTextInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return noteId == note.noteId &&
                dateTime == note.dateTime &&
                color == note.color &&
                inTrash == note.inTrash &&
                Objects.equals(title, note.title) &&
                Objects.equals(firstEdittextInfo, note.firstEdittextInfo) &&
                Objects.equals(imagePath, note.imagePath) &&
                Objects.equals(categoryName, note.categoryName) &&
                Objects.equals(viewTypes, note.viewTypes) &&
                Objects.equals(subsections, note.subsections) &&
                Objects.equals(editTextInfo, note.editTextInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteId, title, firstEdittextInfo, dateTime, imagePath, categoryName, color, inTrash, viewTypes, subsections, editTextInfo);
    }
}
