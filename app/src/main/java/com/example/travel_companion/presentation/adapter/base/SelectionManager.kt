package com.example.travel_companion.presentation.adapter.base

/**
 * Gestore per la selezione multipla usando composizione
 * Può essere utilizzato da qualsiasi adapter che ne abbia bisogno
 */
class SelectionManager<T : Any>(
    private val getItemId: (T) -> Any,
    private val onSelectionChanged: (Int) -> Unit = {},
    private val notifyItemChanged: (Int, Any) -> Unit
) {
    private val selectedItems = mutableSetOf<T>()
    var isSelectionMode = false
        private set

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

    fun clearSelection(notifyAllItems: (Any) -> Unit) {
        if (selectedItems.isEmpty()) return

        selectedItems.clear()
        isSelectionMode = false
        onSelectionChanged(0)

        notifyAllItems(BaseAdapter.PAYLOAD_SELECTION_CHANGED)
    }

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

    fun isSelected(item: T): Boolean = selectedItems.contains(item)
    fun getSelectedItems(): List<T> = selectedItems.toList()
    fun getSelectedCount(): Int = selectedItems.size
}