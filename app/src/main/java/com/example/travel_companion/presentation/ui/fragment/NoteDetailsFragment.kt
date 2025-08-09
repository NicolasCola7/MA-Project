package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.app.AlertDialog
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.databinding.FragmentNoteDetailsBinding
import com.example.travel_companion.presentation.viewmodel.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteDetailsFragment : Fragment() {

    private val args: NoteDetailsFragmentArgs by navArgs()
    private val viewModel: NotesViewModel by viewModels()

    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    private var currentNote: NoteEntity? = null
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_note_details, container, false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeNote()
        setupInitialState()
    }

    private fun setupClickListeners() {
        binding.editButton.setOnClickListener {
            if (isEditMode) {
                saveNote()
            } else {
                enterEditMode()
            }
        }

        binding.cancelButton.setOnClickListener {
            if (isEditMode) {
                exitEditMode()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.deleteButton.setOnClickListener {
            deleteNote()
        }
    }

    private fun observeNote() {
        viewModel.getNoteById(args.noteId).observe(viewLifecycleOwner) { note ->
            currentNote = note
            if (!isEditMode) {
                displayNote(note)
            }
        }
    }

    private fun setupInitialState() {
        setEditMode(false)
    }

    private fun displayNote(note: NoteEntity) {
        binding.titleText.text = note.title
        binding.contentText.text = note.content
        binding.titleEditText.setText(note.title)
        binding.contentEditText.setText(note.content)
    }

    private fun enterEditMode() {
        setEditMode(true)

        // Focus sul primo campo di testo
        binding.titleEditText.requestFocus()
        showKeyboard()
    }

    private fun exitEditMode() {
        setEditMode(false)

        // Ripristina i valori originali
        currentNote?.let { displayNote(it) }
        hideKeyboard()
    }

    private fun setEditMode(editMode: Boolean) {
        isEditMode = editMode

        // Gestisci visibilità TextView vs EditText
        binding.titleText.visibility = if (editMode) View.GONE else View.VISIBLE
        binding.contentText.visibility = if (editMode) View.GONE else View.VISIBLE
        binding.titleEditText.visibility = if (editMode) View.VISIBLE else View.GONE
        binding.contentEditText.visibility = if (editMode) View.VISIBLE else View.GONE

        // Aggiorna testi dei bottoni
        binding.editButton.text = if (editMode) "Salva" else "Modifica"

        // Gestisci visibilità bottone elimina
        binding.deleteButton.visibility = if (editMode) View.GONE else View.VISIBLE
    }

    private fun saveNote() {
        val title = binding.titleEditText.text.toString().trim()
        val content = binding.contentEditText.text.toString().trim()

        if (title.isBlank()) {
            binding.titleEditText.error = "Il titolo non può essere vuoto"
            return
        }

        currentNote?.let { note ->
            val updatedNote = note.copy(
                title = title,
                content = content,
                timestamp = System.currentTimeMillis()
            )

            viewModel.updateNote(updatedNote)
            setEditMode(false)
            hideKeyboard()
        }
    }

    private fun deleteNote() {
        currentNote?.let { note ->
            AlertDialog.Builder(requireContext())
                .setTitle("Elimina nota")
                .setMessage("Sei sicuro di voler eliminare questa nota?")
                .setPositiveButton("Elimina") { _, _ ->
                    viewModel.deleteNotes(listOf(note.id))
                    findNavController().navigateUp()
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    private fun showKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.showSoftInput(binding.titleEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}