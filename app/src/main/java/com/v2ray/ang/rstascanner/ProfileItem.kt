package com.v2ray.ang.rstascanner

data class ProfileItem(
    val configType: EConfigType,
    var remarks: String = "",
    var server: String? = null,
    var serverPort: String? = null,
    var password: String? = null,
    var method: String? = null,
    var flow: String? = null,
    var network: String? = null,
    var headerType: String? = null,
    var host: String? = null,
    var path: String? = null,
    var seed: String? = null,
    var kcpMtu: Int? = null,
    var kcpTti: Int? = null,
    var mode: String? = null,
    var serviceName: String? = null,
    var authority: String? = null,
    var xhttpMode: String? = null,
    var xhttpExtra: String? = null,
    var security: String? = null,
    var sni: String? = null,
    var fingerPrint: String? = null,
    var alpn: String? = null,
    var insecure: Boolean? = null,
    var publicKey: String? = null,
    var shortId: String? = null,
    var spiderX: String? = null,
    var quicSecurity: String? = null,
    var quicKey: String? = null,
    var echConfigList: String? = null,
    var verifyPeerCertByName: String? = null,
    var pinnedCA256: String? = null,
    var mldsa65Verify: String? = null,
    var finalMask: String? = null,
) {
    companion object {
        fun create(type: EConfigType) = ProfileItem(configType = type)
    }
}
