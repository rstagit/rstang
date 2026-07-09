package com.v2ray.ang.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.enums.NotificationChannelType
import com.v2ray.ang.rstascanner.ScannerConfig
import com.v2ray.ang.rstascanner.ScanResult
import com.v2ray.ang.rstascanner.ProfileItem
import com.v2ray.ang.rstascanner.core.V2rayConfigBuilder
import com.v2ray.ang.rstascanner.fmt.ConfigParser
import com.v2ray.ang.rstascanner.util.CidrUtil
import com.v2ray.ang.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class RstaScannerService : Service() {

    companion object {
        const val ACTION_START = "com.v2ray.ang.rstascanner.SCAN_START"
        const val ACTION_STOP = "com.v2ray.ang.rstascanner.SCAN_STOP"
        const val EXTRA_CIDR = "cidr"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_PORTS = "ports"

        const val EVENT_RESULT = "com.v2ray.ang.rstascanner.RESULT"
        const val EVENT_PROGRESS = "com.v2ray.ang.rstascanner.PROGRESS"
        const val EVENT_DONE = "com.v2ray.ang.rstascanner.DONE"

        const val KEY_IP = "ip"
        const val KEY_PORT = "port"
        const val KEY_DELAY = "delay"
        const val KEY_CONFIG = "config"
        const val KEY_PROCESSED = "processed"
        const val KEY_TOTAL = "total"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null
    private val processed = AtomicInteger(0)
    private var total = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val cidr = intent.getStringExtra(EXTRA_CIDR) ?: return START_NOT_STICKY
                val config = intent.getStringExtra(EXTRA_CONFIG) ?: return START_NOT_STICKY
                val ports = intent.getIntArrayExtra(EXTRA_PORTS)?.toList()
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf(ScannerConfig.DEFAULT_PORT.toIntOrNull() ?: 443)
                NotificationHelper.startForeground(
                    this,
                    NotificationChannelType.RSTA_SCANNER,
                    getString(R.string.title_rsta_scanner_setting),
                    getString(R.string.rsta_scanner_notif_scanning)
                )
                CoreNativeManager.initCoreEnv(applicationContext)
                startScan(cidr, config, ports)
            }
            ACTION_STOP -> {
                scanJob?.cancel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startScan(cidr: String, config: String, ports: List<Int>) {
        scanJob = scope.launch {
            val ips = CidrUtil.expandCidr(cidr)
            total = ips.size * ports.size
            processed.set(0)

            
            broadcastProgress(0, total)

            val baseProfile = ConfigParser.parse(config)
            if (baseProfile == null) {
                broadcastDone()
                NotificationHelper.stopForeground(this@RstaScannerService)
                stopSelf()
                return@launch
            }

            val dispatcher = Executors.newFixedThreadPool(ScannerConfig.SCAN_CONCURRENCY).asCoroutineDispatcher()

            val jobs = mutableListOf<Job>()
            for (ip in ips) {
                for (port in ports) {
                    jobs.add(launch(dispatcher) {
                        val result = testIp(ip, port, baseProfile)
                        if (result != null) {
                            broadcastResult(result)
                        }
                        val done = processed.incrementAndGet()
                        broadcastProgress(done, total)
                        NotificationHelper.updateNotification(
                            NotificationChannelType.RSTA_SCANNER,
                            this@RstaScannerService,
                            getString(R.string.rsta_scanner_processed_count, done, total)
                        )
                    })
                }
            }

            joinAll(*jobs.toTypedArray())

            broadcastDone()
            NotificationHelper.stopForeground(this@RstaScannerService)
            stopSelf()
        }
    }

    private fun testIp(ip: String, port: Int, baseProfile: ProfileItem): ScanResult? {
        return try {
            val tcpTime = socketConnect(ip, port)
            if (tcpTime < 0) return null

            val portStr = port.toString()
            val profile = baseProfile.copy(server = ip, serverPort = portStr)
            val measureConfig = V2rayConfigBuilder.buildForMeasure(profile) ?: return null
            val delay = CoreNativeManager.measureOutboundDelay(measureConfig, ScannerConfig.DELAY_TEST_URL)
            if (delay <= 0) return null

            val resultConfig = ConfigParser.toUri(profile)
            ScanResult(ip = ip, port = portStr, delay = delay, config = resultConfig)
        } catch (_: Exception) {
            
            null
        }
    }

    private fun socketConnect(host: String, port: Int): Long {
        var socket: Socket? = null
        val start = System.currentTimeMillis()
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), ScannerConfig.TCP_TIMEOUT_MS)
            System.currentTimeMillis() - start
        } catch (_: Exception) {
            -1L
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun broadcastResult(result: ScanResult) {
        val intent = Intent(EVENT_RESULT).apply {
            setPackage(packageName)
            putExtra(KEY_IP, result.ip)
            putExtra(KEY_PORT, result.port)
            putExtra(KEY_DELAY, result.delay)
            putExtra(KEY_CONFIG, result.config)
        }
        sendBroadcast(intent)
    }

    private fun broadcastProgress(done: Int, total: Int) {
        val intent = Intent(EVENT_PROGRESS).apply {
            setPackage(packageName)
            putExtra(KEY_PROCESSED, done)
            putExtra(KEY_TOTAL, total)
        }
        sendBroadcast(intent)
    }

    private fun broadcastDone() {
        val intent = Intent(EVENT_DONE).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
