package com.v2ray.ang.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.v2ray.ang.AngApplication
import com.v2ray.ang.enums.EConfigType
import es.dmoral.toasty.Toasty
import java.io.Serializable
import java.net.URI
import java.util.Locale

val Context.v2RayApplication: AngApplication?
    get() = applicationContext as? AngApplication


fun Context.toast(message: Int) {
    Toasty.normal(this, message).show()
}


fun Context.toast(message: CharSequence) {
    Toasty.normal(this, message).show()
}


fun Context.toastSuccess(message: Int) {
    Toasty.success(this, message, Toast.LENGTH_SHORT, true).show()
}


fun Context.toastSuccess(message: CharSequence) {
    Toasty.success(this, message, Toast.LENGTH_SHORT, true).show()
}


fun Context.toastError(message: Int) {
    Toasty.error(this, message, Toast.LENGTH_SHORT, true).show()
}


fun Context.toastError(message: CharSequence) {
    Toasty.error(this, message, Toast.LENGTH_SHORT, true).show()
}

const val THRESHOLD = 1000L
const val DIVISOR = 1024.0


fun Long.toSpeedString(): String = this.toTrafficString() + "/s"


fun Long.toTrafficString(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var size = this.toDouble()
    var unitIndex = 0
    while (size >= THRESHOLD && unitIndex < units.size - 1) {
        size /= DIVISOR
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
}

val URI.idnHost: String
    get() = host?.replace("[", "")?.replace("]", "").orEmpty()


fun String?.removeWhiteSpace(): String? = this?.replace(" ", "")


fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }


fun String.toLongEx(): Long = toLongOrNull() ?: 0


fun Context.listenForPackageChanges(onetime: Boolean = true, callback: () -> Unit) =
    object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            callback()
            if (onetime) context.unregisterReceiver(this)
        }
    }.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(this, IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(this, IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            })
        }
    }


inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}


inline fun <reified T : Serializable> Intent.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
}


fun CharSequence?.isNotNullEmpty(): Boolean = !this.isNullOrBlank()

fun String.concatUrl(vararg paths: String): String {
    val builder = StringBuilder(this.trimEnd('/'))

    paths.forEach { path ->
        val trimmedPath = path.trim('/')
        if (trimmedPath.isNotEmpty()) {
            builder.append('/').append(trimmedPath)
        }
    }

    return builder.toString()
}


fun String.matchesPattern(regex: Regex?, keyword: String?, ignoreCase: Boolean = true): Boolean {
    if (keyword.isNullOrEmpty()) {
        return true
    }
    return regex?.containsMatchIn(this)
        ?: this.contains(keyword, ignoreCase = ignoreCase)
}


fun EConfigType.isGroupType(): Boolean {
    return this == EConfigType.POLICYGROUP || this == EConfigType.PROXYCHAIN
}


fun EConfigType.isComplexType(): Boolean {
    return this == EConfigType.CUSTOM || this == EConfigType.POLICYGROUP || this == EConfigType.PROXYCHAIN
}
