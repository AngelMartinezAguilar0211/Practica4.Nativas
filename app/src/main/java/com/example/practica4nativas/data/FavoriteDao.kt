package com.example.practica4nativas.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites")
    fun getAll(): LiveData<List<FavoriteFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteFile)

    @Delete
    suspend fun delete(favorite: FavoriteFile)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean
}
