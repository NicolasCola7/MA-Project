package com.example.travel_companion.presentation.adapter.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.util.managers.SelectionManager

/**
 * Adapter base per adapter che supportano la selezione
 * Combina BaseAdapter con SelectionManager
 */
abstract class SelectableAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val onItemClickCallback: (T) -> Unit = {},
    onSelectionChanged: (Int) -> Unit = {}
) : BaseAdapter<T, VH>(diffCallback) {

    protected val selectionManager = SelectionManager<T>(
        getItemId = { getItemId(it) },
        onSelectionChanged = onSelectionChanged,
        notifyItemChanged = { position, payload -> notifyItemChanged(position, payload) }
    )

    /**
     * Deve restituire l'ID univoco dell'item
     */
    protected abstract fun getItemId(item: T): Any

    /**
     * Bind dell'item considerando lo stato di selezione
     */
    protected abstract fun bindItemWithSelection(holder: VH, item: T, isSelected: Boolean, position: Int)

    /**
     * Aggiorna solo la visualizzazione della selezione
     */
    protected abstract fun updateSelectionVisuals(holder: VH, isSelected: Boolean)

    override fun bindItem(holder: VH, item: T, position: Int) {
        val isSelected = selectionManager.isSelected(item)
        bindItemWithSelection(holder, item, isSelected, position)
    }

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

    override fun onItemClick(item: T, position: Int) {
        if (selectionManager.isSelectionMode) {
            selectionManager.toggleSelection(item, position)
        } else {
            onItemClickCallback(item)
        }
    }

    override fun onItemLongClick(item: T, position: Int): Boolean {
        selectionManager.toggleSelection(item, position)
        return true
    }

    // Metodi pubblici per gestire la selezione
    fun getSelectedItems(): List<T> = selectionManager.getSelectedItems()
    fun clearSelection() {
        selectionManager.clearSelection { payload ->
            for (i in 0 until itemCount) {
                notifyItemChanged(i, payload)
            }
        }
    }
    fun updateSelectionAfterListChange() {
        selectionManager.updateSelectionAfterListChange(currentList) { payload ->
            for (i in 0 until itemCount) {
                notifyItemChanged(i, payload)
            }
        }
    }
    val isSelectionMode: Boolean get() = selectionManager.isSelectionMode
}