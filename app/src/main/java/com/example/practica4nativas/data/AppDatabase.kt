package com.example.practica4nativas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteFile::class, RecentFile::class], version = 2)

abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "file_explorer_db"
                )
                    .fallbackToDestructiveMigration() // ⚠️ Esto borra y recrea la BD si hay cambios
                    .build().also { INSTANCE = it }
            }
        }

    }
}
