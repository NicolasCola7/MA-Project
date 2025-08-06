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

    /**
     * Ridimensiona un bitmap mantenendo le proporzioni
     * @param bitmap Il bitmap originale
     * @param maxWidth Larghezza massima
     * @param maxHeight Altezza massima
     * @return Bitmap ridimensionato
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Calcola il fattore di scala
        val scaleWidth = maxWidth.toFloat() / originalWidth
        val scaleHeight = maxHeight.toFloat() / originalHeight
        val scaleFactor = minOf(scaleWidth, scaleHeight)

        // Calcola le nuove dimensioni
        val newWidth = (originalWidth * scaleFactor).toInt()
        val newHeight = (originalHeight * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Mostra un PopupMenu per la cancellazione
     *
     * @param context Il context del fragment/activity
     * @param anchorView La view su cui ancorare il popup
     * @param itemName Il nome dell'elemento da eliminare (es. "viaggio", "nota", "immagine")
     * @param onDeleteConfirmed Callback eseguito quando l'utente conferma l'eliminazione
     */
    fun showDeletePopup(
        context: Context,
        anchorView: View,
        itemName: String,
        onDeleteConfirmed: () -> Unit
    ) {
        val popupMenu = PopupMenu(context, anchorView)
        popupMenu.menuInflater.inflate(R.menu.delete_context_menu, popupMenu.menu)

        // Forza la visualizzazione delle icone nei menu
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation(context, itemName, onDeleteConfirmed)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    /**
     * Mostra il dialog di conferma per l'eliminazione
     */
    private fun showDeleteConfirmation(
        context: Context,
        itemName: String,
        onDeleteConfirmed: () -> Unit
    ) {
        val message = "Sei sicuro di voler eliminare questo ${itemName.lowercase()}?"

        AlertDialog.Builder(context)
            .setTitle("Elimina ${itemName.lowercase()}")
            .setMessage(message)
            .setPositiveButton("Elimina") { _, _ ->
                onDeleteConfirmed()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}



