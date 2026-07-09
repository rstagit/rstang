package com.v2ray.ang.rstascanner.util

import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder

object Utils {
    fun decode(str: String): String {
        return try {
            String(Base64.decode(str, Base64.NO_WRAP))
        } catch (_: Exception) {
            try {
                String(Base64.decode(str, Base64.DEFAULT))
            } catch (_: Exception) {
                str
            }
        }
    }

    fun encode(str: String, urlSafe: Boolean = false): String {
        val flags = if (urlSafe) Base64.URL_SAFE or Base64.NO_WRAP else Base64.NO_WRAP
        return Base64.encodeToString(str.toByteArray(), flags)
    }

    fun decodeURIComponent(str: String): String {
        return try {
            URLDecoder.decode(str, "UTF-8")
        } catch (_: Exception) {
            str
        }
    }

    fun encodeURIComponent(str: String): String {
        return try {
            URLEncoder.encode(str, "UTF-8").replace("+", "%20")
        } catch (_: Exception) {
            str
        }
    }

    fun fixIllegalUrl(str: String): String {
        return str.replace(" ", "%20")
            .replace("|", "%7C")
    }

    fun getIpv6Address(addr: String): String {
        return if (addr.contains(":") && !addr.startsWith("[")) "[$addr]" else addr
    }
}
