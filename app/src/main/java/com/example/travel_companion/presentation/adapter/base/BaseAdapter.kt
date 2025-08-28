package com.example.travel_companion.presentation.adapter.base

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffCallback) {

    companion object {
        const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
        const val PAYLOAD_SELECTION_MODE_CHANGED = "selection_mode_changed"
    }

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        bindItem(holder, item, position)
        setupClickListeners(holder, item, position)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            handlePayloadUpdate(holder, position, payloads)
        }
    }

    /**
     * Bind the item to the ViewHolder
     */
    protected abstract fun bindItem(holder: VH, item: T, position: Int)

    /**
     * Setup click listeners (can be overridden for custom behavior)
     */
    protected open fun setupClickListeners(holder: VH, item: T, position: Int) {
        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(item, position)
        }
    }

    /**
     * Handle payload updates (default behavior: rebind normally)
     */
    protected open fun handlePayloadUpdate(holder: VH, position: Int, payloads: MutableList<Any>) {
        bindItem(holder, getItem(position), position)
    }

    /**
     * Called when an item is clicked
     */
    protected abstract fun onItemClick(item: T, position: Int)

    /**
     * Called when an item is long-clicked (default: do nothing and return false)
     */
    protected open fun onItemLongClick(item: T, position: Int): Boolean = false
}
