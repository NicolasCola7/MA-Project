package com.example.travel_companion.presentation.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.ItemTripBinding
import com.example.travel_companion.presentation.adapter.base.SelectableAdapter
import com.example.travel_companion.util.Utils
import java.util.Date

/**
 * Adapter for displaying a selectable list of [TripEntity] items.
 *
 * @param onTripClick Callback invoked when a trip item is clicked.
 * @param onSelectionChanged Callback invoked when the selection count changes.
 */
class TripListAdapter(
    onTripClick: (TripEntity) -> Unit,
    onSelectionChanged: (Int) -> Unit
) : SelectableAdapter<TripEntity, TripListAdapter.TripViewHolder>(
    TripDiffCallback(),
    onTripClick,
    onSelectionChanged
) {

    /**
     * Returns a stable ID for the given [TripEntity].
     *
     * @param item The [TripEntity] to get the ID for.
     * @return The unique ID of the item.
     */
    override fun getItemId(item: TripEntity): Any = item.id

    /**
     * Inflates the item layout and creates a [TripViewHolder].
     *
     * @param parent The parent [ViewGroup] in which the new view will be added.
     * @param viewType The view type of the new view.
     * @return A new instance of [TripViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    /**
     * Binds a [TripEntity] to the [TripViewHolder] including selection state.
     *
     * @param holder The [TripViewHolder] to bind data to.
     * @param item The [TripEntity] to display.
     * @param isSelected True if the item is selected.
     * @param position The position of the item in the adapter.
     */
    override fun bindItemWithSelection(holder: TripViewHolder, item: TripEntity, isSelected: Boolean, position: Int) {
        holder.bind(item, isSelected)
    }

    /**
     * Updates the selection visuals for the given [TripViewHolder].
     *
     * @param holder The [TripViewHolder] to update.
     * @param isSelected True if the item is selected.
     */
    override fun updateSelectionVisuals(holder: TripViewHolder, isSelected: Boolean) {
        holder.updateSelectionOnly(isSelected)
    }

    /**
     * Returns the currently selected trips.
     *
     * @return List of selected [TripEntity] items.
     */
    fun getSelectedTrips(): List<TripEntity> = getSelectedItems()

    /**
     * ViewHolder for displaying a single [TripEntity] item.
     *
     * @property binding The [ItemTripBinding] used to access view elements.
     * @param binding The view binding for the item layout.
     */
    inner class TripViewHolder(private val binding: ItemTripBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the [TripEntity] data to the item view and applies selection state.
         *
         * @param trip The [TripEntity] to display.
         * @param isSelected True if the item is selected.
         */
        fun bind(trip: TripEntity, isSelected: Boolean) {
            binding.tvDestination.text = trip.destination

            val startDate = Utils.dateTimeFormat.format(Date(trip.startDate))
            val endDate = trip.endDate?.let {
                Utils.dateTimeFormat.format(Date(it))
            } ?: "—"
            binding.tvDates.text = "$startDate – $endDate"

            setupImageAndOverlay(trip)
            updateSelectionUI(isSelected)
        }

        /**
         * Updates only the selection visuals without rebinding data.
         *
         * @param isSelected True if the item is selected.
         */
        fun updateSelectionOnly(isSelected: Boolean) {
            updateSelectionUI(isSelected)
        }

        /**
         * Sets up the trip image and overlay based on whether image data exists.
         *
         * @param trip The [TripEntity] containing image data.
         */
        private fun setupImageAndOverlay(trip: TripEntity) {
            if (trip.imageData != null) {
                val bitmap = BitmapFactory.decodeByteArray(trip.imageData, 0, trip.imageData.size)
                binding.ivTripImage.setImageBitmap(bitmap)
                binding.ivTripImage.visibility = View.VISIBLE
                binding.viewImagePlaceholder.visibility = View.GONE

                binding.root.findViewById<View>(R.id.overlay_view)?.visibility = View.VISIBLE

                binding.tvDestination.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
                binding.tvDates.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
            } else {
                binding.ivTripImage.setImageDrawable(null)
                binding.ivTripImage.visibility = View.GONE
                binding.viewImagePlaceholder.visibility = View.VISIBLE

                binding.root.findViewById<View>(R.id.overlay_view)?.visibility = View.VISIBLE

                binding.tvDestination.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_primary)
                )
                binding.tvDates.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                )
            }
        }

        /**
         * Updates the selection overlay and placeholder visibility based on selection state.
         *
         * @param isSelected True if the item is selected.
         */
        private fun updateSelectionUI(isSelected: Boolean) {
            val selectionOverlay = binding.root.findViewById<View>(R.id.selection_overlay)

            if (isSelected) {
                selectionOverlay?.visibility = View.VISIBLE
                if(binding.ivTripImage.visibility == View.GONE)
                    binding.viewImagePlaceholder.visibility = View.GONE
            } else {
                selectionOverlay?.visibility = View.GONE
                if(binding.ivTripImage.visibility == View.GONE)
                    binding.viewImagePlaceholder.visibility = View.VISIBLE
            }
        }
    }
}

/**
 * DiffUtil callback for efficiently updating a list of [TripEntity] items.
 */
class TripDiffCallback : DiffUtil.ItemCallback<TripEntity>() {

    /**
     * Checks whether two [TripEntity] items represent the same trip.
     *
     * @param oldItem The old item.
     * @param newItem The new item.
     * @return True if the items have the same ID.
     */
    override fun areItemsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean = oldItem.id == newItem.id

    /**
     * Checks whether the contents of two [TripEntity] items are the same.
     *
     * @param oldItem The old item.
     * @param newItem The new item.
     * @return True if all contents are identical.
     */
    override fun areContentsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean = oldItem == newItem
}
