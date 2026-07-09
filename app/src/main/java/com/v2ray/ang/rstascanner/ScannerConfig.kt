package com.v2ray.ang.rstascanner

object ScannerConfig {
    const val VMESS = "vmess://"
    const val VLESS = "vless://"
    const val TROJAN = "trojan://"
    const val SHADOWSOCKS = "ss://"
    const val SOCKS = "socks://"
    const val SOCKS5 = "socks5://"

    const val DELAY_TEST_URL = "http://www.gstatic.com/generate_204"
    const val DEFAULT_PORT = "443"
    const val DEFAULT_SECURITY = "auto"
    const val TLS = "tls"
    const val REALITY = "reality"
    const val NONE = "none"

    const val SCAN_CONCURRENCY = 16
    const val TCP_TIMEOUT_MS = 5000
}
