package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.databinding.FragmentNoteListBinding
import com.example.travel_companion.presentation.adapter.NotesListAdapter
import com.example.travel_companion.presentation.viewmodel.NotesViewModel
import com.example.travel_companion.util.helpers.EmptyStateHelper
import com.example.travel_companion.util.helpers.SelectionHelper

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotesListFragment : Fragment() {
    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private val args: NotesListFragmentArgs by navArgs()
    private val viewModel: NotesViewModel by viewModels()

    private lateinit var adapter: NotesListAdapter

    /**
     * Inflates the layout and initializes data binding.
     *
     * @param inflater LayoutInflater used to inflate views
     * @param container Optional parent view group
     * @param savedInstanceState Previously saved state
     * @return The root view of the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_note_list, container, false
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupViews()

        return binding.root
    }

    /**
     * Called when the view is created.
     * Sets up UI interactions and observers.
     *
     * @param view The created view
     * @param savedInstanceState Previously saved state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addNote.setOnClickListener {
            val action = NotesListFragmentDirections
                .actionNotesListFragmentToCreateNoteFragment(args.tripId)
            findNavController().navigate(action)
        }

        setupBottomNavigation()
        observeData()
    }

    /**
     * Sets up RecyclerView, adapter, and delete button.
     */
    private fun setupViews() {
        setupAdapter()
        setupRecyclerView()
        setupDeleteButton()
    }

    /**
     * Configures the adapter with item click and selection handling.
     */
    private fun setupAdapter() {
        adapter = NotesListAdapter(
            onItemClick = { note ->
                val action = NotesListFragmentDirections
                    .actionNotesListFragmentToNoteDetailsFragment(note.id)
                findNavController().navigate(action)
            },
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            }
        )
    }

    /**
     * Initializes the RecyclerView with layout manager and adapter.
     */
    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Sets up click listener for the delete button.
     */
    private fun setupDeleteButton() {
        binding.deleteSelectedNotes.setOnClickListener {
            handleMultipleDelete()
        }
    }

    /**
     * Observes notes data from the ViewModel and updates the UI.
     */
    private fun observeData() {
        viewModel.loadNotes(args.tripId)

        viewModel.notes.observe(viewLifecycleOwner) { noteList ->
            adapter.submitList(noteList) {
                adapter.updateSelectionAfterListChange()
            }

            // Handle empty state visibility
            val shouldShowEmptyState = noteList.isEmpty()
            if (shouldShowEmptyState) {
                EmptyStateHelper.showNotesEmptyState(
                    binding.emptyStateLayout.root
                )
            } else {
                EmptyStateHelper.hideEmptyState(
                    binding.emptyStateLayout.root
                )
            }
        }
    }

    /**
     * Configures the bottom navigation and its item selection handling.
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.selectedItemId =  R.id.goToNotes
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    findNavController().navigate(
                        TripDetailsFragmentDirections.actionTripFragmentToHomeFragment()
                    )
                    true
                }
                R.id.goToTripDetails -> {
                    findNavController().navigate(
                        NotesListFragmentDirections.actionNotesListFragmentToTripdetailsFragment(args.tripId)
                    )
                    true
                }
                R.id.goToPhotoGallery -> {
                    findNavController().navigate(
                        NotesListFragmentDirections.actionNotesListFragmentToPhotoGalleryFragment(args.tripId)
                    )
                    true
                }
                R.id.goToNotes -> {
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Updates the delete button text and state based on selected items.
     *
     * @param selectedCount Number of selected notes
     */
    private fun updateDeleteButton(selectedCount: Int) {
        SelectionHelper.updateDeleteButton(
            button = binding.deleteSelectedNotes,
            selectedCount = selectedCount,
            baseText = "Elimina"
        )
    }

    /**
     * Handles deletion of multiple selected notes with confirmation dialog.
     */
    private fun handleMultipleDelete() {
        val selectedNotes = adapter.getSelectedNotes()

        SelectionHelper.handleMultipleDelete(
            context = requireContext(),
            selectedItems = selectedNotes,
            itemType = "note",
            onDelete = { notes -> deleteSelectedNotes(notes) },
            onClearSelection = { adapter.clearSelection() },
            onUpdateButton = { count -> updateDeleteButton(count) }
        )
    }

    /**
     * Deletes the given list of notes from the database.
     *
     * @param notes List of notes to delete
     */
    private fun deleteSelectedNotes(notes: List<NoteEntity>) {
        val noteIds = notes.map { it.id }
        viewModel.deleteNotes(noteIds)
    }

    /**
     * Cleans up binding when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
