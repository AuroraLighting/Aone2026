package com.aurora.aonev3.ui.fragments.schedules

import android.app.Application
import android.view.View
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.rules.NewLogicRule
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.network.logicRule
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class ScheduleEventViewModel(application: Application) : AndroidViewModel(application),
    IEventTargetSelectorViewModel,
    IEventSceneSelectorViewModel,
    IEventDeviceSelectorViewModel,
    IEventActionSelectorViewModel,
    IEventDaySelectorViewModel,
    IScheduleTimeViewModel {
    var logicRule = MutableLiveData<LogicRule?>(null)
    override var eventTarget = MutableLiveData<EventTarget?>(null)
    override var device = MutableLiveData<Pair<Device, String>?>(null)
    override var scene = MutableLiveData<Scene?>(null)
    override var targetGroup: MutableLiveData<Group?> = MutableLiveData(null)
    override var eventAction: MutableLiveData<EventAction?> = MutableLiveData(null)
    override val trigger: LiveData<TriggerTime> = MutableLiveData<TriggerTime>(TriggerTime(0, 0, SunriseSunsetType.TIME, 0))

    override var eventDay = MutableLiveData<EventDay>(EventDay.WEEKDAYS)

    suspend fun createLogicRule(logicCollection: LogicCollection): Boolean {
        val gateway = NabtoHandler.selectedGateway ?: return false
        if (!gateway.isConnected) return false
        val group = targetGroup.value ?: return false
        val target = eventTarget.value ?: return false
        val action = eventAction.value ?: return false
        val trigger = this.trigger.value ?: return false
        val day = eventDay.value ?: return false

        val name: String
        val triggers: Array<Trigger> = arrayOf(TimeOfDayTrigger(trigger.hour, trigger.minute))
        val conditions: Array<Condition> = arrayOf(DayOfWeekCondition(gson.fromJson(day.days.toString(), Array<DayOfWeek>::class.java)))
        val actions = ArrayList<Action>()

        val metadata = RuleMetadata()
        val event = EventMetadata(action = RuleMetadataType.valueOf(action.name.uppercase()))

        when (target) {
            EventTarget.SCENE -> {
                val scene = this.scene.value ?: return false
                name = getApplication<Application>().resources.getString(
                    R.string.rule_name,
                    scene.name,
                    action.name.lowercase()
                )
                event.scene = SceneEventMetadata(scene.id, scene.groupId)
                actions.add(
                    UpdateResourceAction(
                        DevelcoHandler.Endpoints.GROUP_SCENES.url.replace(
                            Pair("{ID}", scene.groupId),
                            Pair("{LDEV}", "generic")
                        ),
                        LogicData(id = scene.id)
                    )
                )
            }
            EventTarget.SPACE -> {
                name = getApplication<Application>().resources.getString(
                    R.string.rule_name,
                    group.name,
                    action.name.lowercase()
                )
                event.group = GroupEventMetadata(group.id)
                actions.add(
                    UpdateResourceAction(
                        DevelcoHandler.Endpoints.GROUP_DATAPOINT.url.replace(
                            Pair("{ID}", group.id),
                            Pair("{LDEV}", "generic"),
                            Pair("{DATAPOINT}", "/onoff")
                        ),
                        LogicData(value = action == EventAction.ON)
                    )
                )
            }
            EventTarget.DEVICE -> {
                val device = this.device.value ?: return false

                name = getApplication<Application>().resources.getString(
                    R.string.rule_name,
                    device.first.name,
                    action.name.lowercase()
                )
                event.device = DeviceEventMetadata(device.first.id, device.second)

                if (device.second != "lock") {
                    actions.add(
                        UpdateResourceAction(
                            DevelcoHandler.Endpoints.DEVICE_DATAPOINT.url.replace(
                                Pair("{ID}", device.first.id),
                                Pair("{LDEV}", device.second),
                                Pair("{DATAPOINT}", "/onoff")
                            ),
                            LogicData(value = action == EventAction.ON)
                        )
                    )
                } else {
                    actions.addAll(
                        arrayOf(
                            UpdateResourceAction(
                                DevelcoHandler.Endpoints.DEVICE_DATAPOINT.url.replace(
                                    Pair("{ID}", device.first.id),
                                    Pair("{LDEV}", device.second),
                                    Pair("{DATAPOINT}", "/lock")
                                ),
                                LogicData(value = action == EventAction.LOCK)
                            ),
                            UpdateResourceAction(
                                DevelcoHandler.Endpoints.DEVICE_DATAPOINT.url.replace(
                                    Pair("{ID}", device.first.id),
                                    Pair("{LDEV}", device.second),
                                    Pair("{DATAPOINT}", "/lock2")
                                ),
                                LogicData(value = action == EventAction.LOCK)
                            )
                        )
                    )
                }
            }
        }

        when (trigger.trigger) {
            SunriseSunsetType.SUNRISE -> {
                if (trigger.offset != 0) event.triggerOffset = trigger.offset
                event.trigger = TriggerEnum.SUNRISE
            }
            SunriseSunsetType.SUNSET -> {
                if (trigger.offset != 0) event.triggerOffset = trigger.offset
                event.trigger = TriggerEnum.SUNSET
            }
            else -> {}
        }

        metadata.event = event

        val rule = NewLogicRule(name, metadata, true, triggers, conditions, actions.toTypedArray())
        val existingRule = logicRule.value
        val ruleId: Int?

        if (existingRule == null) {
            try {
                ruleId = DevelcoHandler.postLogicRule(
                    gateway,
                    logicCollection.id,
                    rule.toJSONObject()
                ).optJSONObject("body")?.optInt("id")
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(
                        gateway,
                        CloudHandler.getCredentials().first
                    )
                }
                err.printStackTrace()
                App.actionFailed()
                return false
            }
        } else {
            try {
                DevelcoHandler.putLogicRule(
                    gateway,
                    logicCollection.id,
                    existingRule.id,
                    rule.toJSONObject()
                )
                ruleId = existingRule.id
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(
                        gateway,
                        CloudHandler.getCredentials().first
                    )
                }
                err.printStackTrace()
                App.actionFailed()

                return false

            }
        }

        if (ruleId != null) {
            addSunriseSunsetAction(
                trigger = trigger,
                logicCollectionId = logicCollection.id,
                logicRuleId = ruleId)
        }

        return true
    }

    private suspend fun addSunriseSunsetAction(trigger: TriggerTime, logicCollectionId: Int, logicRuleId: Int) {

        if (trigger.trigger == SunriseSunsetType.SUNRISE || trigger.trigger == SunriseSunsetType.SUNSET) {
            val offset = trigger.offset
            val triggerValue: TriggerEnum = if (trigger.trigger == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE else TriggerEnum.SUNSET
            val hourString = SunriseSunsetHandler.getSunriseSunsetHourString(triggerValue, offset)
            val minuteString = SunriseSunsetHandler.getSunriseSunsetMinuteString(triggerValue, offset)

            val sunriseSunsetAction = UpdateResourceAction(
                path = logicRule(logicCollectionId, logicRuleId),
                LogicData(triggers = arrayOf(TimeOfDayTrigger(hourString, minuteString)))
            )

            SunriseSunsetHandler
                .addSunriseSunsetAction(
                    arrayOf(SunriseSunsetActionWrapper(
                        sunriseSunsetAction,
                        arrayOf(
                            SunriseSunsetOffsetWrapper(offset, trigger.trigger)
                        )
                    ))
                )
        }
    }

    suspend fun deleteLogicRule(logicCollectionId: Int, ruleId: Int) {
        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return

                SunriseSunsetHandler.removeSunriseSunsetAction()

                try {
                    DevelcoHandler.deleteLogicRule(
                        gateway,
                        logicCollectionId,
                        ruleId
                    )
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

    override fun updateTrigger(hour: Int, minute: Int, triggerType: SunriseSunsetType, offset: Int) {
        (trigger as MutableLiveData).postValue(TriggerTime(hour = hour, minute = minute, trigger = triggerType, offset = offset))
    }

    fun clearViewModel() {
        logicRule = MutableLiveData<LogicRule?>(null)
        eventTarget = MutableLiveData<EventTarget?>(null)
        device = MutableLiveData<Pair<Device, String>?>(null)
        scene = MutableLiveData<Scene?>(null)
        targetGroup = MutableLiveData(null)
        eventAction = MutableLiveData(null)
        updateTrigger(0, 0, SunriseSunsetType.TIME, 0)
        eventDay = MutableLiveData<EventDay>(EventDay.WEEKDAYS)
    }
}

data class TriggerTime(val hour: Int, val minute: Int, val trigger: SunriseSunsetType, val offset: Int)

