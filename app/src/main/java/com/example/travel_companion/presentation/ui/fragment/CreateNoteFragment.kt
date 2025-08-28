package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentCreateNoteBinding
import com.example.travel_companion.presentation.viewmodel.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment for creating a new note associated with a trip.
 * Handles UI binding, input validation, and saving the note through NotesViewModel.
 */
@AndroidEntryPoint
class CreateNoteFragment : Fragment() {

    private val viewModel: NotesViewModel by viewModels()
    private val args: CreateNoteFragmentArgs by navArgs()

    private var _binding: FragmentCreateNoteBinding? = null
    private val binding get() = _binding!!

    /**
     * Inflates the layout and sets up data binding.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views.
     * @param container The parent view that the fragment's UI should attach to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root view of the fragment's layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_note, container, false
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    /**
     * Called immediately after onCreateView().
     * Sets up UI interactions such as the save button click listener.
     *
     * @param view The view returned by onCreateView().
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                // Save the note via ViewModel
                viewModel.insertNote(args.tripId, title, content)
                Toast.makeText(requireContext(), "Nota salvata", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
