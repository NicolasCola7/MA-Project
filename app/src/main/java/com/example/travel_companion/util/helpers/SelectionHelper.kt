package com.example.travel_companion.util.helpers

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import java.util.concurrent.TimeUnit

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