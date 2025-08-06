package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.ItemTripBinding
import com.example.travel_companion.presentation.Utils
import java.util.Date

class TripListAdapter(
    private val onTripClick: (TripEntity) -> Unit,
    private val onTripLongClick: (TripEntity, View) -> Unit
) : ListAdapter<TripEntity, TripListAdapter.TripViewHolder>(TripDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position), onTripClick)
    }

    inner class TripViewHolder(private val binding: ItemTripBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            trip: TripEntity,
            onTripClick: (TripEntity) -> Unit,
        ) {
            binding.tvDestination.text = trip.destination
            val startDate = Utils.dateTimeFormat.format(Date(trip.startDate))
            val endDate = trip.endDate?.let {
                Utils.dateTimeFormat.format(Date(it))
            } ?: "—"
            binding.tvDates.text = "$startDate – $endDate"

            // Gestione dell'immagine
            binding.ivTripImage.apply {
                Glide.with(
                    this
                ).load(trip.imageData).into(binding.ivTripImage)
            } 

            if (binding.ivTripImage != null) {
                binding.ivTripImage.visibility = View.VISIBLE
                // Nascondi il placeholder se presente
                binding.viewImagePlaceholder?.visibility = View.GONE

            } else {
                //mostra il placeholder
                binding.viewImagePlaceholder?.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener { onTripClick(trip) }

            binding.root.setOnLongClickListener { view ->
                onTripLongClick(trip, view)
                true
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
