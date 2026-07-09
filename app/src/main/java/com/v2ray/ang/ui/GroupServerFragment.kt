package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.contracts.MainAdapterListener
import com.v2ray.ang.databinding.FragmentGroupServerBinding
import com.v2ray.ang.databinding.ItemQrcodeBinding
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.helper.SimpleItemTouchHelperCallback
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupServerFragment : BaseFragment<FragmentGroupServerBinding>(),
    SwipeRefreshLayout.OnRefreshListener {
    private val ownerActivity: MainActivity
        get() = requireActivity() as MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: MainRecyclerAdapter
    private var itemTouchHelper: ItemTouchHelper? = null
    private val subId: String by lazy { arguments?.getString(ARG_SUB_ID).orEmpty() }

    private val share_method: Array<out String> by lazy {
        ownerActivity.resources.getStringArray(R.array.share_method)
    }
    private val share_method_more: Array<out String> by lazy {
        ownerActivity.resources.getStringArray(R.array.share_method_more)
    }
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (SettingsChangeManager.consumeRestartService() && mainViewModel.isRunning.value == true) {
            ownerActivity.restartV2Ray()
        }
    }

    companion object {
        private const val ARG_SUB_ID = "subscriptionId"
        fun newInstance(subId: String) = GroupServerFragment().apply {
            arguments = Bundle().apply { putString(ARG_SUB_ID, subId) }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentGroupServerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = MainRecyclerAdapter(mainViewModel, ActivityAdapterListener())
        binding.recyclerView.setHasFixedSize(true)
        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_DOUBLE_COLUMN_DISPLAY, false)) {
            binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        } else {
            binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        }
        addCustomDividerToRecyclerView(binding.recyclerView, R.drawable.custom_divider)
        binding.recyclerView.adapter = adapter

        itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(adapter, allowSwipe = false))
        itemTouchHelper?.attachToRecyclerView(binding.recyclerView)

        binding.refreshLayout.isEnabled = false




        mainViewModel.updateListAction.observe(viewLifecycleOwner) { index ->
            if (mainViewModel.subscriptionId != subId) {
                return@observe
            }
            
            adapter.setData(mainViewModel.serversCache, index)
        }

        
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.subscriptionIdChanged(subId)
    }

    
    private fun shareServer(guid: String, profile: ProfileItem, position: Int, shareOptions: List<String>, skip: Int) {
        AlertDialog.Builder(ownerActivity).setItems(shareOptions.toTypedArray()) { _, i ->
            try {
                when (i + skip) {
                    0 -> showQRCode(guid)
                    1 -> share2Clipboard(guid)
                    2 -> shareFullContent(guid)
                    3 -> editServer(guid, profile)
                    4 -> removeServer(guid, position)
                    else -> ownerActivity.toast("else")
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Error when sharing server", e)
            }
        }.show()
    }

    
    private fun showQRCode(guid: String) {
        val ivBinding = ItemQrcodeBinding.inflate(LayoutInflater.from(ownerActivity))
        ivBinding.ivQcode.setImageBitmap(AngConfigManager.share2QRCode(guid))
        if (share_method.isNotEmpty()) {
            ivBinding.ivQcode.contentDescription = share_method[0]
        } else {
            ivBinding.ivQcode.contentDescription = "QR Code"
        }
        AlertDialog.Builder(ownerActivity).setView(ivBinding.root).show()
    }

    
    private fun share2Clipboard(guid: String) {
        if (AngConfigManager.share2Clipboard(ownerActivity, guid) == 0) {
            ownerActivity.toastSuccess(R.string.toast_success)
        } else {
            ownerActivity.toastError(R.string.toast_failure)
        }
    }

    
    private fun shareFullContent(guid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = AngConfigManager.shareFullContent2Clipboard(ownerActivity, guid)
            launch(Dispatchers.Main) {
                if (result == 0) {
                    ownerActivity.toastSuccess(R.string.toast_success)
                } else {
                    ownerActivity.toastError(R.string.toast_failure)
                }
            }
        }
    }

    
    private fun editServer(guid: String, profile: ProfileItem) {
        val activityClass = when (profile.configType) {
            EConfigType.CUSTOM -> ServerCustomConfigActivity::class.java
            EConfigType.POLICYGROUP -> ServerGroupActivity::class.java
            EConfigType.PROXYCHAIN -> ServerProxyChainActivity::class.java
            else -> ServerActivity::class.java
        }

        val intent = Intent(ownerActivity, activityClass)
            .putExtra("guid", guid)
            .putExtra("isRunning", mainViewModel.isRunning.value)
            .putExtra("createConfigType", profile.configType.value)
            .putExtra("subscriptionId", subId)

        launcher.launch(intent)
    }

    
    private fun removeServer(guid: String, position: Int) {
        if (guid == MmkvManager.getSelectServer()) {
            ownerActivity.toast(R.string.toast_action_not_allowed)
            return
        }

        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_CONFIRM_REMOVE)) {
            AlertDialog.Builder(ownerActivity).setMessage(R.string.del_config_comfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    removeServerSub(guid, position)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    
                }
                .show()
        } else {
            removeServerSub(guid, position)
        }
    }

    
    private fun removeServerSub(guid: String, position: Int) {
        mainViewModel.removeServer(guid)
        adapter.removeServerSub(guid, position)
        ownerActivity.refreshGroupTabTitles()
    }

    
    private fun setSelectServer(guid: String) {
        val selected = MmkvManager.getSelectServer()
        if (guid != selected) {
            MmkvManager.setSelectServer(guid)
            val fromPosition = mainViewModel.getPosition(selected.orEmpty())
            val toPosition = mainViewModel.getPosition(guid)
            adapter.setSelectServer(fromPosition, toPosition)

            if (mainViewModel.isRunning.value == true) {
                ownerActivity.restartV2Ray()
            }
        }
    }

    private inner class ActivityAdapterListener : MainAdapterListener {
        override fun onEdit(guid: String, position: Int) {
        }

        override fun onShare(url: String) {
        }

        override fun onRefreshData() {
        }

        override fun onRemove(guid: String, position: Int) {
            removeServer(guid, position)
        }

        override fun onEdit(guid: String, position: Int, profile: ProfileItem) {
            editServer(guid, profile)
        }

        override fun onSelectServer(guid: String) {
            setSelectServer(guid)
        }

        override fun onShare(guid: String, profile: ProfileItem, position: Int, more: Boolean) {
            val isCustom = profile.configType.isComplexType()

            val (shareOptions, skip) = if (more) {
                val options = if (isCustom) share_method_more.asList().takeLast(3) else share_method_more.asList()
                options to if (isCustom) 2 else 0
            } else {
                val options = if (isCustom) share_method.asList().takeLast(1) else share_method.asList()
                options to if (isCustom) 2 else 0
            }

            shareServer(guid, profile, position, shareOptions, skip)
        }
    }

    override fun onRefresh() {
        ownerActivity.importConfigViaSub()
        
    }

    
    fun scrollToSelectedServer() {
        val selectedGuid = MmkvManager.getSelectServer()
        if (selectedGuid.isNullOrEmpty()) {
            ownerActivity.toast(R.string.title_file_chooser)
            return
        }

        
        val serversCache = mainViewModel.serversCache
        val position = serversCache.indexOfFirst { it.guid == selectedGuid }
        val recyclerView = binding.recyclerView

        if (position >= 0) {
            
            val layoutManager = recyclerView.layoutManager as? GridLayoutManager

            if (layoutManager != null) {
                
                
                recyclerView.post {
                    layoutManager.scrollToPositionWithOffset(position, recyclerView.height / 3)
                }
            } else {
                
                recyclerView.smoothScrollToPosition(position)
            }
        } else {
            ownerActivity.toast(R.string.toast_server_not_found_in_group)
        }
    }
}