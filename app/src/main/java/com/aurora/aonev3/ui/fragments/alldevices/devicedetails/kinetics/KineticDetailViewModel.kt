package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.kinetics

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.indices
import com.aurora.aonev3.network.deviceDatapoint
import com.aurora.aonev3.network.groupDatapoint
import com.aurora.aonev3.network.groupScenes
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import org.json.JSONArray
import org.json.JSONObject

class KineticDetailViewModel : ViewModel() {
    private val logicCollectionRepository = LogicCollectionRepository()
    var selectedGroup: Group? = null
    val targetGroup = MutableLiveData<Group?>(null)
    val targetMode = MutableLiveData<UpDownMode?>(null)
    val secondaryMode = MutableLiveData<UpDownMode?>(null)
    var previousTargetGroup: Group? = null
    var previousTargetMode: UpDownMode? = null
    var previousSecondaryMode: UpDownMode? = null

    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> =
        logicCollectionRepository.getLogicCollections(gateway)

    fun clearViewModel() {
        selectedGroup = null
        targetGroup.postValue(null)
        targetMode.postValue(null)
        secondaryMode.postValue(null)
        previousTargetGroup = null
        previousTargetMode = null
        previousSecondaryMode = null
    }

    suspend fun updateDevice(name: String, device: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return
        if (name.isNotBlank() && name != device.name) {
            if (gateway.isConnected) {
                try {
                    DevelcoHandler.putDevice(
                        gateway,
                        device.id,
                        JSONObject().put("name", name)
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

    suspend fun deleteLogicCollections(device: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return

        SyncHandler
            .logicCollectionsList
            .filter { logicCollection ->
            val metadata = logicCollection.metadata

            metadata.triggerId == device.id && logicCollection.parentGateway == gateway.serial
        }.forEach {
            try {
                DevelcoHandler.deleteLogicCollection(gateway, it.id)
            } catch (err: VolleyError) {
                App.actionFailed()
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(
                        gateway,
                        CloudHandler.getCredentials().first
                    )
                }
            }
        }
    }

    suspend fun deleteDevice(device: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return

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
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
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
                NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
            }
        }
    }

    suspend fun buildRule(
        device: Device,
        group: Group,
        ldev: String,
        logicCollectionId: Int,
        type: KineticDetailFragment.RuleType
    ) {
        val data: JSONObject
        val actionDatapoint: String
        val triggerLdev: String
        val triggerDatapoint: String

        val gateway = NabtoHandler.selectedGateway ?: return

        when (type) {
            KineticDetailFragment.RuleType.ON -> {
                data = JSONObject().put("value", true)
                actionDatapoint = "/onoff"
                triggerLdev = ldev
                triggerDatapoint = "/Ipressed"
            }
            KineticDetailFragment.RuleType.OFF -> {
                data = JSONObject().put("value", false)
                actionDatapoint = "/onoff"
                triggerLdev = ldev
                triggerDatapoint = "/Opressed"
            }
            KineticDetailFragment.RuleType.UP -> {
                data = JSONObject().put("value", 25)
                actionDatapoint = "/stepup"
                triggerLdev = ldev
                triggerDatapoint = "/Ipressed"
            }
            KineticDetailFragment.RuleType.DOWN -> {
                data = JSONObject().put("value", 25)
                actionDatapoint = "/stepdown"
                triggerLdev = ldev
                triggerDatapoint = "/Opressed"
            }
            KineticDetailFragment.RuleType.COLOUR_TEMPERATURE_UP -> {
                data = JSONObject().put("value", -60)
                actionDatapoint = "/miredstep"
                triggerLdev = ldev
                triggerDatapoint = "/Ipressed"
            }
            KineticDetailFragment.RuleType.COLOUR_TEMPERATURE_DOWN -> {
                data = JSONObject().put("value", 60)
                actionDatapoint = "/miredstep"
                triggerLdev = ldev
                triggerDatapoint = "/Opressed"
            }
            KineticDetailFragment.RuleType.CYCLE_UP -> {
                data = JSONObject().put("cycle", "next")
                actionDatapoint = "/scene"
                triggerLdev = ldev
                triggerDatapoint = "/Ipressed"
            }
            KineticDetailFragment.RuleType.CYCLE_DOWN -> {
                data = JSONObject().put("cycle", "prev")
                actionDatapoint = "/scene"
                triggerLdev = ldev
                triggerDatapoint = "/Opressed"
            }
        }

        val actions = JSONArray()
            .put(
                JSONObject()
                    .put("type", "UpdateResource")
                    .put(
                        "path",
                        when (type) {
                            KineticDetailFragment.RuleType.ON,
                            KineticDetailFragment.RuleType.OFF,
                            KineticDetailFragment.RuleType.UP,
                            KineticDetailFragment.RuleType.DOWN,
                            KineticDetailFragment.RuleType.COLOUR_TEMPERATURE_UP,
                            KineticDetailFragment.RuleType.COLOUR_TEMPERATURE_DOWN -> {
                                groupDatapoint(group, datapoint = actionDatapoint)
                            }
                            KineticDetailFragment.RuleType.CYCLE_UP,
                            KineticDetailFragment.RuleType.CYCLE_DOWN -> {
                                groupScenes(group)
                            }
                        }
                    )
                    .put("data", data)
            )
        val triggers = JSONArray()
            .put(
                JSONObject()
                    .put("type", "ResourceUpdate")
                    .put(
                        "path",
                        deviceDatapoint(device, triggerLdev, triggerDatapoint)
                    )
            )
        val metadata = JSONObject()
            .put("type", type.name.toLowerCase())

        val rule = JSONObject()
            .put("actions", actions)
            .put("triggers", triggers)
            .put("metadata", metadata.toString())

        try {
            DevelcoHandler.postLogicRule(gateway, logicCollectionId, rule)
        } catch (err: VolleyError) {
            App.actionFailed()
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
            }
            err.printStackTrace()
        }
    }
}

