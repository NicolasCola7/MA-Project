package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentNoteDetailsBinding
import com.example.travel_companion.presentation.viewmodel.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteDetailsFragment : Fragment() {

    private val args: NoteDetailsFragmentArgs by navArgs()
    private val viewModel: NotesViewModel by viewModels()

    private var _binding: FragmentNoteDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_note_details, container, false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getNoteById(args.noteId).observe(viewLifecycleOwner) { note ->
            binding.titleText.text = note.title
            binding.contentText.text = note.content
            // e qualsiasi altro campo tu abbia
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
