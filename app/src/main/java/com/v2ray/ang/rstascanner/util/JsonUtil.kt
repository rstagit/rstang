package com.v2ray.ang.rstascanner.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

object JsonUtil {
    private val gson = Gson()
    private val gsonPretty = GsonBuilder().setPrettyPrinting().create()

    fun <T> fromJson(json: String, cls: Class<T>): T? {
        return try {
            gson.fromJson(json, cls)
        } catch (_: Exception) {
            null
        }
    }

    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    fun toJsonPretty(obj: Any): String {
        return gsonPretty.toJson(obj)
    }

    fun parseString(str: String) = try {
        JsonParser.parseString(str)
    } catch (_: Exception) {
        null
    }
}
