package com.v2ray.ang.rstascanner

enum class EConfigType(val protocolScheme: String) {
    VMESS(ScannerConfig.VMESS),
    VLESS(ScannerConfig.VLESS),
    TROJAN(ScannerConfig.TROJAN),
    SHADOWSOCKS(ScannerConfig.SHADOWSOCKS),
    SOCKS(ScannerConfig.SOCKS),
    CUSTOM("custom://")
}
