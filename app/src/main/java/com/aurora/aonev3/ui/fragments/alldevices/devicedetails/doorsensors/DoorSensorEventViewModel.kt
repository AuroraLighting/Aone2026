package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.*
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.NewLogicRule
import com.aurora.aonev3.data.logic.timers.NewLogicTimer
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.*
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.ITimeConditionViewModel
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.ITimeoutViewModel
import com.aurora.aonev3.ui.fragments.groups.eventgroupselector.IEventGroupSelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.*
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.*
import org.json.JSONObject
import java.util.*
import kotlin.math.abs

class DoorSensorEventViewModel: ViewModel(),
    IEventGroupSelectorViewModel,
    IEventTargetSelectorViewModel,
    IEventSceneSelectorViewModel,
    IEventDeviceSelectorViewModel,
    IEventTriggerSelectorViewModel,
    IEventActionSelectorViewModel,
    ITimeoutViewModel,
    ITimeConditionViewModel,
    IEventDaySelectorViewModel {

    private var triggerDevice: Device? = null
    var existingLogicCollection: LogicCollection? = null
    override var targetGroup: MutableLiveData<Group?> = MutableLiveData()
    override var eventTarget: MutableLiveData<EventTarget?> = MutableLiveData()
    override var scene: MutableLiveData<Scene?> = MutableLiveData()
    override var device: MutableLiveData<Pair<Device, String>?> = MutableLiveData()
    override var trigger: MutableLiveData<TriggerEnum?> = MutableLiveData()
    override var eventAction: MutableLiveData<EventAction?> = MutableLiveData()
    override var timeout: MutableLiveData<Int?> = MutableLiveData()
    override val isAllDay = MutableLiveData(false)
    override val startTime = MutableLiveData<TriggerTime>()
    override val endTime = MutableLiveData<TriggerTime>()

    override var eventDay: MutableLiveData<EventDay> = MutableLiveData()

    private val sunriseSunsetActions = ArrayList<SunriseSunsetActionWrapper>()

    fun setTriggerDevice(device: Device) {
        this.triggerDevice = device
    }

    suspend fun createLogic() {
        val existingLogicCollection = existingLogicCollection
        val triggerDevice = this.triggerDevice ?: throw UninitializedPropertyAccessException()
        val gateway = NabtoHandler.selectedGateway ?: return
        val target = eventTarget.value ?: return
        val group = targetGroup.value ?: return
        val eventAction = eventAction.value ?: return
        val timeout = timeout.value ?: 0
        val scene = scene.value
        val device = device.value
        val trigger = trigger.value ?: return
        if ((target == EventTarget.SCENE && scene == null)
            || (target == EventTarget.DEVICE && device == null)) {
            return
        }

        if (existingLogicCollection != null) {
            DevelcoHandler.deleteLogicCollection(gateway, existingLogicCollection.id)
            SyncHandler.syncLogicCollectionsCached(gateway, force = true)
            SyncHandler.syncLogicRulesAndTimersCached(gateway, force = true)
        }

        val logicCollectionJson = createLogicCollection(triggerDevice, gateway, target)
        if (!logicCollectionJson.has("id")) throw Exception("Failed to create logic")
        val collectionId = logicCollectionJson.getInt("id")

        if (timeout > 0) {
            val offTimerJson = createTimer(gateway, collectionId, true, timeout, group, target)
            val overrideTimerJson = createTimer(gateway, collectionId, false, timeout, group, target)

            if (!offTimerJson.has("id") || !overrideTimerJson.has("id")) {
                DevelcoHandler.deleteLogicCollection(gateway, collectionId)
                throw UnknownApiException()
            }
            val offTimerId = offTimerJson.getInt("id")
            val overrideTimerId = overrideTimerJson.getInt("id")

            val timerStartRule = createTimerRule(triggerDevice, gateway, collectionId, offTimerId, overrideTimerId, true)
            val timerStopRule = createTimerRule(triggerDevice, gateway, collectionId, offTimerId, overrideTimerId, false)

            val eventRule = createEventRule(triggerDevice, gateway, collectionId, offTimerId, overrideTimerId, group, target, scene, device, trigger, eventAction)

            if (!timerStartRule.has("id") || !timerStopRule.has("id") || !eventRule.has("id")) {
                DevelcoHandler.deleteLogicCollection(gateway, collectionId)
                throw UnknownApiException()
            }
        } else {
            val eventRule = createEventRule(triggerDevice, gateway, collectionId, null, null, group, target, scene, device, trigger, eventAction)

            if (!eventRule.has("id")) {
                DevelcoHandler.deleteLogicCollection(gateway, collectionId)
                throw UnknownApiException()
            }
        }

        SunriseSunsetHandler.addSunriseSunsetAction(sunriseSunsetActions.toTypedArray())
        sunriseSunsetActions.clear()
    }

    @Throws(UninitializedPropertyAccessException::class)
    private suspend fun createLogicCollection(
        triggerDevice: Device,
        gateway: NabtoHandler.NabtoGateway,
        target: EventTarget): JSONObject {

        val group = targetGroup.value
        val scene = scene.value
        val device = device.value
        val isAllDay = isAllDay.value
        val startTime = startTime.value
        val endTime = endTime.value
        val day = eventDay.value ?: EventDay.EVERYDAY

        val metadata = CollectionMetadata()
        metadata.collectionType = CollectionType.SENSOR
        metadata.triggerId = triggerDevice.id

        val event = EventMetadata()
        event.days = day.name.lowercase(Locale.UK)
        if (isAllDay != true && startTime != null && endTime != null) {
            val startHour = if (startTime.trigger == SunriseSunsetType.TIME) {
                startTime.hour
            } else {
                null
            }
            val startMinute = if (startTime.trigger == SunriseSunsetType.TIME) {
                startTime.minute
            } else {
                null
            }
            val start = if (startTime.trigger != SunriseSunsetType.TIME) {
                startTime.trigger.name.lowercase()
            } else {
                null
            }
            val startOffset =
                if (startTime.trigger != SunriseSunsetType.TIME && startTime.offset != 0) {
                    startTime.offset
                } else {
                    null
                }
            val endHour = if (endTime.trigger == SunriseSunsetType.TIME) {
                endTime.hour
            } else {
                null
            }
            val endMinute = if (endTime.trigger == SunriseSunsetType.TIME) {
                endTime.minute
            } else {
                null
            }
            val end = if (endTime.trigger != SunriseSunsetType.TIME) {
                endTime.trigger.name.lowercase()
            } else {
                null
            }
            val endOffset = if (endTime.trigger != SunriseSunsetType.TIME && endTime.offset != 0) {
                endTime.offset
            } else {
                null
            }

            event.time = TimeEventMetadata(
                start = start,
                startOffset = startOffset,
                startHour = startHour,
                startMinute = startMinute,
                end = end,
                endOffset = endOffset,
                endHour = endHour,
                endMinute = endMinute
            )
        }
        when (target) {
            EventTarget.SCENE -> {
                scene?.let { event.scene = SceneEventMetadata(scene.id, scene.groupId) }
            }
            EventTarget.SPACE -> {
                group?.let { event.group = GroupEventMetadata(group.id) }
            }
            EventTarget.DEVICE -> {
                device?.let { event.device = DeviceEventMetadata(device.first.id, device.second) }
            }
        }
        event.trigger = trigger.value

        event.action = if (eventAction.value == EventAction.OFF) {
            RuleMetadataType.OFF
        } else {
            RuleMetadataType.ON
        }
        metadata.event = event

        val logicCollection = LogicCollection(gateway.serial, -1, triggerDevice.name, metadata, true).toJSONObject()
        logicCollection.remove("id")

        return DevelcoHandler.postLogicCollection(gateway, logicCollection).optJSONObject("body") ?: JSONObject()
    }

    private suspend fun createTimer(
        gateway: NabtoHandler.NabtoGateway,
        collectionId: Int,
        onoff: Boolean,
        timeout: Int,
        group: Group,
        target: EventTarget): JSONObject {
        val actions: Array<Action>? = if (onoff) {
            val action = when (target) {
                EventTarget.SCENE -> {
                    UpdateResourceAction(groupDatapoint(group, datapoint = "/onoff"), LogicData(value = false))
                }
                EventTarget.SPACE -> {
                    UpdateResourceAction(groupDatapoint(group, datapoint = "/onoff"), LogicData(value = false))
                }
                EventTarget.DEVICE -> {
                    val device = device.value ?: return JSONObject()
                    UpdateResourceAction(deviceDatapoint(device.first, device.second, datapoint = "/onoff"), LogicData(value = false))
                }
            }
            arrayOf(action)
        } else {
            null
        }

        val logicTimer = NewLogicTimer("", JSONObject(), timeout, actions).toJSONObject()

        return DevelcoHandler.postLogicTimer(gateway, collectionId, logicTimer).optJSONObject("body") ?: JSONObject()
    }

    private suspend fun createTimerRule(
        triggerDevice: Device,
        gateway: NabtoHandler.NabtoGateway,
        collectionId: Int,
        offTimerId: Int,
        overrideTimerId: Int,
        startTimers: Boolean
    ): JSONObject {
        val day = eventDay.value ?: EventDay.EVERYDAY
        val isAllDay = isAllDay.value ?: true
        val startTime = startTime.value
        val endTime = endTime.value

        val trigger = buildTrigger(deviceDatapoint(triggerDevice, "alarm", "/alarm"))
        val triggers = arrayOf(trigger)

        val triggerCondition = buildCondition(deviceDatapoint(triggerDevice, "alarm", "/alarm.value"), !startTimers)
        val dayOfWeekCondition = DayOfWeekCondition(gson.fromJson(day.days.toString(), Array<DayOfWeek>::class.java))

        val conditions = arrayListOf(triggerCondition, dayOfWeekCondition)
        if (startTimers) {
            val overrideCondition = buildCondition(logicTimer(collectionId, overrideTimerId) + ".status", "!str_is_equal('##INVAL##', 'idle')")
            conditions.add(overrideCondition)
        }

        if (!isAllDay && startTime != null && endTime != null) {
            conditions.add(
                TimeIntervalCondition(
                    startTime.hour,
                    startTime.minute,
                    endTime.hour,
                    endTime.minute
                )
            )
        }

        val offTimerAction = UpdateResourceAction(logicTimer(collectionId, offTimerId), LogicData(command = if (startTimers) "start" else "stop"))
        val actions: Array<Action> = if (startTimers) {
            val overrideTimerAction = UpdateResourceAction(logicTimer(collectionId, overrideTimerId), LogicData(command = if (startTimers) "start" else "stop"))
            arrayOf(offTimerAction, overrideTimerAction)
        } else {
            arrayOf(offTimerAction)
        }

        val metadata = if (startTimers) {
            RuleMetadata(RuleMetadataType.START_TIMER)
        } else {
            RuleMetadata(RuleMetadataType.STOP_TIMER)
        }

        val logicRule = NewLogicRule("Timer rule", metadata, triggers = triggers, conditions = conditions.toTypedArray(), actions = actions).toJSONObject()

        val rule = DevelcoHandler.postLogicRule(gateway, collectionId, logicRule).optJSONObject("body") ?: JSONObject()

        if (startTime != null && endTime != null) {
            if (startTime.trigger == SunriseSunsetType.SUNRISE ||
                startTime.trigger == SunriseSunsetType.SUNSET ||
                endTime.trigger == SunriseSunsetType.SUNRISE ||
                endTime.trigger == SunriseSunsetType.SUNSET
            ) {
                val sunriseSunsetConditions =
                    ArrayList(conditions.filter { it !is TimeIntervalCondition })

                val sunriseSunsetCondition = sunriseSunsetCondition(startTime, endTime)
                sunriseSunsetConditions.add(sunriseSunsetCondition)

                val sunriseSunsetAction = UpdateResourceAction(
                    path = logicRule(collectionId, rule.optInt("id")),
                    data = LogicData(conditions = sunriseSunsetConditions.toTypedArray())
                )
                val offsets: Array<SunriseSunsetOffsetWrapper> = arrayOf(startTime, endTime).mapNotNull {
                    if (it.offset != 0) {
                        SunriseSunsetOffsetWrapper(offset = it.offset, sunriseSunset = it.trigger)
                    } else {
                        null
                    }
                }.toTypedArray()
                sunriseSunsetActions.add(SunriseSunsetActionWrapper(action = sunriseSunsetAction, offsets))
            }
        }

        return rule
    }

    private suspend fun createEventRule(
        triggerDevice: Device,
        gateway: NabtoHandler.NabtoGateway,
        collectionId: Int,
        offTimerId: Int?,
        overrideTimerId: Int?,
        group: Group,
        target: EventTarget,
        scene: Scene?,
        device: Pair<Device, String>?,
        triggerEnum: TriggerEnum,
        eventAction: EventAction
    ): JSONObject {
        val day = eventDay.value ?: EventDay.EVERYDAY
        val isAllDay = isAllDay.value ?: true
        val startTime = startTime.value
        val endTime = endTime.value

        val trigger = buildTrigger(deviceDatapoint(triggerDevice, "alarm", "/alarm"))
        val triggers = arrayOf(trigger)

        val triggerCondition = buildCondition(deviceDatapoint(triggerDevice, "alarm", "/alarm.value"), triggerEnum == TriggerEnum.OPEN)
        val dayOfWeekCondition = DayOfWeekCondition(gson.fromJson(day.days.toString(), Array<DayOfWeek>::class.java))

        val conditions = arrayListOf(triggerCondition, dayOfWeekCondition)

        if (overrideTimerId != null) {
            val overrideCondition = buildCondition(logicTimer(collectionId, overrideTimerId) + ".status", "str_is_equal('##INVAL##', 'idle')")
            conditions.add(overrideCondition)
        }

        if (!isAllDay && startTime != null && endTime != null) {
            conditions.add(
                TimeIntervalCondition(
                    startTime.hour,
                    startTime.minute,
                    endTime.hour,
                    endTime.minute
                )
            )
        }

        val action = when (target) {
            EventTarget.SCENE -> {
                UpdateResourceAction(groupScenes(group), LogicData(id = scene!!.id))
            }
            EventTarget.SPACE -> {
                UpdateResourceAction(groupDatapoint(group, datapoint = "/onoff"), LogicData(value = eventAction == EventAction.ON))
            }
            EventTarget.DEVICE -> {
                UpdateResourceAction(deviceDatapoint(device!!.first, device.second, datapoint = "/onoff"), LogicData(value = eventAction == EventAction.ON))
            }
        }
        val actions: Array<Action> = if (overrideTimerId != null && offTimerId != null) {
            val offTimerAction = UpdateResourceAction(
                logicTimer(collectionId, offTimerId),
                LogicData(command = "start")
            )
            val overrideTimerAction = UpdateResourceAction(
                logicTimer(collectionId, overrideTimerId),
                LogicData(command = "start")
            )
            arrayOf(action, offTimerAction, overrideTimerAction)
        } else {
            arrayOf(action)
        }

        val metadata = if (eventAction == EventAction.ON) {
            RuleMetadata(RuleMetadataType.ON)
        } else {
            RuleMetadata(RuleMetadataType.OFF)
        }

        val logicRule = NewLogicRule("Event rule", metadata, triggers = triggers, conditions = conditions.toTypedArray(), actions = actions).toJSONObject()

        val rule = DevelcoHandler.postLogicRule(gateway, collectionId, logicRule).optJSONObject("body") ?: JSONObject()

        if (startTime != null && endTime != null) {
            if (startTime.trigger == SunriseSunsetType.SUNRISE ||
                startTime.trigger == SunriseSunsetType.SUNSET ||
                endTime.trigger == SunriseSunsetType.SUNRISE ||
                endTime.trigger == SunriseSunsetType.SUNSET
            ) {
                val sunriseSunsetConditions =
                    ArrayList(conditions.filter { it !is TimeIntervalCondition })

                val sunriseSunsetCondition = sunriseSunsetCondition(startTime, endTime)
                sunriseSunsetConditions.add(sunriseSunsetCondition)

                debug("$collectionId ${rule.optInt("id")}")
                val sunriseSunsetAction = UpdateResourceAction(
                    path = logicRule(collectionId, rule.optInt("id")),
                    LogicData(conditions = sunriseSunsetConditions.toTypedArray())
                )
                val offsets: Array<SunriseSunsetOffsetWrapper> = arrayOf(startTime, endTime).mapNotNull {
                    if (it.offset != 0) {
                        SunriseSunsetOffsetWrapper(offset = it.offset, sunriseSunset = it.trigger)
                    } else {
                        null
                    }
                }.toTypedArray()

                sunriseSunsetActions.add(SunriseSunsetActionWrapper(action = sunriseSunsetAction, offsets))
            }
        }

        return rule
    }

    private fun sunriseSunsetCondition(
        startTime: TriggerTime,
        endTime: TriggerTime
    ): TimeIntervalCondition {
        val startHour: Any
        val startMinute: Any
        val endHour: Any
        val endMinute: Any
        if (startTime.trigger == SunriseSunsetType.TIME) {
            startHour = startTime.hour
            startMinute = startTime.minute
        } else {
            when (startTime.trigger) {
                SunriseSunsetType.SUNRISE -> {
                    when {
                        startTime.offset > 0 -> {
                            startHour =
                                App.context.getString(R.string.sunrisePositiveOffsetHours, startTime.offset)
                            startMinute =
                                App.context.getString(R.string.sunrisePositiveOffsetMinutes, startTime.offset)
                        }
                        startTime.offset < 0 -> {
                            startHour = App.context.getString(
                                R.string.sunriseNegativeOffsetHours,
                                abs(startTime.offset)
                            )
                            startMinute = App.context.getString(
                                R.string.sunriseNegativeOffsetMinutes,
                                abs(startTime.offset)
                            )
                        }
                        else -> {
                            startHour = getString(R.string.sunriseHour)
                            startMinute = getString(R.string.sunriseHour)
                        }
                    }
                }
                else -> {
                    when {
                        startTime.offset > 0 -> {
                            startHour =
                                App.context.getString(R.string.sunsetPositiveOffsetHours, startTime.offset)
                            startMinute =
                                App.context.getString(R.string.sunsetPositiveOffsetMinutes, startTime.offset)
                        }
                        startTime.offset < 0 -> {
                            startHour =
                                App.context.getString(R.string.sunsetNegativeOffsetHours, abs(startTime.offset))
                            startMinute = App.context.getString(
                                R.string.sunsetNegativeOffsetMinutes,
                                abs(startTime.offset)
                            )
                        }
                        else -> {
                            startHour = getString(R.string.sunsetHour)
                            startMinute = getString(R.string.sunsetMinute)
                        }
                    }
                }
            }
        }
        if (endTime.trigger == SunriseSunsetType.TIME) {
            endHour = endTime.hour
            endMinute = endTime.minute
        } else {
            when (endTime.trigger) {
                SunriseSunsetType.SUNRISE -> {
                    when {
                        endTime.offset > 0 -> {
                            endHour = App.context.getString(R.string.sunrisePositiveOffsetHours, endTime.offset)
                            endMinute =
                                App.context.getString(R.string.sunrisePositiveOffsetMinutes, endTime.offset)
                        }
                        endTime.offset < 0 -> {
                            endHour =
                                App.context.getString(R.string.sunriseNegativeOffsetHours, abs(endTime.offset))
                            endMinute = App.context.getString(
                                R.string.sunriseNegativeOffsetMinutes,
                                abs(endTime.offset)
                            )
                        }
                        else -> {
                            endHour = getString(R.string.sunriseHour)
                            endMinute = getString(R.string.sunriseHour)
                        }
                    }
                }
                else -> {
                    when {
                        endTime.offset > 0 -> {
                            endHour = App.context.getString(R.string.sunsetPositiveOffsetHours, endTime.offset)
                            endMinute =
                                App.context.getString(R.string.sunsetPositiveOffsetMinutes, endTime.offset)
                        }
                        endTime.offset < 0 -> {
                            endHour =
                                App.context.getString(R.string.sunsetNegativeOffsetHours, abs(endTime.offset))
                            endMinute =
                                App.context.getString(R.string.sunsetNegativeOffsetMinutes, abs(endTime.offset))
                        }
                        else -> {
                            endHour = getString(R.string.sunsetHour)
                            endMinute = getString(R.string.sunsetMinute)
                        }
                    }
                }
            }
        }

        return TimeIntervalCondition(
            startHour,
            startMinute,
            endHour,
            endMinute
        )
    }

    override fun setIsAllDay(isAllDay: Boolean) {
        this.isAllDay.postValue(isAllDay)
    }

    override fun updateStartTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        startTime.postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }

    override fun updateEndTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        endTime.postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }

    fun clearViewModel() {

        triggerDevice = null
        targetGroup = MutableLiveData()
        eventTarget = MutableLiveData()
        scene = MutableLiveData()
        device = MutableLiveData()
        trigger = MutableLiveData()
        eventAction = MutableLiveData()
        timeout = MutableLiveData()
        isAllDay.postValue(false)
        startTime.postValue(null)
        endTime.postValue(null)
        eventDay = MutableLiveData()
    }
}