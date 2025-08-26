package com.example.travel_companion.util.helpers

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.travel_companion.R

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
        val buttonView = emptyStateView.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.emptyStateButton)

        iconView?.setImageResource(R.drawable.ic_detail_trip_24)
        buttonView?.setOnClickListener { onButtonClick() }

        showEmptyState(emptyStateView)
    }
}