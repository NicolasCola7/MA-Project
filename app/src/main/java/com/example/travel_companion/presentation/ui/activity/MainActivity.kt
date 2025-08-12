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
import com.example.travel_companion.util.PermissionsManager.CURRENT_LOCATION_PERMISSIONS_REQUEST
import com.example.travel_companion.util.PermissionsManager.OLDER_LOCATION_PERMISSIONS_REQUEST
import com.example.travel_companion.util.PermissionsManager.POST_NOTIFICATION_PERMISSIONS_REQUEST
import com.example.travel_companion.util.PermissionsManager.checkLocationPermission
import com.example.travel_companion.util.PermissionsManager.checkNotificationsPermissions
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point of Application
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        checkLocationPermission(this)
        checkNotificationsPermissions(this)
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

        when (requestCode) {
            CURRENT_LOCATION_PERMISSIONS_REQUEST -> {
                handleLocationPermissionResults(grantResults, true)
            }
            OLDER_LOCATION_PERMISSIONS_REQUEST -> {
                handleLocationPermissionResults(grantResults, false)
            }
        }
    }

    private fun handleLocationPermissionResults(grantResults: IntArray, current: Boolean) {
        if(current) {
            if (grantResults.isEmpty() ||
                grantResults[0] == PackageManager.PERMISSION_DENIED ||
                grantResults[1] == PackageManager.PERMISSION_DENIED) {

                Toast.makeText(
                    this,
                    "Concedi tutti i permessi di localizzazione per usare l'app",
                    Toast.LENGTH_LONG
                ).show()

                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", this.packageName, null),
                    ),
                )
            }
        } else {
            if (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // permission denied
                Toast.makeText(
                    this,
                    "Concedi tutti i permessi di localizzazione per usare l'app",
                    Toast.LENGTH_LONG
                ).show()

                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", this.packageName, null),
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkLocationPermission(this)
    }

}