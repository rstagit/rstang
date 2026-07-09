package com.v2ray.ang.rstascanner.fmt

import com.v2ray.ang.rstascanner.ScannerConfig
import com.v2ray.ang.rstascanner.EConfigType
import com.v2ray.ang.rstascanner.ProfileItem
import com.v2ray.ang.rstascanner.util.Utils
import java.net.URI

object TrojanFmt : FmtBase() {
    fun parse(str: String): ProfileItem? {
        return try {
            val config = ProfileItem.create(EConfigType.TROJAN)
            val uri = URI(Utils.fixIllegalUrl(str))
            config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifEmpty { "none" }
            config.server = uri.idnHostVal()
            config.serverPort = uri.port.toString()
            config.password = uri.userInfo
            if (uri.rawQuery.isNullOrEmpty()) {
                config.network = "tcp"
                config.security = ScannerConfig.TLS
                config.insecure = false
            } else {
                val qp = getQueryParam(uri)
                getItemFromQuery(config, qp)
                config.security = qp["security"] ?: ScannerConfig.TLS
            }
            config
        } catch (_: Exception) {
            null
        }
    }

    fun toUri(config: ProfileItem): String {
        val dic = getQueryDic(config)
        return "${EConfigType.TROJAN.protocolScheme}${toUri(config, config.password, dic)}"
    }
}
