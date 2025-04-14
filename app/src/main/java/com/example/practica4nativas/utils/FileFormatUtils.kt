package com.example.practica4nativas.utils

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import java.io.BufferedReader
import java.io.InputStreamReader

object FileFormatUtils {

    fun leerContenido(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = reader.readText()
        reader.close()
        return content
    }

    fun aplicarColoreadoJSON(texto: String): Spannable {
        val spannable = SpannableStringBuilder(texto)

        val regexKeys = Regex("(?<=\"|\\s)([\\w\\d_]+)(?=\":)")
        val regexStrings = Regex(":\\s*\"(.*?)\"")
        val regexNumbers = Regex(":\\s*\\d+")

        regexKeys.findAll(texto).forEach {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#8800AA")),
                it.range.first,
                it.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        regexStrings.findAll(texto).forEach {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#009900")),
                it.range.first,
                it.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        regexNumbers.findAll(texto).forEach {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#0000AA")),
                it.range.first,
                it.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    fun aplicarColoreadoXML(texto: String): Spannable {
        val spannable = SpannableStringBuilder(texto)

        val regexTags = Regex("<[^>]+>")
        val regexStrings = Regex(">[^<]+<")

        regexTags.findAll(texto).forEach {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#AA5500")),
                it.range.first,
                it.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        regexStrings.findAll(texto).forEach {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#0066CC")),
                it.range.first,
                it.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }
}
