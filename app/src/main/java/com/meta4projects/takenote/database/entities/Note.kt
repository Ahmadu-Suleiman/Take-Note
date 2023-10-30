package com.meta4projects.takenote.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.meta4projects.takenote.database.typeconverters.StringArrayConverter
import com.meta4projects.takenote.database.Lists
import com.meta4projects.takenote.database.typeconverters.SubsectionConverter
import java.io.Serializable
import java.util.Objects

@Entity(tableName = "notes")
class Note : Serializable {
    @JvmField
    @ColumnInfo(name = "note_id")
    @PrimaryKey(autoGenerate = true)
    var noteId = 0

    @JvmField
    @ColumnInfo(name = "note_title")
    var title: String = ""

    @JvmField
    @ColumnInfo(name = "note_first_edittext_info")
    var firstEdittextInfo: String = ""

    @JvmField
    @ColumnInfo(name = "note_date_time")
    var dateTime: Long = 0

    @JvmField
    @ColumnInfo(name = "note_image_path")
    var imagePath: String? = null

    @JvmField
    @ColumnInfo(name = "note_category_name")
    var categoryName: String? = null

    @JvmField
    @ColumnInfo(name = "note_color")
    var color = 0

    @ColumnInfo(name = "note_in_trash")
    var isInTrash = false

    @JvmField
    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "note_viewtypes")
    var viewTypes: Lists.Items

    @JvmField
    @TypeConverters(SubsectionConverter::class)
    @ColumnInfo(name = "note_subsections")
    var subsections: Lists.Subsections

    @JvmField
    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "note_edittext_info")
    var editTextInfo: Lists.Items

    constructor(title: String, firstEdittextInfo: String) {
        this.title = title
        this.firstEdittextInfo = firstEdittextInfo
        viewTypes = Lists.Items()
        subsections = Lists.Subsections()
        editTextInfo = Lists.Items()
    }

    @Ignore
    constructor() {
        viewTypes = Lists.Items()
        subsections = Lists.Subsections()
        editTextInfo = Lists.Items()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val note = other as Note
        return noteId == note.noteId && dateTime == note.dateTime && color == note.color && isInTrash == note.isInTrash && title == note.title && firstEdittextInfo == note.firstEdittextInfo && imagePath == note.imagePath && categoryName == note.categoryName && viewTypes == note.viewTypes && subsections == note.subsections && editTextInfo == note.editTextInfo
    }

    override fun hashCode(): Int {
        return Objects.hash(noteId, title, firstEdittextInfo, dateTime, imagePath, categoryName, color, isInTrash, viewTypes, subsections, editTextInfo)
    }
}