package com.example.travel_companion.presentation.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.travel_companion.R
import com.example.travel_companion.databinding.ActivityMainBinding
import com.example.travel_companion.util.managers.PermissionsManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point of the Travel Companion application.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isAppReady = false

    /**
     * Called when the activity is starting.
     * Checks if all essential permissions are granted and either initializes the app
     * or shows the permissions request screen.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     * this contains the data it most recently supplied in onSaveInstanceState.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PermissionsManager.areAllEssentialPermissionsGranted(this)) {
            initializeApp()
        } else {
            showPermissionsScreen()
        }
    }

    /**
     * Initializes the application by setting the app ready flag and configuring the UI.
     */
    private fun initializeApp() {
        isAppReady = true
        setupUI()
    }

    /**
     * Displays the permissions request screen and starts requesting all essential permissions sequentially.
     */
    private fun showPermissionsScreen() {
        setContentView(R.layout.activity_permissions)
        PermissionsManager.requestAllPermissionsSequentially(this)
    }

    /**
     * Configures the main UI, including binding views, setting up navigation controller,
     * and controlling the visibility of the bottom navigation and app bar depending on the current fragment.
     */
    private fun setupUI() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = binding.bottomNavigationView

        bottomNavigationView.setupWithNavController(navController)

        val fragmentsWithBottomNav = setOf(
            R.id.homeFragment,
            R.id.tripsFragment,
            R.id.statisticsFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigationView.visibility = if (destination.id in fragmentsWithBottomNav) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.appBarLayout.visibility = if (destination.id == R.id.photoFullScreenFragment) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    /**
     * Called when the user responds to permission requests.
     * Handles the permission result and initializes the app if all essential permissions are granted.
     *
     * @param requestCode The request code passed in requestPermissions.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
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
            }
        )
    }

    /**
     * Called when the activity becomes visible to the user.
     * Ensures the app is initialized if all essential permissions are granted.
     */
    override fun onResume() {
        super.onResume()

        if (!isAppReady && PermissionsManager.areAllEssentialPermissionsGranted(this)) {
            initializeApp()
        }
    }

    /**
     * Called when the activity is about to be destroyed.
     * Resets the app ready flag.
     */
    override fun onDestroy() {
        super.onDestroy()
        isAppReady = false
    }
}
