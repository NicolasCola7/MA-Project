package com.example.travel_companion.util.helpers

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.travel_companion.R

/**
 * Helper object for managing empty states consistently across the application.
 * Provides utility methods to show or hide empty state views with specific icons and messages.
 */
object EmptyStateHelper {

    /**
     * Configures the empty state view with the provided icon and text.
     *
     * @param emptyStateView The ConstraintLayout representing the empty state
     * @param iconRes Resource ID of the icon to display
     * @param text The text message to display
     */
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

    /**
     * Makes the empty state view visible.
     *
     * @param emptyStateView The ConstraintLayout representing the empty state
     */
    private fun showEmptyState(emptyStateView: ConstraintLayout) {
        emptyStateView.visibility = View.VISIBLE
    }

    /**
     * Hides the empty state view.
     *
     * @param emptyStateView The ConstraintLayout representing the empty state
     */
    fun hideEmptyState(emptyStateView: ConstraintLayout) {
        emptyStateView.visibility = View.GONE
    }

    /**
     * Shows the empty state for trips.
     * Displays different messages depending on whether filters are applied.
     *
     * @param emptyStateView The ConstraintLayout representing the empty state
     * @param hasFilters True if filters are applied, false otherwise
     */
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

    /**
     * Shows the empty state for notes.
     *
     * @param emptyStateView The ConstraintLayout representing the empty state
     */
    fun showNotesEmptyState(emptyStateView: ConstraintLayout) {
        setupEmptyState(
            emptyStateView,
            R.drawable.ic_edit_note_24,
            "Non hai ancora scritto nessuna nota per questo viaggio"
        )
        showEmptyState(emptyStateView)
    }

    /**
     * Shows the empty state for photos.
     *
     * @param emptyStateView The ConstraintLayout representing the empty state
     */
    fun showPhotosEmptyState(emptyStateView: ConstraintLayout) {
        setupEmptyState(
            emptyStateView,
            R.drawable.ic_menu_gallery,
            "Non hai ancora scattato nessuna foto per questo viaggio"
        )
        showEmptyState(emptyStateView)
    }

    /**
     * Shows a home-specific empty state with a clickable button.
     *
     * @param emptyStateView The ConstraintLayout representing the empty state
     * @param onButtonClick Lambda executed when the empty state button is clicked
     */
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
