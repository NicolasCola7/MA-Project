package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.ItemTripBinding
import com.example.travel_companion.presentation.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripListAdapter(
    private val onTripClick: (TripEntity) -> Unit
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
            binding.ivTripImage = trip.imageData // assumendo che si mettano i metodi di conversione in un Converter.kt, da provare per essere sicuri
            if (binding.ivTripImage != null) {
               binding.ivTripImage.visibility = View.VISIBLE
                // Nascondi il placeholder se presente
                binding.viewImagePlaceholder?.visibility = View.GONE
            } else {
                //mostra il placeholder
                binding.viewImagePlaceholder?.visibility = View.VISIBLE
            }

             binding.root.setOnClickListener { onTripClick(trip) }
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