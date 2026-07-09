package com.v2ray.ang.rstascanner.core

import com.v2ray.ang.rstascanner.ScannerConfig
import com.v2ray.ang.rstascanner.EConfigType
import com.v2ray.ang.rstascanner.ProfileItem
import com.v2ray.ang.rstascanner.util.JsonUtil
import com.google.gson.JsonArray
import com.google.gson.JsonObject

object V2rayConfigBuilder {

    fun build(profile: ProfileItem): String? {
        return try {
            val root = JsonObject()
            root.add("log", buildLog())
            root.add("outbounds", buildOutbounds(profile))
            root.add("inbounds", JsonArray())
            JsonUtil.toJsonPretty(root)
        } catch (_: Exception) {
            null
        }
    }

    fun buildForMeasure(profile: ProfileItem): String? {
        return try {
            val root = JsonObject()
            root.add("log", buildLog())

            val outbounds = JsonArray()
            val outbound = buildOutbound(profile) ?: return null
            outbound.addProperty("tag", "proxy")

            
            val xudpSettings = JsonObject()
            xudpSettings.addProperty("baseKey", "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=")

            if (outbound.has("streamSettings")) {
                outbound.getAsJsonObject("streamSettings").add("xudpSettings", xudpSettings)
            } else {
                val streamSettings = JsonObject()
                streamSettings.add("xudpSettings", xudpSettings)
                outbound.add("streamSettings", streamSettings)
            }

            outbounds.add(outbound)
            root.add("outbounds", outbounds)
            root.add("inbounds", JsonArray())

            val json = JsonUtil.toJsonPretty(root)
            android.util.Log.d("V2RAY_CONFIG", "Measure JSON: $json") 
            json
        } catch (_: Exception) {
            null
        }
    }

    private fun buildLog(): JsonObject {
        val log = JsonObject()
        log.addProperty("loglevel", "warning")
        return log
    }

    private fun buildOutbounds(profile: ProfileItem): JsonArray {
        val arr = JsonArray()
        val outbound = buildOutbound(profile) ?: return arr
        outbound.addProperty("tag", "proxy")
        
        
        
        val xudpSettings = JsonObject()
        xudpSettings.addProperty("baseKey", "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=") 
        outbound.add("xudpSettings", xudpSettings)
        
        
        if (outbound.has("settings")) {
            outbound.getAsJsonObject("settings").add("xudpSettings", xudpSettings)
        }

        arr.add(outbound)
        return arr
    }

    private fun buildOutbound(profile: ProfileItem): JsonObject? {
        return when (profile.configType) {
            EConfigType.VMESS -> buildVmess(profile)
            EConfigType.VLESS -> buildVless(profile)
            EConfigType.TROJAN -> buildTrojan(profile)
            EConfigType.SHADOWSOCKS -> buildShadowsocks(profile)
            EConfigType.SOCKS -> buildSocks(profile)
            else -> null
        }
    }

    private fun buildSocks(p: ProfileItem): JsonObject {
        val ob = JsonObject()
        ob.addProperty("protocol", "socks")
        val settings = JsonObject()
        val serversArr = JsonArray()
        val server = JsonObject()
        server.addProperty("address", p.server.orEmpty())
        server.addProperty("port", p.serverPort?.toIntOrNull() ?: 1080)
        val usersArr = JsonArray()
        if (!p.password.isNullOrBlank()) {
            val user = JsonObject()
            user.addProperty("user", p.method ?: "")
            user.addProperty("pass", p.password)
            usersArr.add(user)
        }
        server.add("users", usersArr)
        serversArr.add(server)
        settings.add("servers", serversArr)
        ob.add("settings", settings)
        return ob
    }

    private fun buildVmess(p: ProfileItem): JsonObject {
        val ob = JsonObject()
        ob.addProperty("protocol", "vmess")
        val settings = JsonObject()
        val vnextArr = JsonArray()
        val vnext = JsonObject()
        vnext.addProperty("address", p.server.orEmpty())
        vnext.addProperty("port", p.serverPort?.toIntOrNull() ?: 443)
        val usersArr = JsonArray()
        val user = JsonObject()
        user.addProperty("id", p.password.orEmpty())
        user.addProperty("security", p.method ?: ScannerConfig.DEFAULT_SECURITY)
        user.addProperty("alterId", 0)
        usersArr.add(user)
        vnext.add("users", usersArr)
        vnextArr.add(vnext)
        settings.add("vnext", vnextArr)
        ob.add("settings", settings)
        ob.add("streamSettings", buildStreamSettings(p))
        return ob
    }

    private fun buildVless(p: ProfileItem): JsonObject {
        val ob = JsonObject()
        ob.addProperty("protocol", "vless")
        val settings = JsonObject()
        val vnextArr = JsonArray()
        val vnext = JsonObject()
        vnext.addProperty("address", p.server.orEmpty())
        vnext.addProperty("port", p.serverPort?.toIntOrNull() ?: 443)
        val usersArr = JsonArray()
        val user = JsonObject()
        user.addProperty("id", p.password.orEmpty())
        user.addProperty("encryption", p.method ?: "none")
        if (!p.flow.isNullOrBlank()) user.addProperty("flow", p.flow)
        usersArr.add(user)
        vnext.add("users", usersArr)
        vnextArr.add(vnext)
        settings.add("vnext", vnextArr)
        ob.add("settings", settings)
        ob.add("streamSettings", buildStreamSettings(p))
        return ob
    }

    private fun buildTrojan(p: ProfileItem): JsonObject {
        val ob = JsonObject()
        ob.addProperty("protocol", "trojan")
        val settings = JsonObject()
        val serversArr = JsonArray()
        val server = JsonObject()
        server.addProperty("address", p.server.orEmpty())
        server.addProperty("port", p.serverPort?.toIntOrNull() ?: 443)
        server.addProperty("password", p.password.orEmpty())
        serversArr.add(server)
        settings.add("servers", serversArr)
        ob.add("settings", settings)
        ob.add("streamSettings", buildStreamSettings(p))
        return ob
    }

    private fun buildShadowsocks(p: ProfileItem): JsonObject {
        val ob = JsonObject()
        ob.addProperty("protocol", "shadowsocks")
        val settings = JsonObject()
        val serversArr = JsonArray()
        val server = JsonObject()
        server.addProperty("address", p.server.orEmpty())
        server.addProperty("port", p.serverPort?.toIntOrNull() ?: 443)
        server.addProperty("password", p.password.orEmpty())
        server.addProperty("method", p.method.orEmpty())
        serversArr.add(server)
        settings.add("servers", serversArr)
        ob.add("settings", settings)
        ob.add("streamSettings", buildStreamSettings(p))
        return ob
    }

    private fun buildStreamSettings(p: ProfileItem): JsonObject {
        val ss = JsonObject()
        val network = p.network ?: "tcp"
        ss.addProperty("network", network)

        
        val xudpSettings = JsonObject()
        xudpSettings.addProperty("baseKey", "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=") 
        ss.add("xudpSettings", xudpSettings)

        when (network.lowercase()) {
            "ws" -> {
                val wsSettings = JsonObject()
                if (!p.host.isNullOrBlank()) {
                    wsSettings.addProperty("host", p.host)
                    val wsHeaders = JsonObject()
                    wsHeaders.addProperty("Host", p.host)
                    wsSettings.add("headers", wsHeaders)
                }
                wsSettings.addProperty("path", p.path ?: "/")
                ss.add("wsSettings", wsSettings)
            }
            "grpc" -> {
                val grpcSettings = JsonObject()
                grpcSettings.addProperty("serviceName", p.serviceName ?: "")
                if (!p.authority.isNullOrBlank()) grpcSettings.addProperty("authority", p.authority)
                ss.add("grpcSettings", grpcSettings)
            }
            "h2", "http" -> {
                val httpSettings = JsonObject()
                val hostsArr = JsonArray()
                if (!p.host.isNullOrBlank()) hostsArr.add(p.host)
                httpSettings.add("host", hostsArr)
                httpSettings.addProperty("path", p.path ?: "/")
                ss.add("httpSettings", httpSettings)
            }
            "kcp" -> {
                val kcpSettings = JsonObject()
                if (!p.headerType.isNullOrBlank()) {
                    val header = JsonObject()
                    header.addProperty("type", p.headerType)
                    kcpSettings.add("header", header)
                }
                if (!p.seed.isNullOrBlank()) kcpSettings.addProperty("seed", p.seed)
                p.kcpMtu?.let { kcpSettings.addProperty("mtu", it) }
                p.kcpTti?.let { kcpSettings.addProperty("tti", it) }
                ss.add("kcpSettings", kcpSettings)
            }
            "quic" -> {
                val quicSettings = JsonObject()
                if (!p.headerType.isNullOrBlank()) {
                    val header = JsonObject()
                    header.addProperty("type", p.headerType)
                    quicSettings.add("header", header)
                }
                if (!p.quicSecurity.isNullOrBlank()) quicSettings.addProperty("security", p.quicSecurity)
                if (!p.quicKey.isNullOrBlank()) quicSettings.addProperty("key", p.quicKey)
                ss.add("quicSettings", quicSettings)
            }
            "xhttp" -> {
                val xhttpSettings = JsonObject()
                if (!p.host.isNullOrBlank()) xhttpSettings.addProperty("host", p.host)
                xhttpSettings.addProperty("path", p.path ?: "/")
                if (!p.xhttpMode.isNullOrBlank()) xhttpSettings.addProperty("mode", p.xhttpMode)
                if (!p.xhttpExtra.isNullOrBlank()) xhttpSettings.addProperty("extra", p.xhttpExtra)
                ss.add("xhttpSettings", xhttpSettings)
            }
            "tcp" -> {
                if (p.headerType == "http") {
                    val tcpSettings = JsonObject()
                    val header = JsonObject()
                    header.addProperty("type", "http")
                    val request = JsonObject()
                    if (!p.host.isNullOrBlank()) {
                        val hostsArr = JsonArray()
                        hostsArr.add(p.host)
                        request.add("headers", JsonObject().apply {
                            add("Host", hostsArr)
                        })
                    }
                    if (!p.path.isNullOrBlank()) request.addProperty("path", p.path)
                    header.add("request", request)
                    tcpSettings.add("header", header)
                    ss.add("tcpSettings", tcpSettings)
                }
            }
        }

        val security = p.security
        if (security == ScannerConfig.TLS || security == ScannerConfig.REALITY) {
            ss.addProperty("security", security)
            val tlsSettings = JsonObject()
            val sni = p.sni ?: p.host ?: p.server ?: ""
            if (sni.isNotBlank()) tlsSettings.addProperty("serverName", sni)
            if (!p.fingerPrint.isNullOrBlank()) tlsSettings.addProperty("fingerprint", p.fingerPrint)
            val alpn = p.alpn
            if (!alpn.isNullOrBlank()) {
                val alpnArr = JsonArray()
                alpn.split(",").forEach { alpnArr.add(it.trim()) }
                tlsSettings.add("alpn", alpnArr)
            }
            tlsSettings.addProperty("allowInsecure", p.insecure ?: false)
            if (security == ScannerConfig.REALITY) {
                if (!p.publicKey.isNullOrBlank()) tlsSettings.addProperty("publicKey", p.publicKey)
                if (!p.shortId.isNullOrBlank()) tlsSettings.addProperty("shortId", p.shortId)
                if (!p.spiderX.isNullOrBlank()) tlsSettings.addProperty("spiderX", p.spiderX)
            }
            if (!p.finalMask.isNullOrBlank()) {
                tlsSettings.addProperty("finalMask", p.finalMask)
            }
            val settingsKey = if (security == ScannerConfig.REALITY) "realitySettings" else "tlsSettings"
            ss.add(settingsKey, tlsSettings)
        }

        return ss
    }
}
