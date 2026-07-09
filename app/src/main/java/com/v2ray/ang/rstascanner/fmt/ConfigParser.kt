package com.v2ray.ang.rstascanner.fmt

import com.v2ray.ang.rstascanner.ScannerConfig
import com.v2ray.ang.rstascanner.EConfigType
import com.v2ray.ang.rstascanner.ProfileItem

object ConfigParser {

    fun parse(str: String): ProfileItem? {
        val s = str.trim()
        return when {
            s.startsWith(ScannerConfig.VMESS) -> VmessFmt.parse(s)
            s.startsWith(ScannerConfig.VLESS) -> VlessFmt.parse(s)
            s.startsWith(ScannerConfig.TROJAN) -> TrojanFmt.parse(s)
            s.startsWith(ScannerConfig.SHADOWSOCKS) -> ShadowsocksFmt.parse(s)
            else -> null
        }
    }

    fun replaceServer(originalConfig: String, newIp: String, newPort: String): String? {
        val profile = parse(originalConfig) ?: return null
        profile.server = newIp
        profile.serverPort = newPort
        return toUri(profile)
    }

    fun toUri(profile: ProfileItem): String {
        return when (profile.configType) {
            EConfigType.VMESS -> VmessFmt.toUri(profile)
            EConfigType.VLESS -> VlessFmt.toUri(profile)
            EConfigType.TROJAN -> TrojanFmt.toUri(profile)
            EConfigType.SHADOWSOCKS -> ShadowsocksFmt.toUri(profile)
            else -> ""
        }
    }
}
