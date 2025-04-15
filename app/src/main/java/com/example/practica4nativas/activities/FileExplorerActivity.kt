package com.example.practica4nativas.activities

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practica4nativas.R
import com.example.practica4nativas.adapters.FileListAdapter
import com.example.practica4nativas.data.AppDatabase
import com.example.practica4nativas.data.FavoriteFile
import com.example.practica4nativas.data.FileMetadata
import com.example.practica4nativas.data.RecentFile
import com.example.practica4nativas.databinding.ActivityFileExplorerBinding
import com.example.practica4nativas.utils.FileUtils
import com.example.practica4nativas.utils.ThemeManager
import kotlinx.coroutines.launch

class FileExplorerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileExplorerBinding
    private lateinit var adapter: FileListAdapter
    private lateinit var currentDirectoryUri: Uri

    private lateinit var moveLauncher: ActivityResultLauncher<Intent>
    private var fileToMove: FileMetadata? = null


    // Lista: nombre legible → (rootUri, childrenUri)
    private val navigationStack = mutableListOf<Pair<String, Pair<Uri, Uri>>>()

    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            // Guardar en SharedPreferences
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().putString("last_directory_uri", uri.toString()).apply()

            loadFilesFromUri(uri)

        } else {
            Toast.makeText(this, "No se seleccionó ninguna carpeta", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_file_explorer, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorites -> {
                startActivity(Intent(this, FavoritesActivity::class.java))
                true
            }
            R.id.action_theme -> {
                ThemeManager.toggleTheme(this)
                recreate() // recarga la actividad con el nuevo tema
                true
            }
            R.id.action_recent -> {
                startActivity(Intent(this, RecentActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        guardarArchivosDePruebaEnDocuments()
        binding = ActivityFileExplorerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔧 Configura el Toolbar con título
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Explorador de Archivos"

        // 🔧 Inicializa el adaptador
        val dao = AppDatabase.getInstance(this).favoriteDao()
        adapter = FileListAdapter(
            onItemClick = { onFileClick(it) },
            isFavorite = { dao.isFavorite(it) },
            onFavoriteToggle = { file, add ->
                val fav = FavoriteFile(file.uri.toString(), file.name)
                if (add) dao.insert(fav) else dao.delete(fav)
            },
            onRename = { onRename(it) },
            onDelete = { onDelete(it) },
            onCopy = { onCopy(it) },
            onMove = { onMove(it) }
        )


        // 🔧 Configura el RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 🧠 Verifica si hay carpeta guardada
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val lastUriString = prefs.getString("last_directory_uri", null)
        moveLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val targetUri = result.data?.data
                val source = fileToMove ?: return@registerForActivityResult

                try {
                    val inputStream = contentResolver.openInputStream(source.uri)
                    val type = contentResolver.getType(source.uri) ?: "application/octet-stream"
                    val targetDir = DocumentFile.fromTreeUri(this, targetUri!!)
                    val newFile = targetDir?.createFile(type, source.name)

                    if (inputStream != null && newFile != null) {
                        val outputStream = contentResolver.openOutputStream(newFile.uri)
                        inputStream.copyTo(outputStream!!)
                        inputStream.close()
                        outputStream.close()

                        // Eliminar original
                        DocumentFile.fromSingleUri(this, source.uri)?.delete()

                        // Actualizar favoritos y recientes
                        lifecycleScope.launch {
                            val db = AppDatabase.getInstance(this@FileExplorerActivity)
                            db.favoriteDao().delete(FavoriteFile(source.uri.toString(), source.name))
                            db.favoriteDao().insert(FavoriteFile(newFile.uri.toString(), newFile.name ?: source.name))
                            db.recentDao().deleteByUri(source.uri.toString())
                            db.recentDao().insert(RecentFile(newFile.uri.toString(), newFile.name ?: source.name))
                        }

                        Toast.makeText(this, "Archivo movido", Toast.LENGTH_SHORT).show()
                        currentDirectoryUri?.let { val last = navigationStack.lastOrNull()
                            if (last != null) {
                                val (label, uris) = last
                                loadFilesFromDirectoryUri(uris.first, uris.second)
                            } }

                    } else {
                        Toast.makeText(this, "Error al mover archivo", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al mover archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (lastUriString != null) {
            val uri = Uri.parse(lastUriString)
            val persisted = contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }

            if (persisted) {
                loadFilesFromUri(uri)
                return
            }
        }

        // 🗂️ Si no hay carpeta previa o sin permiso, pedirla
        openDocumentTreeLauncher.launch(null)

    }



    private fun loadFilesFromUri(uri: Uri) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
        navigationStack.clear()
        val name = FileUtils.obtenerRutaLegible(uri).split("/").lastOrNull() ?: "Raíz"
        navigationStack.add(Pair(name, Pair(uri, childrenUri)))
        loadFilesFromDirectoryUri(uri, childrenUri)
    }

    private fun loadFilesFromDirectoryUri(rootUri: Uri, childrenUri: Uri) {

        val children = mutableListOf<FileMetadata>()

        val cursor = contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val documentId = it.getString(1)
                val mime = it.getString(2)
                val size = it.getLong(3)
                val lastModified = it.getLong(4)

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, documentId)

                children.add(
                    FileMetadata(
                        uri = documentUri,
                        name = name,
                        isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        size = size,
                        lastModified = lastModified
                    )
                )
            }
        }

        adapter.submitList(children)
        binding.emptyText.visibility = if (children.isEmpty()) View.VISIBLE else View.GONE

        actualizarBreadcrumbs()
    }

    private fun onFileClick(fileMeta: FileMetadata) {
        if (fileMeta.isDirectory) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                fileMeta.uri,
                DocumentsContract.getDocumentId(fileMeta.uri)
            )
            val name = fileMeta.name
            navigationStack.add(Pair(name, Pair(fileMeta.uri, childrenUri)))
            loadFilesFromDirectoryUri(fileMeta.uri, childrenUri)
            currentDirectoryUri = fileMeta.uri


        } else {
            val recent = RecentFile(fileMeta.uri.toString(), fileMeta.name)
            lifecycleScope.launch {
                AppDatabase.getInstance(this@FileExplorerActivity).recentDao().insert(recent)
            }
            val mime = FileUtils.getMimeType(this, fileMeta.uri)
            if (mime.contains("json") || mime.contains("xml") || mime.startsWith("text")) {
                val intent = Intent(this, TextViewerActivity::class.java).apply {
                    putExtra("fileUri", fileMeta.uri)
                }
                startActivity(intent)
            } else {
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileMeta.uri, mime)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
                }

                try {
                    startActivity(openIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No se puede abrir este archivo", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onBackPressed() {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
            val (_, pair) = navigationStack.last()
            loadFilesFromDirectoryUri(pair.first, pair.second)
        } else {
            super.onBackPressed()
        }
    }

    private fun actualizarBreadcrumbs() {
        binding.breadcrumbLayout.removeAllViews()

        val primaryColor = ThemeManager.resolveThemeColor(this, com.google.android.material.R.attr.colorPrimary)

        navigationStack.forEachIndexed { index, (nombre, pair) ->
            val textView = TextView(this).apply {
                text = "📁 $nombre"
                setTextColor(resources.getColor(android.R.color.white, theme))
                textSize = 14f
                background = resources.getDrawable(R.drawable.breadcrumb_item_background, theme)
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    navigationStack.subList(index + 1, navigationStack.size).clear()
                    loadFilesFromDirectoryUri(pair.first, pair.second)
                }
            }

            binding.breadcrumbLayout.addView(textView)

            if (index < navigationStack.size - 1) {
                val separator = TextView(this).apply {
                    text = " > "
                    setTextColor(primaryColor)
                    textSize = 14f
                    setPadding(8, 8, 8, 8)
                }
                binding.breadcrumbLayout.addView(separator)
            }
        }

        binding.breadcrumbScroll.post {
            binding.breadcrumbScroll.fullScroll(View.FOCUS_RIGHT)
        }
    }



    private fun guardarArchivosDePruebaEnDocuments() {
        val archivos = listOf(
            Triple("ejemplo.txt", R.raw.ejemplo_texto, "text/plain"),
            Triple("ejemplo.json", R.raw.ejemplo_json, "application/json"),
            Triple("ejemplo.xml", R.raw.ejemplo_xml, "application/xml")
        )

        val resolver = contentResolver

        archivos.forEach { (nombre, rawId, mimeType) ->
            val existing = resolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf("Documents/", nombre),
                null
            )

            val yaExiste = existing?.use { it.moveToFirst() } == true
            if (yaExiste) return@forEach

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    resources.openRawResource(rawId).use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
    private fun onDelete(file: FileMetadata) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar archivo")
            .setMessage("¿Estás segura de que quieres eliminar \"${file.name}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                try {
                    val docFile = DocumentFile.fromSingleUri(this, file.uri)
                    if (docFile?.delete() == true) {
                        Toast.makeText(this, "Archivo eliminado", Toast.LENGTH_SHORT).show()

                        // Eliminar también de favoritos y recientes
                        val db = AppDatabase.getInstance(this)
                        lifecycleScope.launch {
                            db.recentDao().deleteByUri(file.uri.toString())
                            db.favoriteDao().delete(FavoriteFile(file.uri.toString(), file.name))
                        }

                        // Recargar la carpeta actual
                        val last = navigationStack.lastOrNull()
                        if (last != null) {
                            val (label, uris) = last
                            loadFilesFromDirectoryUri(uris.first, uris.second)
                        }

                    } else {
                        Toast.makeText(this, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al eliminar el archivo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun onRename(file: FileMetadata) {
        currentDirectoryUri = file.uri
        val input = EditText(this).apply {
            setText(file.name)
            setSelection(file.name.lastIndexOf('.').takeIf { it > 0 } ?: file.name.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Renombrar archivo")
            .setView(input)
            .setPositiveButton("Renombrar") { _, _ ->
                val nuevoNombre = input.text.toString()
                if (nuevoNombre.isNotBlank() && nuevoNombre != file.name) {
                    val treeRoot = DocumentFile.fromTreeUri(this, currentDirectoryUri!!)
                    val targetFile = treeRoot?.findFile(file.name)

                    val renamed = targetFile?.renameTo(nuevoNombre) ?: false

                    if (renamed) {
                        val renamedFile = treeRoot?.findFile(nuevoNombre)

                        renamedFile?.let { newDoc ->
                            val newUri = newDoc.uri
                            val newName = newDoc.name ?: nuevoNombre

                            lifecycleScope.launch {
                                val db = AppDatabase.getInstance(this@FileExplorerActivity)

                                db.favoriteDao().delete(FavoriteFile(file.uri.toString(), file.name))
                                db.favoriteDao().insert(FavoriteFile(newUri.toString(), newName))

                                db.recentDao().deleteByUri(file.uri.toString())
                                db.recentDao().insert(RecentFile(newUri.toString(), newName))
                            }

                            Toast.makeText(this, "Archivo renombrado", Toast.LENGTH_SHORT).show()
                            val last = navigationStack.lastOrNull()
                            if (last != null) {
                                val (label, uris) = last
                                loadFilesFromDirectoryUri(uris.first, uris.second)
                            }

                        } ?: Toast.makeText(this, "Renombrado, pero no se pudo actualizar registros", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, "No se pudo renombrar", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "Nombre inválido o sin cambios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun onCopy(file: FileMetadata) {
        currentDirectoryUri = file.uri
        try {
            val srcUri = file.uri
            val inputStream = contentResolver.openInputStream(srcUri)

            if (inputStream == null) {
                Toast.makeText(this, "No se pudo abrir el archivo de origen", Toast.LENGTH_SHORT).show()
                return
            }

            val extension = file.name.substringAfterLast('.', "")
            val baseName = file.name.removeSuffix(".$extension")
            val newName = "${baseName}_copia${if (extension.isNotEmpty()) ".$extension" else ""}"

            val treeUri = currentDirectoryUri
            if (treeUri == null) {
                Toast.makeText(this, "Carpeta actual no disponible", Toast.LENGTH_SHORT).show()
                return
            }

            val currentDir = DocumentFile.fromTreeUri(this, treeUri)
            if (currentDir == null || !currentDir.isDirectory || !currentDir.canWrite()) {
                Toast.makeText(this, "No se puede escribir en la carpeta actual", Toast.LENGTH_SHORT).show()
                return
            }

            // Crear archivo nuevo
            val mimeType = contentResolver.getType(srcUri) ?: "*/*"
            val newFile = currentDir.createFile(mimeType, newName)

            if (newFile == null) {
                Toast.makeText(this, "No se pudo crear el archivo", Toast.LENGTH_SHORT).show()
                return
            }

            val outputStream = contentResolver.openOutputStream(newFile.uri)
            if (outputStream == null) {
                Toast.makeText(this, "No se pudo abrir el destino", Toast.LENGTH_SHORT).show()
                return
            }

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            Toast.makeText(this, "Archivo copiado", Toast.LENGTH_SHORT).show()
            val last = navigationStack.lastOrNull()
            if (last != null) {
                val (label, uris) = last
                loadFilesFromDirectoryUri(uris.first, uris.second)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al copiar archivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun onMove(file: FileMetadata) {
        currentDirectoryUri = file.uri
        fileToMove = file
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (::currentDirectoryUri.isInitialized) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, currentDirectoryUri)
        }
        moveLauncher.launch(intent)
    }
}