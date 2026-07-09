package com.v2ray.ang.ui

import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.v2ray.ang.R
import com.v2ray.ang.rstascanner.util.CidrUtil
import com.v2ray.ang.databinding.ActivityRstaScannerIpRangeBinding
import androidx.recyclerview.widget.LinearLayoutManager

class RstaScannerIpRangeActivity : BaseActivity() {

    private lateinit var binding: ActivityRstaScannerIpRangeBinding
    private lateinit var adapter: RstaScannerIpRangeAdapter

    companion object {
        const val EXTRA_INITIAL_SELECTION = "initial_selection"
        const val EXTRA_RESULT_SELECTION = "result_selection"

        
        private val CURATED_RANGES = listOf(
            "104.16.174.0/24",
            "104.16.75.0/24",
            "104.18.152.0/24",
            "45.131.4.0/24",
            "103.21.244.0/22",
            "103.22.200.0/22",
            "103.31.4.0/22",
            "104.16.0.0/13",
            "104.16.0.0/16",
            "104.24.0.0/14",
            "108.162.192.0/18",
            "131.0.72.0/22",
            "141.101.64.0/18",
            "162.158.0.0/15",
            "172.64.0.0/13",
            "173.245.48.0/20",
            "188.114.96.0/20",
            "190.93.240.0/20",
            "197.234.240.0/22",
            "198.41.128.0/17"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRstaScannerIpRangeBinding.inflate(layoutInflater)
        setContentViewWithToolbar(binding.root, title = getString(R.string.title_rsta_scanner_ip_range))

        val initialSelection = intent.getStringArrayListExtra(EXTRA_INITIAL_SELECTION)?.toSet() ?: emptySet()

        adapter = RstaScannerIpRangeAdapter { _, _ ->
            updateSelectedCount()
        }
        binding.rvRanges.layoutManager = LinearLayoutManager(this)
        binding.rvRanges.adapter = adapter

        loadRanges(initialSelection)
        updateSelectedCount()

        binding.etSearch.addTextChangedListener {
            adapter.filter(it?.toString().orEmpty())
        }

        binding.btnSelectAll.setOnClickListener {
            adapter.selectAllVisible()
            updateSelectedCount()
        }

        binding.btnDeselectAll.setOnClickListener {
            adapter.deselectAllVisible()
            updateSelectedCount()
        }

        binding.btnConfirmSelection.setOnClickListener {
            val selected = adapter.getCheckedRanges()
            val resultIntent = android.content.Intent().apply {
                putStringArrayListExtra(EXTRA_RESULT_SELECTION, ArrayList(selected))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun loadRanges(initialSelection: Set<String>) {
        val allLines = try {
            resources.openRawResource(R.raw.rsta_scanner_cloudflare_ranges)
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && CidrUtil.isValidCidr(it) }
        } catch (_: Exception) {
            emptyList()
        }

        val others = allLines.filter { it !in CURATED_RANGES }

        val rows = mutableListOf<RangeRow>()
        rows.add(RangeRow.Header(getString(R.string.rsta_scanner_header_curated, CURATED_RANGES.size)))
        CURATED_RANGES.forEach { rows.add(RangeRow.Item(it)) }
        rows.add(RangeRow.Header(getString(R.string.rsta_scanner_header_others, others.size)))
        others.forEach { rows.add(RangeRow.Item(it)) }

        adapter.submitRows(rows, initialSelection)
    }

    private fun updateSelectedCount() {
        binding.tvSelectedCount.text = getString(R.string.rsta_scanner_selected_count, adapter.getCheckedCount())
    }
}