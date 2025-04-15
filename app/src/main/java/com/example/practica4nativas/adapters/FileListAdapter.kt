package com.example.practica4nativas.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.practica4nativas.R
import com.example.practica4nativas.data.FileMetadata
import com.example.practica4nativas.databinding.ItemFileBinding
import com.example.practica4nativas.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileListAdapter(
    private val onItemClick: (file: FileMetadata) -> Unit,
    private val isFavorite: suspend (String) -> Boolean,
    private val onFavoriteToggle: suspend (FileMetadata, Boolean) -> Unit,
    private val onRename: (FileMetadata) -> Unit,
    private val onDelete: (FileMetadata) -> Unit,
    private val onCopy: (FileMetadata) -> Unit,
    private val onMove: (FileMetadata) -> Unit
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {


    private var fileList: List<FileMetadata> = emptyList()

    fun submitList(files: List<FileMetadata>) {
        fileList = files
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(fileList[position])
    }

    override fun getItemCount(): Int = fileList.size

    inner class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fileMeta: FileMetadata) {
            binding.fileName.text = fileMeta.name

            val context = binding.root.context

            val extension = fileMeta.name.substringAfterLast('.', "").lowercase()
            val iconRes = when {
                fileMeta.isDirectory -> R.drawable.ic_folder
                extension == "txt" -> R.drawable.ic_file_text
                extension == "json" -> R.drawable.ic_file_json
                extension == "xml" -> R.drawable.ic_file_xml
                extension in listOf("jpg", "jpeg", "png") -> R.drawable.ic_file_image
                else -> R.drawable.ic_file_generic
            }
            binding.fileIcon.setImageResource(iconRes)

            val tipo = when {
                fileMeta.isDirectory -> "Carpeta"
                extension == "json" -> "Archivo JSON"
                extension == "xml" -> "Archivo XML"
                extension == "txt" -> "Texto plano"
                extension in listOf("jpg", "jpeg", "png", "gif") -> "Imagen"
                else -> "Archivo"
            }

            val extraInfo = if (fileMeta.isDirectory) {
                "Últ. mod: ${FileUtils.formatDate(fileMeta.lastModified)}"
            } else {
                "${FileUtils.formatSize(fileMeta.size)} - ${FileUtils.formatDate(fileMeta.lastModified)}"
            }

            binding.fileDetails.text = "$tipo · $extraInfo"

            // ⭐ Lógica de favoritos
            CoroutineScope(Dispatchers.Main).launch {
                val favorito = isFavorite(fileMeta.uri.toString())
                binding.favoriteIcon.setImageResource(
                    if (favorito) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                )

                binding.favoriteIcon.setOnClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        onFavoriteToggle(fileMeta, !favorito)
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            notifyItemChanged(position)
                        }
                    }
                }

                binding.optionsIcon.setOnClickListener { view ->
                    val popup = PopupMenu(view.context, view)
                    popup.inflate(R.menu.menu_file_options)

                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_rename -> {
                                onRename(fileMeta)
                                true
                            }
                            R.id.action_delete -> {
                                onDelete(fileMeta)
                                true
                            }
                            R.id.action_copy -> {
                                onCopy(fileMeta)
                                true
                            }
                            R.id.action_move -> {
                                onMove(fileMeta)
                                true
                            }
                            else -> false
                        }
                    }

                    popup.show()
                }

            }

            binding.root.setOnClickListener {
                onItemClick(fileMeta)
            }
        }
    }

}
