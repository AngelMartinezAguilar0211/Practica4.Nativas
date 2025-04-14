package com.example.practica4nativas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteFile(
    @PrimaryKey val uri: String,
    val name: String,
    val dateAdded: Long = System.currentTimeMillis()
)
