// File: presentation/adapter/POISuggestionAdapter.kt - VERSIONE CORRETTA
package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.databinding.ItemPoiSuggestionBinding
import com.example.travel_companion.domain.model.POISuggestion

class POISuggestionAdapter(
    private val onItemClick: (POISuggestion) -> Unit
) : ListAdapter<POISuggestion, POISuggestionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPoiSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPoiSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(suggestion: POISuggestion) {
            with(binding) {
                // ID corretti dal layout
                textPoiName.text = suggestion.name
                textPoiCategory.text = suggestion.category
                textDistance.text = String.format("%.1f km", suggestion.estimatedDistance)

                progressConfidence.progress = (suggestion.confidence * 100).toInt()
                textConfidence.text = "${(suggestion.confidence * 100).toInt()}%"

                textReasoning.text = suggestion.reasoning

                root.setOnClickListener { onItemClick(suggestion) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<POISuggestion>() {
        override fun areItemsTheSame(oldItem: POISuggestion, newItem: POISuggestion): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.latitude == newItem.latitude &&
                    oldItem.longitude == newItem.longitude
        }

        override fun areContentsTheSame(oldItem: POISuggestion, newItem: POISuggestion): Boolean {
            return oldItem == newItem
        }
    }
}