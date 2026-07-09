package com.v2ray.ang.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.contracts.ServiceControl
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.MyContextWrapper
import java.lang.ref.SoftReference

class CoreProxyOnlyService : Service(), ServiceControl {
    
    override fun onCreate() {
        super.onCreate()
        LogUtil.i(AppConfig.TAG, "StartCore-Proxy: Service created")
        CoreServiceManager.serviceControl = SoftReference(this)
    }

    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.i(AppConfig.TAG, "StartCore-Proxy: Service command received")
        CoreServiceManager.startCoreLoop(null)
        return START_STICKY
    }

    
    override fun onDestroy() {
        super.onDestroy()
        CoreServiceManager.stopCoreLoop()
    }

    
    override fun getService(): Service {
        return this
    }

    
    override fun startService() {
        
    }

    
    override fun stopService() {
        stopSelf()
    }

    
    override fun vpnProtect(socket: Int): Boolean {
        return true
    }

    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    
    override fun attachBaseContext(newBase: Context?) {
        val context = newBase?.let {
            MyContextWrapper.wrap(newBase, SettingsManager.getLocale())
        }
        super.attachBaseContext(context)
    }
}
