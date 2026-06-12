package com.aurora.aonev3.ui.fragments.controls

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.DeviceDatapointRepository
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.data.datapoints.GroupDatapointRepository
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.devices.DeviceRepository
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.GroupRepository
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.groups.groupmembers.GroupMemberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ControlsViewModel : ViewModel() {
    private val deviceRepository = DeviceRepository()
    private val deviceDatapointRepository = DeviceDatapointRepository()
    private val groupRepository = GroupRepository()
    private val groupDatapointRepository = GroupDatapointRepository()
    private val groupMemberRepository = GroupMemberRepository()
    var selectedGroup: Group? = null
    var isGroupRgb = false
    var isGroupCt = false
    var groupColourTemperatureMax = 0
    var groupColourTemperatureMin = 999
    var selectedDevice: Device? = null

    fun getDeviceDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<DeviceDatapoint>? {
        val device = selectedDevice

        return if (device != null) {
            deviceDatapointRepository.getAllDeviceDatapoints(gateway)
        } else {
            null
        }
    }

    fun getGroupDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupDatapoint>? {
        val group = selectedGroup

        return if (group != null) {
            groupDatapointRepository.getAllDatapoints(gateway)
        } else {
            null
        }
    }

    fun getGroupMembers(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupMember>? {
        val group = selectedGroup

        return if (group != null) {
            groupMemberRepository.getAllGroupMembers(gateway)
        } else {
            return null
        }
    }

    fun setLevel(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            NabtoHandler.selectedGateway?.let { gateway ->
                if (!gateway.isConnected) return@let

                try {
                    selectedDevice?.let { device ->
                        if (100 - level > 0) {
                            DevelcoHandler.putDeviceDatapoint(
                                gateway,
                                device.id,
                                device.ldevs.firstOrNull() ?: "",
                                "level",
                                100 - level,
                                first = true
                            )
                        } else {
                            DevelcoHandler.putDeviceDatapoint(
                                gateway,
                                device.id,
                                device.ldevs.firstOrNull() ?: "",
                                "movetolevel",
                                100 - level,
                                first = true
                            )
                        }
                    }
                    selectedGroup?.let { group ->
                        if (100 - level > 0) {
                            DevelcoHandler.putGroupDatapoint(
                                gateway,
                                group.id,
                                group.ldevs.firstOrNull() ?: "",
                                "level",
                                100 - level,
                                first = true
                            )
                        } else {
                            DevelcoHandler.putGroupDatapoint(
                                gateway,
                                group.id,
                                group.ldevs.firstOrNull() ?: "",
                                "setlevel",
                                100 - level,
                                first = true
                            )
                        }
                    }
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
                    }
                    err.printStackTrace()
                    App.actionFailed()
                }
            }
        }
    }

    fun setMired(mired: Int) {
        val miredValue = if (mired != 0) mired else 454
        viewModelScope.launch(Dispatchers.IO) {
            NabtoHandler.selectedGateway?.let { gateway ->
                if (!gateway.isConnected) return@let

                try {
                    selectedDevice?.let { device ->
                        DevelcoHandler.putDeviceDatapoint(
                            gateway,
                            device.id,
                            device.ldevs.firstOrNull() ?: "",
                            "mired",
                            1000000 / (((1000000 / miredValue) / 100) * 100),
                            first = true
                        )
                    }
                    selectedGroup?.let { group ->
                        DevelcoHandler.putGroupDatapoint(
                            gateway,
                            group.id,
                            group.ldevs.firstOrNull() ?: "",
                            "mired",
                            1000000 / (((1000000 / miredValue) / 100) * 100),
                            first = true
                        )
                    }
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
                    }
                    err.printStackTrace()
                    App.actionFailed()
                }
            }
        }
    }

    fun reportStates() {
        SyncHandler.syncHandlerCoroutineScope.launch {
            NabtoHandler.selectedGateway?.let { gateway ->
                if (!gateway.isConnected) return@let

                try {
                    selectedDevice?.let { device ->
                        SyncHandler.reportDeviceStates(gateway, device)
                    }
                    selectedGroup?.let { group ->
                        SyncHandler.reportGroupStates(gateway, group)
                    }
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                    }
                    err.printStackTrace()
                }
            }
        }
    }

    fun setColour(hueSat: Pair<Int, Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            NabtoHandler.selectedGateway?.let { gateway ->
                if (!gateway.isConnected) return@let

                try {
                    selectedDevice?.let { device ->
                        DevelcoHandler.putDeviceDatapoint(
                            gateway,
                            device.id,
                            device.ldevs.firstOrNull() ?: "",
                            "sat",
                            hueSat.second,
                            first = true
                        )
                        DevelcoHandler.putDeviceDatapoint(
                            gateway,
                            device.id,
                            device.ldevs.firstOrNull() ?: "",
                            "hue",
                            hueSat.first,
                            first = true
                        )
                    }
                    selectedGroup?.let { group ->
                        DevelcoHandler.putGroupDatapoint(
                            gateway,
                            group.id,
                            group.ldevs.firstOrNull() ?: "",
                            "sat",
                            hueSat.second,
                            first = true
                        )
                        DevelcoHandler.putGroupDatapoint(
                            gateway,
                            group.id,
                            group.ldevs.firstOrNull() ?: "",
                            "hue",
                            hueSat.first,
                            first = true
                        )
                    }
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                    }
                    err.printStackTrace()
                    App.actionFailed()
                }
            }
        }
    }

    suspend fun setHueMoveRate(rate: Int) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            try {
                selectedDevice?.let { device ->
                    DevelcoHandler.putDeviceDatapoint(
                        gateway,
                        device.id,
                        device.ldevs.firstOrNull() ?: "",
                        "moverate",
                        rate,
                        first = true
                    )
                }
                selectedGroup?.let { group ->
                    DevelcoHandler.putGroupDatapoint(
                        gateway,
                        group.id,
                        group.ldevs.firstOrNull() ?: "",
                        "moverate",
                        rate,
                        first = true
                    )
                }
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
                App.actionFailed()
            }
        }
    }

    suspend fun setHueMove(move: Boolean) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            try {
                selectedDevice?.let { device ->
                    DevelcoHandler.putDeviceDatapoint(
                        gateway,
                        device.id,
                        device.ldevs.firstOrNull() ?: "",
                        "huemove",
                        if (move) 1 else 0,
                        first = true
                    )
                }
                selectedGroup?.let { group ->
                    DevelcoHandler.putGroupDatapoint(
                        gateway,
                        group.id,
                        group.ldevs.firstOrNull() ?: "",
                        "huemove",
                        if (move) 1 else 0,
                        first = true
                    )
                }
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
                App.actionFailed()
            }
        }
    }
}
