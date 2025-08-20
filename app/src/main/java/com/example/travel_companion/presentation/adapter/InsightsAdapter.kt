package com.example.travel_companion.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.R
import com.example.travel_companion.databinding.ItemInsightBinding
import com.example.travel_companion.domain.model.InsightType
import com.example.travel_companion.domain.model.PredictionInsight

class InsightsAdapter(
    private val onInsightClick: (PredictionInsight) -> Unit
) : ListAdapter<PredictionInsight, InsightsAdapter.InsightViewHolder>(InsightDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val binding = ItemInsightBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InsightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InsightViewHolder(
        private val binding: ItemInsightBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(insight: PredictionInsight) {
            binding.apply {
                insightMessage.text = insight.message

                // Configura il pulsante di azione
                if (insight.actionText != null) {
                    insightAction.text = insight.actionText
                    insightAction.visibility = android.view.View.VISIBLE
                    insightAction.setOnClickListener { onInsightClick(insight) }
                } else {
                    insightAction.visibility = android.view.View.GONE
                }

                // Stile basato sul tipo
                val (backgroundColor, textColor) = when (insight.type) {
                    InsightType.ACHIEVEMENT -> Pair(
                        ContextCompat.getColor(itemView.context, R.color.success_light),
                        ContextCompat.getColor(itemView.context, R.color.success_dark)
                    )
                    InsightType.WARNING -> Pair(
                        ContextCompat.getColor(itemView.context, R.color.warning_light),
                        ContextCompat.getColor(itemView.context, R.color.warning_dark)
                    )
                    InsightType.SUGGESTION -> Pair(
                        ContextCompat.getColor(itemView.context, R.color.info_light),
                        ContextCompat.getColor(itemView.context, R.color.info_dark)
                    )
                    InsightType.INFO -> Pair(
                        ContextCompat.getColor(itemView.context, R.color.primary_light),
                        ContextCompat.getColor(itemView.context, R.color.primary_dark)
                    )
                }

                insightCard.setCardBackgroundColor(backgroundColor)
                insightMessage.setTextColor(textColor)
            }
        }
    }

    private class InsightDiffCallback : DiffUtil.ItemCallback<PredictionInsight>() {
        override fun areItemsTheSame(
            oldItem: PredictionInsight,
            newItem: PredictionInsight
        ): Boolean = oldItem.message == newItem.message

        override fun areContentsTheSame(
            oldItem: PredictionInsight,
            newItem: PredictionInsight
        ): Boolean = oldItem == newItem
    }
}