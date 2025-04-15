package com.example.practica4nativas.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.practica4nativas.databinding.ActivityMainBinding
import com.example.practica4nativas.utils.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFileExplorer.setOnClickListener {
            startActivity(Intent(this, FileExplorerActivity::class.java))
        }

        binding.btnChangeTheme.setOnClickListener {
            ThemeManager.toggleTheme(this)
            recreate()
        }

        binding.btnSecondPart.setOnClickListener {
            startActivity(Intent(this, MemoryGameMenuActivity::class.java))
        }
    }
}
