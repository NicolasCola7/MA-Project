package com.example.travel_companion.presentation.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.databinding.ItemPhotoBinding
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

class PhotoAdapter(
    private val onSelectionChanged: (Int) -> Unit = {},
    private val onPhotoClick: (PhotoEntity) -> Unit = {}
) : ListAdapter<PhotoEntity, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    private val selectedPhotos = mutableSetOf<PhotoEntity>()
    private var isSelectionMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = getItem(position)
        holder.bind(photo, selectedPhotos.contains(photo), isSelectionMode)
    }

    // Payload per ottimizzare gli aggiornamenti
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val photo = getItem(position)
            when (payloads[0]) {
                PAYLOAD_SELECTION_CHANGED -> {
                    holder.updateSelectionVisuals(
                        isSelected = selectedPhotos.contains(photo),
                        isSelectionMode = isSelectionMode
                    )
                }
                PAYLOAD_SELECTION_MODE_CHANGED -> {
                    holder.updateSelectionMode(isSelectionMode, selectedPhotos.contains(photo))
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoEntity, isSelected: Boolean, isSelectionMode: Boolean) {
            // Carica l'immagine solo se necessario (ottimizzazione)
            Glide.with(binding.root.context)
                .load(photo.uri)
                .centerCrop()
                .placeholder(R.drawable.ic_menu_gallery)
                .into(binding.imageView)

            updateSelectionVisuals(isSelected, isSelectionMode)
            setupClickListeners(photo)
        }

        fun updateSelectionVisuals(isSelected: Boolean, isSelectionMode: Boolean) {
            // Aggiorna solo gli elementi visivi della selezione
            binding.root.alpha = if (isSelectionMode && !isSelected) 0.5f else 1.0f
        }

        fun updateSelectionMode(isSelectionMode: Boolean, isSelected: Boolean) {
            // Aggiorna solo quando cambia la modalità di selezione
            updateSelectionVisuals(isSelected, isSelectionMode)
        }

        private fun setupClickListeners(photo: PhotoEntity) {
            binding.root.setOnClickListener {
                if (!isSelectionMode) {
                    onPhotoClick(photo)
                } else {
                    toggleSelection(photo)
                }
            }

            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    enterSelectionMode(photo)
                } else {
                    toggleSelection(photo)
                }
                true
            }
        }
    }

    private fun enterSelectionMode(photo: PhotoEntity) {
        val wasInSelectionMode = isSelectionMode
        isSelectionMode = true
        selectedPhotos.add(photo)
        onSelectionChanged(selectedPhotos.size)

        if (!wasInSelectionMode) {
            // Aggiorna tutti gli item per mostrare la modalità selezione
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION_MODE_CHANGED)
        } else {
            // Aggiorna solo l'item selezionato
            val position = currentList.indexOf(photo)
            if (position != -1) {
                notifyItemChanged(position, PAYLOAD_SELECTION_CHANGED)
            }
        }
    }

    private fun toggleSelection(photo: PhotoEntity) {
        val position = currentList.indexOf(photo)
        if (position == -1) return

        if (selectedPhotos.contains(photo)) {
            selectedPhotos.remove(photo)
        } else {
            selectedPhotos.add(photo)
        }

        if (selectedPhotos.isEmpty()) {
            exitSelectionMode()
        } else {
            onSelectionChanged(selectedPhotos.size)
            notifyItemChanged(position, PAYLOAD_SELECTION_CHANGED)
        }
    }

    private fun exitSelectionMode() {
        if (!isSelectionMode) return

        isSelectionMode = false
        selectedPhotos.clear()
        onSelectionChanged(0)

        // Aggiorna tutti gli item per uscire dalla modalità selezione
        notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION_MODE_CHANGED)
    }

    fun clearSelection() {
        if (selectedPhotos.isNotEmpty() || isSelectionMode) {
            exitSelectionMode()
        }
    }

    fun getSelectedPhotos(): List<PhotoEntity> = selectedPhotos.toList()

    fun updateSelectionAfterListChange() {
        if (!isSelectionMode) return

        val previousSize = selectedPhotos.size
        selectedPhotos.retainAll { currentList.contains(it) }

        if (selectedPhotos.isEmpty()) {
            exitSelectionMode()
        } else if (selectedPhotos.size != previousSize) {
            onSelectionChanged(selectedPhotos.size)
            // Aggiorna solo gli item visibili se necessario
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION_CHANGED)
        }
    }

    // Aggiorna il DiffCallback per gestire i cambiamenti di selezione
    class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoEntity>() {
        override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: PhotoEntity, newItem: PhotoEntity): Any? {
            // Se i contenuti sono diversi, usa un payload specifico
            return if (oldItem.id == newItem.id && oldItem != newItem) {
                PAYLOAD_SELECTION_CHANGED
            } else null
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
        private const val PAYLOAD_SELECTION_MODE_CHANGED = "selection_mode_changed"
    }
}