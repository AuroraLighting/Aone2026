package com.aurora.aonev3.ui.fragments.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.logic.UpdateResourceAction
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.rules.LogicRuleRepository
import com.aurora.aonev3.logic.TimeOfDayTrigger
import com.aurora.aonev3.logic.TriggerEnum
import com.aurora.aonev3.network.handlers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ScheduleViewModel(val logicCollection: LogicCollection) : ViewModel() {
    private val logicRuleRepository = LogicRuleRepository()

    val logicRules: MutableLiveDataArrayList<LogicRule> = logicRuleRepository.getLogicRules( logicCollection)

    val sortedLogicRules: List<LogicRule>
    get() {
        val rules = logicRules.value?.toList()
        return if (rules.isNullOrEmpty()) {
            return emptyList()
        } else {
            rules
                .filter { it?.logicCollectionId == logicCollection.id }
                .sortedWith(
                    compareBy(
                        { rule ->
                            val triggers = rule.triggers ?: emptyArray()
                            var comparable = 0

                            when (rule.metadata.event?.trigger) {
                                TriggerEnum.SUNRISE -> {
                                    comparable = 100
                                }
                                TriggerEnum.SUNSET -> {
                                    comparable = 200
                                }
                                else -> {
                                    for (trigger in triggers) {
                                        if (trigger !is TimeOfDayTrigger) continue
                                        val hour = trigger.hour as? Number ?: continue
                                        comparable = hour.toInt()
                                    }
                                }
                            }

                            comparable
                        },
                        { rule ->
                            val triggers = rule.triggers ?: emptyArray()
                            var comparable = 0

                            for (trigger in triggers) {
                                if (trigger !is TimeOfDayTrigger) continue
                                val min = trigger.min as? Number ?: continue
                                comparable = min.toInt()
                            }

                            comparable
                        }
                    )
                )
        }
    }

    fun deleteLogicRule(logicCollectionId: Int, logicRuleId: Int) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    DevelcoHandler.deleteLogicRule(
                        gateway,
                        logicCollectionId,
                        logicRuleId
                    )
                    SunriseSunsetHandler.removeSunriseSunsetAction()
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
                    if (err.networkResponse.statusCode != 404) {
                        throw Exception("Failed to delete")
                    }
                }
            }
        }
    }
}

class ScheduleViewModelFactory(private val logicCollection: LogicCollection): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            return ScheduleViewModel(logicCollection = logicCollection) as T
        }

        throw IllegalArgumentException("Unknown ViewModelClass")
    }

}
