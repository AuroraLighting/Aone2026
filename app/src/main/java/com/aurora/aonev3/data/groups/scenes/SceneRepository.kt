package com.aurora.aonev3.data.groups.scenes

import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.groups.Group
import kotlinx.coroutines.launch

class SceneRepository {
    fun getScenes(gateway: NabtoHandler.NabtoGateway, group: Group): MutableLiveDataArrayList<Scene> {
        refreshScenes(gateway, group)
        return SyncHandler.scenes
    }

    private fun refreshScenes(gateway: NabtoHandler.NabtoGateway, group: Group) {
        if (!gateway.isConnected) return
        SyncHandler.syncHandlerCoroutineScope.launch {
            try {
                SyncHandler.syncGroupScenes(gateway, group)
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