package com.example.travel_companion.presentation.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.databinding.FragmentPhotoGalleryBinding
import com.example.travel_companion.presentation.adapter.NotesListAdapter
import com.example.travel_companion.presentation.adapter.PhotoAdapter
import com.example.travel_companion.presentation.viewmodel.PhotoGalleryViewModel
import com.example.travel_companion.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        setupAdapter()
        observeData()
    }

    private fun observeData() {
        viewModel.loadPhotos(args.tripId)

        viewModel.photos.observe(viewLifecycleOwner) { photoList ->
            adapter.submitList(photoList) {
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
                R.id.goToNotes -> {
                    findNavController().navigate(
                        PhotoGalleryFragmentDirections.actionPhotoGalleryFragmenttToNoteListFragment(args.tripId)
                    )
                    true
                }
                R.id.goToTripDetails -> {
                    findNavController().navigate(
                        PhotoGalleryFragmentDirections.actionPhotoGalleryFragmentToTripDetailsFragment(args.tripId)
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
        binding.deleteSelectedPhotos.setOnClickListener {
            handleMultipleDelete()
        }
    }

    // Modifica solo il metodo setupAdapter() nel tuo PhotoGalleryFragment:

    private fun setupAdapter() {
        adapter = PhotoAdapter(
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            },
            onPhotoClick = { photo ->
                // Naviga al fragment a schermo intero
                findNavController().navigate(
                    PhotoGalleryFragmentDirections.actionPhotoGalleryFragmentToPhotoFullScreenFragment(
                        photoUri = photo.uri,
                        tripId = args.tripId
                    )
                )
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun initGalleryData() {
        viewModel.loadPhotos(args.tripId).observe(viewLifecycleOwner) { photos ->
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
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun updateDeleteButton(selectedCount: Int) {
        Utils.SelectionHelper.updateDeleteButton(
            button = binding.deleteSelectedPhotos,
            selectedCount = selectedCount,
            baseText = "Elimina"
        )
    }

    private fun handleMultipleDelete() {
        val selectedPhotos = adapter.getSelectedPhotos()

        Utils.SelectionHelper.handleMultipleDelete(
            context = requireContext(),
            selectedItems = selectedPhotos,
            itemType = "foto",
            onDelete = { photos -> deleteSelectedPhotos(photos) },
            onClearSelection = { adapter.clearSelection() },
            onUpdateButton = { count -> updateDeleteButton(count) }
        )
    }

    private fun deleteSelectedPhotos(photos: List<PhotoEntity>) {
        val photoIds = photos.map { it.id }
        viewModel.deletePhotos(photoIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}