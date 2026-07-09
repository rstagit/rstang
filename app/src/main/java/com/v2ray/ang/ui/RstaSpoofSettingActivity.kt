package com.v2ray.ang.ui

import android.app.AlertDialog
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.v2ray.ang.R
import com.v2ray.ang.helper.MmkvPreferenceDataStore
import com.v2ray.ang.rsta.RstaSpoofEngine


class RstaSpoofSettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithToolbar(
            R.layout.activity_rsta_spoof_setting,
            showHomeAsUp = true,
            title = getString(R.string.title_rsta_spoof_setting)
        )
    }

    class RstaSpoofSettingFragment : PreferenceFragmentCompat() {

        private val connectPort by lazy { findPreference<EditTextPreference>("pref_rsta_spoof_connect_port") }
        private val statusPref by lazy { findPreference<Preference>("pref_rsta_spoof_status") }
        private val viewLogPref by lazy { findPreference<Preference>("pref_rsta_spoof_view_log") }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            preferenceManager.preferenceDataStore = MmkvPreferenceDataStore()
            addPreferencesFromResource(R.xml.pref_rsta_spoof)

            connectPort?.setOnBindEditTextListener { editText ->
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            viewLogPref?.setOnPreferenceClickListener {
                showLogDialog()
                true
            }
        }

        override fun onResume() {
            super.onResume()
            updateStatusSummary()
        }

        private fun updateStatusSummary() {
            val available = RstaSpoofEngine.isAvailable()
            if (!available) {
                statusPref?.summary = getString(R.string.title_rsta_spoof_unavailable)
                return
            }
            
            statusPref?.summary = RstaSpoofEngine.statusSummary()
            
            
            
        }

        private fun showLogDialog() {
            val lines = RstaSpoofEngine.recentLogLines()
            val text = if (lines.isEmpty()) getString(R.string.title_rsta_spoof_log_empty) else lines.joinToString("\n")
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_rsta_spoof_view_log)
                .setMessage(text)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
