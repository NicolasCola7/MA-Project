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
import com.example.travel_companion.databinding.FragmentNoteListBinding
import com.example.travel_companion.presentation.adapter.NotesListAdapter
import com.example.travel_companion.presentation.adapter.TripListAdapter
import com.example.travel_companion.presentation.viewmodel.NotesViewModel
import com.example.travel_companion.presentation.viewmodel.TripsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotesListFragment : Fragment() {
    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private val args: NotesListFragmentArgs by navArgs()
    private val viewModel: NotesViewModel by viewModels()

    private var adapter = NotesListAdapter()

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
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addNote.setOnClickListener {
            val action = NotesListFragmentDirections
                .actionNotesListFragmentToCreateNoteFragment(args.tripId)
            findNavController().navigate(action)
        }

        initNotesData()
    }

    private fun initNotesData() {
        viewModel.loadNotes(args.tripId)

        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}