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

    const val LOCATION_PERMISSIONS_REQUEST = 99 // Per tutti i permessi di localizzazione base
    const val BACKGROUND_LOCATION_PERMISSIONS_REQUEST = 98 // Per Android 10+ background location separato
    const val POST_NOTIFICATION_PERMISSIONS_REQUEST = 77
    const val CAMERA_PERMISSIONS_REQUEST = 88

    private const val PREFS_NAME = "permissions_prefs"
    private const val KEY_LOCATION_REQUESTED = "location_requested"
    private const val KEY_BACKGROUND_LOCATION_REQUESTED = "background_location_requested"
    private const val KEY_CAMERA_REQUESTED = "camera_requested"
    private const val KEY_NOTIFICATION_REQUESTED = "notification_requested"

    /**
     * Controlla e richiede i permessi di localizzazione
     */
    fun checkLocationPermission(context: Activity) {
        val coarseLocation = checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val fineLocation = checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocation = checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            // Debug: Log dello stato attuale
            android.util.Log.d("PermissionsManager", "Coarse Location: $coarseLocation, Fine Location: $fineLocation, Background Location: $backgroundLocation")

            // Prima controlla i permessi base (coarse e fine)
            if (!coarseLocation || !fineLocation) {
                requestLocationPermissionWithCheck(context)
            } else if (!backgroundLocation) {
                // Se i permessi base sono concessi ma non il background, richiedilo separatamente
                checkBackgroundLocationPermission(context)
            }
        } else {
            // Debug: Log dello stato attuale
            android.util.Log.d("PermissionsManager", "Coarse Location: $coarseLocation, Fine Location: $fineLocation (Android < Q)")

            if (!coarseLocation || !fineLocation) {
                requestLocationPermissionWithCheck(context)
            }
        }
    }

    /**
     * Controlla se dobbiamo mostrare la spiegazione prima di richiedere i permessi base
     */
    private fun requestLocationPermissionWithCheck(context: Activity) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasBeenRequested = prefs.getBoolean(KEY_LOCATION_REQUESTED, false)

        val coarseLocationRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val fineLocationRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Debug: Log dello stato delle richieste
        android.util.Log.d("PermissionsManager", "Location - HasBeenRequested: $hasBeenRequested, CoarseRationale: $coarseLocationRationale, FineRationale: $fineLocationRationale")

        when {
            // Prima richiesta - mostra direttamente il dialog nativo
            !hasBeenRequested -> {
                android.util.Log.d("PermissionsManager", "Prima richiesta localizzazione - mostro dialog nativo")
                requestLocationPermission(context)
                // Segna che abbiamo richiesto il permesso
                prefs.edit().putBoolean(KEY_LOCATION_REQUESTED, true).apply()
            }

            // L'utente ha negato ma non ha selezionato "Non chiedere più" - mostra spiegazione
            coarseLocationRationale || fineLocationRationale -> {
                android.util.Log.d("PermissionsManager", "Localizzazione negata una volta - mostro spiegazione")
                showLocationPermissionExplanationDialog(context)
            }

            // L'utente ha selezionato "Non chiedere più" - vai direttamente alle impostazioni
            else -> {
                android.util.Log.d("PermissionsManager", "Localizzazione negata definitivamente - vado alle impostazioni")
                showPermissionPermanentlyDeniedDialog(
                    context,
                    "Permessi Localizzazione",
                    "I permessi di localizzazione sono stati negati permanentemente. Devi concederli manualmente dalle impostazioni per utilizzare l'app."
                )
            }
        }
    }

    /**
     * Mostra dialog di spiegazione per i permessi di localizzazione base
     */
    private fun showLocationPermissionExplanationDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permessi Localizzazione Richiesti")
            .setMessage("Questa app richiede i permessi di localizzazione per funzionare correttamente. Senza questi permessi non potrai utilizzare l'app.")
            .setPositiveButton("Riprova") { _, _ ->
                requestLocationPermission(context)
            }
            .setNegativeButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Richiede i permessi di localizzazione base (coarse e fine)
     */
    private fun requestLocationPermission(context: Activity) {
        // Richiedi sempre i permessi base con lo stesso request code
        requestPermissions(
            context,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_PERMISSIONS_REQUEST
        )
    }

    /**
     * Richiede il permesso background location separatamente (Android 10+)
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun checkBackgroundLocationPermission(context: Activity) {
        val backgroundLocation = checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!backgroundLocation) {
            requestBackgroundLocationPermissionWithCheck(context)
        }
    }

    /**
     * Controlla se dobbiamo mostrare la spiegazione per il background location
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermissionWithCheck(context: Activity) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasBeenRequested = prefs.getBoolean(KEY_BACKGROUND_LOCATION_REQUESTED, false)

        val backgroundLocationRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        android.util.Log.d("PermissionsManager", "Background Location - HasBeenRequested: $hasBeenRequested, BackgroundRationale: $backgroundLocationRationale")

        when {
            !hasBeenRequested -> {
                android.util.Log.d("PermissionsManager", "Prima richiesta background location - mostro dialog nativo")
                requestPermissions(
                    context,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_PERMISSIONS_REQUEST
                )
                prefs.edit().putBoolean(KEY_BACKGROUND_LOCATION_REQUESTED, true).apply()
            }

            backgroundLocationRationale -> {
                showBackgroundLocationPermissionExplanationDialog(context)
            }

            else -> {
                showPermissionPermanentlyDeniedDialog(
                    context,
                    "Permesso Background Location",
                    "Il permesso per accedere alla posizione in background è stato negato permanentemente."
                )
            }
        }
    }

    /**
     * Mostra dialog di spiegazione per il background location
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showBackgroundLocationPermissionExplanationDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Background Location")
            .setMessage("Per tracciare i tuoi viaggi anche quando l'app è in background, abbiamo bisogno del permesso di localizzazione sempre attivo.")
            .setPositiveButton("Riprova") { _, _ ->
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

    /**
     * Controlla e richiede il permesso camera
     */
    fun checkCameraPermission(context: Activity) {
        val cameraPermission = checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!cameraPermission) {
            requestCameraPermissionWithCheck(context)
        }
    }

    /**
     * Controlla se dobbiamo mostrare la spiegazione per la camera
     */
    private fun requestCameraPermissionWithCheck(context: Activity) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasBeenRequested = prefs.getBoolean(KEY_CAMERA_REQUESTED, false)

        val cameraRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.CAMERA
        )

        android.util.Log.d("PermissionsManager", "Camera - HasBeenRequested: $hasBeenRequested, CameraRationale: $cameraRationale")

        when {
            // Prima richiesta - mostra direttamente il dialog nativo
            !hasBeenRequested -> {
                android.util.Log.d("PermissionsManager", "Prima richiesta camera - mostro dialog nativo")
                requestPermissions(
                    context,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSIONS_REQUEST
                )
                // Segna che abbiamo richiesto il permesso
                prefs.edit().putBoolean(KEY_CAMERA_REQUESTED, true).apply()
            }

            // L'utente ha negato ma non ha selezionato "Non chiedere più" - mostra spiegazione
            cameraRationale -> {
                android.util.Log.d("PermissionsManager", "Camera negata una volta - mostro spiegazione")
                showCameraPermissionExplanationDialog(context)
            }

            // L'utente ha selezionato "Non chiedere più" - vai direttamente alle impostazioni
            else -> {
                android.util.Log.d("PermissionsManager", "Camera negata definitivamente - vado alle impostazioni")
                showPermissionPermanentlyDeniedDialog(
                    context,
                    "Permesso Camera",
                    "Il permesso camera è stato negato permanentemente. Devi concederlo manualmente dalle impostazioni per utilizzare tutte le funzionalità dell'app."
                )
            }
        }
    }

    /**
     * Mostra dialog di spiegazione per il permesso camera
     */
    private fun showCameraPermissionExplanationDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Camera Richiesto")
            .setMessage("Questa app richiede il permesso camera per scattare foto durante i viaggi. Senza questo permesso non potrai utilizzare tutte le funzionalità dell'app.")
            .setPositiveButton("Riprova") { _, _ ->
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkNotificationsPermissions(context: Activity) {
        if (!hasNotificationPermissions(context)) {
            requestNotificationPermissionWithCheck(context)
        }
    }

    /**
     * Controlla se dobbiamo mostrare la spiegazione per le notifiche
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissionWithCheck(context: Activity) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasBeenRequested = prefs.getBoolean(KEY_NOTIFICATION_REQUESTED, false)

        val notificationRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )

        android.util.Log.d("PermissionsManager", "Notifications - HasBeenRequested: $hasBeenRequested, NotificationRationale: $notificationRationale")

        when {
            // Prima richiesta - mostra direttamente il dialog nativo
            !hasBeenRequested -> {
                android.util.Log.d("PermissionsManager", "Prima richiesta notifiche - mostro dialog nativo")
                requestPermissions(
                    context,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATION_PERMISSIONS_REQUEST
                )
                // Segna che abbiamo richiesto il permesso
                prefs.edit().putBoolean(KEY_NOTIFICATION_REQUESTED, true).apply()
            }

            // L'utente ha negato ma non ha selezionato "Non chiedere più" - mostra spiegazione
            notificationRationale -> {
                android.util.Log.d("PermissionsManager", "Notifiche negate una volta - mostro spiegazione")
                showNotificationPermissionExplanationDialog(context)
            }

            // L'utente ha selezionato "Non chiedere più" - vai direttamente alle impostazioni
            else -> {
                android.util.Log.d("PermissionsManager", "Notifiche negate definitivamente - vado alle impostazioni")
                showPermissionPermanentlyDeniedDialog(
                    context,
                    "Permesso Notifiche",
                    "Il permesso notifiche è stato negato permanentemente. Devi concederlo manualmente dalle impostazioni per ricevere aggiornamenti importanti."
                )
            }
        }
    }

    /**
     * Mostra dialog di spiegazione per il permesso notifiche
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showNotificationPermissionExplanationDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Notifiche Richiesto")
            .setMessage("Questa app richiede il permesso per inviare notifiche per tenerti aggiornato sui tuoi viaggi. Senza questo permesso non potrai utilizzare tutte le funzionalità dell'app.")
            .setPositiveButton("Riprova") { _, _ ->
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

    fun hasNotificationPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            return permission == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

    /**
     * Verifica se tutti i permessi essenziali sono stati concessi
     */
    fun areAllEssentialPermissionsGranted(context: Context): Boolean {
        // Controlla localizzazione
        val coarseLocation = checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val fineLocation = checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        // Controlla camera
        val camera = checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        // Controlla notifiche (opzionale per Android 13+, ma richiesto per il funzionamento completo)
        val notifications = hasNotificationPermissions(context)

        return coarseLocation && fineLocation && backgroundLocation && camera && notifications
    }

    /**
     * Gestisce il risultato delle richieste di permessi
     */
    fun handlePermissionResult(
        context: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onAllPermissionsGranted: () -> Unit = {},
        onPermissionDenied: (Int) -> Unit = {}
    ) {
        when (requestCode) {
            LOCATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Permessi di localizzazione base concessi
                    // Per Android 10+, ora richiedi il background location separatamente
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val backgroundLocation = checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!backgroundLocation) {
                            // Richiedi background location separatamente
                            checkBackgroundLocationPermission(context)
                            return
                        }
                    }

                    // Tutti i permessi di localizzazione concessi, controlla se tutti i permessi sono concessi
                    if (areAllEssentialPermissionsGranted(context)) {
                        onAllPermissionsGranted()
                    }
                } else {
                    // Permesso negato
                    onPermissionDenied(requestCode)
                    // Mostra dialog per spiegare l'importanza del permesso
                    showPermissionDeniedFinalDialog(
                        context,
                        "Permessi Localizzazione Negati",
                        "I permessi di localizzazione sono essenziali per il funzionamento dell'app. Senza questi permessi non puoi utilizzare l'app."
                    )
                }
            }

            BACKGROUND_LOCATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Background location concesso, controlla se tutti i permessi sono concessi
                    if (areAllEssentialPermissionsGranted(context)) {
                        onAllPermissionsGranted()
                    }
                } else {
                    // Background location negato
                    onPermissionDenied(requestCode)
                    // Mostra dialog per spiegare l'importanza del permesso
                    showPermissionDeniedFinalDialog(
                        context,
                        "Permesso Background Location Negato",
                        "Il permesso per accedere alla posizione in background è essenziale per tracciare i tuoi viaggi. Senza questo permesso non puoi utilizzare l'app."
                    )
                }
            }

            CAMERA_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permesso camera concesso, controlla se tutti i permessi sono concessi
                    if (areAllEssentialPermissionsGranted(context)) {
                        onAllPermissionsGranted()
                    }
                } else {
                    // Permesso negato
                    onPermissionDenied(requestCode)
                    // Mostra dialog per spiegare l'importanza del permesso
                    showPermissionDeniedFinalDialog(
                        context,
                        "Permesso Camera Negato",
                        "Il permesso camera è essenziale per scattare foto durante i tuoi viaggi. Senza questo permesso non puoi utilizzare tutte le funzionalità dell'app."
                    )
                }
            }

            POST_NOTIFICATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permesso notifiche concesso, controlla se tutti i permessi sono concessi
                    if (areAllEssentialPermissionsGranted(context)) {
                        onAllPermissionsGranted()
                    }
                } else {
                    // Permesso negato
                    onPermissionDenied(requestCode)
                    // Mostra dialog per spiegare l'importanza del permesso
                    showPermissionDeniedFinalDialog(
                        context,
                        "Permesso Notifiche Negato",
                        "Il permesso notifiche è importante per ricevere aggiornamenti sui tuoi viaggi. Senza questo permesso non potrai ricevere notifiche importanti."
                    )
                }
            }
        }
    }

    /**
     * Mostra un dialog quando un permesso viene negato (sia temporaneamente che definitivamente)
     */
    private fun showPermissionDeniedFinalDialog(
        context: Activity,
        title: String,
        message: String
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setNegativeButton("Esci dall'App") { _, _ ->
                context.finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Mostra un dialog quando un permesso viene negato definitivamente
     */
    private fun showPermissionPermanentlyDeniedDialog(
        context: Activity,
        title: String,
        message: String
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setNegativeButton("Esci dall'App") { _, _ ->
                context.finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Mostra dialog generico quando tutti i permessi non sono stati concessi
     */
    fun showAllPermissionsDeniedDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permessi Richiesti")
            .setMessage("L'app richiede tutti i permessi per funzionare correttamente. Puoi concederli dalle impostazioni dell'app.")
            .setPositiveButton("Apri Impostazioni") { _, _ ->
                openAppSettings(context)
            }
            .setNegativeButton("Esci dall'App") { _, _ ->
                context.finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Reset dello stato dei permessi richiesti (utile per test o reset completo)
     */
    fun resetPermissionRequestState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        android.util.Log.d("PermissionsManager", "Stato dei permessi resettato")
    }

    /**
     * Apre le impostazioni dell'app
     */
    private fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Impossibile aprire le impostazioni",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Verifica se l'app può schedulare allarmi esatti usando il metodo più appropriato
     */
    fun canScheduleExactAlarmsUniversal(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true // Prima di Android 12 non serve alcun permesso
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Versione smart che non richiede gestione dell'utente se USE_EXACT_ALARM è disponibile
     */
    fun checkExactAlarmPermissionSmart(context: Activity, onPermissionGranted: () -> Unit = {}) {
        if (!isExactAlarmPermissionRequired()) {
            onPermissionGranted()
            return
        }

        if (canScheduleExactAlarmsUniversal(context)) {
            onPermissionGranted()
            return
        }

        showExactAlarmPermissionDialog(context, onPermissionGranted)
    }

    /**
     * Verifica se il permesso è necessario per la versione Android corrente
     */
    fun isExactAlarmPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Mostra un dialog per spiegare e richiedere il permesso degli allarmi esatti
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun showExactAlarmPermissionDialog(context: Activity, onPermissionGranted: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Permesso Allarmi Richiesto")
            .setMessage("Per schedulare correttamente i viaggi e inviarti notifiche puntuali, l'app ha bisogno del permesso per impostare allarmi precisi. Vuoi concedere questo permesso?")
            .setPositiveButton("Concedi") { _, _ ->
                openExactAlarmSettings(context)
            }
            .setNegativeButton("Continua senza") { _, _ ->
                Toast.makeText(
                    context,
                    "I viaggi verranno schedulati con allarmi meno precisi",
                    Toast.LENGTH_LONG
                ).show()
                onPermissionGranted()
            }
            .setNeutralButton("Annulla", null)
            .setCancelable(false)
            .show()
    }

    /**
     * Apre le impostazioni per permettere all'utente di concedere il permesso degli allarmi esatti
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun openExactAlarmSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            Toast.makeText(
                context,
                "Cerca 'Allarmi e promemoria' nelle impostazioni dell'app",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}