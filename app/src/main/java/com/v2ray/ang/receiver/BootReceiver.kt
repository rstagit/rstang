package com.v2ray.ang.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.util.LogUtil

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        LogUtil.i(AppConfig.TAG, "BootReceiver received: ${intent?.action}")

        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            LogUtil.w(AppConfig.TAG, "BootReceiver: Invalid context or action")
            return
        }

        if (!MmkvManager.decodeStartOnBoot()) {
            LogUtil.i(AppConfig.TAG, "BootReceiver: Auto-start on boot is disabled")
            return
        }

        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            LogUtil.w(AppConfig.TAG, "BootReceiver: No server selected")
            return
        }

        LogUtil.i(AppConfig.TAG, "BootReceiver: Starting V2Ray service")
        CoreServiceManager.startVService(context)
        SubscriptionUpdater.sync(context)
    }
}
