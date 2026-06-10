package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import androidx.lifecycle.ViewModel
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import com.aurora.aonev3.data.logic.rules.LogicRuleRepository
import com.aurora.aonev3.data.logic.timers.LogicTimerRepository

class MotionSensorDetailViewModel : ViewModel() {
    private val logicCollectionRepository = LogicCollectionRepository()
    private val logicRuleRepository = LogicRuleRepository()
    private val logicTimerRepository = LogicTimerRepository()
    var initialEnabled: Boolean? = null
    var logicCollections: List<LogicCollection> = emptyList()

    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway) = logicCollectionRepository.getLogicCollections(gateway)
    fun getLogicRules(gateway: NabtoHandler.NabtoGateway) = logicRuleRepository.getAllLogicRules(gateway)
    fun getLogicTimers(gateway: NabtoHandler.NabtoGateway) = logicTimerRepository.getAllLogicTimers(gateway)
    fun clearViewModel() {
        initialEnabled = null
        logicCollections = emptyList()
    }
}
