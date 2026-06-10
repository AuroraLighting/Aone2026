package com.aurora.aonev3.data.groups.groupmembers

import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.DeviceRepository
import kotlinx.coroutines.launch

class GroupMemberRepository {

//    fun getGroupMembers(gateway: NabtoHandler.NabtoGateway, group: Group): LiveData<List<Device>> {
//        DeviceRepository().getAllDevices(gateway)
//        refreshGroupMembers(gateway, group.id)
//        return groupMemberDao.getDevicesForGroup(gateway.serial, group.id)
//    }

//    suspend fun getGroupMemberDeviceEntities(gateway: NabtoHandler.NabtoGateway, group: Group): List<Device> {
//        return getGroupMemberDeviceEntities(gateway, group.id)
//    }

//    suspend fun getGroupMemberDeviceEntities(gateway: NabtoHandler.NabtoGateway, groupId: Int): List<Device> {
//        DeviceRepository().getAllDevices(gateway)
//        refreshGroupMembers(gateway, groupId)
//        return groupMemberDao.getDeviceEntitiesForGroup(gateway.serial, groupId)
//    }

    fun getGroupMemberEntities(gateway: NabtoHandler.NabtoGateway, groupId: Int): List<GroupMember> {
        DeviceRepository().getAllDevices(gateway)
        refreshGroupMembers(gateway, groupId)
        return SyncHandler.groupMembersList.filter { it.parentGateway == gateway.serial && it.groupId == groupId }
    }

    private fun refreshGroupMembers(gateway: NabtoHandler.NabtoGateway, groupId: Int) {
        if (!gateway.isConnected) return
        SyncHandler.syncHandlerCoroutineScope.launch {
            try {
                SyncHandler.syncGroupMembers(gateway, groupId)
            } catch (err: VolleyError) {
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

    fun getAllGroupMembers(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupMember> {
        SyncHandler.syncHandlerCoroutineScope.launch {
            if (!gateway.isConnected) return@launch
            SyncHandler
                .groupsList
                .filter { it.parentGateway == gateway.serial }
                .forEach {
                    try {
                        SyncHandler.syncGroupMembers(gateway, it)
                    } catch (err: VolleyError) {
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
        return SyncHandler.groupMembers
    }
}