package com.example.travel_companion.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SuggestionAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    private var suggestions = listOf<String>()

    fun submitList(newSuggestions: List<String>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount() = suggestions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(suggestion: String) {
            (itemView as android.widget.TextView).apply {
                text = suggestion
                setOnClickListener { onItemClick(suggestion) }
                setPadding(32, 16, 32, 16)
            }
        }
    }
}