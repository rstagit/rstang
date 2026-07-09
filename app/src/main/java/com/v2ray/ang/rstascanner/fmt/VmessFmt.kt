package com.v2ray.ang.rstascanner.fmt

import com.v2ray.ang.rstascanner.ScannerConfig
import com.v2ray.ang.rstascanner.EConfigType
import com.v2ray.ang.rstascanner.ProfileItem
import com.v2ray.ang.rstascanner.VmessQRCode
import com.v2ray.ang.rstascanner.util.JsonUtil
import com.v2ray.ang.rstascanner.util.Utils
import java.net.URI

object VmessFmt : FmtBase() {
    fun parse(str: String): ProfileItem? {
        if (str.indexOf('?') > 0 && str.indexOf('&') > 0) {
            return parseVmessStd(str)
        }
        return try {
            val config = ProfileItem.create(EConfigType.VMESS)
            var result = str.removePrefix(EConfigType.VMESS.protocolScheme)
            result = Utils.decode(result)
            if (result.isEmpty()) return null
            val qr = JsonUtil.fromJson(result, VmessQRCode::class.java) ?: return null
            if (qr.add.isEmpty() || qr.port.isEmpty() || qr.id.isEmpty() || qr.net.isEmpty()) return null
            config.remarks = qr.ps
            config.server = qr.add
            config.serverPort = qr.port
            config.password = qr.id
            config.method = if (qr.scy.isEmpty()) ScannerConfig.DEFAULT_SECURITY else qr.scy
            config.network = qr.net.ifEmpty { "tcp" }
            config.headerType = qr.type
            config.host = qr.host
            config.path = qr.path
            config.security = qr.tls
            config.sni = qr.sni
            config.fingerPrint = qr.fp
            config.alpn = qr.alpn
            config.insecure = qr.insecure == "1"
            config.verifyPeerCertByName = qr.vcn
            config.pinnedCA256 = qr.pcs
            config
        } catch (_: Exception) {
            null
        }
    }

    fun toUri(config: ProfileItem): String {
        val qr = VmessQRCode()
        qr.v = "2"
        qr.ps = config.remarks
        qr.add = config.server.orEmpty()
        qr.port = config.serverPort.orEmpty()
        qr.id = config.password.orEmpty()
        qr.scy = config.method.orEmpty()
        qr.aid = "0"
        qr.net = config.network.orEmpty()
        qr.type = config.headerType.orEmpty()
        config.host?.takeIf { it.isNotBlank() }?.let { qr.host = it }
        config.path?.takeIf { it.isNotBlank() }?.let { qr.path = it }
        qr.tls = config.security.orEmpty()
        qr.sni = config.sni.orEmpty()
        qr.fp = config.fingerPrint.orEmpty()
        qr.alpn = config.alpn.orEmpty()
        qr.insecure = if (config.insecure == true) "1" else "0"
        val json = JsonUtil.toJson(qr)
        return "${EConfigType.VMESS.protocolScheme}${Utils.encode(json)}"
    }

    private fun parseVmessStd(str: String): ProfileItem? {
        return try {
            val config = ProfileItem.create(EConfigType.VMESS)
            val uri = URI(Utils.fixIllegalUrl(str))
            if (uri.rawQuery.isNullOrEmpty()) return null
            val qp = getQueryParam(uri)
            config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifEmpty { "none" }
            config.server = uri.idnHostVal()
            config.serverPort = uri.port.toString()
            config.password = uri.userInfo
            config.method = ScannerConfig.DEFAULT_SECURITY
            getItemFromQuery(config, qp)
            config
        } catch (_: Exception) {
            null
        }
    }
}
