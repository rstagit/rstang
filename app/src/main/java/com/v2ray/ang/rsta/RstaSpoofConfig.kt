package com.v2ray.ang.rsta

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager


object RstaSpoofConfig {

    const val LISTEN_HOST = AppConfig.LOOPBACK
    const val LISTEN_PORT = AppConfig.RSTA_SPOOF_LISTEN_PORT

    const val DEFAULT_CONNECT_IP = "104.18.38.202"
    const val DEFAULT_CONNECT_PORT = "443"
    const val DEFAULT_FAKE_SNI = "cdnjs.cloudflare.com"
    const val DEFAULT_METHOD = "combined"

    
    val METHODS = listOf(
        "combined",
        "fragment",
        "random_split",
        "seg2delay",
        "sni_triplicate",
        "oob",
        "fake_sni",
        "auto_ttl",
    )

    fun connectIp(): String =
        MmkvManager.decodeSettingsString(AppConfig.PREF_RSTA_SPOOF_CONNECT_IP, DEFAULT_CONNECT_IP)
            ?.trim().orEmpty().ifBlank { DEFAULT_CONNECT_IP }

    fun connectPort(): Int =
        MmkvManager.decodeSettingsString(AppConfig.PREF_RSTA_SPOOF_CONNECT_PORT, DEFAULT_CONNECT_PORT)
            ?.trim()?.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: DEFAULT_CONNECT_PORT.toInt()

    fun fakeSni(): String =
        MmkvManager.decodeSettingsString(AppConfig.PREF_RSTA_SPOOF_FAKE_SNI, DEFAULT_FAKE_SNI)
            ?.trim().orEmpty().ifBlank { DEFAULT_FAKE_SNI }

    fun method(): String =
        MmkvManager.decodeSettingsString(AppConfig.PREF_RSTA_SPOOF_METHOD, DEFAULT_METHOD)
            ?.trim().orEmpty().ifBlank { DEFAULT_METHOD }

    
    fun isSpoofTarget(server: String?, serverPort: String?): Boolean {
        val s = server?.trim() ?: return false
        if (s != LISTEN_HOST && s.lowercase() != "localhost") return false
        val port = serverPort?.trim()?.toIntOrNull() ?: return false
        return port == LISTEN_PORT
    }
}
