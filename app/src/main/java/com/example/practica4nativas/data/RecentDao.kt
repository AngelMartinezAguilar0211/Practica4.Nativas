package com.example.practica4nativas.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RecentDao {

    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC")
    fun getAll(): LiveData<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recent: RecentFile)

    @Query("DELETE FROM recent_files WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}
