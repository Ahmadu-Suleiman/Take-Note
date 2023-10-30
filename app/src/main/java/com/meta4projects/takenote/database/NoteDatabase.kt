package com.meta4projects.takenote.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meta4projects.takenote.database.daos.CategoryDao
import com.meta4projects.takenote.database.daos.NoteDao
import com.meta4projects.takenote.database.entities.Category
import com.meta4projects.takenote.database.entities.Note

@Database(entities = [Note::class, Category::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        @JvmStatic
        fun getINSTANCE(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(NoteDatabase::class.java) {
                val instance = getDataBase(context)
                INSTANCE = instance
                instance
            }
        }

        private fun getDataBase(context: Context): NoteDatabase {
            return databaseBuilder(context.applicationContext, NoteDatabase::class.java, "note_database").addCallback(object :Callback(){
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("INSERT INTO categories(category_name) VALUES('Home')")
                    db.execSQL("INSERT INTO categories(category_name) VALUES('Work')")
                    db.execSQL("INSERT INTO categories(category_name) VALUES('Study')")
                    db.execSQL("INSERT INTO categories(category_name) VALUES('Ideas')")
                }
            }).build()
        }
    }
}