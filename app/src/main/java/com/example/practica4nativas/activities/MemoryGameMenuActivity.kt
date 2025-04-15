package com.example.practica4nativas.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practica4nativas.databinding.ActivityMemoryGameMenuBinding
import com.example.practica4nativas.utils.ThemeManager
import java.io.File

class MemoryGameMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryGameMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryGameMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Menú del Juego"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnNuevaPartida.setOnClickListener {
            val intent = Intent(this, MemoryGameActivity::class.java)
            intent.putExtra("modo", "nuevo")
            startActivity(intent)
        }

        binding.btnCargarPartida.setOnClickListener {
            val intent = Intent(this, SavedGamesActivity::class.java)
            startActivityForResult(intent, 1234)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234 && resultCode == RESULT_OK) {
            val archivo = data?.getStringExtra("archivoSeleccionado")
            val tipo = data?.getStringExtra("tipoArchivo")
            if (archivo != null && tipo != null) {
                val intent = Intent(this, MemoryGameActivity::class.java)
                intent.putExtra("modo", "archivo")
                intent.putExtra("archivo", archivo)
                intent.putExtra("tipo", tipo)
                startActivity(intent)
            }
        }
    }


}