package com.example.travel_companion.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.startActivity

object PermissionsManager {

    const val CURRENT_LOCATION_PERMISSIONS_REQUEST = 99 // Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    const val OLDER_LOCATION_PERMISSIONS_REQUEST = 66  //  Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

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

    fun requestLocationPermission(context: Activity) {
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
}