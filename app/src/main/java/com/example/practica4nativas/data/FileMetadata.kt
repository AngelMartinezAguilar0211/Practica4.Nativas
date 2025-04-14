package com.example.practica4nativas.data

import android.net.Uri

data class FileMetadata(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)
