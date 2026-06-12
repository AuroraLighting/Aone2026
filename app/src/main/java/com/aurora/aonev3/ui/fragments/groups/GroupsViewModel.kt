package com.aurora.aonev3.ui.fragments.groups

import com.aurora.aonev3.synthetic.*
import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.NoConnectionError
import com.android.volley.TimeoutError
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
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class GroupsViewModel : ViewModel() {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private val groupRepository = GroupRepository()
    private val groupMembersRepository = GroupMemberRepository()
    private val deviceRepository = DeviceRepository()
    private val deviceDatapointRepository = DeviceDatapointRepository()
    private val logicCollectionRepository = LogicCollectionRepository()
    private val groupDatapointRepository = GroupDatapointRepository()
    var selectedGroup: Group? = null

    fun getGroups(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Group> = groupRepository.getAllGroups(gateway)

    fun getGroupMembers(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupMember> = groupMembersRepository.getAllGroupMembers(gateway)

    fun getDevices(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Device> = deviceRepository.getAllDevices(gateway)

    fun getDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupDatapoint> = groupDatapointRepository.getAllDatapoints(gateway)

    fun getLogicCollection(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> = logicCollectionRepository.getLogicCollections(gateway)

    fun getDeviceDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<DeviceDatapoint> = deviceDatapointRepository.getAllDeviceDatapoints(gateway)

    fun toggleOnOff(group: Group, activity: Activity) {
        NabtoHandler.selectedGateway?.let{ gateway ->
            if (!gateway.isConnected) return@let

            viewModelScope.launch(Dispatchers.IO) {
                val groupOnOff = SyncHandler
                    .groupDatapointsList
                    .find { it.parentGateway == gateway.serial && it.id == group.id && it.key == "onoff" }
                    ?.value as? Boolean ?: false
                val groupMemberIds = SyncHandler.groupMembersList.filter { it.parentGateway == group.parentGateway && it.groupId == group.id && !it.isVirtualMember }.map { it.deviceId }
                val devices = SyncHandler.devicesList.toList().filter { it.parentGateway == group.parentGateway && it.id in groupMemberIds }
                val devicesOnOff = SyncHandler.deviceDatapointsList.filter { deviceDatapoint ->
                    deviceDatapoint.parentGateway == group.parentGateway &&
                            deviceDatapoint.id in groupMemberIds &&
                            devices.find { deviceDatapoint.id == it.id }?.online == true
                }.mapNotNull { it.value as? Boolean }.any { it }

                val onOff = if (devices.any { !it.online }) {
                    devicesOnOff
                } else {
                    groupOnOff
                }
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
                    App.actionFailed()
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
                    DevelcoHandler.putGroupDatapoint(
                        gateway,
                        group.id,
                        group.ldevs.firstOrNull() ?: "",
                        "reportonoff",
                        value = true,
                        first = true
                    )
                    App.requestReviewIfAppropriate(activity)
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
                }, 500)
            }
        }
    }

    suspend fun getGateways(): ArrayList<NabtoHandler.NabtoGateway> {
        try {
            CloudHandler.getGateways()
        } catch (ex: NoConnectionError) {
            crashlytics.recordException(ex)
        } catch (ex: TimeoutError) {
            crashlytics.recordException(ex)
        }
        return NabtoHandler.nabtoGateways
    }
}
