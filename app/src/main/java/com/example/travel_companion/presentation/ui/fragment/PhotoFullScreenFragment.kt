package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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

    /**
     * Inflates the layout and initializes data binding.
     *
     * @param inflater LayoutInflater used to inflate the layout
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
            inflater, R.layout.fragment_photo_fullscreen, container, false
        )
        return binding.root
    }

    /**
     * Called when the view is created.
     * Sets up UI interactions and loads the photo.
     *
     * @param view The created view
     * @param savedInstanceState Previously saved state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadPhoto()
    }

    /**
     * Configures UI interactions, such as toggling controls visibility
     * when the photo is tapped.
     */
    private fun setupUI() {
        binding.photoImageView.setOnClickListener {
            toggleControlsVisibility()
        }
    }

    /**
     * Loads the photo passed via arguments into the ImageView using Glide.
     */
    private fun loadPhoto() {
        Glide.with(this)
            .load(args.photoUri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.ic_menu_gallery)
            .into(binding.photoImageView)
    }

    /**
     * Toggles the visibility of the toolbar when the image is tapped.
     */
    private fun toggleControlsVisibility() {
        val newVisibility = if (isVisible) View.GONE else View.VISIBLE
        binding.toolbar.visibility = newVisibility
    }

    /**
     * Cleans up binding when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
