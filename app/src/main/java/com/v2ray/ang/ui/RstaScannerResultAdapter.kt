package com.v2ray.ang.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.rstascanner.ScanResult
import com.v2ray.ang.databinding.ItemRstaScannerResultBinding

class RstaScannerResultAdapter(
    private val onCopyIp: (ScanResult) -> Unit,
    private val onCopyConfig: (ScanResult) -> Unit
) : RecyclerView.Adapter<RstaScannerResultAdapter.ViewHolder>() {

    private val items = mutableListOf<ScanResult>()

    fun addResult(result: ScanResult): Int {
        val insertIdx = items.indexOfFirst { it.delay > result.delay }
        return if (insertIdx < 0) {
            items.add(result)
            notifyItemInserted(items.size - 1)
            items.size - 1
        } else {
            items.add(insertIdx, result)
            notifyItemInserted(insertIdx)
            insertIdx
        }
    }

    
    
    fun setUploadSpeed(ip: String, port: String, uploadSpeedMbps: Double) {
        val idx = items.indexOfFirst { it.ip == ip && it.port == port }
        if (idx < 0) return
        items[idx].uploadSpeed = uploadSpeedMbps
    }

    fun setDownloadSpeed(ip: String, port: String, downloadSpeedMbps: Double) {
        val idx = items.indexOfFirst { it.ip == ip && it.port == port }
        if (idx < 0) return
        items[idx].downloadSpeed = downloadSpeedMbps
    }

    
    fun applySpeedAndResort() {
        val hasAny = items.any { it.uploadSpeed >= 0 || it.downloadSpeed >= 0 }
        if (!hasAny) {
            notifyDataSetChanged()
            return
        }
        val sorted = items.sortedWith(Comparator { a, b ->
            val sa = speedScore(a)
            val sb = speedScore(b)
            if (sa != sb) sb.compareTo(sa) else a.delay.compareTo(b.delay)
        })
        items.clear()
        items.addAll(sorted)
        notifyDataSetChanged()
    }

    private fun speedScore(r: ScanResult): Double {
        val up   = if (r.uploadSpeed   >= 0) r.uploadSpeed   else 0.0
        val down = if (r.downloadSpeed >= 0) r.downloadSpeed else 0.0
        val hasUp   = r.uploadSpeed   >= 0
        val hasDown = r.downloadSpeed >= 0
        return when {
            hasUp && hasDown -> up * 0.7 + down * 0.3
            hasUp            -> up * 0.7
            hasDown          -> down * 0.3
            else             -> -1.0
        }
    }

    fun clear() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getAllIps(): String     = items.joinToString("\n") { "${it.ip}:${it.port}" }
    fun getAllConfigs(): String = items.joinToString("\n") { it.config }
    fun getCount(): Int        = items.size
    fun getTop20(): List<ScanResult> = items.take(20)

    
    
    fun getSpeedTestCandidates(budget: Int = 20): List<ScanResult> {
        val ranges = items.map { it.sourceCidr }.distinct()
        if (ranges.size <= 1) {
            return items.sortedBy { it.delay }.take(budget)
        }
        val perRange = kotlin.math.ceil(budget.toDouble() / ranges.size).toInt().coerceAtLeast(1)
        val result = mutableListOf<ScanResult>()
        for (range in ranges) {
            result.addAll(items.filter { it.sourceCidr == range }.sortedBy { it.delay }.take(perRange))
        }
        return result
    }

    fun getTop10ByPing(): String {
        val top10 = items.sortedBy { it.delay }.take(10)
        return top10.joinToString("\n") { it.config }
    }

    fun getTop10ResultsByPing(): List<ScanResult> = items.sortedBy { it.delay }.take(10)

    fun getSpeedResults(): String {
        val tested = items.filter { it.uploadSpeed >= 0 || it.downloadSpeed >= 0 }
        if (tested.isEmpty()) return ""
        return tested.joinToString("\n") { it.config }
    }

    fun getSpeedTestedIps(): String {
        val tested = items.filter { it.uploadSpeed >= 0 || it.downloadSpeed >= 0 }
        if (tested.isEmpty()) return ""
        return tested.joinToString("\n") { "${it.ip}:${it.port}" }
    }

    private fun formatKbps(mbps: Double): String = "${(mbps * 1024).toInt()} KB/s"

    inner class ViewHolder(val binding: ItemRstaScannerResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemRstaScannerResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = holder.binding
        val r    = items[position]
        val context = item.root.context

        item.tvIp.text    = "${r.ip}:${r.port}"
        item.tvDelay.text = "${r.delay} ms"
        item.tvDelay.setBackgroundColor(when {
            r.delay < 500  -> ContextCompat.getColor(context, R.color.rsta_scanner_delay_good)
            r.delay < 1500 -> ContextCompat.getColor(context, R.color.rsta_scanner_delay_medium)
            else           -> ContextCompat.getColor(context, R.color.rsta_scanner_delay_bad)
        })

        val hasUp   = r.uploadSpeed   >= 0
        val hasDown = r.downloadSpeed >= 0

        if (hasUp || hasDown) {
            val downPart = if (hasDown) "↓ ${formatKbps(r.downloadSpeed)}" else "↓ ---"
            val upPart   = if (hasUp)   "↑ ${formatKbps(r.uploadSpeed)}"   else "↑ ---"
            item.tvSpeed.text = "$downPart  $upPart"
            item.tvSpeed.setBackgroundColor(ContextCompat.getColor(context, R.color.rsta_scanner_speed_badge))
            item.tvSpeed.visibility = View.VISIBLE
        } else {
            item.tvSpeed.visibility = View.GONE
        }

        item.btnCopyIp.setOnClickListener     { onCopyIp(r) }
        item.btnCopyConfig.setOnClickListener { onCopyConfig(r) }
    }

    override fun getItemCount() = items.size
}