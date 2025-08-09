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

        // Aggiorna solo l'item specifico con payload
        notifyItemChanged(position, PAYLOAD_SELECTION_CHANGED)
    }

    fun getSelectedNotes(): List<NoteEntity> = selectedNotes.toList()

    fun clearSelection() {
        if (selectedNotes.isEmpty()) return

        selectedNotes.clear()
        selectionMode = false

        // Aggiorna tutti gli elementi usando payload specifico per la selezione
        for (i in 0 until itemCount) {
            notifyItemChanged(i, PAYLOAD_SELECTION_CHANGED)
        }

        onSelectionChanged(0)
    }

    companion object {
        private const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
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

        // Aggiorna tutti gli elementi usando payload specifico
        for (i in 0 until itemCount) {
            notifyItemChanged(i, PAYLOAD_SELECTION_CHANGED)
        }
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

        // IMPORTANTE: Chiama sempre bind per garantire che lo stato
        // sia corretto, anche con ViewHolder riutilizzate
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

    // AGGIUNTO: Override di onBindViewHolder con payloads per gestire
    // aggiornamenti parziali più efficienti
    override fun onBindViewHolder(holder: NotesViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Gestisci aggiornamenti parziali quando c'è un payload
            if (payloads.contains(PAYLOAD_SELECTION_CHANGED)) {
                val note = getItem(position)
                val isSelected = selectedNotes.contains(note)
                holder.updateSelectionOnly(isSelected)
            }
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

            // IMPORTANTE: Gestione selezione visuale sempre chiamata
            updateSelectionUI(isSelected)
        }

        // AGGIUNTO: Metodo per aggiornare solo lo stato di selezione
        fun updateSelectionOnly(isSelected: Boolean) {
            updateSelectionUI(isSelected)
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            val cardView = binding.root

            // IMPORTANTE: Ripristina sempre lo stato di default prima di applicare
            // quello nuovo per evitare stati inconsistenti
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

            // AGGIUNTO: Forza l'invalidamento della vista per garantire
            // che i cambiamenti siano visibili
            cardView.invalidate()
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