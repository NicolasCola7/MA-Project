package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.ItemTripBinding
import com.example.travel_companion.presentation.Utils
import java.util.Date

class TripListAdapter(
    private val onTripClick: (TripEntity) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : ListAdapter<TripEntity, TripListAdapter.TripViewHolder>(TripDiffCallback()) {

    private val selectedTrips = mutableSetOf<TripEntity>()
    var selectionMode = false

    fun toggleSelection(trip: TripEntity) {
        if (selectedTrips.contains(trip)) {
            selectedTrips.remove(trip)
        } else {
            selectedTrips.add(trip)
        }
        selectionMode = selectedTrips.isNotEmpty()
        onSelectionChanged(selectedTrips.size)
        notifyDataSetChanged()
    }

    fun getSelectedTrips(): List<TripEntity> = selectedTrips.toList()

    fun clearSelection() {
        selectedTrips.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = getItem(position)
        val isSelected = selectedTrips.contains(trip)

        holder.bind(trip, isSelected)

        holder.itemView.setOnClickListener {
            if (selectionMode) {
                toggleSelection(trip)
            } else {
                onTripClick(trip)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(trip)
            true
        }
    }

    inner class TripViewHolder(private val binding: ItemTripBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            trip: TripEntity,
            isSelected: Boolean
        ) {
            // Dati testuali
            binding.tvDestination.text = trip.destination

            val startDate = Utils.dateTimeFormat.format(Date(trip.startDate))
            val endDate = trip.endDate?.let {
                Utils.dateTimeFormat.format(Date(it))
            } ?: "—"
            binding.tvDates.text = "$startDate – $endDate"

            // Immagine o placeholder
            if (trip.imageData != null) {
                binding.ivTripImage.visibility = View.VISIBLE
                binding.viewImagePlaceholder.visibility = View.GONE
                Glide.with(binding.ivTripImage)
                    .load(trip.imageData)
                    .into(binding.ivTripImage)
            } else {
                binding.ivTripImage.visibility = View.GONE
                binding.viewImagePlaceholder.visibility = View.VISIBLE
            }

            // Sfondo se selezionato
            if (isSelected) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.selectedItemBackground)
                )
            }
        }
    }
}

class TripDiffCallback : DiffUtil.ItemCallback<TripEntity>() {
    override fun areItemsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
        return oldItem == newItem
    }
}
