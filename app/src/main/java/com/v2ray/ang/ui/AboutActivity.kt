package com.v2ray.ang.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.databinding.ActivityAboutBinding
import com.v2ray.ang.util.Utils

class AboutActivity : BaseActivity() {
    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentViewWithToolbar(binding.root, showHomeAsUp = true, title = getString(R.string.title_about))

        binding.layoutSoureCcode.setOnClickListener {
            val url = AppConfig.APP_URL
            Utils.openUri(this, url)
        }

        binding.layoutFeedback.setOnClickListener {
            val url = AppConfig.APP_ISSUES_URL
            Utils.openUri(this, url)
        }

        binding.layoutOssLicenses.setOnClickListener {
            val webView = android.webkit.WebView(this)
            webView.loadUrl("file:///android_asset/licenses.html")
            AlertDialog.Builder(this)
                .setTitle("Open source licenses")
                .setView(webView)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        binding.layoutTgChannel.setOnClickListener {
            val url = AppConfig.TG_CHANNEL_URL
            Utils.openUri(this, url)
        }

        binding.layoutPrivacyPolicy.setOnClickListener {
            val url = AppConfig.APP_PRIVACY_POLICY
            Utils.openUri(this, url)
        }

        "v${BuildConfig.VERSION_NAME} (${CoreNativeManager.getLibVersion()})".also {
            binding.tvVersion.text = it
        }
        BuildConfig.APPLICATION_ID.also {
            binding.tvAppId.text = it
        }
    }
}
