package com.example.travel_companion.presentation.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentPhotoGalleryBinding
import com.example.travel_companion.presentation.adapter.PhotoAdapter
import com.example.travel_companion.presentation.viewmodel.PhotoGalleryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PhotoGalleryFragment: Fragment() {
    private  var _binding: FragmentPhotoGalleryBinding? = null
    private val binding get() = _binding!!
    private var adapter = PhotoAdapter()
    private val args: PhotoGalleryFragmentArgs by navArgs()
    private val viewModel: PhotoGalleryViewModel by viewModels()
    private lateinit var currentPhotoUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_photo_gallery, container, false
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavigation()
        setupClickListeners()
        initGalleryData()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.goToNotes -> {
                    findNavController().navigate(
                        PhotoGalleryFragmentDirections.actionPhotoGalleryFragmenttToNoteListFragment(args.tripId)
                    )
                    true
                }
                R.id.goToTripDetails -> {
                    findNavController().navigate(
                        PhotoGalleryFragmentDirections.actionPhotoGalleryFragmentToTripDetailsFragment(args.tripId, "home")
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        binding.takePicture.setOnClickListener {
            handleTakePhoto()
        }
    }

    private fun initGalleryData() {
        viewModel.loadPhotos(args.tripId)

        viewModel.photos.observe(viewLifecycleOwner) { photos ->
            adapter.submitList(photos)
        }
    }

    private fun handleTakePhoto() {
        val hasCamera = checkSelfPermission(requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera) {
            val perms = mutableListOf(Manifest.permission.CAMERA)
            permissionsLauncher.launch(perms.toTypedArray())
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        currentPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(currentPhotoUri)
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Permessi fotocamera richiesti", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.insert(args.tripId, currentPhotoUri.toString())
            Toast.makeText(requireContext(), "Foto salvata", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Scatto annullato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}