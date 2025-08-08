package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentPhotoFullscreenBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoFullScreenFragment : Fragment() {

    private var _binding: FragmentPhotoFullscreenBinding? = null
    private val binding get() = _binding!!
    private val args: PhotoFullScreenFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_photo_fullscreen, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadPhoto()
    }

    private fun setupUI() {
        // Click sull'immagine per nascondere/mostrare i controlli
        binding.photoImageView.setOnClickListener {
            toggleControlsVisibility()
        }
    }

    private fun loadPhoto() {
        Glide.with(this)
            .load(args.photoUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.ic_menu_gallery)
            .into(binding.photoImageView)
    }

    private fun toggleControlsVisibility() {
        val newVisibility = if (isVisible) View.GONE else View.VISIBLE

        binding.toolbar.visibility = newVisibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}