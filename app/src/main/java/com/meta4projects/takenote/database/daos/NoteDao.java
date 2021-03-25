package com.meta4projects.takenote.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.meta4projects.takenote.database.entities.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Query("SELECT * FROM notes WHERE note_id = :noteId")
    Note getNote(int noteId);

    @Update
    void updateNote(Note note);

    @Update
    void updateNotes(List<Note> notes);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM notes WHERE note_in_trash = 0 ORDER BY note_date_time DESC")
    List<Note> getAllNotes();

    @Query("SELECT * FROM notes WHERE note_in_trash = 1 ORDER BY note_date_time DESC")
    List<Note> getAllNotesInTrash();

    @Query("SELECT * FROM notes WHERE note_category_name = :categoryName AND note_in_trash = 0 ORDER BY note_date_time DESC")
    List<Note> getNotesInCategory(String categoryName);
}
