package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.databinding.ItemSuggestionBinding
import com.example.travel_companion.domain.model.TripSuggestion

class SuggestionsAdapter
    : ListAdapter<TripSuggestion, SuggestionsAdapter.SuggestionViewHolder>(
    SuggestionDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SuggestionViewHolder(
        private val binding: ItemSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(suggestion: TripSuggestion) {
            binding.apply {
                suggestionDestination.text = suggestion.destination
                suggestionTitle.text = suggestion.title
                suggestionType.text = suggestion.type
                suggestionReason.text = suggestion.reason

                val iconColor = when (suggestion.type) {
                    "Cultura" -> ContextCompat.getColor(itemView.context, R.color.culture_color)
                    "Natura" -> ContextCompat.getColor(itemView.context, R.color.nature_color)
                    "Mare" -> ContextCompat.getColor(itemView.context, R.color.sea_color)
                    "Romantico" -> ContextCompat.getColor(itemView.context, R.color.romantic_color)
                    "Gastronomia" -> ContextCompat.getColor(itemView.context, R.color.food_color)
                    "Business" -> ContextCompat.getColor(itemView.context, R.color.business_color)
                    "Relax" -> ContextCompat.getColor(itemView.context, R.color.relax_color)
                    else -> ContextCompat.getColor(itemView.context, R.color.primary_color)
                }

                // Badge del tipo di viaggio
                suggestionTypeBadge.setCardBackgroundColor(iconColor)
            }
        }
    }

    private class SuggestionDiffCallback : DiffUtil.ItemCallback<TripSuggestion>() {
        override fun areItemsTheSame(
            oldItem: TripSuggestion,
            newItem: TripSuggestion
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: TripSuggestion,
            newItem: TripSuggestion
        ): Boolean = oldItem == newItem
    }
}