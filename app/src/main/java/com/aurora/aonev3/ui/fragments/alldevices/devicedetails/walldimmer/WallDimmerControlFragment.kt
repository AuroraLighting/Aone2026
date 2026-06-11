package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.walldimmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.allowEditing
import com.aurora.aonev3.indices
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.logic.UpdateResourceAction
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.ui.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class WallDimmerControlFragment : WallDimmerInlineFragment() {

    private val wallDimmerControlViewModel: WallDimmerControlViewModel by viewModels()

    private var mLogicCollection: LogicCollection? = null
    private val gateway = NabtoHandler.selectedGateway ?: throw Exception("Invalid gateway")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        val device = viewModel.selectedDevice ?: return

        btnIdentify.visibility = View.GONE

        walldimmerTargetOuterLayout.visibility = View.VISIBLE

        walldimmerTargetLayout.setOnClickListener {
            val action = WallDimmerControlFragmentDirections
                    .actionWallDimmerControlFragmentToWallDimmerControlTargetFragment(
                        wallDimmerControlViewModel.target.value?.id ?: -1
                    )
            findNavController().navigate(action)
        }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>("targetId")
            ?.observe(viewLifecycleOwner) { targetId ->
                wallDimmerControlViewModel.target.postValue(SyncHandler
                    .devicesList
                    .find { it.parentGateway == gateway.serial && it.id == targetId })
            }

        viewModel.group.observe(viewLifecycleOwner, { group ->
            if (group != null) {
                walldimmerTargetLayout.isClickable = allowEditing()
            }
        })

        wallDimmerControlViewModel.target.observe(viewLifecycleOwner) { target ->
            val context = context ?: return@observe

            when {
                target != null -> {
                    walldimmerTargetLayout.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.colorTileActive)
                    tvWalldimmerTarget.setTextColor(
                        ContextCompat.getColor(context, R.color.colorPrimary)
                    )
                    tvWalldimmerTarget.text = target.name
                }
                viewModel.group.value != null -> {
                    walldimmerTargetLayout.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.colorTileInactive)
                    tvWalldimmerTarget.setTextColor(
                        ContextCompat.getColor(context, R.color.colorTextPrimary)
                    )
                    tvWalldimmerTarget.text = getString(R.string.select_inline_dimmer_to_control)
                }
                else -> {
                    walldimmerTargetLayout.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.colorTileInactive)
                    tvWalldimmerTarget.setTextColor(
                        ContextCompat.getColor(context, R.color.colorPrimaryBackground)
                    )
                    tvWalldimmerTarget.text = getString(R.string.select_inline_dimmer_to_control)
                }
            }
        }

        NabtoHandler.selectedGateway?.let { gateway ->
            wallDimmerControlViewModel.getLogicCollections(gateway)
                .observe(viewLifecycleOwner, Observer { logicCollections ->
                    if (mLogicCollection != null) return@Observer
                    mLogicCollection = logicCollections?.toList()?.firstOrNull { logicCollection ->
                        val metadata = logicCollection.metadata

                        metadata.collectionType == CollectionType.WALLDIMMER &&
                                metadata.triggerId == device.id
                    }

                    val allLogicRules = SyncHandler
                        .logicRulesList
                        .filter { logicRule -> logicRule.parentGateway == gateway.serial && logicRule.logicCollectionId == mLogicCollection?.id }

                    for (logicRule in allLogicRules) {
                        val targetId = (logicRule
                            .actions?.get(0) as? UpdateResourceAction)
                            ?.path
                            ?.split("/")
                            ?.mapNotNull { it.toIntOrNull() }
                            ?.firstOrNull() ?: continue

                        val targetDevice = SyncHandler
                            .devicesList
                            .firstOrNull { it.parentGateway == logicRule.parentGateway && it.id == targetId } ?: continue
                        wallDimmerControlViewModel.existingTarget = targetDevice
                        wallDimmerControlViewModel.target.postValue(targetDevice)
                    }
                })
        }

        walldimmerTargetLayout.isClickable = allowEditing()
    }

    override fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val device = viewModel.selectedDevice ?: return@OnClickListener
            binding.btnSave.isEnabled = false
            activity?.layoutGreyOut?.visibility = View.VISIBLE

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val target = wallDimmerControlViewModel.target.value
                viewModel.updateDeviceName(device)

                if (target != null && wallDimmerControlViewModel.existingTarget != target) {
                    wallDimmerControlViewModel.saveLogic(device, target)
                }

                activity?.runOnUiThread {
                    binding.btnSave.isEnabled = true
                    activity?.layoutGreyOut?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun btnDeleteClickListener() {
        val device = viewModel.selectedDevice ?: return
        activity?.layoutGreyOut?.visibility = View.VISIBLE
        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val logicCollectionsToDelete = ArrayList<Int>()
                SyncHandler
                    .logicCollectionsList
                    .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
                    val metadata = logicCollection.metadata

                    if (metadata.collectionType == CollectionType.WALLDIMMER &&
                        metadata.triggerId == device.id
                    ) {
                        logicCollectionsToDelete.add(logicCollection.id)
                    }
                }

                logicCollectionsToDelete.forEach {
                    try {
                        DevelcoHandler.deleteLogicCollection(
                            gateway,
                            it
                        )
                    } catch (err: VolleyError) {
                        App.actionFailed()
                        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                            gateway.isConnected = false
                            val credentials = CloudHandler.getCredentials()
                            if (credentials.first.isEmpty()) {
                                activity?.finishAffinity()
                                startActivity(Intent(context, MainActivity::class.java))
                            }
                            NabtoHandler.openTunnel(gateway, credentials.first)
                        }

                    }
                }

                SyncHandler
                    .groupsList
                    .filter {
                        it.parentGateway == gateway.serial
                    }
                    .forEach { group ->
                    val metadata = group.metadata
                    val virtualMembers = metadata.optJSONArray("virtual_members") ?: JSONArray()
                    val newVirtualMembers = JSONArray()
                    var updateGroup = false

                    for (i in virtualMembers.indices()) {
                        val virtualMember = virtualMembers.optJSONObject(i)

                        if (virtualMember.optInt("id") != device.id) {
                            newVirtualMembers.put(virtualMember)
                        } else {
                            updateGroup = true
                        }
                    }

                    if (updateGroup) {
                        metadata.put("virtual_members", newVirtualMembers)
                        try {
                            DevelcoHandler.putGroup(
                                gateway,
                                group.id,
                                JSONObject()
                                    .put("metadata", metadata.toString())
                            )
                        } catch (err: VolleyError) {
                            App.actionFailed()
                            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                gateway.isConnected = false
                                val credentials = CloudHandler.getCredentials()
                                if (credentials.first.isEmpty()) {
                                    activity?.finishAffinity()
                                    startActivity(Intent(context, MainActivity::class.java))
                                }
                                NabtoHandler.openTunnel(gateway, credentials.first)
                            }

                        }
                    }
                }

                try {
                    DevelcoHandler.deleteDevice(gateway, device.id)
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        val credentials = CloudHandler.getCredentials()
                        if (credentials.first.isEmpty()) {
                            activity?.finishAffinity()
                            startActivity(Intent(context, MainActivity::class.java))
                        }
                        NabtoHandler.openTunnel(gateway, credentials.first)
                    }

                }

                activity?.runOnUiThread {
                    activity?.layoutGreyOut?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WallDimmerControlFragment"
        fun newInstance() =
            WallDimmerControlFragment()
    }
}