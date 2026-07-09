package com.v2ray.ang.rstascanner.util

object CidrUtil {
    fun expandCidr(cidr: String): List<String> {
        val parts = cidr.trim().split("/")
        if (parts.size != 2) return emptyList()

        val ip = parts[0].trim()
        val prefixLen = parts[1].trim().toIntOrNull() ?: return emptyList()

        if (prefixLen < 0 || prefixLen > 32) return emptyList()

        val ipParts = ip.split(".")
        if (ipParts.size != 4) return emptyList()

        val baseInt = try {
            ipParts.fold(0L) { acc, part ->
                (acc shl 8) or part.toLong()
            }
        } catch (_: Exception) {
            return emptyList()
        }

        val mask = if (prefixLen == 0) 0L else (0xFFFFFFFFL shl (32 - prefixLen)) and 0xFFFFFFFFL
        val networkInt = baseInt and mask
        val count = 1L shl (32 - prefixLen)

        val result = mutableListOf<String>()
        for (i in 0 until count) {
            val hostInt = networkInt + i
            val a = (hostInt shr 24) and 0xFF
            val b = (hostInt shr 16) and 0xFF
            val c = (hostInt shr 8) and 0xFF
            val d = hostInt and 0xFF
            result.add("$a.$b.$c.$d")
        }
        return result
    }

    fun isValidCidr(cidr: String): Boolean {
        val parts = cidr.trim().split("/")
        if (parts.size != 2) return false
        val ip = parts[0].trim()
        val prefix = parts[1].trim().toIntOrNull() ?: return false
        if (prefix < 0 || prefix > 32) return false
        val ipParts = ip.split(".")
        if (ipParts.size != 4) return false
        return ipParts.all { it.toIntOrNull()?.let { v -> v in 0..255 } == true }
    }
}
