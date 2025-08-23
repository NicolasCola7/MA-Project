package com.example.travel_companion.util

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.travel_companion.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object Utils {
    const val TRACKING_TIME: Long = 1000
    val dateTimeFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

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

    fun getFormattedTrackingTime(timestamp: Long): String {
        var milliseconds = timestamp
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if(hours < 10) "0" else ""}$hours:" +
                "${if(minutes < 10) "0" else ""}$minutes:" +
                "${if(seconds < 10) "0" else ""}$seconds"
    }

    fun createNotificationChannel(notificationManager: NotificationManager, channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
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
                val singularItem = when (itemType) {
                    "note" -> "nota"
                    "viaggi" -> "viaggio"
                    else -> itemType
                }
                "Sei sicuro di voler eliminare 1 $singularItem?"
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

        fun Long.toDurationString(): String {
            val hours = TimeUnit.MILLISECONDS.toHours(this)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
            return String.format("%02d:%02d", hours, minutes)
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

    /**
     * Helper class per gestire gli empty states in modo uniforme nell'applicazione
     */
    object EmptyStateHelper {
        private fun setupEmptyState(
            emptyStateView: ConstraintLayout,
            @DrawableRes iconRes: Int,
            text: String
        ) {
            val iconView = emptyStateView.findViewById<ImageView>(R.id.emptyStateIcon)
            val textView = emptyStateView.findViewById<TextView>(R.id.emptyStateText)

            iconView?.setImageResource(iconRes)
            textView?.text = text
        }

        private fun showEmptyState(emptyStateView: ConstraintLayout) {
            emptyStateView.visibility = View.VISIBLE
        }

        fun hideEmptyState(emptyStateView: ConstraintLayout) {
            emptyStateView.visibility = View.GONE
        }

        fun showTripsEmptyState(
            emptyStateView: ConstraintLayout,
            hasFilters: Boolean = false
        ) {
            val text = if (hasFilters) {
                "Nessun viaggio trovato con i filtri attuali"
            } else {
                "Non hai ancora pianificato nessun viaggio"
            }
            setupEmptyState(emptyStateView, R.drawable.ic_detail_trip_24, text)
            showEmptyState(emptyStateView)
        }

        fun showNotesEmptyState(emptyStateView: ConstraintLayout) {
            setupEmptyState(
                emptyStateView,
                R.drawable.ic_edit_note_24,
                "Non hai ancora scritto nessuna nota per questo viaggio"
            )
            showEmptyState(emptyStateView)
        }

        fun showPhotosEmptyState(emptyStateView: ConstraintLayout) {
            setupEmptyState(
                emptyStateView,
                R.drawable.ic_menu_gallery,
                "Non hai ancora scattato nessuna foto per questo viaggio"
            )
            showEmptyState(emptyStateView)
        }

        fun showHomeEmptyState(
            emptyStateView: ConstraintLayout,
            onButtonClick: () -> Unit
        ) {
            val iconView = emptyStateView.findViewById<ImageView>(R.id.emptyStateIcon)
            val buttonView = emptyStateView.findViewById<com.google.android.material.button.MaterialButton>(R.id.emptyStateButton)

            iconView?.setImageResource(R.drawable.ic_detail_trip_24)
            buttonView?.setOnClickListener { onButtonClick() }

            showEmptyState(emptyStateView)
        }
    }
}