package com.example.travel_companion.presentation.adapter

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.databinding.ItemNoteBinding
import com.example.travel_companion.presentation.adapter.base.SelectableAdapter
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying a list of notes with support for selection.
 *
 * Extends [SelectableAdapter] to handle item selection and selection mode.
 *
 * @param onItemClick Callback invoked when a note is clicked (outside selection mode).
 * @param onSelectionChanged Callback invoked when selection changes.
 */
class NotesListAdapter(
    onItemClick: (NoteEntity) -> Unit,
    onSelectionChanged: (Int) -> Unit = { }
) : SelectableAdapter<NoteEntity, NotesListAdapter.NotesViewHolder>(
    NotesDiffCallback(),
    onItemClick,
    onSelectionChanged
) {

    /**
     * Returns a unique identifier for the note item.
     */
    override fun getItemId(item: NoteEntity): Any = item.id

    /**
     * Creates a ViewHolder for a note item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotesViewHolder(binding)
    }

    /**
     * Bind a note to the ViewHolder, including selection state.
     */
    override fun bindItemWithSelection(
        holder: NotesViewHolder,
        item: NoteEntity,
        isSelected: Boolean,
        position: Int
    ) {
        holder.bind(item, isSelected)
    }

    /**
     * Update only the selection visuals of a note item.
     */
    override fun updateSelectionVisuals(holder: NotesViewHolder, isSelected: Boolean) {
        holder.updateSelectionOnly(isSelected)
    }

    /**
     * Convenience method for retrieving currently selected notes.
     */
    fun getSelectedNotes(): List<NoteEntity> = getSelectedItems()

    /**
     * ViewHolder for individual note items.
     *
     * @param binding View binding for the note item layout.
     */
    inner class NotesViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind a note's data to the UI elements and update selection state.
         *
         * @param note The note entity to bind.
         * @param isSelected Whether the note is currently selected.
         */
        fun bind(note: NoteEntity, isSelected: Boolean) {
            binding.tvNoteTitle.text = note.title
            binding.tvNoteContent.text = note.content

            val date = Date(note.timestamp)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvNoteDate.text = dateFormat.format(date)

            updateSelectionUI(isSelected)
        }

        /**
         * Update only the selection UI without rebinding data.
         *
         * @param isSelected Whether the note is currently selected.
         */
        fun updateSelectionOnly(isSelected: Boolean) {
            updateSelectionUI(isSelected)
        }

        /**
         * Internal helper to update selection overlay visibility.
         *
         * @param isSelected Whether the note is selected.
         */
        private fun updateSelectionUI(isSelected: Boolean) {
            val cardView = binding.root
            val selectionOverlay = binding.root.findViewById<View>(R.id.selection_overlay)

            if (isSelected) {
                // Show selection overlay
                selectionOverlay?.visibility = View.VISIBLE
            } else {
                // Hide overlay and restore normal state
                selectionOverlay?.visibility = View.GONE
            }
            cardView.invalidate()
        }
    }
}

/**
 * DiffUtil callback for calculating changes between note items.
 */
class NotesDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {

    /**
     * Check if two note items represent the same entity (by ID).
     */
    override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
        oldItem.id == newItem.id

    /**
     * Check if the contents of two notes are identical.
     */
    override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
        oldItem == newItem
}
