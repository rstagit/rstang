package com.v2ray.ang.rstascanner

import com.google.gson.annotations.SerializedName

data class VmessQRCode(
    @SerializedName("v") var v: String = "",
    @SerializedName("ps") var ps: String = "",
    @SerializedName("add") var add: String = "",
    @SerializedName("port") var port: String = "",
    @SerializedName("id") var id: String = "",
    @SerializedName("aid") var aid: String = "0",
    @SerializedName("scy") var scy: String = "",
    @SerializedName("net") var net: String = "",
    @SerializedName("type") var type: String = "",
    @SerializedName("host") var host: String = "",
    @SerializedName("path") var path: String = "",
    @SerializedName("tls") var tls: String = "",
    @SerializedName("sni") var sni: String = "",
    @SerializedName("fp") var fp: String = "",
    @SerializedName("alpn") var alpn: String = "",
    @SerializedName("insecure") var insecure: String = "",
    @SerializedName("vcn") var vcn: String = "",
    @SerializedName("pcs") var pcs: String = "",
)
