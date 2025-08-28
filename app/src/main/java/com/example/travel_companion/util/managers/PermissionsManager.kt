package com.example.travel_companion.util.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission

object PermissionsManager {

    const val LOCATION_PERMISSIONS_REQUEST = 99
    const val BACKGROUND_LOCATION_PERMISSIONS_REQUEST = 98
    const val POST_NOTIFICATION_PERMISSIONS_REQUEST = 77
    const val CAMERA_PERMISSIONS_REQUEST = 88

    private const val PREFS_NAME = "permissions_prefs"
    private const val KEY_ANY_PERMISSION_DENIED = "any_permission_denied"

    /**
     * Initiates the sequential request of all required permissions.
     * If any permission has been previously denied, prompts the user to open app settings.
     *
     * @param context The activity from which to request permissions.
     */
    fun requestAllPermissionsSequentially(context: Activity) {
        if (hasAnyPermissionBeenDenied(context)) {
            showGoToSettingsDialog(context)
        } else {
            startPermissionSequence(context)
        }
    }

    /**
     * Begins the sequence of permission requests in a logical order.
     *
     * @param context The activity from which to request permissions.
     */
    private fun startPermissionSequence(context: Activity) {
        when {
            !hasLocationPermissions(context) -> requestLocationPermission(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission(context) ->
                requestBackgroundLocationPermission(context)
            !hasCameraPermission(context) -> requestCameraPermission(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermissions(context) ->
                requestNotificationPermission(context)
            else -> clearDeniedFlag(context)
        }
    }

    // === LOCATION PERMISSIONS ===

    /**
     * Checks if both coarse and fine location permissions are granted.
     *
     * @param context The context to check permissions for.
     * @return True if both location permissions are granted, false otherwise.
     */
    private fun hasLocationPermissions(context: Context): Boolean {
        val coarse = checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse && fine
    }

    /**
     * Checks if background location permission is granted (Android Q and above).
     *
     * @param context The context to check permission for.
     * @return True if background location permission is granted, false otherwise.
     */
    private fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Requests location permissions from the user.
     * Shows a rationale dialog if necessary.
     *
     * @param context The activity from which to request permissions.
     */
    private fun requestLocationPermission(context: Activity) {
        if (shouldShowLocationRationale(context)) {
            showLocationPermissionDialog(context)
        } else {
            requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSIONS_REQUEST
            )
        }
    }

    /**
     * Determines if a rationale should be shown for location permissions.
     *
     * @param context The activity to check.
     * @return True if a rationale should be shown, false otherwise.
     */
    private fun shouldShowLocationRationale(context: Activity): Boolean {
        return shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Displays a dialog explaining why location permissions are required.
     *
     * @param context The activity to show the dialog in.
     */
    private fun showLocationPermissionDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permessi Localizzazione Richiesti")
            .setMessage("Questa app richiede i permessi di localizzazione per funzionare correttamente. Senza questi permessi non potrai utilizzare l'app.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissions(
                    context,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSIONS_REQUEST
                )
            }
            .setNegativeButton("Apri Impostazioni") { _, _ -> openAppSettings(context) }
            .setCancelable(false)
            .show()
    }

    // === BACKGROUND LOCATION PERMISSION ===

    /**
     * Requests background location permission (Android Q and above).
     *
     * @param context The activity from which to request permission.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermission(context: Activity) {
        if (shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            showBackgroundLocationPermissionDialog(context)
        } else {
            requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSIONS_REQUEST
            )
        }
    }

    /**
     * Displays a dialog explaining why background location permission is required.
     *
     * @param context The activity to show the dialog in.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBackgroundLocationPermissionDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Background Location")
            .setMessage("Per tracciare i tuoi viaggi anche quando l'app è in background, abbiamo bisogno del permesso di localizzazione sempre attivo.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissions(
                    context,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_PERMISSIONS_REQUEST
                )
            }
            .setNegativeButton("Apri Impostazioni") { _, _ -> openAppSettings(context) }
            .setCancelable(false)
            .show()
    }

    // === CAMERA PERMISSION ===

    /**
     * Checks if camera permission is granted.
     *
     * @param context The context to check permission for.
     * @return True if camera permission is granted, false otherwise.
     */
    private fun hasCameraPermission(context: Context): Boolean {
        return checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests camera permission from the user.
     *
     * @param context The activity from which to request permission.
     */
    private fun requestCameraPermission(context: Activity) {
        if (shouldShowRequestPermissionRationale(context, Manifest.permission.CAMERA)) {
            showCameraPermissionDialog(context)
        } else {
            requestPermissions(context, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSIONS_REQUEST)
        }
    }

    /**
     * Displays a dialog explaining why camera permission is required.
     *
     * @param context The activity to show the dialog in.
     */
    private fun showCameraPermissionDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Camera Richiesto")
            .setMessage("Questa app richiede il permesso camera per scattare foto durante i viaggi. Senza questo permesso non potrai utilizzare tutte le funzionalità dell'app.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissions(context, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSIONS_REQUEST)
            }
            .setNegativeButton("Apri Impostazioni") { _, _ -> openAppSettings(context) }
            .setCancelable(false)
            .show()
    }

    // === NOTIFICATION PERMISSION ===

    /**
     * Checks if notification permission is granted (Android Tiramisu and above).
     *
     * @param context The context to check permission for.
     * @return True if notification permission is granted, false otherwise.
     */
    fun hasNotificationPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Requests notification permission from the user (Android Tiramisu and above).
     *
     * @param context The activity from which to request permission.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission(context: Activity) {
        if (shouldShowRequestPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionDialog(context)
        } else {
            requestPermissions(context, arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATION_PERMISSIONS_REQUEST)
        }
    }

    /**
     * Displays a dialog explaining why notification permission is required.
     *
     * @param context The activity to show the dialog in.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showNotificationPermissionDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Notifiche Richiesto")
            .setMessage("Questa app richiede il permesso per inviare notifiche per tenerti aggiornato sui tuoi viaggi. Senza questo permesso non potrai utilizzare tutte le funzionalità dell'app.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissions(context, arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATION_PERMISSIONS_REQUEST)
            }
            .setNegativeButton("Apri Impostazioni") { _, _ -> openAppSettings(context) }
            .setCancelable(false)
            .show()
    }

    // === PERMISSION RESULT HANDLING ===

    /**
     * Handles the result of permission requests.
     * Continues the permission sequence if granted or shows a denied dialog if rejected.
     *
     * @param context The activity that received the permission result.
     * @param requestCode The request code passed in requestPermissions().
     * @param grantResults The grant results for the corresponding permissions.
     * @param onAllPermissionsGranted Callback invoked when all essential permissions are granted.
     */
    fun handlePermissionResult(
        context: Activity,
        requestCode: Int,
        grantResults: IntArray,
        onAllPermissionsGranted: () -> Unit = {}
    ) {
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            continuePermissionSequence(context, onAllPermissionsGranted)
        } else {
            markPermissionDenied(context)

            val message = when (requestCode) {
                LOCATION_PERMISSIONS_REQUEST -> "I permessi di localizzazione sono essenziali per il funzionamento dell'app."
                BACKGROUND_LOCATION_PERMISSIONS_REQUEST -> "Il permesso per accedere alla posizione in background è essenziale per tracciare i tuoi viaggi."
                CAMERA_PERMISSIONS_REQUEST -> "Il permesso camera è essenziale per scattare foto durante i tuoi viaggi."
                POST_NOTIFICATION_PERMISSIONS_REQUEST -> "Il permesso notifiche è importante per ricevere aggiornamenti sui tuoi viaggi."
                else -> "Questo permesso è necessario per il corretto funzionamento dell'app."
            }

            showPermissionDeniedDialog(context, message)
        }
    }

    /**
     * Continues requesting remaining permissions or calls the callback if all are granted.
     *
     * @param context The activity to continue requesting permissions.
     * @param onAllPermissionsGranted Callback invoked when all essential permissions are granted.
     */
    private fun continuePermissionSequence(context: Activity, onAllPermissionsGranted: () -> Unit) {
        if (areAllEssentialPermissionsGranted(context)) {
            clearDeniedFlag(context)
            onAllPermissionsGranted()
        } else {
            context.runOnUiThread {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startPermissionSequence(context)
                }, 300)
            }
        }
    }

    // === UTILITY METHODS ===

    /**
     * Checks if all essential permissions are granted.
     *
     * @param context The context to check permissions for.
     * @return True if all essential permissions are granted, false otherwise.
     */
    fun areAllEssentialPermissionsGranted(context: Context): Boolean {
        return hasLocationPermissions(context) &&
                hasBackgroundLocationPermission(context) &&
                hasCameraPermission(context) &&
                hasNotificationPermissions(context)
    }

    /**
     * Returns true if any permission has been denied in the past.
     *
     * @param context The context to check.
     * @return True if any permission has been denied, false otherwise.
     */
    private fun hasAnyPermissionBeenDenied(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ANY_PERMISSION_DENIED, false)
    }

    /**
     * Marks that a permission was denied by the user.
     *
     * @param context The context to store the flag in.
     */
    private fun markPermissionDenied(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ANY_PERMISSION_DENIED, true).apply()
    }

    /**
     * Clears the permission denied flag.
     *
     * @param context The context to clear the flag from.
     */
    private fun clearDeniedFlag(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ANY_PERMISSION_DENIED, false).apply()
    }

    // === DIALOG METHODS ===

    /**
     * Shows a dialog informing the user that a permission was denied and directs them to settings.
     *
     * @param context The activity to show the dialog in.
     * @param message The message to display in the dialog.
     */
    private fun showPermissionDeniedDialog(context: Activity, message: String) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Negato")
            .setMessage("$message\n\nPer utilizzare l'app, concedi tutti i permessi dalle impostazioni.")
            .setPositiveButton("Apri Impostazioni") { _, _ -> openAppSettings(context) }
            .setNegativeButton("Esci dall'App") { _, _ -> context.finish() }
            .setCancelable(false)
            .show()
    }

    /**
     * Shows a dialog prompting the user to go to settings to grant all required permissions.
     *
     * @param context The activity to show the dialog in.
     */
    private fun showGoToSettingsDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permessi Richiesti")
            .setMessage("Per utilizzare l'app, devi concedere tutti i permessi richiesti dalle impostazioni.")
            .setPositiveButton("Apri Impostazioni") { _, _ -> openAppSettings(context) }
            .setNegativeButton("Esci dall'App") { _, _ -> context.finish() }
            .setCancelable(false)
            .show()
    }

    /**
     * Opens the app's settings page for the user to manage permissions.
     *
     * @param context The activity from which to open settings.
     */
    private fun openAppSettings(context: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }
}
