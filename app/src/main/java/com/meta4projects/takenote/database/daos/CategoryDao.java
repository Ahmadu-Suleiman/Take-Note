package com.meta4projects.takenote.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.meta4projects.takenote.database.entities.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategory(Category category);

    @Insert
    void insertCategory(Category... categories);

    @Update
    void updateCategory(Category category);

    @Query("SELECT * FROM categories WHERE category_name = :name")
    Category getCategory(String name);

    @Query("SELECT category_name FROM categories")
    List<String> getAllCategoryNames();

    @Delete
    void deleteCategory(Category category);

    @Query("SELECT * FROM categories ORDER BY category_id ASC")
    List<Category> getAllCategories();
}
