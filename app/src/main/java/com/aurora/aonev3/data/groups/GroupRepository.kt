package com.aurora.aonev3.data.groups

import com.aurora.aonev3.synthetic.*
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.coroutines.launch

class GroupRepository {
    fun getAllGroups(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Group> {
        refreshGroups(gateway)
        return SyncHandler.groups
    }

    private fun refreshGroups(gateway: NabtoHandler.NabtoGateway) {
        if (!gateway.isConnected) return
        SyncHandler.syncHandlerCoroutineScope.launch {
            try {
                SyncHandler.syncGroups(gateway)
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()

                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
            }
        }
    }

    fun getGroupEntity(gateway: NabtoHandler.NabtoGateway, id: Int): Group? {
        refreshGroups(gateway)
        return SyncHandler.groupsList.find { it.parentGateway == gateway.serial && it.id == id }
    }
}
