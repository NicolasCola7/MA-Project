package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.databinding.ItemNoteBinding
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

class NotesListAdapter (private var notes: List<NoteEntity>) :
    RecyclerView.Adapter<NotesListAdapter.NoteViewHolder>() {

    //Ogni elemento mostra il titolo e il contenuto della nota.
    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(note: NoteEntity) {
            binding.tvNoteTitle.text = note.title
            binding.tvNoteContent.text = note.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    //Metodo per aggiornare l'intera lista di note nella RecyclerView.
    fun submitList(newNotes: List<NoteEntity>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}