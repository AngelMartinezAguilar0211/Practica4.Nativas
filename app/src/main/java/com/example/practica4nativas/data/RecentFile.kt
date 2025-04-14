package com.example.practica4nativas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey val uri: String,
    val name: String,
    val lastOpened: Long = System.currentTimeMillis()
)
