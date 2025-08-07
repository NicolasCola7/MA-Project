package com.example.travel_companion.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.travel_companion.service.Polyline
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.Locale

object Utils {
    const val TRACKING_TIME: Long = 3000
    val dateTimeFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
    val timeFormat: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.ITALY)

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

        /**
         * Aggiorna la visibilità e il testo di un pulsante di eliminazione
         * @param button Il pulsante da aggiornare
         * @param selectedCount Numero di elementi selezionati
         * @param baseText Testo base del pulsante (es. "Elimina")
         */
        fun updateDeleteButton(
            button: View,
            selectedCount: Int,
            baseText: String = "Elimina"
        ) {
            button.isVisible = selectedCount > 0

            if (selectedCount > 0 && button is TextView) {
                button.text = "$baseText ( $selectedCount )"
            }
        }

        /**
         * Gestisce l'operazione di eliminazione multipla con conferma
         * @param context Context
         * @param selectedItems Lista degli elementi selezionati
         * @param itemType Tipo di elemento (es. "viaggi", "note", "immagini")
         * @param onDelete Callback per eseguire l'eliminazione effettiva
         * @param onClearSelection Callback per pulire la selezione
         * @param onUpdateButton Callback per aggiornare il pulsante
         */
        fun <T> handleMultipleDelete(
            context: Context,
            selectedItems: List<T>,
            itemType: String,
            onDelete: (List<T>) -> Unit,
            onClearSelection: () -> Unit,
            onUpdateButton: (Int) -> Unit
        ) {
            if (selectedItems.isEmpty()) return

            showMultipleDeleteConfirmation(
                context = context,
                count = selectedItems.size,
                itemType = itemType,
                onConfirmed = {
                    onDelete(selectedItems)
                    onClearSelection()
                    onUpdateButton(0)
                }
            )
        }
    }
}