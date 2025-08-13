package com.example.travel_companion.presentation.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.travel_companion.R
import com.example.travel_companion.databinding.ActivityMainBinding
import com.example.travel_companion.util.PermissionsManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point of Application
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prima controlla se tutti i permessi sono già concessi
        if (PermissionsManager.areAllEssentialPermissionsGranted(this)) {
            // Tutti i permessi sono concessi, inizializza l'app
            initializeApp()
        } else {
            // Alcuni permessi mancano, mostra schermata di permessi
            showPermissionsScreen()
        }
    }

    private fun initializeApp() {
        isAppReady = true
        setupUI()

        // Controlla permesso allarmi (opzionale, non blocca l'app)
        PermissionsManager.checkExactAlarmPermissionSmart(this) {
            // Callback quando il permesso è concesso o non necessario
        }
    }

    private fun showPermissionsScreen() {
        // Mostra una schermata di loading o placeholder mentre richiedi i permessi
        setContentView(R.layout.activity_permissions) // Crea questo layout se non esiste

        // Avvia la richiesta sequenziale dei permessi
        requestAllPermissionsSequentially()
    }

    private fun requestAllPermissionsSequentially() {
        // Richiedi prima i permessi di localizzazione
        PermissionsManager.checkLocationPermission(this)
    }

    private fun setupUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = binding.bottomNavigationView

        setSupportActionBar(binding.toolbar)
        bottomNavigationView.setupWithNavController(navController)

        // Lista dei fragment che mostrano la bottom navigation
        val fragmentsWithBottomNav = setOf(
            R.id.homeFragment,
            R.id.tripsFragment,
            R.id.statisticsFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.tvToolbarTitle.text = destination.label ?: "Travel Companion"

            // Gestione BottomNavigationView
            binding.bottomNavigationView.visibility = if (destination.id in fragmentsWithBottomNav) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Gestione AppBarLayout (Toolbar)
            binding.appBarLayout.visibility = if (destination.id == R.id.photoFullScreenFragment) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Delega la gestione al PermissionsManager
        PermissionsManager.handlePermissionResult(
            context = this,
            requestCode = requestCode,
            permissions = permissions,
            grantResults = grantResults,
            onAllPermissionsGranted = {
                // Tutti i permessi essenziali sono stati concessi
                if (!isAppReady) {
                    initializeApp()
                }
            },
            onPermissionDenied = { code ->
                // Un permesso è stato negato
                // Il PermissionsManager mostrerà automaticamente il dialog per le impostazioni
                // o chiuderà l'app se l'utente sceglie "Esci dall'App"

                // Facoltativo: aggiungi log o analytics qui
                when (code) {
                    PermissionsManager.CURRENT_LOCATION_PERMISSIONS_REQUEST,
                    PermissionsManager.OLDER_LOCATION_PERMISSIONS_REQUEST -> {
                        // Log: Permesso localizzazione negato
                    }

                    PermissionsManager.CAMERA_PERMISSIONS_REQUEST -> {
                        // Log: Permesso camera negato
                    }

                    PermissionsManager.POST_NOTIFICATION_PERMISSIONS_REQUEST -> {
                        // Log: Permesso notifiche negato
                    }
                }
            }
        )

        // Dopo aver gestito un permesso, richiedi il prossimo se necessario
        if (!PermissionsManager.areAllEssentialPermissionsGranted(this)) {
            requestNextPermission(requestCode)
        }
    }

    /**
     * Richiede il prossimo permesso mancante in sequenza
     */
    private fun requestNextPermission(completedRequestCode: Int) {
        when (completedRequestCode) {
            PermissionsManager.CURRENT_LOCATION_PERMISSIONS_REQUEST,
            PermissionsManager.OLDER_LOCATION_PERMISSIONS_REQUEST -> {
                // Localizzazione completata (concessa o negata), richiedi camera
                val cameraGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (!cameraGranted) {
                    PermissionsManager.checkCameraPermission(this)
                } else {
                    // Camera già concessa, vai alle notifiche
                    requestNotificationPermissionIfNeeded()
                }
            }

            PermissionsManager.CAMERA_PERMISSIONS_REQUEST -> {
                // Camera completata, richiedi notifiche
                requestNotificationPermissionIfNeeded()
            }

            PermissionsManager.POST_NOTIFICATION_PERMISSIONS_REQUEST -> {
                // Tutte le richieste completate
                // Se arriviamo qui e non tutti i permessi sono concessi,
                // significa che l'utente ne ha negato almeno uno
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionsManager.hasNotificationPermissions(this)) {
                PermissionsManager.checkNotificationsPermissions(this)
            }
        }
        // Per versioni più vecchie, le notifiche sono automaticamente concesse
    }

    override fun onResume() {
        super.onResume()

        if (!isAppReady) {
            // L'app non è ancora pronta, controlla se i permessi sono stati concessi
            if (PermissionsManager.areAllEssentialPermissionsGranted(this)) {
                // Tutti i permessi sono ora concessi, inizializza l'app
                initializeApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAppReady = false
    }
}