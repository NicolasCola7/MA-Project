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

    /**
     * Inflates the layout and initializes data binding.
     *
     * @param inflater LayoutInflater used to inflate views
     * @param container Optional parent view group
     * @param savedInstanceState Previously saved state
     * @return The root view of the fragment
     */
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

    /**
     * Called when the view is created.
     * Sets up listeners, observers, and the initial state.
     *
     * @param view The created view
     * @param savedInstanceState Previously saved state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeNote()
        setupInitialState()
    }

    /**
     * Sets up button click listeners for edit, cancel, and delete actions.
     */
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

    /**
     * Observes the note from the ViewModel by its ID and updates UI when changed.
     */
    private fun observeNote() {
        viewModel.getNoteById(args.noteId).observe(viewLifecycleOwner) { note ->
            currentNote = note
            if (!isEditMode) {
                displayNote(note)
            }
        }
    }

    /**
     * Configures the initial UI state when the fragment is first shown.
     */
    private fun setupInitialState() {
        setEditMode(false)
    }

    /**
     * Displays the note data in the UI.
     *
     * @param note The note entity to display
     */
    private fun displayNote(note: NoteEntity) {
        binding.titleText.text = note.title
        binding.contentText.text = note.content
        binding.titleEditText.setText(note.title)
        binding.contentEditText.setText(note.content)
    }

    /**
     * Switches the UI into edit mode and focuses on the title field.
     */
    private fun enterEditMode() {
        setEditMode(true)

        // Focus on the first text field
        binding.titleEditText.requestFocus()
        showKeyboard()
    }

    /**
     * Exits edit mode, restoring original note data and hiding the keyboard.
     */
    private fun exitEditMode() {
        setEditMode(false)

        // Restore original values
        currentNote?.let { displayNote(it) }
        hideKeyboard()
    }

    /**
     * Updates the UI to reflect edit mode or view mode.
     *
     * @param editMode Whether the fragment is in edit mode
     */
    private fun setEditMode(editMode: Boolean) {
        isEditMode = editMode

        // Toggle visibility of text vs editable fields
        binding.titleText.visibility = if (editMode) View.GONE else View.VISIBLE
        binding.contentText.visibility = if (editMode) View.GONE else View.VISIBLE
        binding.titleEditText.visibility = if (editMode) View.VISIBLE else View.GONE
        binding.contentEditText.visibility = if (editMode) View.VISIBLE else View.GONE

        // Update button text
        binding.editButton.text = if (editMode) "Salva" else "Modifica"

        // Hide delete button while editing
        binding.deleteButton.visibility = if (editMode) View.GONE else View.VISIBLE
    }

    /**
     * Saves the edited note, validates input, and updates it in the database.
     */
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

    /**
     * Shows a confirmation dialog to delete the current note.
     */
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

    /**
     * Shows the soft keyboard for the title field.
     */
    private fun showKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.showSoftInput(binding.titleEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Hides the soft keyboard.
     */
    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    /**
     * Cleans up binding when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
