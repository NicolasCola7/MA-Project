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
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
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

    override fun onResume() {
        super.onResume()
        // Verifica e sincronizza la galleria quando si torna al fragment
        syncGalleryWithSystem()
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

    private fun setupAdapter() {
        adapter = PhotoAdapter(
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            },
            onPhotoClick = { photo ->
                // Verifica se la foto esiste prima di navigare
                if (isPhotoAccessible(photo.uri)) {
                    findNavController().navigate(
                        PhotoGalleryFragmentDirections.actionPhotoGalleryFragmentToPhotoFullScreenFragment(
                            photoUri = photo.uri,
                            tripId = args.tripId
                        )
                    )
                } else {
                    // La foto non esiste più, rimuovila dal database
                    viewModel.deletePhotosFromAppOnly(listOf(photo.id))
                    Toast.makeText(requireContext(), "Foto non più disponibile, rimossa dalla galleria", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun initGalleryData() {
        viewModel.loadPhotos(args.tripId).observe(viewLifecycleOwner) { photos ->
            adapter.submitList(photos)
        }
    }

    /**
     * Sincronizza la galleria dell'app con il sistema:
     * rimuove i riferimenti alle foto che non esistono più
     */
    private fun syncGalleryWithSystem() {
        viewModel.syncPhotosWithSystem(requireContext(), args.tripId)
    }

    /**
     * Verifica se una foto è ancora accessibile nel sistema
     */
    private fun isPhotoAccessible(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.use { true } ?: false
        } catch (e: Exception) {
            when (e) {
                is SecurityException,
                is FileNotFoundException,
                is IllegalArgumentException -> {
                    Timber.d("Photo not accessible: $uriString - ${e.message}")
                    false
                }
                else -> {
                    Timber.e(e, "Unexpected error checking photo accessibility")
                    true // In caso di errore sconosciuto, assumiamo che sia accessibile
                }
            }
        }
    }

    private fun handleTakePhoto() {
        val hasCamera = checkSelfPermission(requireContext(),
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
            Toast.makeText(requireContext(), "Permessi fotocamera e storage richiesti", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Salva riferimento nel database della tua app
            viewModel.insert(args.tripId, currentPhotoUri.toString())
        } else {
            Toast.makeText(requireContext(), "Errore nel salvare la foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFileInPublicDirectory(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // Crea directory pubblica per le foto dell'app
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "TravelCompanion")

        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        return File(appDir, "TravelCompanion_${timeStamp}.jpg")
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
        // Usa il nuovo metodo che elimina anche dalla galleria di sistema
        viewModel.deletePhotos(requireContext(), photoIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}