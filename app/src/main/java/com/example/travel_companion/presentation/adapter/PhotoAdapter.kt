package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.databinding.ItemPhotoBinding
import com.example.travel_companion.databinding.ItemPhotoDateHeaderBinding
import com.example.travel_companion.domain.model.PhotoGalleryItem
import com.example.travel_companion.presentation.adapter.base.BaseAdapter
import com.example.travel_companion.util.managers.SelectionManager

/*
L'adapter gestisce due tipi di elementi nella stessa RecyclerView:
    - DateHeader: Intestazioni con data e conteggio foto
    - Photo: Le foto vere e proprie
 */
class PhotoAdapter(
    onSelectionChanged: (Int) -> Unit = {},
    private val onPhotoClick: (PhotoEntity) -> Unit = {}
) : BaseAdapter<PhotoGalleryItem, RecyclerView.ViewHolder>(PhotoGalleryDiffCallback()) {

    // Uso SelectionManager con composizione solo per le foto
    private val photoSelectionManager = SelectionManager<PhotoEntity>(
        getItemId = { it.id },
        onSelectionChanged = onSelectionChanged,
        notifyItemChanged = { position, payload ->
            if (position < itemCount && getItem(position) is PhotoGalleryItem.Photo) {
                notifyItemChanged(position, payload)
            }
        }
    )

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_PHOTO = 1
        const val SPAN_COUNT = 3
    }

    //decide quale layout usare per ogni posizione
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PhotoGalleryItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is PhotoGalleryItem.Photo -> VIEW_TYPE_PHOTO
        }
    }

    //creo un viewHolder diverso in base al tipo
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

    override fun bindItem(holder: RecyclerView.ViewHolder, item: PhotoGalleryItem, position: Int) {
        when (item) {
            is PhotoGalleryItem.DateHeader -> {
                (holder as DateHeaderViewHolder).bind(item)
            }
            is PhotoGalleryItem.Photo -> {
                (holder as PhotoViewHolder).bind(
                    item.photoEntity,
                    photoSelectionManager.isSelected(item.photoEntity),
                    photoSelectionManager.isSelectionMode
                )
            }
        }
    }

    //aggiorno solo ciò che è cambiato (le foto), evitando di ricaricare tutto il viewHolder
    override fun handlePayloadUpdate(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && holder is PhotoViewHolder) {
            val item = getItem(position) as PhotoGalleryItem.Photo
            val photo = item.photoEntity
            when (payloads[0]) {
                PAYLOAD_SELECTION_CHANGED -> {
                    holder.updateSelectionVisuals(
                        isSelected = photoSelectionManager.isSelected(photo),
                        isSelectionMode = photoSelectionManager.isSelectionMode
                    )
                }
                PAYLOAD_SELECTION_MODE_CHANGED -> {
                    holder.updateSelectionMode(photoSelectionManager.isSelectionMode, photoSelectionManager.isSelected(photo))
                }
            }
        } else {
            super.handlePayloadUpdate(holder, position, payloads)
        }
    }

    override fun onItemClick(item: PhotoGalleryItem, position: Int) {
        if (item is PhotoGalleryItem.Photo) {
            if (!photoSelectionManager.isSelectionMode) {
                onPhotoClick(item.photoEntity)
            } else {
                photoSelectionManager.toggleSelection(item.photoEntity, position)
            }
        }
    }

    override fun onItemLongClick(item: PhotoGalleryItem, position: Int): Boolean {
        return if (item is PhotoGalleryItem.Photo) {
            if (!photoSelectionManager.isSelectionMode) {
                photoSelectionManager.enterSelectionMode(item.photoEntity, position) { payload ->
                    notifyPhotoItemsChanged(payload)
                }
            } else {
                photoSelectionManager.toggleSelection(item.photoEntity, position)
            }
            true
        } else {
            false
        }
    }

    private fun notifyPhotoItemsChanged(payload: Any) {
        currentList.forEachIndexed { index, item ->
            if (item is PhotoGalleryItem.Photo) {
                notifyItemChanged(index, payload)
            }
        }
    }

    // Metodi pubblici per gestire la selezione delle foto
    fun clearSelection() {
        if (photoSelectionManager.getSelectedCount() > 0 || photoSelectionManager.isSelectionMode) {
            photoSelectionManager.clearSelection { payload ->
                notifyPhotoItemsChanged(payload)
            }
        }
    }

    fun getSelectedPhotos(): List<PhotoEntity> = photoSelectionManager.getSelectedItems()

    fun updateSelectionAfterListChange() {
        if (!photoSelectionManager.isSelectionMode) return

        val currentPhotos = currentList.filterIsInstance<PhotoGalleryItem.Photo>()
            .map { it.photoEntity }

        photoSelectionManager.updateSelectionAfterListChange(currentPhotos) { payload ->
            notifyPhotoItemsChanged(payload)
        }
    }

    inner class DateHeaderViewHolder(private val binding: ItemPhotoDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dateHeader: PhotoGalleryItem.DateHeader) {
            binding.dateText.text = dateHeader.formattedDate
            binding.photoCountText.text = "${dateHeader.photoCount} foto"
        }
    }

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoEntity, isSelected: Boolean, isSelectionMode: Boolean) {
            Glide.with(binding.root.context)
                .load(photo.uri)
                .centerCrop()
                .placeholder(R.drawable.ic_menu_gallery)
                .into(binding.imageView)

            updateSelectionVisuals(isSelected, isSelectionMode)
        }

        fun updateSelectionVisuals(isSelected: Boolean, isSelectionMode: Boolean) {
            val selectionOverlay = binding.root.findViewById<View>(R.id.selection_overlay)
            selectionOverlay?.visibility = if (isSelected && isSelectionMode) View.VISIBLE else View.GONE
        }

        fun updateSelectionMode(isSelectionMode: Boolean, isSelected: Boolean) {
            updateSelectionVisuals(isSelected, isSelectionMode)
        }
    }

    class PhotoSpanSizeLookup(private val adapter: PhotoAdapter) : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (adapter.getItemViewType(position)) {
                VIEW_TYPE_DATE_HEADER -> SPAN_COUNT
                VIEW_TYPE_PHOTO -> 1
                else -> 1
            }
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
            BaseAdapter.PAYLOAD_SELECTION_CHANGED
        } else null
    }
}