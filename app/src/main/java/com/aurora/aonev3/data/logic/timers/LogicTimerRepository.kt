package com.aurora.aonev3.data.logic.timers

import com.aurora.aonev3.synthetic.*
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.coroutines.launch

class LogicTimerRepository {

    fun getAllLogicTimers(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicTimer> {
        refreshLogicTimers(gateway)
        return SyncHandler.logicTimers
    }

    private fun refreshLogicTimers(gateway: NabtoHandler.NabtoGateway) {
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
}
