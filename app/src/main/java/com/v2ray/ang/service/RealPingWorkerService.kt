package com.v2ray.ang.service

import android.content.Context
import android.content.Intent
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.dto.RealPingEvent
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.rsta.RstaSpoofConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class RealPingWorkerService(
    private val context: Context,
    private val guids: List<String>,
    private val realPingConcurrency: Int = 0,
    private val onEvent: (RealPingEvent) -> Unit = {}
) {
    private val job = SupervisorJob()
    private val concurrency = if (realPingConcurrency > 0) realPingConcurrency else SettingsManager.getRealPingConcurrency()
    private val dispatcher = Executors.newFixedThreadPool(concurrency).asCoroutineDispatcher()
    private val scope = CoroutineScope(job + dispatcher + CoroutineName("RealPingBatchWorker"))

    private val runningCount = AtomicInteger(0)
    private val totalCount = AtomicInteger(0)

    fun start() {
        scope.launch {
            var needsSpoof = false
            for (guid in guids) {
                val config = MmkvManager.decodeServerConfig(guid)
                if (config != null && RstaSpoofConfig.isSpoofTarget(config.server, config.serverPort)) {
                    needsSpoof = true
                    break
                }
            }

            if (needsSpoof) {
                val intent = Intent(context, RstaSpoofService::class.java).apply {
                    action = "START"
                    putExtra("IP", RstaSpoofConfig.connectIp())
                    putExtra("PORT", RstaSpoofConfig.connectPort())
                    putExtra("SNI", RstaSpoofConfig.fakeSni())
                    putExtra("METHOD", RstaSpoofConfig.method())
                }
                context.startService(intent)

                delay(800)
            }

            val jobs = guids.map { guid ->
                totalCount.incrementAndGet()
                launch {
                    runningCount.incrementAndGet()
                    try {
                        val result = startRealPing(guid)
                        onEvent(RealPingEvent.Result(guid, result))
                    } catch (_: Throwable) {
                    } finally {
                        val count = totalCount.decrementAndGet()
                        val left = runningCount.decrementAndGet()
                        onEvent(RealPingEvent.Progress("$left / $count"))
                    }
                }
            }

            try {
                joinAll(*jobs.toTypedArray())
                onEvent(RealPingEvent.Finish("0"))
            } catch (_: CancellationException) {
                onEvent(RealPingEvent.Finish("-1"))
            } finally {
                close()
            }
        }
    }

    fun cancel() {
        job.cancel()
    }

    private fun close() {
        try {
            dispatcher.close()
        } catch (_: Throwable) {
        }
    }

    private fun startRealPing(guid: String): Long {
        val retFailure = -1L
        val configResult = CoreConfigManager.getV2rayConfig4Speedtest(context, guid)
        if (!configResult.status) {
            return retFailure
        }
        return CoreNativeManager.measureOutboundDelay(configResult.content, SettingsManager.getDelayTestUrl())
    }
}
