package com.aurora.aonev3.ui.fragments.alldevices

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.devices.DeviceRepository
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.GroupRepository
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.groups.groupmembers.GroupMemberRepository
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import org.json.JSONObject

class AllDevicesViewModel : ViewModel() {
    private val deviceRepository = DeviceRepository()
    private val groupMemberRepository = GroupMemberRepository()
    private val groupRepository = GroupRepository()
    private val collectionRepository = LogicCollectionRepository()
    val group: MutableLiveData<Group?> = MutableLiveData(null)
    var selectedDevice: Device? = null
    var selectedGroup: Group? = null
    var deviceName: String = ""

    fun getDevices(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Device> = deviceRepository.getAllDevices(gateway)

    fun getGroupMembers(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupMember> = groupMemberRepository.getAllGroupMembers(gateway)

    fun getGroups(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Group> = groupRepository.getAllGroups(gateway)

    fun getCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> = collectionRepository.getLogicCollections(gateway)

    suspend fun updateDeviceName(device: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return

        if (deviceName.isNotBlank() && deviceName != device.name) {
            if (gateway.isConnected) {
                try {
                    DevelcoHandler.putDevice(
                        gateway,
                        device.id,
                        JSONObject().put("name", deviceName)
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

                deviceName = ""
            }
        }
    }

    fun clearViewModel() {
        group.postValue(null)
        selectedDevice = null
        selectedGroup = null
        deviceName = ""
    }
}

