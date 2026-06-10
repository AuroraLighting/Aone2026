package com.aurora.aonev3.ui.fragments.legacyschedule

import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRuleRepository

class LegacyScheduleViewModel: ViewModel() {
    private val logicRuleRepository = LogicRuleRepository()
    var logicCollection: LogicCollection? = null

    fun getLogicRules(collection: LogicCollection)
            = logicRuleRepository.getLogicRules(collection)

    suspend fun deleteCollection() {
        val gateway = NabtoHandler.selectedGateway ?: return

        try {
            logicCollection?.id?.let { DevelcoHandler.deleteLogicCollection(gateway, it) }
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
    }
}