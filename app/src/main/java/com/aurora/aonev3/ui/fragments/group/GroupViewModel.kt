package com.aurora.aonev3.ui.fragments.group

import com.aurora.aonev3.synthetic.*
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.DeviceDatapointRepository
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.data.datapoints.GroupDatapointRepository
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.GroupRepository
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.groups.groupmembers.GroupMemberRepository
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.groups.scenes.SceneRepository
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import com.aurora.aonev3.data.logic.NewLogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.rules.LogicRuleRepository
import com.aurora.aonev3.logic.CollectionMetadata
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class GroupViewModel : ViewModel() {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val groupRepository = GroupRepository()
    private val groupMemberRepository = GroupMemberRepository()
    private val deviceDatapointRepository = DeviceDatapointRepository()
    private val groupDatapointRepository = GroupDatapointRepository()
    private val sceneRepository = SceneRepository()
    private val logicCollectionRepository = LogicCollectionRepository()
    private val logicRuleRepository = LogicRuleRepository()
    val selectedGroup: MutableLiveData<Group> = MutableLiveData()

    fun getAllGroups(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Group> =
        groupRepository.getAllGroups(gateway)

    fun getGroupDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupDatapoint> =
        groupDatapointRepository.getAllDatapoints(gateway)

    fun getMembers(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupMember> =
        groupMemberRepository.getAllGroupMembers(gateway)

    fun getAllDeviceDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<DeviceDatapoint> =
        deviceDatapointRepository.getAllDeviceDatapoints(gateway)

    fun getScenes(
        gateway: NabtoHandler.NabtoGateway,
        group: Group
    ): MutableLiveDataArrayList<Scene> = sceneRepository.getScenes(gateway, group)

    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> =
        logicCollectionRepository.getLogicCollections(gateway)

    fun getLogicRules(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicRule> =
        logicRuleRepository.getAllLogicRules(gateway)

    suspend fun deleteGroup(group: Group) {
        val gateway = NabtoHandler.selectedGateway ?: return
        val groupsNestedIn = group.findGroupsNestedIn(ArrayList())

        groupsNestedIn.forEach { parentGroup ->
            val metadata = parentGroup.metadata
            val nestedGroups = metadata.optJSONArray("nested_groups") ?: JSONArray()

            val nestedGroupsArray = nestedGroups.toIntArray()
            val newNestedGroups = nestedGroupsArray.filter { it != group.id }

            metadata.put("nested_groups", JSONArray(newNestedGroups))

            try {
                DevelcoHandler.putGroup(
                    gateway,
                    parentGroup.id,
                    JSONObject().put("metadata", metadata.toString())
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

        try {
            DevelcoHandler.deleteGroup(gateway, group.id)
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

    fun reportStates() {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            SyncHandler.syncHandlerCoroutineScope.launch {
                try {
                    selectedGroup.value?.let { SyncHandler.reportGroupStates(gateway, it) }
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

    suspend fun triggerScene(scene: Scene) {
        try {
            NabtoHandler.selectedGateway?.let { gateway ->
                if (!gateway.isConnected) return@let

                DevelcoHandler.putGroupScenes(
                    gateway,
                    scene.groupId,
                    JSONObject().put("id", scene.id),
                    first = true
                )
                selectedGroup.value?.let { SyncHandler.reportGroupStates(gateway, it) }
            }
        } catch (err: VolleyError) {
            App.actionFailed()
            NabtoHandler.selectedGateway?.let { gateway ->
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(
                        gateway,
                        CloudHandler.getCredentials().first
                    )
                }
            }
            err.printStackTrace()
            crashlytics.recordException(err)
        }
    }

    suspend fun toggleOnOff(group: Group, onOff: Boolean) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            try {
                SyncHandler
                    .groupDatapoints
                    .replace(
                        GroupDatapoint(
                            gateway.serial,
                            group.id,
                            group.ldevs.firstOrNull() ?: "",
                            "onoff",
                            !onOff,
                            ""
                        )
                    )
            } catch (ex: Exception) {
                ex.printStackTrace()
                crashlytics.recordException(ex)
            }
            try {
                DevelcoHandler.putGroupDatapoint(
                    gateway,
                    group.id,
                    group.ldevs.firstOrNull() ?: "",
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

            Handler(Looper.getMainLooper()).postDelayed({
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = DevelcoHandler.getGroupDatapoint(
                            gateway,
                            group.id,
                            group.ldevs.firstOrNull() ?: "",
                            "onoff",
                            first = true
                        ).optJSONObject("body") ?: JSONObject()

                        try {
                            SyncHandler
                                .groupDatapoints
                                .replace(
                                    GroupDatapoint(
                                        gateway.serial,
                                        group.id,
                                        group.ldevs.firstOrNull() ?: "",
                                        "onoff",
                                        response.opt("value"),
                                        response.optString("lastUpdated")
                                    )
                                )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            crashlytics.recordException(ex)
                        }
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
            }, 500)
        }
    }

    suspend fun toggleOnOff(group: Group) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            val onOff = SyncHandler
                .groupDatapointsList
                .find { it.parentGateway == gateway.serial && it.id == group.id && it.key == "onoff" }
                ?.value as? Boolean ?: false
            try {
                DevelcoHandler.putGroupDatapoint(
                    gateway,
                    group.id,
                    group.ldevs.firstOrNull() ?: "",
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

    suspend fun toggleOnOff(device: Device, ldev: String = device.ldevs.firstOrNull() ?: "") {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

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

    fun toggleLock(device: Device, key: String) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            viewModelScope.launch(Dispatchers.IO) {
                val isLocked = SyncHandler
                    .deviceDatapointsList
                    .find {
                        it.parentGateway == gateway.serial
                                && it.id == device.id
                                && it.ldev == "lock"
                                && it.key == key
                    }
                    ?.value as? Boolean ?: false
                try {
                    DevelcoHandler.putDeviceDatapoint(
                        gateway,
                        device.id,
                        "lock",
                        key,
                        !isLocked,
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

    suspend fun createDynamicEventCollection(): Int? {
        val gateway = NabtoHandler.selectedGateway ?: return null
        val group = selectedGroup.value ?: return null

        val existingLogicCollection = logicCollectionRepository.getLogicCollection(gateway) {
            it.parentGateway == gateway.serial &&
                    it.metadata.parentSpace == group.id &&
                    it.metadata.collectionType == CollectionType.DYNAMIC_EVENT
        }

        existingLogicCollection?.let {
            return it.id
        }

        val collectionMetadata = CollectionMetadata(
            collectionType = CollectionType.DYNAMIC_EVENT,
            parentSpace = group.id
        )
        val newLogicCollection = NewLogicCollection(
            name = App.context.getString(R.string.dynamic_event_name, group.name),
            metadata = collectionMetadata
        ).toJSONObject()

        val response = DevelcoHandler.postLogicCollection(gateway, newLogicCollection)

        return response.optJSONObject("body")?.optInt("id")
    }
}
