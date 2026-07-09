package com.v2ray.ang.rsta

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import com.rstagit.androidasdd.core.protocol.GoNativeBridge
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ArrayDeque


object RstaSpoofEngine {

    private const val LOG_BUFFER_MAX_LINES = 200
    private const val MMKV_SESSION_ID = "rsta_session_id"
    private const val MMKV_LAST_IP = "rsta_last_ip"
    private const val MMKV_LAST_PORT = "rsta_last_port"
    private const val MMKV_LAST_SNI = "rsta_last_sni"
    private const val MMKV_LAST_METHOD = "rsta_last_method"
    private const val MMKV_LAST_ERROR = "rsta_last_error"

    private var localSessionId: Long = 0L

    private var pollJob: Job? = null
    private val pollScope = CoroutineScope(Dispatchers.IO)

    private val logBuffer = ArrayDeque<String>()
    private val logLock = Any()

    val isRunning: Boolean
        get() = MmkvManager.decodeSettingsLong(MMKV_SESSION_ID, 0L) != 0L

    
    fun isAvailable(): Boolean = try {
        GoNativeBridge.isAvailable()
    } catch (_: Throwable) {
        false
    }

    
    fun statusSummary(): String {
        if (isRunning) {
            val ip = MmkvManager.decodeSettingsString(MMKV_LAST_IP, "")
            val port = MmkvManager.decodeSettingsInt(MMKV_LAST_PORT, 0)
            val sni = MmkvManager.decodeSettingsString(MMKV_LAST_SNI, "")
            val method = MmkvManager.decodeSettingsString(MMKV_LAST_METHOD, "")
            return "active \u2192 $ip:$port (sni=$sni, method=$method)"
        } else {
            val error = MmkvManager.decodeSettingsString(MMKV_LAST_ERROR, "")
            return if (error.isNullOrEmpty()) "inactive" else "failed: $error"
        }
    }

    
    @Synchronized
    fun start(connectIp: String, connectPort: Int, fakeSni: String, method: String): Boolean {
        val currentIp = MmkvManager.decodeSettingsString(MMKV_LAST_IP, "")
        val currentPort = MmkvManager.decodeSettingsInt(MMKV_LAST_PORT, 0)
        val currentSni = MmkvManager.decodeSettingsString(MMKV_LAST_SNI, "")
        val currentMethod = MmkvManager.decodeSettingsString(MMKV_LAST_METHOD, "")

        if (localSessionId != 0L) {
            
            if (currentIp == connectIp && currentPort == connectPort && 
                currentSni == fakeSni && currentMethod == method) {
                return true
            }
            
            stop()
        }

        if (!isAvailable()) {
            val err = "Native engine unavailable. Load error: ${GoNativeBridge.getLoadError()?.message ?: "unknown"}"
            LogUtil.e(AppConfig.TAG, "RstaSpoof: $err")
            MmkvManager.encodeSettings(MMKV_LAST_ERROR, err)
            return false
        }

        return try {
            val remote = "$connectIp:$connectPort"
            LogUtil.i(AppConfig.TAG, "RstaSpoof: Starting engine for $remote (sni=$fakeSni, method=$method)")
            
            val id = GoNativeBridge.spfStart(RstaSpoofConfig.LISTEN_PORT, remote, fakeSni, method)
            if (id == 0L) {
                val err = "spfStart returned 0 (failed). Port ${RstaSpoofConfig.LISTEN_PORT} might be in use or native bridge failed."
                LogUtil.e(AppConfig.TAG, "RstaSpoof: $err for $remote.")
                MmkvManager.encodeSettings(MMKV_LAST_ERROR, err)
                false
            } else {
                localSessionId = id
                MmkvManager.encodeSettings(MMKV_SESSION_ID, id)
                MmkvManager.encodeSettings(MMKV_LAST_IP, connectIp)
                MmkvManager.encodeSettings(MMKV_LAST_PORT, connectPort)
                MmkvManager.encodeSettings(MMKV_LAST_SNI, fakeSni)
                MmkvManager.encodeSettings(MMKV_LAST_METHOD, method)
                MmkvManager.encodeSettings(MMKV_LAST_ERROR, "")

                appendLog("started: listen=127.0.0.1:${RstaSpoofConfig.LISTEN_PORT} -> $remote sni=$fakeSni method=$method")
                startPolling()
                true
            }
        } catch (t: Throwable) {
            val err = t.message ?: t.javaClass.simpleName
            LogUtil.e(AppConfig.TAG, "RstaSpoof: failed to start", t)
            MmkvManager.encodeSettings(MMKV_LAST_ERROR, err)
            false
        }
    }

    @Synchronized
    fun stop() {
        val id = localSessionId
        localSessionId = 0L
        MmkvManager.encodeSettings(MMKV_SESSION_ID, 0L)

        stopPolling()
        if (id != 0L) {
            try {
                GoNativeBridge.spfStop(id)
                appendLog("stopped")
            } catch (t: Throwable) {
                LogUtil.e(AppConfig.TAG, "RstaSpoof: failed to stop cleanly", t)
            }
        }
    }

    fun version(): String = try {
        if (isAvailable()) GoNativeBridge.spfVersion() ?: "unknown" else "unavailable"
    } catch (_: Throwable) {
        "unknown"
    }

    
    fun recentLogLines(): List<String> = synchronized(logLock) { logBuffer.toList() }

    private fun appendLog(line: String) {
        synchronized(logLock) {
            logBuffer.addLast(line)
            while (logBuffer.size > LOG_BUFFER_MAX_LINES) {
                logBuffer.removeFirst()
            }
        }
    }

    private fun startPolling() {
        stopPolling()
        pollJob = pollScope.launch {
            while (isRunning) {
                try {
                    val line = GoNativeBridge.spfPollLog()
                    if (!line.isNullOrEmpty()) {
                        appendLog(line)
                    }
                } catch (_: Throwable) {
                    
                }
                delay(500)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }
}
