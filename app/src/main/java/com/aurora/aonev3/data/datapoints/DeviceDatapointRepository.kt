package com.aurora.aonev3.data.datapoints

import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.coroutines.launch

class DeviceDatapointRepository {

    private fun refreshDatapoints(gateway: NabtoHandler.NabtoGateway) {
        if (!gateway.isConnected) return
        SyncHandler.syncHandlerCoroutineScope.launch {
            try {
                SyncHandler.syncDeviceDatapoints(gateway)
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
            }
        }
    }

    fun getAllDeviceDatapoints(
        gateway: NabtoHandler.NabtoGateway
    ): MutableLiveDataArrayList<DeviceDatapoint> {
        refreshDatapoints(gateway)
        return SyncHandler.deviceDatapoints
    }
}