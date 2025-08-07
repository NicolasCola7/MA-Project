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
import com.example.travel_companion.util.Utils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotesListFragment : Fragment() {
    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private val args: NotesListFragmentArgs by navArgs()
    private val viewModel: NotesViewModel by viewModels()

    private lateinit var adapter: NotesListAdapter

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

    private fun setupViews() {
        setupAdapter()
        setupRecyclerView()
        setupDeleteButton()
    }

    private fun setupAdapter() {
        adapter = NotesListAdapter(
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            }
        )
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupDeleteButton() {
        binding.deleteSelectedNotes.setOnClickListener {
            handleMultipleDelete()
        }
    }

    private fun observeData() {
        viewModel.loadNotes(args.tripId)

        viewModel.notes.observe(viewLifecycleOwner) { noteList ->
            adapter.submitList(noteList) {
                adapter.updateSelectionAfterListChange()
            }
        }
    }

    private fun setupBottomNavigation() {
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
                else -> false
            }
        }
    }

    private fun updateDeleteButton(selectedCount: Int) {
        Utils.SelectionHelper.updateDeleteButton(
            button = binding.deleteSelectedNotes,
            selectedCount = selectedCount,
            baseText = "Elimina"
        )
    }

    private fun handleMultipleDelete() {
        val selectedNotes = adapter.getSelectedNotes()

        Utils.SelectionHelper.handleMultipleDelete(
            context = requireContext(),
            selectedItems = selectedNotes,
            itemType = "note",
            onDelete = { notes -> deleteSelectedNotes(notes) },
            onClearSelection = { adapter.clearSelection() },
            onUpdateButton = { count -> updateDeleteButton(count) }
        )
    }

    private fun deleteSelectedNotes(notes: List<NoteEntity>) {
        val noteIds = notes.map { it.id }
        viewModel.deleteNotes(noteIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}