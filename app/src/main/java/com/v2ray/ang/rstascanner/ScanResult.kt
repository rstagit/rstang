package com.v2ray.ang.rstascanner

data class ScanResult(
    val ip: String,
    val port: String,
    val delay: Long,
    val config: String,
    val sourceCidr: String = "",
    var uploadSpeed: Double = -1.0,
    var downloadSpeed: Double = -1.0
)
