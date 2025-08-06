package com.example.travel_companion.presentation

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.Build
import android.view.View
import android.widget.PopupMenu
import com.example.travel_companion.R
import com.example.travel_companion.service.Polyline
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.Locale

object Utils {
    const val TRACKING_TIME: Long = 3000

    val dateTimeFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY)

    fun hasLocationPermissions(context: Context) =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    fun calculatePolylineLength(polyline: Polyline): Float {
        var distance = 0f
        for(i in 0..polyline.size - 2) {
            val pos1 = polyline[i]
            val pos2 = polyline[i + 1]

            val result = FloatArray(1)
            Location.distanceBetween(
                pos1.latitude,
                pos1.longitude,
                pos2.latitude,
                pos2.longitude,
                result
            )
            distance += result[0]
        }
        return distance
    }

    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scaleWidth = maxWidth.toFloat() / originalWidth
        val scaleHeight = maxHeight.toFloat() / originalHeight
        val scaleFactor = minOf(scaleWidth, scaleHeight)

        val newWidth = (originalWidth * scaleFactor).toInt()
        val newHeight = (originalHeight * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Helper per gestire le operazioni di selezione multipla
     */
    object SelectionHelper {

        /**
         * Mostra dialog di conferma per eliminazione multipla
         * @param context Context
         * @param count Numero di elementi selezionati
         * @param itemType Tipo di elemento (es. "viaggi", "note", "immagini")
         * @param onConfirmed Callback eseguito alla conferma
         */
        fun showMultipleDeleteConfirmation(
            context: Context,
            count: Int,
            itemType: String,
            onConfirmed: () -> Unit
        ) {
            val message = if (count == 1) {
                "Sei sicuro di voler eliminare 1 ${itemType.dropLast(1)}?" // Rimuove la 's' finale per il singolare
            } else {
                "Sei sicuro di voler eliminare $count $itemType?"
            }

            AlertDialog.Builder(context)
                .setTitle("Elimina $itemType")
                .setMessage(message)
                .setPositiveButton("Elimina") { _, _ -> onConfirmed() }
                .setNegativeButton("Annulla", null)
                .show()
        }

    }
}