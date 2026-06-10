package com.aurora.aonev3.ui.fragments.scenes

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
import com.aurora.aonev3.data.datapoints.*
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.GroupRepository
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.groups.groupmembers.GroupMemberRepository
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.ui.IconsScenes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class NewSceneViewModel : ViewModel() {
    private val groupRepository = GroupRepository()
    private val groupMemberRepository = GroupMemberRepository()
    private val deviceDatapointRepository = DeviceDatapointRepository()
    private val groupDatapointRepository = GroupDatapointRepository()

    var scene: Scene? = null
    var selectedGroup: Group? = null
    var selectedColour: String? = null
    var selectedIcon: IconsScenes = IconsScenes.NULL

    var isGroupRgb = false
    var isGroupCt = false
    var groupColourTemperatureMax = 0
    var groupColourTemperatureMin = 999

    fun getGroupDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupDatapoint> = groupDatapointRepository.getAllDatapoints(gateway)

    fun getGroupEntity(gateway: NabtoHandler.NabtoGateway, id: Int): Group? = groupRepository.getGroupEntity(gateway, id)

    fun getMembersEntities(gateway: NabtoHandler.NabtoGateway, group: Group): List<GroupMember> = groupMemberRepository.getGroupMemberEntities(gateway, group.id)

    fun getAllDeviceDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<DeviceDatapoint> = deviceDatapointRepository.getAllDeviceDatapoints(gateway)

    suspend fun reportGroupDatapoints(gateway: NabtoHandler.NabtoGateway) {
        try {
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

    fun toggleDeviceOnOff(device: Device, ldev: String = device.ldevs.firstOrNull() ?: "") {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            viewModelScope.launch(Dispatchers.IO) {
                val onOff = SyncHandler
                    .deviceDatapointsList
                    .find {
                        it.parentGateway == gateway.serial
                                && it.id == device.id
                                && it.ldev == ldev
                                && it.key == "onoff"
                    }
                    ?.value as? Boolean ?: false

                try {
                    DevelcoHandler.putDeviceDatapoint(
                        gateway,
                        device.id,
                        ldev,
                        "onoff",
                        !onOff,
                        first = true
                    )
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
                    }
                    err.printStackTrace()
                }
            }
        }
    }

    fun toggleGroupOnOff(group: Group) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            viewModelScope.launch(Dispatchers.IO) {
                val onOff = SyncHandler
                    .groupDatapointsList
                    .find { it.parentGateway == gateway.serial && it.id == group.id && it.key == "onoff" }
                    ?.value as? Boolean ?: false

                try {
                    DevelcoHandler.putGroupDatapoint(
                        gateway,
                        group.id,
                        group.ldevs.first(),
                        "onoff",
                        !onOff,
                        first = true
                    )
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
                    }
                    err.printStackTrace()
                }
            }
        }
    }

    fun turnGroupOn() {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return

            val group = selectedGroup ?: return

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    DevelcoHandler.putGroupDatapoint(
                        gateway,
                        group.id,
                        group.ldevs.first(),
                        "onoff",
                        value = true,
                        first = true
                    )
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
                    }
                    err.printStackTrace()
                }
            }
        }
    }

    fun saveScene(name: String, colour: String, icon: String, completion: () -> Unit) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let
            viewModelScope.launch(Dispatchers.IO) {
                val metadata = JSONObject()
                    .put("icon_colour", colour)
                    .put("icon", icon)

                val newScene = JSONObject()
                    .put("name", name)
                    .put("metadata", metadata.toString())
                    .put("transitionTime", 0)

                if (scene == null) {
                    selectedGroup?.let { group ->
                        try {
                            DevelcoHandler.postGroupScenes(
                                gateway,
                                group.id,
                                newScene
                            )

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
                } else {
                    scene?.let { scene ->
                        try {
                            DevelcoHandler.putGroupScene(
                                gateway,
                                scene.groupId,
                                scene.id,
                                newScene
                            )
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

                completion()
            }
        }
    }

    fun clearViewModel() {
        scene = null
        selectedGroup = null
        selectedColour = null
        selectedIcon = IconsScenes.NULL

        isGroupRgb = false
        isGroupCt = false
        groupColourTemperatureMax = 0
        groupColourTemperatureMin = 999
    }

    fun deleteScene(scene: Scene, completion: () -> Unit) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    DevelcoHandler.deleteGroupScene(
                        gateway,
                        scene.groupId,
                        scene.id
                    )
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
                    }
                    err.printStackTrace()
                }

                completion()
            }
        }
    }
}
