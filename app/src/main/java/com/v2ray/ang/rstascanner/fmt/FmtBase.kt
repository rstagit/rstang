package com.v2ray.ang.rstascanner.fmt

import com.v2ray.ang.rstascanner.ScannerConfig
import com.v2ray.ang.rstascanner.ProfileItem
import com.v2ray.ang.rstascanner.util.Utils
import java.net.URI

open class FmtBase {

    fun getQueryParam(uri: URI): Map<String, String> {
        if (uri.rawQuery.isNullOrEmpty()) return emptyMap()
        return uri.rawQuery.split("&")
            .mapNotNull {
                val idx = it.indexOf("=")
                if (idx < 0) null
                else it.substring(0, idx) to Utils.decodeURIComponent(it.substring(idx + 1))
            }.toMap()
    }

    fun getItemFromQuery(config: ProfileItem, queryParam: Map<String, String>) {
        config.network = queryParam["type"] ?: "tcp"
        config.headerType = queryParam["headerType"]
        config.host = queryParam["host"]
        config.path = queryParam["path"]
        config.seed = queryParam["seed"]
        config.kcpMtu = queryParam["mtu"]?.toIntOrNull()
        config.kcpTti = queryParam["tti"]?.toIntOrNull()
        config.quicSecurity = queryParam["quicSecurity"]
        config.quicKey = queryParam["key"]
        config.mode = queryParam["mode"]
        config.serviceName = queryParam["serviceName"]
        config.authority = queryParam["authority"]
        config.xhttpMode = queryParam["mode"]
        config.xhttpExtra = queryParam["extra"]
        config.finalMask = queryParam["fm"]

        config.security = queryParam["security"]
        if (config.security != ScannerConfig.TLS && config.security != ScannerConfig.REALITY) {
            config.security = null
        }
        val allowInsecureKeys = arrayOf("insecure", "allowInsecure", "allow_insecure")
        config.insecure = when {
            allowInsecureKeys.any { queryParam[it] == "1" } -> true
            allowInsecureKeys.any { queryParam[it] == "0" } -> false
            else -> false
        }
        config.sni = queryParam["sni"]
        config.fingerPrint = queryParam["fp"]
        config.alpn = queryParam["alpn"]
        config.echConfigList = queryParam["ech"]
        config.verifyPeerCertByName = queryParam["vcn"]
        config.pinnedCA256 = queryParam["pcs"]
        config.publicKey = queryParam["pbk"]
        config.shortId = queryParam["sid"]
        config.spiderX = queryParam["spx"]
        config.mldsa65Verify = queryParam["pqv"]
        config.flow = queryParam["flow"]
    }

    fun toUri(config: ProfileItem, userInfo: String?, dicQuery: HashMap<String, String>?): String {
        val query = if (dicQuery != null)
            "?" + dicQuery.toList().joinToString("&") { "${it.first}=${Utils.encodeURIComponent(it.second)}" }
        else ""
        val url = "${Utils.encodeURIComponent(userInfo ?: "")}@${Utils.getIpv6Address(config.server.orEmpty())}:${config.serverPort}"
        return "${url}${query}#${Utils.encodeURIComponent(config.remarks)}"
    }

    fun getQueryDic(config: ProfileItem): HashMap<String, String> {
        val dic = HashMap<String, String>()
        dic["security"] = config.security?.ifEmpty { "none" }.orEmpty()
        config.sni?.takeIf { it.isNotBlank() }?.let { dic["sni"] = it }
        config.fingerPrint?.takeIf { it.isNotBlank() }?.let { dic["fp"] = it }
        config.alpn?.takeIf { it.isNotBlank() }?.let { dic["alpn"] = it }
        config.publicKey?.takeIf { it.isNotBlank() }?.let { dic["pbk"] = it }
        config.shortId?.takeIf { it.isNotBlank() }?.let { dic["sid"] = it }
        config.spiderX?.takeIf { it.isNotBlank() }?.let { dic["spx"] = it }
        config.flow?.takeIf { it.isNotBlank() }?.let { dic["flow"] = it }
        config.echConfigList?.takeIf { it.isNotBlank() }?.let { dic["ech"] = it }
        config.verifyPeerCertByName?.takeIf { it.isNotBlank() }?.let { dic["vcn"] = it }
        config.pinnedCA256?.takeIf { it.isNotBlank() }?.let { dic["pcs"] = it }
        config.finalMask?.takeIf { it.isNotBlank() }?.let { dic["fm"] = it }
        if (config.security == ScannerConfig.TLS) {
            val ins = if (config.insecure == true) "1" else "0"
            dic["insecure"] = ins
            dic["allowInsecure"] = ins
        }
        dic["type"] = config.network ?: "tcp"
        config.host?.takeIf { it.isNotBlank() }?.let { dic["host"] = it }
        config.path?.takeIf { it.isNotBlank() }?.let { dic["path"] = it }
        config.mode?.takeIf { it.isNotBlank() }?.let { dic["mode"] = it }
        config.serviceName?.takeIf { it.isNotBlank() }?.let { dic["serviceName"] = it }
        config.authority?.takeIf { it.isNotBlank() }?.let { dic["authority"] = it }
        return dic
    }

    protected val String.idnHost: String
        get() = this

    protected fun URI.idnHostVal(): String {
        return host?.removePrefix("[")?.removeSuffix("]") ?: ""
    }
}
