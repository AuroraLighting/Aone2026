package com.aurora.aonev3.data.datapoints

import com.aurora.aonev3.synthetic.*
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.coroutines.launch

class GroupDatapointRepository {

    fun getAllDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<GroupDatapoint> {
        refreshDatapoints(gateway)
        return SyncHandler.groupDatapoints
    }

    private fun refreshDatapoints(gateway: NabtoHandler.NabtoGateway) {
        if (!gateway.isConnected) return

        SyncHandler.syncHandlerCoroutineScope.launch {
            SyncHandler.groupsList.filter { it.parentGateway == gateway.serial }.forEach {
                try {
                    SyncHandler.syncGroupDatapoints(gateway, it)
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                    }

                }
            }
        }
    }
}
