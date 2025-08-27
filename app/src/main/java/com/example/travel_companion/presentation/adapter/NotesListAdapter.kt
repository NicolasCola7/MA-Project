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

class NotesListAdapter(
    onItemClick: (NoteEntity) -> Unit,
    onSelectionChanged: (Int) -> Unit = { }
) : SelectableAdapter<NoteEntity, NotesListAdapter.NotesViewHolder>(
    NotesDiffCallback(),
    onItemClick,
    onSelectionChanged
) {

    override fun getItemId(item: NoteEntity): Any = item.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotesViewHolder(binding)
    }

    override fun bindItemWithSelection(holder: NotesViewHolder, item: NoteEntity, isSelected: Boolean, position: Int) {
        holder.bind(item, isSelected)
    }

    override fun updateSelectionVisuals(holder: NotesViewHolder, isSelected: Boolean) {
        holder.updateSelectionOnly(isSelected)
    }

    // Metodo di convenienza per compatibilità
    fun getSelectedNotes(): List<NoteEntity> = getSelectedItems()

    inner class NotesViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity, isSelected: Boolean) {
            binding.tvNoteTitle.text = note.title
            binding.tvNoteContent.text = note.content

            val date = Date(note.timestamp)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvNoteDate.text = dateFormat.format(date)

            updateSelectionUI(isSelected)
        }

        fun updateSelectionOnly(isSelected: Boolean) {
            updateSelectionUI(isSelected)
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            val cardView = binding.root
            val defaultBackground = cardView.cardBackgroundColor.defaultColor
            // IMPORTANTE: Ripristina sempre lo stato di default prima di applicare
            // quello nuovo per evitare stati inconsistenti
            val selectionOverlay = binding.root.findViewById<View>(R.id.selection_overlay)

            if (isSelected) {
                // Mostra overlay di selezione (stesso stile di Trip e Photo)
                selectionOverlay?.visibility = View.VISIBLE
            } else {
                // Nascondi overlay e ripristina stato normale
                selectionOverlay?.visibility = View.GONE
            }
            cardView.invalidate()
        }
    }
}

class NotesDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
    override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean = oldItem == newItem
}