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

class TripListAdapter(
    onTripClick: (TripEntity) -> Unit,
    onSelectionChanged: (Int) -> Unit
) : SelectableAdapter<TripEntity, TripListAdapter.TripViewHolder>(
    TripDiffCallback(),
    onTripClick,
    onSelectionChanged
) {

    override fun getItemId(item: TripEntity): Any = item.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun bindItemWithSelection(holder: TripViewHolder, item: TripEntity, isSelected: Boolean, position: Int) {
        holder.bind(item, isSelected)
    }

    override fun updateSelectionVisuals(holder: TripViewHolder, isSelected: Boolean) {
        holder.updateSelectionOnly(isSelected)
    }

    fun getSelectedTrips(): List<TripEntity> = getSelectedItems()

    inner class TripViewHolder(private val binding: ItemTripBinding) :
        RecyclerView.ViewHolder(binding.root) {

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

        fun updateSelectionOnly(isSelected: Boolean) {
            updateSelectionUI(isSelected)
        }

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

class TripDiffCallback : DiffUtil.ItemCallback<TripEntity>() {
    override fun areItemsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean = oldItem == newItem
}