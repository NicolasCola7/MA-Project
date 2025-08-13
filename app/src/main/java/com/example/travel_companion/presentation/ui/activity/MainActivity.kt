package com.example.travel_companion.presentation.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
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

        if (PermissionsManager.areAllEssentialPermissionsGranted(this)) {
            initializeApp()
        } else {
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
        setContentView(R.layout.activity_permissions)
        PermissionsManager.requestAllPermissionsSequentially(this)
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

        PermissionsManager.handlePermissionResult(
            context = this,
            requestCode = requestCode,
            grantResults = grantResults,
            onAllPermissionsGranted = {
                if (!isAppReady) {
                    initializeApp()
                }
            },
            onPermissionDenied = { code ->
                // Log per debugging
                android.util.Log.d("MainActivity", "Permesso negato: $code")
            }
        )
    }

    override fun onResume() {
        super.onResume()

        if (!isAppReady && PermissionsManager.areAllEssentialPermissionsGranted(this)) {
            initializeApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAppReady = false
    }
}