package com.aurora.aonev3.data.logic.rules

import androidx.core.util.Predicate
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.logic.LogicCollection
import kotlinx.coroutines.launch

class LogicRuleRepository {

    fun getLogicRules(
        collection: LogicCollection
    ): MutableLiveDataArrayList<LogicRule> {
        val gateway = NabtoHandler.selectedGateway ?: return MutableLiveDataArrayList()
        refreshLogicCollectionRules(gateway, collection)
        return SyncHandler.logicRules
    }

    private fun refreshLogicCollectionRules(
        gateway: NabtoHandler.NabtoGateway,
        collection: LogicCollection
    ) {
        if (!gateway.isConnected) return
        SyncHandler.syncHandlerCoroutineScope.launch {
            try {
                SyncHandler.syncLogicRulesCached(gateway, collection)
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
            }
        }
    }

    fun getAllLogicRules(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicRule> {
        refreshLogicRules(gateway)
        return SyncHandler.logicRules
    }

    private fun refreshLogicRules(gateway: NabtoHandler.NabtoGateway) {
        if (!gateway.isConnected) return
        SyncHandler.syncHandlerCoroutineScope.launch {
            try {
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

    suspend fun getLogicRules(gateway: NabtoHandler.NabtoGateway, predicate: (LogicRule) -> Boolean): List<LogicRule> {
        if (SyncHandler.logicRulesList.isEmpty()) {
            SyncHandler.syncLogicRulesAndTimersCached(gateway)
        }

        return SyncHandler.logicRulesList.filter(predicate)
    }

    suspend fun getLogicRule(gateway: NabtoHandler.NabtoGateway, predicate: (LogicRule) -> Boolean): LogicRule? {
        if (SyncHandler.logicRulesList.isEmpty()) {
            SyncHandler.syncLogicRulesAndTimersCached(gateway)
        }

        return SyncHandler.logicRulesList.firstOrNull(predicate)
    }
}