package com.example.practica4nativas.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practica4nativas.adapters.FileListAdapter
import com.example.practica4nativas.data.AppDatabase
import com.example.practica4nativas.data.FileMetadata
import com.example.practica4nativas.data.FavoriteFile
import com.example.practica4nativas.databinding.ActivityFavoritesBinding
import com.example.practica4nativas.utils.ThemeManager
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var adapter: FileListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Favoritos"
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val favDao = AppDatabase.getInstance(this).favoriteDao()

        adapter = FileListAdapter(
            onItemClick = { openFile(it) },
            isFavorite = { favDao.isFavorite(it) },
            onFavoriteToggle = { file, add ->
                val fav = FavoriteFile(file.uri.toString(), file.name)
                if (add) favDao.insert(fav) else favDao.delete(fav)
            },
            onRename = { showNotSupportedToast() },
            onDelete = { showNotSupportedToast() },
            onCopy = { showNotSupportedToast() },
            onMove = { showNotSupportedToast() }
        )


        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            favDao.getAll().observe(this@FavoritesActivity) { lista ->
                val metadataList = lista.map {
                    FileMetadata(
                        uri = Uri.parse(it.uri),
                        name = it.name,
                        isDirectory = false, // no lo necesitamos aquí
                        size = 0L,
                        lastModified = 0L
                    )
                }
                adapter.submitList(metadataList)
            }
        }
    }

    private fun openFile(fileMeta: FileMetadata) {
        val intent = Intent(this, TextViewerActivity::class.java).apply {
            putExtra("fileUri", fileMeta.uri)
        }
        startActivity(intent)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // regresa a la actividad anterior
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private fun showNotSupportedToast() {
        Toast.makeText(this, "Esta acción no está disponible en esta vista", Toast.LENGTH_SHORT).show()
    }

}
