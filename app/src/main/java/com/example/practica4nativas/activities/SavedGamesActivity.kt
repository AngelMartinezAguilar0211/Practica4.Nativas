package com.example.practica4nativas.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practica4nativas.databinding.ActivitySavedGamesBinding
import com.example.practica4nativas.utils.ThemeManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SavedGamesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedGamesBinding
    private var archivos: List<File> = emptyList()
    private var tipoSeleccionado: String = "json"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySavedGamesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Seleccionar Partida"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tipos = listOf("json", "xml", "txt")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTipo.adapter = spinnerAdapter

        binding.spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                tipoSeleccionado = tipos[position]
                mostrarArchivos()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun mostrarArchivos() {
        archivos = filesDir.listFiles()?.filter {
            it.extension == tipoSeleccionado
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ArchivosAdapter(archivos) { archivo ->
            val intent = Intent()
            intent.putExtra("archivoSeleccionado", archivo.name)
            intent.putExtra("tipoArchivo", tipoSeleccionado)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class ArchivosAdapter(
        private val archivos: List<File>,
        private val onClick: (File) -> Unit
    ) : RecyclerView.Adapter<ArchivosAdapter.ArchivoViewHolder>() {

        class ArchivoViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
            return ArchivoViewHolder(view)
        }

        override fun onBindViewHolder(holder: ArchivoViewHolder, position: Int) {
            val archivo = archivos[position]
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(archivo.lastModified()))
            holder.view.text = "${archivo.name}\nÚltima modificación: $date"
            holder.view.setOnClickListener { onClick(archivo) }
        }

        override fun getItemCount() = archivos.size
    }
}
