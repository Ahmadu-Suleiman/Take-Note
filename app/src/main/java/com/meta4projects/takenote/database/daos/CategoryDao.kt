package com.meta4projects.takenote.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meta4projects.takenote.database.entities.Category

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: Category)

    @Insert
    fun insertCategory(vararg categories: Category)

    @Update
    fun updateCategory(category: Category)

    @Query("SELECT * FROM categories WHERE category_name = :name")
    fun getCategory(name: String?): Category

    @get:Query("SELECT category_name FROM categories")
    val allCategoryNames: List<String>

    @Delete
    fun deleteCategory(category: Category)

    @get:Query("SELECT * FROM categories ORDER BY category_id ASC")
    val allCategories: List<Category>
}