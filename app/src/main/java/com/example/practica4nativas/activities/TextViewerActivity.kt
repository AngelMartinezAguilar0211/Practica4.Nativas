package com.example.practica4nativas.activities

import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practica4nativas.databinding.ActivityTextViewerBinding
import com.example.practica4nativas.utils.FileFormatUtils
import com.example.practica4nativas.utils.FileUtils
import com.example.practica4nativas.utils.ThemeManager

class TextViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityTextViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Visor de Texto"

        val uri = intent.getParcelableExtra<Uri>("fileUri")

        if (uri != null) {
            try {
                val tipoMime = contentResolver.getType(uri) ?: "text/plain"
                val contenido = FileFormatUtils.leerContenido(this, uri)
                binding.textTitle.text = FileUtils.getDocumentName(this, uri) ?: "Archivo de texto"

                val textoFormateado: Spannable = when {
                    tipoMime.contains("json") -> FileFormatUtils.aplicarColoreadoJSON(contenido)
                    tipoMime.contains("xml") -> FileFormatUtils.aplicarColoreadoXML(contenido)
                    else -> SpannableStringBuilder(contenido)
                }

                binding.textContent.text = textoFormateado

            } catch (e: Exception) {
                Toast.makeText(this, "Error al leer el archivo", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "URI no válida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
