package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.databinding.ItemSuggestionBinding
import com.example.travel_companion.domain.model.TripSuggestion
import com.example.travel_companion.presentation.adapter.base.BaseAdapter

/**
 * Adapter for displaying a list of [TripSuggestion] items in a RecyclerView.
 */
class SuggestionsAdapter : BaseAdapter<TripSuggestion, SuggestionsAdapter.SuggestionViewHolder>(
    SuggestionDiffCallback()
) {

    /**
     * Inflates the item view and creates a [SuggestionViewHolder].
     *
     * @param parent The parent [ViewGroup] in which the new view will be added.
     * @param viewType The view type of the new view.
     * @return A new instance of [SuggestionViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SuggestionViewHolder(binding)
    }

    /**
     * Handles item click events.
     *
     * @param item The clicked [TripSuggestion] item.
     * @param position The position of the clicked item in the adapter.
     */
    override fun onItemClick(item: TripSuggestion, position: Int) {}

    /**
     * Binds a [TripSuggestion] to the given [SuggestionViewHolder].
     *
     * @param holder The [SuggestionViewHolder] to bind data to.
     * @param item The [TripSuggestion] to display.
     * @param position The position of the item in the adapter.
     */
    override fun bindItem(holder: SuggestionViewHolder, item: TripSuggestion, position: Int) {
        holder.bind(item)
    }

    /**
     * ViewHolder for displaying a single [TripSuggestion] item.
     *
     * @property binding The [ItemSuggestionBinding] used to access view elements.
     * @param binding The view binding for the item layout.
     */
    inner class SuggestionViewHolder(
        private val binding: ItemSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the provided [TripSuggestion] data to the view elements.
         *
         * @param suggestion The [TripSuggestion] instance to display.
         */
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

                suggestionTypeBadge.setCardBackgroundColor(iconColor)
            }
        }
    }

    /**
     * DiffUtil callback for efficiently updating the list of [TripSuggestion] items.
     */
    private class SuggestionDiffCallback : DiffUtil.ItemCallback<TripSuggestion>() {

        /**
         * Checks whether two items represent the same [TripSuggestion].
         *
         * @param oldItem The old item in the list.
         * @param newItem The new item in the list.
         * @return True if the items have the same ID.
         */
        override fun areItemsTheSame(oldItem: TripSuggestion, newItem: TripSuggestion): Boolean =
            oldItem.id == newItem.id

        /**
         * Checks whether the contents of two [TripSuggestion] items are equal.
         *
         * @param oldItem The old item in the list.
         * @param newItem The new item in the list.
         * @return True if the contents are identical.
         */
        override fun areContentsTheSame(oldItem: TripSuggestion, newItem: TripSuggestion): Boolean =
            oldItem == newItem
    }
}
