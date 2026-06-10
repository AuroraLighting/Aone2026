package com.aurora.aonev3.data.logic

import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.coroutines.launch

class LogicCollectionRepository {
    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> {
        refreshLogicCollections(gateway)
        return SyncHandler.logicCollections
    }

    private fun refreshLogicCollections(gateway: NabtoHandler.NabtoGateway) {
        if (!gateway.isConnected) return
        SyncHandler.syncHandlerCoroutineScope.launch {
            try {
                SyncHandler.syncLogicCollectionsCached(gateway)
                SyncHandler.syncLogicRulesAndTimersCached(gateway)
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
            }
        }
    }

    suspend fun getLogicCollections(gateway: NabtoHandler.NabtoGateway, predicate: (LogicCollection) -> Boolean): List<LogicCollection> {
        if (SyncHandler.logicCollectionsList.isEmpty()) {
            try {
                SyncHandler.syncLogicCollectionsCached(gateway)
                SyncHandler.syncLogicRulesAndTimersCached(gateway)
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
            }
        }

        return SyncHandler.logicCollectionsList.filter(predicate)
    }

    suspend fun getLogicCollection(gateway: NabtoHandler.NabtoGateway, predicate: (LogicCollection) -> Boolean): LogicCollection? {
        if (SyncHandler.logicCollectionsList.isEmpty()) {
            try {
                SyncHandler.syncLogicCollectionsCached(gateway)
                SyncHandler.syncLogicRulesAndTimersCached(gateway)
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
            }
        }

        return SyncHandler.logicCollectionsList.firstOrNull(predicate)
    }
}