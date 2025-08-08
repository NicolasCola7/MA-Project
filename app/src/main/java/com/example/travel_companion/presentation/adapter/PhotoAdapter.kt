package com.example.travel_companion.presentation.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.databinding.ItemPhotoBinding
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

class PhotoAdapter(
    private val onSelectionChanged: (Int) -> Unit = {}
) : ListAdapter<PhotoEntity, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    private val selectedPhotos = mutableSetOf<PhotoEntity>()
    var selectionMode = false
        private set

    fun getSelectedPhotos(): List<PhotoEntity> = selectedPhotos.toList()

    fun clearSelection() {
        if (selectedPhotos.isEmpty()) return

        val positionsToUpdate = mutableListOf<Int>()
        currentList.forEachIndexed { index, photo ->
            if (selectedPhotos.contains(photo)) {
                positionsToUpdate.add(index)
            }
        }

        selectedPhotos.clear()
        selectionMode = false
        positionsToUpdate.forEach { notifyItemChanged(it) }

        onSelectionChanged(0)
    }

    fun updateSelectionAfterListChange() {
        val currentIds = currentList.map { it.id }.toSet()
        val iterator = selectedPhotos.iterator()

        while (iterator.hasNext()) {
            val selected = iterator.next()
            if (!currentIds.contains(selected.id)) {
                iterator.remove()
            }
        }

        selectionMode = selectedPhotos.isNotEmpty()
        onSelectionChanged(selectedPhotos.size)
    }

    private fun toggleSelection(photo: PhotoEntity, position: Int) {
        if (selectedPhotos.contains(photo)) {
            selectedPhotos.remove(photo)
        } else {
            selectedPhotos.add(photo)
        }

        selectionMode = selectedPhotos.isNotEmpty()
        onSelectionChanged(selectedPhotos.size)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = getItem(position)
        val isSelected = selectedPhotos.contains(photo)
        holder.bind(photo, isSelected)

        holder.itemView.setOnClickListener {
            if (selectionMode) {
                toggleSelection(photo, position)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(photo, position)
            true
        }
    }

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoEntity, isSelected: Boolean) {
            Glide.with(binding.root.context)
                .load(Uri.parse(photo.uri))
                .centerCrop()
                .into(binding.imageView)

            updateSelectionUI(isSelected)
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            if (isSelected) {
                binding.imageView.alpha = 0.5f
                binding.imageView.setBackgroundColor(
                    binding.root.context.getColor(com.google.android.material.R.color.design_default_color_secondary)
                )
            } else {
                binding.imageView.alpha = 1.0f
                binding.imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
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