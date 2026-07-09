package com.v2ray.ang.rstascanner.fmt

import com.v2ray.ang.rstascanner.EConfigType
import com.v2ray.ang.rstascanner.ProfileItem
import com.v2ray.ang.rstascanner.util.Utils
import java.net.URI

object ShadowsocksFmt : FmtBase() {
    fun parse(str: String): ProfileItem? {
        return parseSip002(str) ?: parseLegacy(str)
    }

    private fun parseSip002(str: String): ProfileItem? {
        return try {
            val config = ProfileItem.create(EConfigType.SHADOWSOCKS)
            val uri = URI(Utils.fixIllegalUrl(str))
            val host = uri.idnHostVal()
            if (host.isEmpty() || uri.port <= 0 || uri.userInfo.isNullOrEmpty()) return null
            config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).ifEmpty { "none" }
            config.server = host
            config.serverPort = uri.port.toString()
            val userInfo = if (uri.userInfo.contains(":")) {
                uri.userInfo.split(":", limit = 2)
            } else {
                Utils.decode(uri.userInfo).split(":", limit = 2)
            }
            if (userInfo.size == 2) {
                config.method = userInfo[0]
                config.password = userInfo[1]
            }
            config
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLegacy(str: String): ProfileItem? {
        return try {
            val config = ProfileItem.create(EConfigType.SHADOWSOCKS)
            var result = str.removePrefix(EConfigType.SHADOWSOCKS.protocolScheme)
            val hashIdx = result.indexOf("#")
            if (hashIdx > 0) {
                config.remarks = try { Utils.decodeURIComponent(result.substring(hashIdx + 1)) } catch (_: Exception) { "" }
                result = result.substring(0, hashIdx)
            }
            val atIdx = result.indexOf("@")
            result = if (atIdx > 0) {
                Utils.decode(result.substring(0, atIdx)) + result.substring(atIdx)
            } else {
                Utils.decode(result)
            }
            val pattern = "^(.+?):(.*)@(.+?):(\\d+?)/?$".toRegex()
            val m = pattern.matchEntire(result) ?: return null
            config.server = m.groupValues[3].removeSurrounding("[", "]")
            config.serverPort = m.groupValues[4]
            config.password = m.groupValues[2]
            config.method = m.groupValues[1].lowercase()
            config
        } catch (_: Exception) {
            null
        }
    }

    fun toUri(config: ProfileItem): String {
        val pw = "${config.method}:${config.password}"
        return "${EConfigType.SHADOWSOCKS.protocolScheme}${toUri(config, Utils.encode(pw, true), null)}"
    }
}
