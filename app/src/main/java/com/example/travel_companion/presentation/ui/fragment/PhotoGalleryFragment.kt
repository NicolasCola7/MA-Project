package com.example.travel_companion.presentation.ui.fragment

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.databinding.FragmentPhotoGalleryBinding
import com.example.travel_companion.presentation.adapter.PhotoAdapter
import com.example.travel_companion.presentation.viewmodel.PhotoGalleryViewModel
import com.example.travel_companion.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PhotoGalleryFragment : Fragment() {

    private var _binding: FragmentPhotoGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PhotoAdapter
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

        setupRecyclerView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomNavigation()
        setupClickListeners()
        setupAdapter()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        // Sincronizza automaticamente quando si torna al fragment
        viewModel.syncWithSystemGallery(requireContext())
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    private fun observeData() {
        viewModel.loadPhotos(args.tripId)

        viewModel.photos.observe(viewLifecycleOwner) { photos ->
            adapter.submitList(photos) {
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

    private fun setupAdapter() {
        adapter = PhotoAdapter(
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            },
            onPhotoClick = { photo ->
                handlePhotoClick(photo)
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun handlePhotoClick(photo: PhotoEntity) {
        findNavController().navigate(
            PhotoGalleryFragmentDirections.actionPhotoGalleryFragmentToPhotoFullScreenFragment(
                photoUri = photo.uri,
                tripId = args.tripId
            )
        )
    }

    private fun handleTakePhoto() {
        val hasCamera = checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCamera) {
            permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "TravelCompanion_${timeStamp}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TravelCompanion")
        }

        val resolver = requireContext().contentResolver
        currentPhotoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Impossibile creare URI per la foto")

        takePictureLauncher.launch(currentPhotoUri)
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Permessi fotocamera e storage richiesti",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.insertPhoto(args.tripId, currentPhotoUri.toString())
        } else {
            Toast.makeText(
                requireContext(),
                "Errore nel salvare la foto",
                Toast.LENGTH_SHORT
            ).show()
        }
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

        if (selectedPhotos.isEmpty()) return

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
        viewModel.deletePhotosWithSystemSync(requireContext(), photoIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}