package com.example.travel_companion.util

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
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
     * Punto di ingresso principale per richiedere tutti i permessi in sequenza
     */
    fun requestAllPermissionsSequentially(context: Activity) {
        if (hasAnyPermissionBeenDenied(context)) {
            // Se qualche permesso è già stato negato in passato,
            // vai direttamente alle impostazioni
            showGoToSettingsDialog(context)
        } else {
            // Prima volta o tutti i permessi non sono mai stati negati
            startPermissionSequence(context)
        }
    }

    /**
     * Inizia la sequenza di richiesta permessi
     */
    private fun startPermissionSequence(context: Activity) {
        when {
            !hasLocationPermissions(context) -> {
                requestLocationPermission(context)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission(context) -> {
                requestBackgroundLocationPermission(context)
            }
            !hasCameraPermission(context) -> {
                requestCameraPermission(context)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermissions(context) -> {
                requestNotificationPermission(context)
            }
            else -> {
                // Tutti i permessi sono concessi
                clearDeniedFlag(context)
            }
        }
    }

    // === LOCATION PERMISSIONS ===

    private fun hasLocationPermissions(context: Context): Boolean {
        val coarse = checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse && fine
    }

    private fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestLocationPermission(context: Activity) {
        if (shouldShowLocationRationale(context)) {
            showLocationPermissionDialog(context)
        } else {
            requestPermissions(
                context,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                LOCATION_PERMISSIONS_REQUEST
            )
        }
    }

    private fun shouldShowLocationRationale(context: Activity): Boolean {
        return shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun showLocationPermissionDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permessi Localizzazione Richiesti")
            .setMessage("Questa app richiede i permessi di localizzazione per funzionare correttamente. Senza questi permessi non potrai utilizzare l'app.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissions(
                    context,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    LOCATION_PERMISSIONS_REQUEST
                )
            }
            .setNegativeButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setCancelable(false)
            .show()
    }

    // === BACKGROUND LOCATION PERMISSION ===

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
            .setNegativeButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setCancelable(false)
            .show()
    }

    // === CAMERA PERMISSION ===

    private fun hasCameraPermission(context: Context): Boolean {
        return checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission(context: Activity) {
        if (shouldShowRequestPermissionRationale(context, Manifest.permission.CAMERA)) {
            showCameraPermissionDialog(context)
        } else {
            requestPermissions(
                context,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSIONS_REQUEST
            )
        }
    }

    private fun showCameraPermissionDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Camera Richiesto")
            .setMessage("Questa app richiede il permesso camera per scattare foto durante i viaggi. Senza questo permesso non potrai utilizzare tutte le funzionalità dell'app.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissions(
                    context,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSIONS_REQUEST
                )
            }
            .setNegativeButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setCancelable(false)
            .show()
    }

    // === NOTIFICATION PERMISSION ===

    fun hasNotificationPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission(context: Activity) {
        if (shouldShowRequestPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionDialog(context)
        } else {
            requestPermissions(
                context,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                POST_NOTIFICATION_PERMISSIONS_REQUEST
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showNotificationPermissionDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Notifiche Richiesto")
            .setMessage("Questa app richiede il permesso per inviare notifiche per tenerti aggiornato sui tuoi viaggi. Senza questo permesso non potrai utilizzare tutte le funzionalità dell'app.")
            .setPositiveButton("Concedi") { _, _ ->
                requestPermissions(
                    context,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATION_PERMISSIONS_REQUEST
                )
            }
            .setNegativeButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setCancelable(false)
            .show()
    }

    // === PERMISSION RESULT HANDLING ===

    fun handlePermissionResult(
        context: Activity,
        requestCode: Int,
        grantResults: IntArray,
        onAllPermissionsGranted: () -> Unit = {}
    ) {
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            // Permesso corrente concesso, continua con il prossimo
            continuePermissionSequence(context, onAllPermissionsGranted)
        } else {
            // Permesso negato, segna il flag e mostra dialog per andare alle impostazioni
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

    private fun continuePermissionSequence(context: Activity, onAllPermissionsGranted: () -> Unit) {
        if (areAllEssentialPermissionsGranted(context)) {
            clearDeniedFlag(context)
            onAllPermissionsGranted()
        } else {
            // Continua con il prossimo permesso mancante
            context.runOnUiThread {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startPermissionSequence(context)
                }, 300)
            }
        }
    }

    // === UTILITY METHODS ===

    fun areAllEssentialPermissionsGranted(context: Context): Boolean {
        return hasLocationPermissions(context) &&
                hasBackgroundLocationPermission(context) &&
                hasCameraPermission(context) &&
                hasNotificationPermissions(context)
    }

    private fun hasAnyPermissionBeenDenied(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ANY_PERMISSION_DENIED, false)
    }

    private fun markPermissionDenied(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ANY_PERMISSION_DENIED, true).apply()
    }

    private fun clearDeniedFlag(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ANY_PERMISSION_DENIED, false).apply()
    }

    // === DIALOG METHODS ===

    private fun showPermissionDeniedDialog(context: Activity, message: String) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Negato")
            .setMessage("$message\n\nPer utilizzare l'app, concedi tutti i permessi dalle impostazioni.")
            .setPositiveButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setNegativeButton("Esci dall'App") { _, _ ->
                context.finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGoToSettingsDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permessi Richiesti")
            .setMessage("Per utilizzare l'app, devi concedere tutti i permessi richiesti dalle impostazioni.")
            .setPositiveButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setNegativeButton("Esci dall'App") { _, _ ->
                context.finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Impossibile aprire le impostazioni", Toast.LENGTH_LONG).show()
        }
    }
}