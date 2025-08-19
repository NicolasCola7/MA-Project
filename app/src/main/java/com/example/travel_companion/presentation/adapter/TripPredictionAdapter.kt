package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.databinding.ItemTripPredictionBinding
import com.example.travel_companion.domain.model.TripPrediction
import java.text.SimpleDateFormat
import java.util.*

class TripPredictionAdapter(
    private val onItemClick: (TripPrediction) -> Unit
) : ListAdapter<TripPrediction, TripPredictionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTripPredictionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTripPredictionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(prediction: TripPrediction) {
            with(binding) {
                textDestination.text = prediction.suggestedDestination
                textTripType.text = prediction.predictedType

                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                textStartDate.text = dateFormat.format(Date(prediction.suggestedStartDate))
                textEndDate.text = dateFormat.format(Date(prediction.suggestedEndDate))

                progressConfidence.progress = (prediction.confidence * 100).toInt()
                textConfidence.text = "${(prediction.confidence * 100).toInt()}%"

                textReasoning.text = prediction.reasoning

                root.setOnClickListener { onItemClick(prediction) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TripPrediction>() {
        override fun areItemsTheSame(oldItem: TripPrediction, newItem: TripPrediction): Boolean {
            return oldItem.suggestedDestination == newItem.suggestedDestination &&
                    oldItem.suggestedStartDate == newItem.suggestedStartDate
        }

        override fun areContentsTheSame(oldItem: TripPrediction, newItem: TripPrediction): Boolean {
            return oldItem == newItem
        }
    }
}