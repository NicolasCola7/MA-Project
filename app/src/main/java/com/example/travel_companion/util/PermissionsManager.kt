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

    const val CURRENT_LOCATION_PERMISSIONS_REQUEST = 99 // Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    const val OLDER_LOCATION_PERMISSIONS_REQUEST = 66  //  Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    const val POST_NOTIFICATION_PERMISSIONS_REQUEST = 77
    const val CAMERA_PERMISSIONS_REQUEST = 88

    /**
     * Controlla e richiede i permessi di localizzazione
     */
    fun checkLocationPermission(context: Activity) {
        val fineLocation = checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocation = checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!fineLocation || !backgroundLocation) {
                requestLocationPermissionWithCheck(context)
            }
        } else {
            if (!fineLocation) {
                requestLocationPermissionWithCheck(context)
            }
        }
    }

    /**
     * Controlla se dobbiamo mostrare la spiegazione prima di richiedere i permessi
     */
    private fun requestLocationPermissionWithCheck(context: Activity) {
        val fineLocationRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val backgroundLocationRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            shouldShowRequestPermissionRationale(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else false

        if (fineLocationRationale || backgroundLocationRationale) {
            // L'utente ha negato il permesso in precedenza, mostra spiegazione
            showLocationPermissionExplanationDialog(context)
        } else {
            // Prima richiesta o "Non chiedere più" - richiedi direttamente
            requestLocationPermission(context)
        }
    }

    /**
     * Mostra dialog di spiegazione per i permessi di localizzazione
     */
    private fun showLocationPermissionExplanationDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle("Permessi Localizzazione Richiesti")
            .setMessage("Questa app richiede tutti i permessi di localizzazione per funzionare correttamente. Senza questi permessi non potrai utilizzare l'app.")
            .setPositiveButton("Concedi") { _, _ ->
                requestLocationPermission(context)
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
        val cameraRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.CAMERA
        )

        if (cameraRationale) {
            // L'utente ha negato il permesso in precedenza, mostra spiegazione
            showCameraPermissionExplanationDialog(context)
        } else {
            // Prima richiesta o "Non chiedere più" - richiedi direttamente
            requestPermissions(
                context,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSIONS_REQUEST
            )
        }
    }

    /**
     * Mostra dialog di spiegazione per il permesso camera
     */
    private fun showCameraPermissionExplanationDialog(context: Activity) {
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

    private fun requestLocationPermission(context: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(
                context,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                CURRENT_LOCATION_PERMISSIONS_REQUEST
            )
        } else {
            requestPermissions(
                context,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                OLDER_LOCATION_PERMISSIONS_REQUEST
            )
        }
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
        val notificationRationale = shouldShowRequestPermissionRationale(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (notificationRationale) {
            // L'utente ha negato il permesso in precedenza, mostra spiegazione
            showNotificationPermissionExplanationDialog(context)
        } else {
            // Prima richiesta o "Non chiedere più" - richiedi direttamente
            requestPermissions(
                context,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                POST_NOTIFICATION_PERMISSIONS_REQUEST
            )
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

    fun hasNotificationPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permission == PackageManager.PERMISSION_GRANTED) {
                return true
            } else {
                return false
            }
        } else {
            return true
        }
    }

    /**
     * Verifica se tutti i permessi essenziali sono stati concessi
     */
    fun areAllEssentialPermissionsGranted(context: Context): Boolean {
        // Controlla localizzazione
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

        return fineLocation && backgroundLocation && camera && notifications
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
            CURRENT_LOCATION_PERMISSIONS_REQUEST,
            OLDER_LOCATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Permessi di localizzazione concessi, controlla se tutti i permessi sono concessi
                    if (areAllEssentialPermissionsGranted(context)) {
                        onAllPermissionsGranted()
                    }
                } else {
                    // Permesso negato, mostra dialog per aprire impostazioni
                    showPermissionDeniedDialog(
                        context,
                        "Permessi Localizzazione Negati",
                        "L'app non può funzionare senza i permessi di localizzazione. Aprire le impostazioni per concederli manualmente?"
                    )
                    onPermissionDenied(requestCode)
                }
            }

            CAMERA_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permesso camera concesso, controlla se tutti i permessi sono concessi
                    if (areAllEssentialPermissionsGranted(context)) {
                        onAllPermissionsGranted()
                    }
                } else {
                    // Permesso negato, mostra dialog per aprire impostazioni
                    showPermissionDeniedDialog(
                        context,
                        "Permesso Camera Negato",
                        "L'app non può funzionare senza il permesso camera. Aprire le impostazioni per concederlo manualmente?"
                    )
                    onPermissionDenied(requestCode)
                }
            }

            POST_NOTIFICATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permesso notifiche concesso, controlla se tutti i permessi sono concessi
                    if (areAllEssentialPermissionsGranted(context)) {
                        onAllPermissionsGranted()
                    }
                } else {
                    // Permesso negato, mostra dialog per aprire impostazioni
                    showPermissionDeniedDialog(
                        context,
                        "Permesso Notifiche Negato",
                        "L'app non può funzionare completamente senza il permesso notifiche. Aprire le impostazioni per concederlo manualmente?"
                    )
                    onPermissionDenied(requestCode)
                }
            }
        }
    }

    /**
     * Mostra un dialog quando un permesso viene negato definitivamente
     */
    private fun showPermissionDeniedDialog(
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
     * Apre le impostazioni per permettere all'utente di concedere il permesso
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