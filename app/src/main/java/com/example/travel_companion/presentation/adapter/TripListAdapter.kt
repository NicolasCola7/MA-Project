package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.presentation.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripListAdapter (
    private var trips: List<TripEntity>,
    private val onTripClick: (TripEntity) -> Unit
) : RecyclerView.Adapter<TripListAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class VH(item: View): RecyclerView.ViewHolder(item) {
        val dest: TextView = item.findViewById(R.id.tvDestination)
        val dates: TextView = item.findViewById(R.id.tvDates)
        val tripImage: ImageView = item.findViewById(R.id.ivTripImage) // Nuovo campo
        val imagePlaceholder: View = item.findViewById(R.id.viewImagePlaceholder) // Placeholder opzionale

        init {
            itemView.setOnClickListener {
                onTripClick(trips[absoluteAdapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val t = trips[pos]
        holder.dest.text = t.destination

        val start = Utils.dateTimeFormat.format(Date(t.startDate))
        val end = Utils.dateTimeFormat.format(Date(t.endDate))
        holder.dates.text = "$start – $end"

        // Gestione dell'immagine
        val bitmap = Utils.byteArrayToBitmap(t.imageData)
        if (bitmap != null) {
            holder.tripImage.setImageBitmap(bitmap)
            holder.tripImage.visibility = View.VISIBLE
            // Nascondi il placeholder se presente
            holder.imagePlaceholder?.visibility = View.GONE
        } else {
            //mostra il placeholder
            holder.imagePlaceholder?.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = trips.size

    fun update(newData: List<TripEntity>) {
        trips = newData
        notifyDataSetChanged()
    }
}