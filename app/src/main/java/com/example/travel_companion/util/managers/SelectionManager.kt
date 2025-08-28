package com.example.travel_companion.util.managers

import com.example.travel_companion.presentation.adapter.base.BaseAdapter

/**
 * Manager for multi-selection using composition.
 * Can be used by any adapter that requires selection functionality.
 *
 * @param T The type of elements managed by the selection
 * @property getItemId Function to obtain a unique identifier for an element
 * @property onSelectionChanged Callback invoked whenever the number of selected items changes
 * @property notifyItemChanged Callback to notify that a single item has changed
 */
class SelectionManager<T : Any>(
    private val getItemId: (T) -> Any,
    private val onSelectionChanged: (Int) -> Unit = {},
    private val notifyItemChanged: (Int, Any) -> Unit
) {
    /** Set of currently selected items */
    private val selectedItems = mutableSetOf<T>()

    /** Indicates whether selection mode is active (true if there are selected items) */
    var isSelectionMode = false
        private set

    /**
     * Toggles the selection state of a given item.
     * If the item is already selected, it will be deselected; otherwise, it will be selected.
     * Updates the selection mode state and notifies the adapter.
     *
     * @param item The item to select or deselect
     * @param position The item's position in the adapter, used to notify changes
     */
    fun toggleSelection(item: T, position: Int) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }

        isSelectionMode = selectedItems.isNotEmpty()
        onSelectionChanged(selectedItems.size)

        notifyItemChanged(position, BaseAdapter.PAYLOAD_SELECTION_CHANGED)
    }

    /**
     * Enters selection mode and selects a given item.
     * If selection mode was already active, only the specific item is notified;
     * otherwise, all items are notified to update the UI.
     *
     * @param item The item to select
     * @param position The item's position in the adapter
     * @param notifyAllItems Callback to notify all items in the adapter
     */
    fun enterSelectionMode(item: T, position: Int, notifyAllItems: (Any) -> Unit) {
        val wasInSelectionMode = isSelectionMode
        isSelectionMode = true
        selectedItems.add(item)
        onSelectionChanged(selectedItems.size)

        if (!wasInSelectionMode) {
            notifyAllItems(BaseAdapter.PAYLOAD_SELECTION_MODE_CHANGED)
        } else {
            notifyItemChanged(position, BaseAdapter.PAYLOAD_SELECTION_CHANGED)
        }
    }

    /**
     * Clears all selections and exits selection mode.
     * Notifies all items in the adapter to update the UI.
     *
     * @param notifyAllItems Callback to notify all items in the adapter
     */
    fun clearSelection(notifyAllItems: (Any) -> Unit) {
        if (selectedItems.isEmpty()) return

        selectedItems.clear()
        isSelectionMode = false
        onSelectionChanged(0)

        notifyAllItems(BaseAdapter.PAYLOAD_SELECTION_CHANGED)
    }

    /**
     * Updates the selection after the adapter's list has changed.
     * Removes all selected items that are no longer present in the current list
     * and updates the selection mode state.
     *
     * @param currentList The current list of adapter items
     * @param notifyAllItems Callback to notify all items in the adapter
     */
    fun updateSelectionAfterListChange(
        currentList: List<T>,
        notifyAllItems: (Any) -> Unit
    ) {
        val currentIds = currentList.map { getItemId(it) }.toSet()
        val iterator = selectedItems.iterator()

        while (iterator.hasNext()) {
            val selectedItem = iterator.next()
            if (!currentIds.contains(getItemId(selectedItem))) {
                iterator.remove()
            }
        }

        isSelectionMode = selectedItems.isNotEmpty()
        onSelectionChanged(selectedItems.size)

        notifyAllItems(BaseAdapter.PAYLOAD_SELECTION_CHANGED)
    }

    /**
     * Checks whether a specific item is currently selected.
     *
     * @param item The item to check
     * @return True if the item is selected, false otherwise
     */
    fun isSelected(item: T): Boolean = selectedItems.contains(item)

    /**
     * Returns the list of currently selected items.
     *
     * @return List of selected items
     */
    fun getSelectedItems(): List<T> = selectedItems.toList()

    /**
     * Returns the number of currently selected items.
     *
     * @return Count of selected items
     */
    fun getSelectedCount(): Int = selectedItems.size
}
