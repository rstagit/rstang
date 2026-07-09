package com.v2ray.ang.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.databinding.ItemRstaScannerIpRangeBinding
import com.v2ray.ang.databinding.ItemRstaScannerIpRangeHeaderBinding

sealed class RangeRow {
    data class Header(val title: String) : RangeRow()
    data class Item(val cidr: String) : RangeRow()
}

class RstaScannerIpRangeAdapter(
    private val onCheckedChange: (String, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var fullRows: List<RangeRow> = emptyList()
    private var displayRows: List<RangeRow> = emptyList()
    private val checkedSet = mutableSetOf<String>()

    fun submitRows(rows: List<RangeRow>, initiallyChecked: Set<String>) {
        fullRows = rows
        displayRows = rows
        checkedSet.clear()
        checkedSet.addAll(initiallyChecked)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        displayRows = if (query.isBlank()) {
            fullRows
        } else {
            fullRows.filter { row -> row is RangeRow.Item && row.cidr.contains(query.trim(), ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    fun getCheckedRanges(): List<String> {
        
        return fullRows.filterIsInstance<RangeRow.Item>()
            .map { it.cidr }
            .filter { it in checkedSet }
    }

    fun getCheckedCount(): Int = checkedSet.size

    
    fun selectAllVisible() {
        displayRows.filterIsInstance<RangeRow.Item>().forEach { checkedSet.add(it.cidr) }
        notifyDataSetChanged()
    }

    fun deselectAllVisible() {
        displayRows.filterIsInstance<RangeRow.Item>().forEach { checkedSet.remove(it.cidr) }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayRows[position]) {
            is RangeRow.Header -> TYPE_HEADER
            is RangeRow.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemRstaScannerIpRangeHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemRstaScannerIpRangeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = displayRows[position]) {
            is RangeRow.Header -> {
                (holder as HeaderViewHolder).binding.tvHeader.text = row.title
            }
            is RangeRow.Item -> {
                val h = holder as ItemViewHolder
                h.binding.tvRange.text = row.cidr
                h.binding.cbRange.isChecked = row.cidr in checkedSet
                h.binding.root.setOnClickListener {
                    val newChecked = !h.binding.cbRange.isChecked
                    h.binding.cbRange.isChecked = newChecked
                    if (newChecked) checkedSet.add(row.cidr) else checkedSet.remove(row.cidr)
                    onCheckedChange(row.cidr, newChecked)
                }
            }
        }
    }

    override fun getItemCount() = displayRows.size

    class HeaderViewHolder(val binding: ItemRstaScannerIpRangeHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class ItemViewHolder(val binding: ItemRstaScannerIpRangeBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}
