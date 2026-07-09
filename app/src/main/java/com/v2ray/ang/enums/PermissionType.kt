package com.v2ray.ang.enums

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi


enum class PermissionType {
    
    CAMERA {
        override fun getPermission(): String = Manifest.permission.CAMERA
    },

    
    POST_NOTIFICATIONS {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun getPermission(): String = Manifest.permission.POST_NOTIFICATIONS
    };

    
    abstract fun getPermission(): String

    
    fun getLabel(): String {
        return when (this) {
            CAMERA -> "Camera"
            POST_NOTIFICATIONS -> "Notification"
        }
    }
}