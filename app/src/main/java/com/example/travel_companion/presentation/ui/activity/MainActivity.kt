package com.example.travel_companion.presentation.ui.activity

import android.Manifest
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
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.travel_companion.R
import com.example.travel_companion.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point of Application
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        checkLocationPermission()
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

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> binding.tvToolbarTitle.text = "Home"
                R.id.tripsFragment -> binding.tvToolbarTitle.text = "Viaggi"
                R.id.statisticsFragment -> binding.tvToolbarTitle.text = "Statistiche"
                R.id.tripDetailFragment -> binding.tvToolbarTitle.text = "Dettaglio viaggio"
            }

            // Mostra/nascondi la BottomNavigation
            when (destination.id) {
                R.id.homeFragment, R.id.tripsFragment, R.id.statisticsFragment ->
                    binding.bottomNavigationView.visibility = View.VISIBLE
                else -> binding.bottomNavigationView.visibility = View.GONE
            }
        }

        navigateToTrackingFragmentIfNeeded(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == "ACTION_SHOW_TRACKING_FRAGMENT") {
            binding.navHostFragment.findNavController().navigate(R.id.action_global_tripDetailFragment)
        }
    }

    private fun checkLocationPermission() {
        val fineLocation = checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocation = checkSelfPermission(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            manageLocationPermission(fineLocation && backgroundLocation)
        } else {
            manageLocationPermission(fineLocation)
        }
    }

    private fun manageLocationPermission(permissionsGranted: Boolean) {
        if(permissionsGranted)
            return

        val dialog =  AlertDialog.Builder(this)
            .setTitle("Permessi Localizzazione Richiesti")
            .setMessage("Questa app richiede tutti i permessi di localizzazione per funzionare correttamente, accettali per poterla usare.")
            .setPositiveButton(
                "OK"
            ) { _, _ ->
                //Prompt the user once explanation has been shown
                requestLocationPermission()
            }
            .create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ) {
                dialog.show()
            } else {
                requestLocationPermission()
            }
        } else {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                dialog.show()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                CURRENT_LOCATION_PERMISSIONS_REQUEST
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                OLDER_LOCATION_PERMISSIONS_REQUEST
            )
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
                grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED) {

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
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
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

        checkLocationPermission()
    }

    companion object {
        private const val CURRENT_LOCATION_PERMISSIONS_REQUEST = 99 // Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        private const val OLDER_LOCATION_PERMISSIONS_REQUEST = 66  //  Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }
}