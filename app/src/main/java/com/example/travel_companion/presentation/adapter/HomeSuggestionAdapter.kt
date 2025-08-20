package com.example.travel_companion.presentation.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.databinding.ItemHomeSuggestionBinding
import com.example.travel_companion.domain.model.SuggestionPriority
import com.example.travel_companion.domain.model.TravelSuggestion

class HomeSuggestionsAdapter(
    private val onSuggestionClick: (TravelSuggestion) -> Unit
) : ListAdapter<TravelSuggestion, HomeSuggestionsAdapter.HomeSuggestionViewHolder>(SuggestionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeSuggestionViewHolder {
        val binding = ItemHomeSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeSuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeSuggestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HomeSuggestionViewHolder(
        private val binding: ItemHomeSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(suggestion: TravelSuggestion) {
            binding.apply {
                suggestionDestination.text = suggestion.destination
                suggestionTitle.text = suggestion.title
                suggestionDistance.text = "${suggestion.estimatedDistance.toInt()} km"
                suggestionType.text = suggestion.type
                suggestionReason.text = suggestion.reason

                // Colore dell'icona basato sul tipo - CORRETTO
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

                // Indicatore di priorità - CORRETTO: usa ColorStateList.valueOf()
                val priorityColor = when (suggestion.priority) {
                    SuggestionPriority.HIGH -> ContextCompat.getColor(itemView.context, R.color.error_color)
                    SuggestionPriority.MEDIUM -> ContextCompat.getColor(itemView.context, R.color.warning_color)
                    SuggestionPriority.LOW -> ContextCompat.getColor(itemView.context, R.color.success_color)
                }
                // QUESTA ERA LA RIGA CHE CAUSAVA L'ERRORE - ora corretta:
                priorityDot.backgroundTintList = ColorStateList.valueOf(priorityColor)

                // Click listener
                suggestionCard.setOnClickListener { onSuggestionClick(suggestion) }
            }
        }
    }

    private class SuggestionDiffCallback : DiffUtil.ItemCallback<TravelSuggestion>() {
        override fun areItemsTheSame(
            oldItem: TravelSuggestion,
            newItem: TravelSuggestion
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: TravelSuggestion,
            newItem: TravelSuggestion
        ): Boolean = oldItem == newItem
    }
}