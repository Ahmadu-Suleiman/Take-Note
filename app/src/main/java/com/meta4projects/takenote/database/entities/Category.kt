package com.meta4projects.takenote.database.entities

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.meta4projects.takenote.database.NoteDatabase

@Entity(tableName = "categories")
class Category(@JvmField @field:ColumnInfo(name = "category_name") var name: String) {
    @Ignore
    var notesInCategory: ArrayList<Note?> = ArrayList()

    @JvmField
    @ColumnInfo(name = "category_id")
    @PrimaryKey(autoGenerate = true)
    var categoryId = 0

    fun update(context: Context) {
        notesInCategory.clear()
        notesInCategory.addAll(NoteDatabase.getINSTANCE(context).noteDao().getNotesInCategory(name))
    }
}