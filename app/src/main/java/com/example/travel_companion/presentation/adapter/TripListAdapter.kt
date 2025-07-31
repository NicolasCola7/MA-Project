package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.presentation.Utils
import dagger.hilt.android.scopes.FragmentScoped

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class TripListAdapter (
    private var trips: List<TripEntity>,
    private val onTripClick: (TripEntity) -> Unit
) : RecyclerView.Adapter<TripListAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class VH(item: View): RecyclerView.ViewHolder(item) {
        val dest: TextView = item.findViewById(R.id.tvDestination)
        val dates: TextView = item.findViewById(R.id.tvDates)

        init {
            itemView.setOnClickListener {
                onTripClick(trips[getAbsoluteAdapterPosition()])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val t = trips[pos]
        holder.dest.text = t.destination
        val start = Utils.dateTimeFormat.format(Date(trips[pos].startDate))
        val end = t.endDate?.let { Utils.dateTimeFormat.format(Date(it)) } ?: "—"
        holder.dates.text = "$start – $end"
    }

    override fun getItemCount() = trips.size

    fun update(newData: List<TripEntity>) {
        trips = newData
        notifyDataSetChanged()
    }
}