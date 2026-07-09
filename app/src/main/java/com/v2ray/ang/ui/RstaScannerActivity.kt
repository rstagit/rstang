package com.v2ray.ang.ui

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.v2ray.ang.R
import com.v2ray.ang.rstascanner.ScanResult
import com.v2ray.ang.service.RstaScannerService
import com.v2ray.ang.databinding.ActivityRstaScannerBinding
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.rstascanner.fmt.ConfigParser
import com.v2ray.ang.rstascanner.util.CidrUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val PREFS_NAME = "rsta_scanner_prefs"
private const val KEY_CIDR = "last_cidr"
private const val KEY_CONFIG = "last_config"
private const val KEY_PORT_443 = "port_443"
private const val KEY_PORT_2053 = "port_2053"
private const val KEY_PORT_8443 = "port_8443"
private const val KEY_PORT_2087 = "port_2087"
private const val KEY_CUSTOM_PORTS = "custom_ports"

class RstaScannerActivity : BaseActivity() {

    private lateinit var binding: ActivityRstaScannerBinding
    private lateinit var adapter: RstaScannerResultAdapter
    private var scanning = false
    private var speedTesting = false
    private val pendingCidrs = mutableListOf<String>()
    @Volatile private var speedStopped = false
    private var processedCount = 0
    private var totalCount = 0
    private var currentCidr = ""
    private var speedTestCountValue = 20
    private val speedTestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    private val ipRangeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selected = result.data?.getStringArrayListExtra(RstaScannerIpRangeActivity.EXTRA_RESULT_SELECTION)
            if (!selected.isNullOrEmpty()) {
                binding.etCidr.setText(selected.joinToString("\n"))
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                RstaScannerService.EVENT_RESULT -> {
                    val ip = intent.getStringExtra(RstaScannerService.KEY_IP) ?: return
                    val port = intent.getStringExtra(RstaScannerService.KEY_PORT) ?: ""
                    val delay = intent.getLongExtra(RstaScannerService.KEY_DELAY, -1L)
                    val config = intent.getStringExtra(RstaScannerService.KEY_CONFIG) ?: return
                    val insertedAt = adapter.addResult(ScanResult(ip, port, delay, config, sourceCidr = currentCidr))
                    if (insertedAt == 0) {
                        binding.rvResults.scrollToPosition(0)
                    }
                    updateResultSection()
                }
                RstaScannerService.EVENT_PROGRESS -> {
                    processedCount = intent.getIntExtra(RstaScannerService.KEY_PROCESSED, 0)
                    totalCount = intent.getIntExtra(RstaScannerService.KEY_TOTAL, 0)
                    updateProgress()
                }
                RstaScannerService.EVENT_DONE -> {
                    if (pendingCidrs.isNotEmpty()) {
                        
                        
                        
                        
                        
                        val nextCidr = pendingCidrs.removeAt(0)
                        val config   = binding.etConfig.text.toString().trim()
                        val ports    = getSelectedPorts() ?: run { onScanDone(); return }
                        binding.root.postDelayed({
                            if (scanning) launchScan(nextCidr, config, ports)
                        }, 400L)
                    } else {
                        scanning = false
                        onScanDone()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRstaScannerBinding.inflate(layoutInflater)
        setContentViewWithToolbar(binding.root, title = getString(R.string.title_rsta_scanner_setting))

        setupRecyclerView()
        setupListeners()
        restoreLastInputs()
        updateUiState()
        updateProgress()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(RstaScannerService.EVENT_RESULT)
            addAction(RstaScannerService.EVENT_PROGRESS)
            addAction(RstaScannerService.EVENT_DONE)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        speedTestScope.cancel()
    }

    private fun setupRecyclerView() {
        adapter = RstaScannerResultAdapter(
            onCopyIp = { copyToClipboard("IP", "${it.ip}:${it.port}") },
            onCopyConfig = { copyToClipboard("Config", it.config) }
        )
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnScan.setOnClickListener {
            if (scanning) stopScan() else startScan()
        }

        binding.btnIpRangeSettings.setOnClickListener {
            val currentCidrs = binding.etCidr.text.toString()
                .split(Regex("[\\s,]+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val intent = Intent(this, RstaScannerIpRangeActivity::class.java).apply {
                putStringArrayListExtra(RstaScannerIpRangeActivity.EXTRA_INITIAL_SELECTION, ArrayList(currentCidrs))
            }
            ipRangeLauncher.launch(intent)
        }

        
        
        binding.btnSpeedCountMinus.setOnClickListener {
            speedTestCountValue = (speedTestCountValue - 10).coerceAtLeast(1)
            binding.tvSpeedCountValue.text = speedTestCountValue.toString()
        }
        binding.btnSpeedCountPlus.setOnClickListener {
            speedTestCountValue += 10
            binding.tvSpeedCountValue.text = speedTestCountValue.toString()
        }

        binding.btnCopyAllIps.setOnClickListener {
            if (adapter.getCount() == 0) return@setOnClickListener
            copyToClipboard("All IPs", adapter.getAllIps())
        }

        binding.btnCopyAllConfigs.setOnClickListener {
            if (adapter.getCount() == 0) return@setOnClickListener
            copyToClipboard("All Configs", adapter.getAllConfigs())
        }

        binding.btnTop10Ping.setOnClickListener {
            val top10 = adapter.getTop10ByPing()
            if (top10.isNotEmpty()) copyToClipboard("Top 10 Configs", top10)
        }

        binding.btnAddTop10Rstang.setOnClickListener {
            addTop10ToRstang()
        }

        binding.btnSpeedTest.setOnClickListener {
            if (!speedTesting && adapter.getCount() > 0) {
                startSpeedTest()
            }
        }

        binding.btnStopSpeedTest.setOnClickListener {
            stopSpeedTest()
        }

        binding.btnCopySpeedResults.setOnClickListener {
            val results = adapter.getSpeedResults()
            if (results.isNotEmpty()) {
                copyToClipboard("Speed Results", results)
            }
        }

        binding.btnCopySpeedIps.setOnClickListener {
            val ips = adapter.getSpeedTestedIps()
            if (ips.isNotEmpty()) {
                copyToClipboard("Speed Tested IPs", ips)
            }
        }

        binding.tvTelegram.setOnClickListener {
            try {
                val url = "https://t.me/rstatel"
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private fun startScan() {
        val rawInput = binding.etCidr.text.toString().trim()
        val config   = binding.etConfig.text.toString().trim()
        val ports    = getSelectedPorts() ?: return

        
        
        val cidrs = rawInput.split(Regex("[\\s,]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { if (it.contains("/")) it else "$it/32" }
        if (cidrs.isEmpty()) {
            showError(getString(R.string.rsta_scanner_err_no_cidr))
            return
        }
        val invalidCidr = cidrs.firstOrNull { !CidrUtil.isValidCidr(it) }
        if (invalidCidr != null) {
            showError(getString(R.string.rsta_scanner_err_invalid_cidr, invalidCidr))
            return
        }
        if (config.isEmpty()) {
            showError(getString(R.string.rsta_scanner_err_no_config))
            return
        }
        val parsed = ConfigParser.parse(config)
        if (parsed == null) {
            showError(getString(R.string.rsta_scanner_err_unsupported_config))
            return
        }
        if (ports.isEmpty()) {
            showError(getString(R.string.rsta_scanner_err_no_port))
            return
        }

        saveLastInputs(rawInput, config)
        adapter.clear()
        processedCount = 0
        totalCount    = 0
        scanning      = true
        binding.groupSpeedCopy.visibility      = View.GONE
        binding.tvSpeedProgress.visibility     = View.GONE
        binding.groupTop10.visibility          = View.GONE
        binding.btnAddTop10Rstang.visibility   = View.GONE
        speedTestCountValue = 20
        binding.tvSpeedCountValue.text = "20"

        
        pendingCidrs.clear()
        pendingCidrs.addAll(cidrs.drop(1))

        updateUiState()
        updateProgress()
        binding.tvNoResults.visibility = View.GONE
        launchScan(cidrs.first(), config, ports)
    }

    private fun launchScan(cidr: String, config: String, ports: List<Int>) {
        currentCidr = cidr
        val intent = Intent(this, RstaScannerService::class.java).apply {
            action = RstaScannerService.ACTION_START
            putExtra(RstaScannerService.EXTRA_CIDR, cidr)
            putExtra(RstaScannerService.EXTRA_CONFIG, config)
            putExtra(RstaScannerService.EXTRA_PORTS, ports.toIntArray())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun getSelectedPorts(): List<Int>? {
        val ports = mutableListOf<Int>()
        if (binding.cbPort443.isChecked) ports.add(443)
        if (binding.cbPort2053.isChecked) ports.add(2053)
        if (binding.cbPort8443.isChecked) ports.add(8443)
        if (binding.cbPort2087.isChecked) ports.add(2087)

        val customInput = binding.etCustomPorts.text.toString().trim()
        if (customInput.isNotEmpty()) {
            val tokens = customInput.split(Regex("[^0-9]+")).filter { it.isNotBlank() }
            for (token in tokens) {
                val p = token.toIntOrNull()
                if (p == null || p < 1 || p > 65535) {
                    showError(getString(R.string.rsta_scanner_err_invalid_port, token))
                    return null
                }
                if (p !in ports) ports.add(p)
            }
        }
        return ports
    }

    private fun stopScan() {
        scanning = false
        pendingCidrs.clear()
        val intent = Intent(this, RstaScannerService::class.java).apply {
            action = RstaScannerService.ACTION_STOP
        }
        startService(intent)
        onScanDone()
    }

    private fun onScanDone() {
        scanning = false
        updateUiState()
        updateResultSection()
        if (totalCount > 0 && adapter.getCount() == 0) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.tvNoResults.text = getString(R.string.rsta_scanner_no_results, processedCount, totalCount)
        }
    }

    
    
    private fun addTop10ToRstang() {
        val top10 = adapter.getTop10ResultsByPing()
        if (top10.isEmpty()) return

        val taggedConfigs = top10.mapNotNull { result ->
            val profile = ConfigParser.parse(result.config) ?: return@mapNotNull null
            profile.remarks = "RSTA-Scan ${result.ip}:${result.port} (${result.delay}ms)"
            ConfigParser.toUri(profile)
        }
        if (taggedConfigs.isEmpty()) return

        val subId = Utils.getUuid()
        val subName = getString(
            R.string.rsta_scanner_subscription_name,
            SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date())
        )
        MmkvManager.encodeSubscription(subId, SubscriptionItem(remarks = subName))

        val (addedCount, _) = AngConfigManager.importBatchConfig(
            taggedConfigs.joinToString("\n"),
            subId,
            true
        )

        SettingsChangeManager.makeSetupGroupTab()

        Toast.makeText(this, getString(R.string.rsta_scanner_added_to_rstang, addedCount), Toast.LENGTH_LONG).show()
    }

    
    private val speedSemaphore = Semaphore(1)
    private val TEST_DURATION_MS = 4000L

    private fun startSpeedTest() {
        
        val top20 = adapter.getSpeedTestCandidates(budget = speedTestCountValue)
        if (top20.isEmpty()) return

        speedTesting = true
        speedStopped = false
        binding.btnSpeedTest.isEnabled = false
        binding.btnSpeedTest.text = getString(R.string.rsta_scanner_btn_speed_test_testing)
        binding.btnStopSpeedTest.visibility = View.VISIBLE
        binding.groupSpeedCopy.visibility = View.GONE
        binding.tvSpeedProgress.visibility = View.VISIBLE
        binding.tvSpeedProgress.text = getString(R.string.rsta_scanner_speed_progress, 0, top20.size)

        val completed = AtomicInteger(0)
        val firstDone = AtomicInteger(0)
        val total = top20.size

        for (result in top20) {
            val ip   = result.ip
            val port = result.port

            
            speedTestScope.launch {
                if (speedStopped) return@launch
                speedSemaphore.withPermit {
                    if (speedStopped) return@withPermit
                    try {
                        
                        val upDeferred = async(Dispatchers.IO) {
                            runCatching { measureUploadSpeed() }.getOrDefault(-1.0)
                        }
                        val downDeferred = async(Dispatchers.IO) {
                            runCatching { measureDownloadSpeed() }.getOrDefault(-1.0)
                        }

                        val upJob = launch {
                            val up = upDeferred.await()
                            withContext(Dispatchers.Main) {
                                if (!speedStopped) {
                                    adapter.setUploadSpeed(ip, port, up)
                                    adapter.applySpeedAndResort()
                                    if (firstDone.incrementAndGet() == 1) {
                                        binding.groupSpeedCopy.visibility = View.VISIBLE
                                    }
                                }
                            }
                        }

                        val downJob = launch {
                            val down = downDeferred.await()
                            withContext(Dispatchers.Main) {
                                if (!speedStopped) {
                                    adapter.setDownloadSpeed(ip, port, down)
                                    adapter.applySpeedAndResort()
                                }
                            }
                        }

                        
                        
                        upJob.join()
                        downJob.join()
                    } catch (e: Exception) {
                        Log.e("SpeedTest", "item failed ip=$ip: ${e.message}")
                    } finally {
                        
                        withContext(NonCancellable) {
                            withContext(Dispatchers.Main) {
                                val done = completed.incrementAndGet()
                                binding.tvSpeedProgress.text = getString(R.string.rsta_scanner_speed_progress, done, total)
                                if (done >= total) onSpeedTestDone()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopSpeedTest() {
        speedStopped = true
        speedTesting = false
        onSpeedTestDone()
    }

    private fun onSpeedTestDone() {
        speedTesting = false
        binding.btnSpeedTest.isEnabled = true
        binding.btnSpeedTest.text = getString(R.string.rsta_scanner_btn_speed_test_default)
        binding.btnStopSpeedTest.visibility = View.GONE
    }

    
    private fun measureDownloadSpeed(): Double {
        val url1 = "https://speed.cloudflare.com/__down?bytes=50000000"
        val url2 = "https://httpbin.org/bytes/10485760"
        val urls = listOf(url1, url2)
        for (url in urls) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = (TEST_DURATION_MS + 3000).toInt()
                conn.requestMethod = "GET"
                conn.connect()
                val code = conn.responseCode
                if (code !in 200..299) { conn.disconnect(); continue }

                val buf = ByteArray(131072)
                val stream = conn.inputStream
                var total = 0L
                val start = System.currentTimeMillis()
                var read: Int
                while (stream.read(buf).also { read = it } != -1) {
                    total += read
                    if (System.currentTimeMillis() - start >= TEST_DURATION_MS) break
                }
                val elapsed = (System.currentTimeMillis() - start) / 1000.0
                stream.close()

                Log.d("SpeedTest", "DOWN url=$url total=${total}B elapsed=${elapsed}s")
                if (elapsed > 0.5 && total > 20000) return (total / 1024.0 / 1024.0) / elapsed
            } catch (e: Exception) {
                Log.e("SpeedTest", "DOWN failed url=$url: ${e.message}")
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        }
        return -1.0
    }

    
    
    private fun measureUploadSpeed(): Double {
        val url1 = "https://speed.cloudflare.com/__up"
        val url2 = "https://httpbin.org/post"
        val url3 = "https://postman-echo.com/post"
        val urls = listOf(url1, url2, url3)
        val payload = ByteArray(512 * 1024) 
        for (url in urls) {
            var totalBytes = 0L
            var successfulRequests = 0
            var consecutiveFailures = 0
            val overallStart = System.currentTimeMillis()

            while (System.currentTimeMillis() - overallStart < TEST_DURATION_MS && consecutiveFailures < 3) {
                var conn: HttpURLConnection? = null
                try {
                    conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 5000
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setFixedLengthStreamingMode(payload.size)
                    conn.setRequestProperty("Content-Type", "application/octet-stream")
                    conn.connect()
                    conn.outputStream.use { it.write(payload) }
                    totalBytes += payload.size
                    successfulRequests++
                    consecutiveFailures = 0
                    
                    try { conn.responseCode } catch (_: Exception) {}
                } catch (e: Exception) {
                    consecutiveFailures++
                    Log.e("SpeedTest", "UP request failed url=$url: ${e.message}")
                } finally {
                    try { conn?.disconnect() } catch (_: Exception) {}
                }
            }

            val elapsed = (System.currentTimeMillis() - overallStart) / 1000.0
            Log.d("SpeedTest", "UP url=$url totalBytes=${totalBytes}B elapsed=${elapsed}s requests=$successfulRequests")
            if (successfulRequests > 0 && elapsed > 0.3 && totalBytes > 0) {
                return (totalBytes / 1024.0 / 1024.0) / elapsed
            }
        }
        return -1.0
    }

    private fun saveLastInputs(cidr: String, config: String) {
        prefs.edit()
            .putString(KEY_CIDR, cidr)
            .putString(KEY_CONFIG, config)
            .putBoolean(KEY_PORT_443, binding.cbPort443.isChecked)
            .putBoolean(KEY_PORT_2053, binding.cbPort2053.isChecked)
            .putBoolean(KEY_PORT_8443, binding.cbPort8443.isChecked)
            .putBoolean(KEY_PORT_2087, binding.cbPort2087.isChecked)
            .putString(KEY_CUSTOM_PORTS, binding.etCustomPorts.text.toString().trim())
            .apply()
    }

    private fun restoreLastInputs() {
        val cidr = prefs.getString(KEY_CIDR, "") ?: ""
        val config = prefs.getString(KEY_CONFIG, "") ?: ""
        val customPorts = prefs.getString(KEY_CUSTOM_PORTS, "") ?: ""

        if (cidr.isNotEmpty()) binding.etCidr.setText(cidr)
        if (config.isNotEmpty()) binding.etConfig.setText(config)
        if (customPorts.isNotEmpty()) binding.etCustomPorts.setText(customPorts)

        binding.cbPort443.isChecked = prefs.getBoolean(KEY_PORT_443, true)
        binding.cbPort2053.isChecked = prefs.getBoolean(KEY_PORT_2053, false)
        binding.cbPort8443.isChecked = prefs.getBoolean(KEY_PORT_8443, false)
        binding.cbPort2087.isChecked = prefs.getBoolean(KEY_PORT_2087, false)
    }

    private fun updateUiState() {
        if (scanning) {
            binding.btnScan.text = getString(R.string.rsta_scanner_btn_stop)
            binding.btnScan.backgroundTintList = ContextCompat.getColorStateList(this, R.color.rsta_scanner_btn_stop)
        } else {
            binding.btnScan.text = getString(R.string.rsta_scanner_btn_start)
            binding.btnScan.backgroundTintList = ContextCompat.getColorStateList(this, R.color.rsta_scanner_btn_scan)
        }
    }

    private fun updateProgress() {
        binding.tvProgress.text = getString(R.string.rsta_scanner_processed_count, processedCount, totalCount)
        val found = adapter.getCount()
        binding.tvFoundCount.text = getString(R.string.rsta_scanner_found_count, found)
    }

    private fun updateResultSection() {
        val found = adapter.getCount()
        binding.tvFoundCount.text = getString(R.string.rsta_scanner_found_count, found)
        if (found > 0) {
            binding.tvNoResults.visibility = View.GONE
            binding.groupCopyAll.visibility = View.VISIBLE
            binding.groupTop10.visibility = View.VISIBLE
            binding.btnAddTop10Rstang.visibility = View.VISIBLE
            binding.btnSpeedTest.visibility = View.VISIBLE
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, getString(R.string.rsta_scanner_copied), Toast.LENGTH_SHORT).show()
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}