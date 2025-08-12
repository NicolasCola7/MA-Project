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

            manageLocationPermission(context,fineLocation && backgroundLocation)
        } else {
            manageLocationPermission(context, fineLocation)
        }
    }

    private fun manageLocationPermission(context: Activity, permissionsGranted: Boolean) {
        if(permissionsGranted)
            return

        val dialog =  AlertDialog.Builder(context)
            .setTitle("Permessi Localizzazione Richiesti")
            .setMessage("Questa app richiede tutti i permessi di localizzazione per funzionare correttamente, accettali per poterla usare.")
            .setPositiveButton(
                "OK"
            ) { _, _ ->
                //Prompt the user once explanation has been shown
                requestLocationPermission(context)
            }
            .create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (shouldShowRequestPermissionRationale(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ) {
                dialog.show()
            } else {
                requestLocationPermission(context)
            }
        } else {
            if (shouldShowRequestPermissionRationale(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                dialog.show()
            } else {
                requestLocationPermission(context)
            }
        }
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
            requestPermissions(
                context,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                ),
                POST_NOTIFICATION_PERMISSIONS_REQUEST
            )
        }
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
     * Verifica se l'app può schedulare allarmi esatti usando il metodo più appropriato
     * Prova prima USE_EXACT_ALARM (non richiede consenso utente),
     * poi fallback su SCHEDULE_EXACT_ALARM se necessario
     */
    fun canScheduleExactAlarmsUniversal(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true // Prima di Android 12 non serve alcun permesso
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Controlla se possiamo schedulare allarmi esatti (copre entrambi i permessi)
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

        // Prova prima a vedere se gli allarmi esatti sono già disponibili
        if (canScheduleExactAlarmsUniversal(context)) {
            onPermissionGranted()
            return
        }

        // Se non sono disponibili, chiedi all'utente di abilitare SCHEDULE_EXACT_ALARM
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
            // Fallback se l'intent specifico non funziona
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