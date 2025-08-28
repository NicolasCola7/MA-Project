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
import com.example.travel_companion.util.helpers.EmptyStateHelper
import com.example.travel_companion.util.helpers.SelectionHelper
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

    /**
     * Inflates the layout and initializes data binding.
     */
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

        return binding.root
    }

    /**
     * Called when the view is created.
     * Sets up adapter, RecyclerView, navigation, click listeners and observers.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        setupBottomNavigation()
        setupClickListeners()
        observeData()
    }

    /**
     * Syncs photos with the system gallery whenever the fragment resumes.
     */
    override fun onResume() {
        super.onResume()
        viewModel.syncWithSystemGallery(requireContext())
    }

    /**
     * Sets up the RecyclerView with a GridLayoutManager and span size lookup.
     */
    private fun setupRecyclerView() {
        val gridLayoutManager = GridLayoutManager(requireContext(), PhotoAdapter.SPAN_COUNT)
        gridLayoutManager.spanSizeLookup = PhotoAdapter.PhotoSpanSizeLookup(adapter)
        binding.recyclerView.layoutManager = gridLayoutManager
    }

    /**
     * Observes photo data and updates the adapter accordingly.
     * Also manages the empty state UI.
     */
    private fun observeData() {
        viewModel.loadPhotos(args.tripId)

        viewModel.groupedPhotos.observe(viewLifecycleOwner) { groupedItems ->
            adapter.submitList(groupedItems) {
                adapter.updateSelectionAfterListChange()
            }

            val shouldShowEmptyState = groupedItems.isEmpty()
            if (shouldShowEmptyState) {
                EmptyStateHelper.showPhotosEmptyState(
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
     * Configures bottom navigation to handle navigation between fragments.
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.selectedItemId = R.id.goToPhotoGallery
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
                R.id.goToPhotoGallery -> {
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Sets up click listeners for taking a picture and deleting selected photos.
     */
    private fun setupClickListeners() {
        binding.takePicture.setOnClickListener {
            handleTakePhoto()
        }

        binding.deleteSelectedPhotos.setOnClickListener {
            handleMultipleDelete()
        }
    }

    /**
     * Initializes the photo adapter with selection handling and photo click navigation.
     */
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

    /**
     * Handles photo click by navigating to the full-screen photo fragment.
     */
    private fun handlePhotoClick(photo: PhotoEntity) {
        findNavController().navigate(
            PhotoGalleryFragmentDirections.actionPhotoGalleryFragmentToPhotoFullScreenFragment(
                photoUri = photo.uri,
                tripId = args.tripId
            )
        )
    }

    /**
     * Handles camera permission check before launching the camera.
     */
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

    /**
     * Launches the camera and creates a new file entry for the photo in MediaStore.
     */
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

    /**
     * Updates the delete button text and state based on selection count.
     */
    private fun updateDeleteButton(selectedCount: Int) {
        SelectionHelper.updateDeleteButton(
            button = binding.deleteSelectedPhotos,
            selectedCount = selectedCount,
            baseText = "Elimina"
        )
    }

    /**
     * Handles deletion of multiple selected photos.
     */
    private fun handleMultipleDelete() {
        val selectedPhotos = adapter.getSelectedPhotos()

        if (selectedPhotos.isEmpty()) return

        SelectionHelper.handleMultipleDelete(
            context = requireContext(),
            selectedItems = selectedPhotos,
            itemType = "foto",
            onDelete = { photos -> deleteSelectedPhotos(photos) },
            onClearSelection = { adapter.clearSelection() },
            onUpdateButton = { count -> updateDeleteButton(count) }
        )
    }

    /**
     * Deletes selected photos and syncs the system gallery.
     */
    private fun deleteSelectedPhotos(photos: List<PhotoEntity>) {
        val photoIds = photos.map { it.id }
        viewModel.deletePhotosWithSystemSync(requireContext(), photoIds)
    }

    /**
     * Cleans up the binding when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Launchers ---

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
}
