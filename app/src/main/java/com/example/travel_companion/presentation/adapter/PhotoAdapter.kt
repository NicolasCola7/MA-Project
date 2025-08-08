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

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoEntity, isSelected: Boolean, isSelectionMode: Boolean) {
            Glide.with(binding.root.context)
                .load(photo.uri)
                .centerCrop()
                .into(binding.imageView)

            // Gestisci la modalità selezione visivamente
            binding.root.alpha = if (isSelectionMode && !isSelected) 0.5f else 1.0f

            // Click handlers
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
        isSelectionMode = true
        selectedPhotos.add(photo)
        onSelectionChanged(selectedPhotos.size)
        notifyDataSetChanged()
    }

    private fun toggleSelection(photo: PhotoEntity) {
        if (selectedPhotos.contains(photo)) {
            selectedPhotos.remove(photo)
        } else {
            selectedPhotos.add(photo)
        }

        if (selectedPhotos.isEmpty()) {
            exitSelectionMode()
        } else {
            onSelectionChanged(selectedPhotos.size)
            notifyItemChanged(currentList.indexOf(photo))
        }
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        exitSelectionMode()
    }

    fun getSelectedPhotos(): List<PhotoEntity> = selectedPhotos.toList()

    fun updateSelectionAfterListChange() {
        if (isSelectionMode) {
            selectedPhotos.retainAll { currentList.contains(it) }
            if (selectedPhotos.isEmpty()) {
                exitSelectionMode()
            } else {
                onSelectionChanged(selectedPhotos.size)
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoEntity>() {
        override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem == newItem
        }
    }
}