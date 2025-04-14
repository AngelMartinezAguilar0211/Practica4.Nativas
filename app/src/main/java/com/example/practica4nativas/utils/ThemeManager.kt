package com.example.practica4nativas.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.example.practica4nativas.R

object ThemeManager {
    private const val PREF_NAME = "theme_pref"
    private const val KEY_THEME = "selected_theme"

    const val THEME_GUINDA = "guinda"
    const val THEME_AZUL = "azul"

    fun applyTheme(activity: AppCompatActivity) {
        val prefs = getPrefs(activity)
        when (prefs.getString(KEY_THEME, THEME_GUINDA)) {
            THEME_AZUL -> activity.setTheme(R.style.Theme_Practica4Nativas_Azul)
            else -> activity.setTheme(R.style.Theme_Practica4Nativas_Guinda)
        }
    }

    fun toggleTheme(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getString(KEY_THEME, THEME_GUINDA)
        val next = if (current == THEME_GUINDA) THEME_AZUL else THEME_GUINDA
        prefs.edit().putString(KEY_THEME, next).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    fun resolveThemeColor(context: Context, attr: Int): Int {
        val typedArray = context.theme.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }

}
