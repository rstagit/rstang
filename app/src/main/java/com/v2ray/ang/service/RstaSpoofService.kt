package com.v2ray.ang.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.enums.NotificationChannelType
import com.v2ray.ang.rsta.RstaSpoofEngine
import com.v2ray.ang.util.NotificationHelper


class RstaSpoofService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "START" -> {
                val ip = intent.getStringExtra("IP") ?: ""
                val port = intent.getIntExtra("PORT", 0)
                val sni = intent.getStringExtra("SNI") ?: ""
                val method = intent.getStringExtra("METHOD") ?: ""
                
                NotificationHelper.startForeground(
                    this,
                    NotificationChannelType.CORE_PROXY,
                    getString(R.string.title_rsta_spoof_setting),
                    "Running local spoof engine..."
                )
                
                RstaSpoofEngine.start(ip, port, sni, method)
            }
            "STOP" -> {
                RstaSpoofEngine.stop()
                NotificationHelper.stopForeground(this)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        RstaSpoofEngine.stop()
        super.onDestroy()
    }
}
