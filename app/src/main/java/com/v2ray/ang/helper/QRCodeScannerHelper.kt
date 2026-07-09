package com.v2ray.ang.helper

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.v2ray.ang.ui.ScannerActivity


class QRCodeScannerHelper(private val activity: AppCompatActivity) {
    private var scanCallback: ((String?) -> Unit)? = null

    private val scanLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanCallback?.invoke(scanResult)
            } else {
                scanCallback?.invoke(null)
            }
            scanCallback = null
        }

    
    fun launch(onResult: (String?) -> Unit) {
        scanCallback = onResult
        scanLauncher.launch(Intent(activity, ScannerActivity::class.java))
    }
}
