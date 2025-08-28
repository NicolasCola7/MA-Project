package com.example.travel_companion.presentation.adapter.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.util.managers.SelectionManager

/**
 * Base adapter for adapters that support selection.
 * Combines BaseAdapter with SelectionManager.
 */
abstract class SelectableAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val onItemClickCallback: (T) -> Unit = {},
    onSelectionChanged: (Int) -> Unit = {}
) : BaseAdapter<T, VH>(diffCallback) {

    /**
     * Manages item selection and selection mode.
     */
    protected val selectionManager = SelectionManager<T>(
        getItemId = { getItemId(it) },
        onSelectionChanged = onSelectionChanged,
        notifyItemChanged = { position, payload -> notifyItemChanged(position, payload) }
    )

    /**
     * Returns a unique identifier for the given item.
     */
    protected abstract fun getItemId(item: T): Any

    /**
     * Bind the item to the ViewHolder, including its selection state.
     *
     * @param holder The ViewHolder to bind the item to.
     * @param item The item at the given position.
     * @param isSelected Whether the item is currently selected.
     * @param position Position of the item in the adapter.
     */
    abstract fun bindItemWithSelection(holder: VH, item: T, isSelected: Boolean, position: Int)

    /**
     * Update only the selection visuals of the ViewHolder.
     *
     * @param holder The ViewHolder whose visuals should be updated.
     * @param isSelected Whether the item is selected.
     */
    protected abstract fun updateSelectionVisuals(holder: VH, isSelected: Boolean)

    /**
     * Bind an item in the adapter to a ViewHolder.
     * This automatically considers selection state.
     */
    override fun bindItem(holder: VH, item: T, position: Int) {
        val isSelected = selectionManager.isSelected(item)
        bindItemWithSelection(holder, item, isSelected, position)
    }

    /**
     * Handle partial updates using payloads.
     * Updates only selection-related visuals if needed.
     */
    override fun handlePayloadUpdate(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION_CHANGED) ||
            payloads.contains(PAYLOAD_SELECTION_MODE_CHANGED)) {
            val item = getItem(position)
            val isSelected = selectionManager.isSelected(item)
            updateSelectionVisuals(holder, isSelected)
        } else {
            super.handlePayloadUpdate(holder, position, payloads)
        }
    }

    /**
     * Handles item clicks.
     * - If selection mode is active, toggles selection.
     * - Otherwise, invokes the item click callback.
     */
    override fun onItemClick(item: T, position: Int) {
        if (selectionManager.isSelectionMode) {
            selectionManager.toggleSelection(item, position)
        } else {
            onItemClickCallback(item)
        }
    }

    /**
     * Handles long clicks to enter or modify selection mode.
     *
     * @return true to indicate the event was handled.
     */
    override fun onItemLongClick(item: T, position: Int): Boolean {
        selectionManager.toggleSelection(item, position)
        return true
    }

    /**
     * Returns a list of all currently selected items.
     */
    fun getSelectedItems(): List<T> = selectionManager.getSelectedItems()

    /**
     * Clears the selection and updates the adapter to reflect it.
     */
    fun clearSelection() {
        selectionManager.clearSelection { payload ->
            for (i in 0 until itemCount) {
                notifyItemChanged(i, payload)
            }
        }
    }

    /**
     * Updates the selection state after the adapter's list changes.
     * Ensures previously selected items remain selected if still present.
     */
    fun updateSelectionAfterListChange() {
        selectionManager.updateSelectionAfterListChange(currentList) { payload ->
            for (i in 0 until itemCount) {
                notifyItemChanged(i, payload)
            }
        }
    }

    /**
     * Returns true if the adapter is currently in selection mode.
     */
    val isSelectionMode: Boolean get() = selectionManager.isSelectionMode
}
