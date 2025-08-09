package com.example.travel_companion.presentation.adapter

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.databinding.ItemNoteBinding
import java.util.Date
import java.util.Locale

class NotesListAdapter(
    private val onItemClick: (NoteEntity) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = { }
) : ListAdapter<NoteEntity, NotesListAdapter.NotesViewHolder>(NotesDiffCallback()) {

    private val selectedNotes = mutableSetOf<NoteEntity>()
    var selectionMode = false
        private set

    private fun toggleSelection(note: NoteEntity, position: Int) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note)
        } else {
            selectedNotes.add(note)
        }

        selectionMode = selectedNotes.isNotEmpty()
        onSelectionChanged(selectedNotes.size)

        // Aggiorna solo l'item specifico invece di tutto il dataset
        notifyItemChanged(position)
    }

    fun getSelectedNotes(): List<NoteEntity> = selectedNotes.toList()

    fun clearSelection() {
        if (selectedNotes.isEmpty()) return

        // Trova le posizioni degli elementi selezionati per aggiornarli
        val positionsToUpdate = mutableListOf<Int>()
        currentList.forEachIndexed { index, note ->
            if (selectedNotes.contains(note)) {
                positionsToUpdate.add(index)
            }
        }

        selectedNotes.clear()
        selectionMode = false

        // Aggiorna solo gli item che erano selezionati
        positionsToUpdate.forEach { position ->
            notifyItemChanged(position)
        }

        onSelectionChanged(0)
    }

    // Metodo per aggiornare la selezione dopo modifiche alla lista
    fun updateSelectionAfterListChange() {
        val currentIds = currentList.map { it.id }.toSet()
        val iterator = selectedNotes.iterator()

        while (iterator.hasNext()) {
            val selectedNote = iterator.next()
            if (!currentIds.contains(selectedNote.id)) {
                iterator.remove()
            }
        }

        selectionMode = selectedNotes.isNotEmpty()
        onSelectionChanged(selectedNotes.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        val note = getItem(position)
        val isSelected = selectedNotes.contains(note)

        holder.bind(note, isSelected)

        holder.itemView.setOnClickListener {
            if (selectionMode) {
                toggleSelection(note, position)
            } else {
                onItemClick(note)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(note, position)
            true
        }
    }

    inner class NotesViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity, isSelected: Boolean) {
            // Dati principali
            binding.tvNoteTitle.text = note.title
            binding.tvNoteContent.text = note.content

            // Formatta e mostra la data
            val date = Date(note.timestamp)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvNoteDate.text = dateFormat.format(date)

            // Gestione selezione visuale
            updateSelectionUI(isSelected)
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            val cardView = binding.root

            if (isSelected) {
                // Cambia lo sfondo per indicare la selezione
                cardView.setBackgroundResource(R.drawable.note_card_selected)

                // Aggiungi una leggera elevazione
                cardView.cardElevation = 8f
            } else {
                // Ripristina lo sfondo normale
                cardView.setBackgroundResource(R.drawable.note_card_gradient)

                // Ripristina l'elevazione normale
                cardView.cardElevation = 6f
            }
        }
    }
}

class NotesDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
    override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
        return oldItem == newItem
    }
}