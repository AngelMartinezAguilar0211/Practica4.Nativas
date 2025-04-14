package com.example.practica4nativas.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatSize(size: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format("%.2f GB", size / gb)
            size >= mb -> String.format("%.2f MB", size / mb)
            size >= kb -> String.format("%.2f KB", size / kb)
            else -> "$size B"
        }
    }

    fun getMimeType(context: Context, uri: Uri): String {
        // 1. Intenta obtener el tipo MIME directamente del ContentResolver
        val type = context.contentResolver.getType(uri)
        if (type != null) return type

        // 2. Si no está disponible, intenta con la extensión del archivo
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
            ?: "*/*"
    }

    fun getDocumentName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return null
    }

    fun obtenerRutaLegible(uri: Uri): String {
        val pathSegments = uri.pathSegments
        val index = pathSegments.indexOf("document")
        if (index != -1 && index + 1 < pathSegments.size) {
            return pathSegments[index + 1]
                .replace("primary:", "")
                .replace(":", "/")
        }
        return pathSegments[1]
    }

}
