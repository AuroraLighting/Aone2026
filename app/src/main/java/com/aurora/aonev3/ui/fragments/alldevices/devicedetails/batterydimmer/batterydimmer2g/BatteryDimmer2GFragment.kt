package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer2g

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.allowEditing
import com.aurora.aonev3.indices
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.DeviceDetailFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g.BatteryDimmerMode
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g.SECONDARY_MODE_2_KEY
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g.SECONDARY_MODE_KEY
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

const val TARGET_GROUP_KEY = "groupId"
const val TARGET_GROUP_2_KEY = "group2Id"

class BatteryDimmer2GFragment : DeviceDetailFragment() {

    private val batteryDimmerViewModel: BatteryDimmer2GViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)

        btnIdentify.visibility = View.GONE
        batteryDimmerTargetOuterLayout.visibility = View.VISIBLE
        batteryDimmerModeOuterLayout.visibility = View.VISIBLE
        batteryDimmerTarget2OuterLayout.visibility = View.VISIBLE
        batteryDimmerMode2OuterLayout.visibility = View.VISIBLE

        tvBatteryDimmerModeTitle.text = getString(R.string.left_secondary_mode)

        batteryDimmerModeLayout.setOnClickListener {
            val action =
                BatteryDimmer2GFragmentDirections.actionBatteryDimmer2GFragmentToBatteryDimmerSecondaryModeFragment(
                    SECONDARY_MODE_KEY,
                    batteryDimmerViewModel.mode.value ?: BatteryDimmerMode.NONE
                )
            findNavController().navigate(action)
        }

        batteryDimmerMode2Layout.setOnClickListener {
            val action =
                BatteryDimmer2GFragmentDirections.actionBatteryDimmer2GFragmentToBatteryDimmerSecondaryModeFragment(
                    SECONDARY_MODE_2_KEY,
                    batteryDimmerViewModel.mode2.value ?: BatteryDimmerMode.NONE
                )
            findNavController().navigate(action)
        }

        batteryDimmerTargetLayout.setOnClickListener {
            val action =
                BatteryDimmer2GFragmentDirections.actionBatteryDimmer2GFragmentToBatteryDimmerTargetGroupSelectorFragment(
                    TARGET_GROUP_KEY,
                    batteryDimmerViewModel.targetGroup.value?.id ?: -1
                )
            findNavController().navigate(action)
        }

        batteryDimmerTarget2Layout.setOnClickListener {
            val action =
                BatteryDimmer2GFragmentDirections.actionBatteryDimmer2GFragmentToBatteryDimmerTargetGroupSelectorFragment(
                    TARGET_GROUP_2_KEY,
                    batteryDimmerViewModel.targetGroup2.value?.id ?: -1
                )
            findNavController().navigate(action)
        }

        batteryDimmerViewModel.targetGroup.observe(viewLifecycleOwner, { group ->
            setDimmerModeColour(group, batteryDimmerViewModel.mode.value, batteryDimmerModeLayout, tvBatteryDimmerMode)
            if (group != null) {
                tvBatteryDimmerTarget.text = group.name
                batteryDimmerModeLayout.isClickable = allowEditing()
                batteryDimmerTargetLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                tvBatteryDimmerTarget.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            } else {
                tvBatteryDimmerTarget.text = getString(R.string.select_target_space)
                batteryDimmerModeLayout.isClickable = false
                batteryDimmerTargetLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvBatteryDimmerTarget.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
                )
            }
        })

        batteryDimmerViewModel.targetGroup2.observe(viewLifecycleOwner, { group ->
            setDimmerModeColour(group, batteryDimmerViewModel.mode2.value, batteryDimmerMode2Layout, tvBatteryDimmerMode2)
            if (group != null) {
                tvBatteryDimmerTarget2.text = group.name
                batteryDimmerMode2Layout.isClickable = allowEditing()
                batteryDimmerTarget2Layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                tvBatteryDimmerTarget2.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            } else {
                tvBatteryDimmerTarget2.text = getString(R.string.select_target_space)
                batteryDimmerMode2Layout.isClickable = false
                batteryDimmerTarget2Layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvBatteryDimmerTarget2.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
                )
            }
        })

        batteryDimmerViewModel.mode.observe(viewLifecycleOwner) { mode ->
            setDimmerModeColour(batteryDimmerViewModel.targetGroup.value, mode, batteryDimmerModeLayout, tvBatteryDimmerMode)
            tvBatteryDimmerMode.text = mode.displayName
        }

        batteryDimmerViewModel.mode2.observe(viewLifecycleOwner) { mode ->
            setDimmerModeColour(batteryDimmerViewModel.targetGroup2.value, mode, batteryDimmerMode2Layout, tvBatteryDimmerMode2)
            tvBatteryDimmerMode2.text = mode.displayName
        }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<BatteryDimmerMode>(SECONDARY_MODE_KEY)
            ?.observe(viewLifecycleOwner) { mode ->
                batteryDimmerViewModel.mode.postValue(mode)
            }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<BatteryDimmerMode>(SECONDARY_MODE_2_KEY)
            ?.observe(viewLifecycleOwner) { mode ->
                batteryDimmerViewModel.mode2.postValue(mode)
            }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int?>(TARGET_GROUP_KEY)
            ?.observe(viewLifecycleOwner) { id ->
                val gateway = NabtoHandler.selectedGateway ?: return@observe
                val group = SyncHandler.groupsList.find { it.parentGateway == gateway.serial && it.id == id}
                batteryDimmerViewModel.targetGroup.postValue(group)
            }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int?>(TARGET_GROUP_2_KEY)
            ?.observe(viewLifecycleOwner) { id ->
                val gateway = NabtoHandler.selectedGateway ?: return@observe
                val group = SyncHandler.groupsList.find { it.parentGateway == gateway.serial && it.id == id}
                batteryDimmerViewModel.targetGroup2.postValue(group)
            }

        NabtoHandler.selectedGateway?.let { gateway ->
            batteryDimmerViewModel.getLogicCollections(gateway).observe(viewLifecycleOwner) {
                val device = viewModel.selectedDevice ?: return@observe

                batteryDimmerViewModel.loadExistingLogic(device, it)
            }
        }

        if (!allowEditing()) {
            batteryDimmerTargetLayout.isClickable = false
            batteryDimmerModeLayout.isClickable = false
            batteryDimmerTarget2Layout.isClickable = false
            batteryDimmerMode2Layout.isClickable = false
        }
    }

    override fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val device = viewModel.selectedDevice ?: return@OnClickListener
            binding.btnSave.isEnabled = false
            activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.VISIBLE

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val mode = batteryDimmerViewModel.mode.value
                val mode2 = batteryDimmerViewModel.mode2.value
                viewModel.updateDeviceName(device)

                if (mode != batteryDimmerViewModel.existingMode ||
                    mode2 != batteryDimmerViewModel.existingMode2) {
                    batteryDimmerViewModel.saveLogic(device)
                }

                activity?.runOnUiThread {
                    binding.btnSave.isEnabled = true
                    activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun btnDeleteClickListener() {
        val device = viewModel.selectedDevice ?: return
        activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.VISIBLE
        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val logicCollectionsToDelete = ArrayList<Int>()
                SyncHandler
                    .logicCollectionsList
                    .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
                    val metadata = logicCollection.metadata

                    if (metadata.collectionType == CollectionType.BATTERY_DIMMER_2 &&
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
                    .filter { it.parentGateway == gateway.serial }
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
                    activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun setDimmerModeColour(group: Group?, mode: BatteryDimmerMode?, layout: MaterialCardView, textView: TextView) {
        when {
            group == null -> {
                layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                textView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimaryBackground
                    )
                )
            }
            mode == null || mode == BatteryDimmerMode.NONE -> {
                layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                textView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorTextPrimary
                    )
                )
            }
            else -> {
                layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                textView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimary
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "BatteryDimmer2GFragment"
        fun newInstance() =
            BatteryDimmer2GFragment()
    }
}