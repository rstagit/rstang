package com.v2ray.ang.rstascanner.fmt

import com.v2ray.ang.rstascanner.EConfigType
import com.v2ray.ang.rstascanner.ProfileItem
import com.v2ray.ang.rstascanner.util.Utils
import java.net.URI

object VlessFmt : FmtBase() {
    fun parse(str: String): ProfileItem? {
        return try {
            val config = ProfileItem.create(EConfigType.VLESS)
            val uri = URI(Utils.fixIllegalUrl(str))
            if (uri.rawQuery.isNullOrEmpty()) return null
            val queryParam = getQueryParam(uri)
            config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifEmpty { "none" }
            config.server = uri.idnHostVal()
            config.serverPort = uri.port.toString()
            config.password = uri.userInfo
            config.method = queryParam["encryption"] ?: "none"
            getItemFromQuery(config, queryParam)
            config
        } catch (_: Exception) {
            null
        }
    }

    fun toUri(config: ProfileItem): String {
        val dic = getQueryDic(config)
        dic["encryption"] = config.method ?: "none"
        return "${EConfigType.VLESS.protocolScheme}${toUri(config, config.password, dic)}"
    }
}
