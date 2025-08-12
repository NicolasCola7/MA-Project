package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.databinding.ItemPhotoBinding
import com.example.travel_companion.databinding.ItemPhotoDateHeaderBinding
import com.example.travel_companion.domain.model.PhotoGalleryItem

class PhotoAdapter(
    private val onSelectionChanged: (Int) -> Unit = {},
    private val onPhotoClick: (PhotoEntity) -> Unit = {}
) : ListAdapter<PhotoGalleryItem, RecyclerView.ViewHolder>(PhotoGalleryDiffCallback()) {

    private val selectedPhotos = mutableSetOf<PhotoEntity>()
    private var isSelectionMode = false

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_PHOTO = 1
        private const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
        private const val PAYLOAD_SELECTION_MODE_CHANGED = "selection_mode_changed"
        const val SPAN_COUNT = 3 // Per il GridLayoutManager
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PhotoGalleryItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is PhotoGalleryItem.Photo -> VIEW_TYPE_PHOTO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemPhotoDateHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                DateHeaderViewHolder(binding)
            }
            VIEW_TYPE_PHOTO -> {
                val binding = ItemPhotoBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                PhotoViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PhotoGalleryItem.DateHeader -> {
                (holder as DateHeaderViewHolder).bind(item)
            }
            is PhotoGalleryItem.Photo -> {
                (holder as PhotoViewHolder).bind(
                    item.photoEntity,
                    selectedPhotos.contains(item.photoEntity),
                    isSelectionMode
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && holder is PhotoViewHolder) {
            val item = getItem(position) as PhotoGalleryItem.Photo
            val photo = item.photoEntity
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

    /**
     * ViewHolder per le intestazioni delle date
     */
    inner class DateHeaderViewHolder(private val binding: ItemPhotoDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dateHeader: PhotoGalleryItem.DateHeader) {
            binding.dateText.text = dateHeader.formattedDate
            binding.photoCountText.text = "${dateHeader.photoCount} foto"
        }
    }

    /**
     * ViewHolder per le foto
     */
    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoEntity, isSelected: Boolean, isSelectionMode: Boolean) {
            // Carica l'immagine
            Glide.with(binding.root.context)
                .load(photo.uri)
                .centerCrop()
                .placeholder(R.drawable.ic_menu_gallery)
                .into(binding.imageView)

            updateSelectionVisuals(isSelected, isSelectionMode)
            setupClickListeners(photo)
        }

        fun updateSelectionVisuals(isSelected: Boolean, isSelectionMode: Boolean) {
            binding.root.alpha = if (isSelectionMode && !isSelected) 0.5f else 1.0f
        }

        fun updateSelectionMode(isSelectionMode: Boolean, isSelected: Boolean) {
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
            // Aggiorna solo le foto (non le intestazioni)
            notifyPhotoItemsChanged(PAYLOAD_SELECTION_MODE_CHANGED)
        } else {
            val position = findPhotoPosition(photo)
            if (position != -1) {
                notifyItemChanged(position, PAYLOAD_SELECTION_CHANGED)
            }
        }
    }

    private fun toggleSelection(photo: PhotoEntity) {
        val position = findPhotoPosition(photo)
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

        // Aggiorna solo le foto
        notifyPhotoItemsChanged(PAYLOAD_SELECTION_MODE_CHANGED)
    }

    private fun findPhotoPosition(photo: PhotoEntity): Int {
        return currentList.indexOfFirst {
            it is PhotoGalleryItem.Photo && it.photoEntity.id == photo.id
        }
    }

    private fun notifyPhotoItemsChanged(payload: String) {
        currentList.forEachIndexed { index, item ->
            if (item is PhotoGalleryItem.Photo) {
                notifyItemChanged(index, payload)
            }
        }
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
        val currentPhotos = currentList.filterIsInstance<PhotoGalleryItem.Photo>()
            .map { it.photoEntity }

        selectedPhotos.retainAll { photo ->
            currentPhotos.any { it.id == photo.id }
        }

        if (selectedPhotos.isEmpty()) {
            exitSelectionMode()
        } else if (selectedPhotos.size != previousSize) {
            onSelectionChanged(selectedPhotos.size)
            notifyPhotoItemsChanged(PAYLOAD_SELECTION_CHANGED)
        }
    }

    /**
     * SpanSizeLookup per il GridLayoutManager
     */
    class PhotoSpanSizeLookup(private val adapter: PhotoAdapter) : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (adapter.getItemViewType(position)) {
                VIEW_TYPE_DATE_HEADER -> SPAN_COUNT // L'intestazione occupa tutta la larghezza
                VIEW_TYPE_PHOTO -> 1 // Ogni foto occupa una cella
                else -> 1
            }
        }
    }

    class PhotoGalleryDiffCallback : DiffUtil.ItemCallback<PhotoGalleryItem>() {
        override fun areItemsTheSame(oldItem: PhotoGalleryItem, newItem: PhotoGalleryItem): Boolean {
            return when {
                oldItem is PhotoGalleryItem.DateHeader && newItem is PhotoGalleryItem.DateHeader ->
                    oldItem.date == newItem.date
                oldItem is PhotoGalleryItem.Photo && newItem is PhotoGalleryItem.Photo ->
                    oldItem.photoEntity.id == newItem.photoEntity.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: PhotoGalleryItem, newItem: PhotoGalleryItem): Boolean {
            return when {
                oldItem is PhotoGalleryItem.DateHeader && newItem is PhotoGalleryItem.DateHeader ->
                    oldItem == newItem
                oldItem is PhotoGalleryItem.Photo && newItem is PhotoGalleryItem.Photo ->
                    oldItem.photoEntity == newItem.photoEntity
                else -> false
            }
        }

        override fun getChangePayload(oldItem: PhotoGalleryItem, newItem: PhotoGalleryItem): Any? {
            return if (oldItem is PhotoGalleryItem.Photo && newItem is PhotoGalleryItem.Photo &&
                oldItem.photoEntity.id == newItem.photoEntity.id && oldItem != newItem) {
                PAYLOAD_SELECTION_CHANGED
            } else null
        }
    }
}