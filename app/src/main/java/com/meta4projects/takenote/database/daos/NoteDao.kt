package com.meta4projects.takenote.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meta4projects.takenote.database.entities.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(note: Note)

    @Query("SELECT * FROM notes WHERE note_id = :noteId")
    fun getNote(noteId: Int): Note

    @Update
    fun updateNote(note: Note)

    @Update
    fun updateNotes(notes: List<Note>)

    @Delete
    fun deleteNote(note: Note)

    @get:Query("SELECT * FROM notes WHERE note_in_trash = 0 ORDER BY note_date_time DESC")
    val allNotes: List<Note>

    @get:Query("SELECT * FROM notes WHERE note_in_trash = 1 ORDER BY note_date_time DESC")
    val allNotesInTrash: List<Note>

    @Query("SELECT * FROM notes WHERE note_category_name = :categoryName AND note_in_trash = 0 ORDER BY note_date_time DESC")
    fun getNotesInCategory(categoryName: String?): List<Note>

    @Query("DELETE FROM notes WHERE note_in_trash = 1")
    fun deleteAllTrashNotes()
}