package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.DeviceDetailFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_device_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

const val SECONDARY_MODE_KEY = "mode"
const val SECONDARY_MODE_2_KEY = "mode2"

class BatteryDimmer1GFragment : DeviceDetailFragment() {

    private val batteryDimmerViewModel: BatteryDimmer1GViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)

        btnIdentify.visibility = View.GONE
        batteryDimmerModeOuterLayout.visibility = View.VISIBLE

        batteryDimmerModeLayout.setOnClickListener {
            val action =
                BatteryDimmer1GFragmentDirections.actionBatteryDimmer1GFragmentToBatteryDimmerSecondaryModeFragment(
                    SECONDARY_MODE_KEY,
                    batteryDimmerViewModel.mode.value ?: BatteryDimmerMode.NONE
                )
            findNavController().navigate(action)
        }


        viewModel.group.observe(viewLifecycleOwner, { group ->
            setDimmerModeColour()
            if (group != null) {
                batteryDimmerModeLayout.isClickable = allowEditing()
            }
        })

        batteryDimmerViewModel.mode.observe(viewLifecycleOwner) { mode ->
            setDimmerModeColour()
            tvBatteryDimmerMode.text = mode.displayName
        }

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<BatteryDimmerMode>(SECONDARY_MODE_KEY)
            ?.observe(viewLifecycleOwner) { mode ->
                batteryDimmerViewModel.mode.postValue(mode)
            }

        NabtoHandler.selectedGateway?.let { gateway ->
            batteryDimmerViewModel.getLogicCollections(gateway).observe(viewLifecycleOwner) {
                val device = viewModel.selectedDevice ?: return@observe

                batteryDimmerViewModel.loadExistingLogic(device, it)
            }
        }

        batteryDimmerModeLayout.isClickable = allowEditing()
    }

    private fun setDimmerModeColour() {
        val group = viewModel.group.value
        when {
            group == null -> {
                batteryDimmerModeLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvBatteryDimmerMode.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimaryBackground
                    )
                )
            }
            batteryDimmerViewModel.mode.value == BatteryDimmerMode.NONE -> {
                batteryDimmerModeLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvBatteryDimmerMode.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorTextPrimary
                    )
                )
            }
            else -> {
                batteryDimmerModeLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                tvBatteryDimmerMode.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimary
                    )
                )
            }
        }
    }

    override fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val device = viewModel.selectedDevice ?: return@OnClickListener
            btnSave.isEnabled = false
            activity?.layoutGreyOut?.visibility = View.VISIBLE

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val mode = batteryDimmerViewModel.mode.value
                viewModel.updateDeviceName(device)

                if (mode != batteryDimmerViewModel.existingMode) {
                    viewModel.group.value?.let { group ->
                        batteryDimmerViewModel.saveLogic(
                            device,
                            group
                        )
                    }
                }

                activity?.runOnUiThread {
                    btnSave.isEnabled = true
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
                    .filter { it.parentGateway == gateway.serial }
                    .forEach { logicCollection ->
                        val metadata = logicCollection.metadata

                        if (metadata.collectionType == CollectionType.BATTERY_DIMMER
                            && metadata.triggerId == device.id) {
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

                SyncHandler.groupsList.filter { it.parentGateway == gateway.serial }.forEach { group ->
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
        private const val TAG = "BatteryDimmer1GFragment"
        fun newInstance() =
            BatteryDimmer1GFragment()
    }
}