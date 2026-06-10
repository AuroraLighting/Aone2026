package com.aurora.aonev3.ui.fragments.dynamicevents

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.rules.NewLogicRule
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.*
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.ITimeConditionViewModel
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.IEventDaySelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.IEventSceneSelectorViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonObject
import kotlin.math.abs

class DynamicEventViewModel(val logicCollection: LogicCollection, logicRule: LogicRule?, val group: Group):
    ViewModel(),
    IEventSceneSelectorViewModel,
    ITimeConditionViewModel,
    IEventDaySelectorViewModel {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    val logicRule: MutableLiveData<LogicRule> = logicRule?.let { MutableLiveData(it) } ?: MutableLiveData()
    override var targetGroup: MutableLiveData<Group?> = MutableLiveData(group)
    override var scene: MutableLiveData<Scene?> = MutableLiveData()

    override val isAllDay: LiveData<Boolean> = MutableLiveData()
    override val startTime: LiveData<TriggerTime> = MutableLiveData()
    override val endTime: LiveData<TriggerTime> = MutableLiveData()

    override var eventDay = MutableLiveData(EventDay.EVERYDAY)

    init {
        setUpExistingRule()
    }

    private fun setUpExistingRule() {
        val rule = logicRule.value ?: return
        val event = rule.metadata.event ?: return
        val scene = SyncHandler.scenesList.firstOrNull { it.id == event.scene?.id && it.groupId == event.scene?.group } ?: return

        this.scene.postValue(scene)

        if (event.time != null) {
            val time = event.time ?: TimeEventMetadata()
            val start = SunriseSunsetType.fromMetadata(time.start) ?: SunriseSunsetType.TIME
            val startOffset = time.startOffset ?: 0
            val startHour = time.startHour ?: 0
            val startMinute = time.startMinute ?: 0
            val end = SunriseSunsetType.fromMetadata(time.end) ?: SunriseSunsetType.TIME
            val endOffset = time.endOffset ?: 0
            val endHour = time.endHour ?: 0
            val endMinute = time.endMinute ?: 0

            updateStartTime(hour = startHour, minute = startMinute, trigger = start, offset = startOffset)
            updateEndTime(hour = endHour, minute = endMinute, trigger = end, offset = endOffset)
            setIsAllDay(false)
        } else {
            setIsAllDay(true)
        }

        try {
            eventDay.postValue(EventDay.valueOf(event.days?.uppercase() ?: ""))
        } catch (ex: IllegalArgumentException) {
            crashlytics.log("E/${TAG}: ${event.days}")
            crashlytics.recordException(ex)
        }
    }

    suspend fun createLogicRule(): Boolean {
        val gateway = NabtoHandler.selectedGateway ?: return false
        if (!gateway.isConnected) return false

        val scene = scene.value ?: return false
        val startTime = startTime.value
        val endTime = endTime.value
        val day = eventDay.value
        val existingRule = logicRule.value

        val logicRuleMetadata = createMetadata(scene, startTime, endTime, day)

        val triggers = createTriggers(startTime)

        val conditions = createConditions(day, startTime, endTime)

        val actions = createActions(scene)

        val rule = NewLogicRule(
            name = App.context.getString(R.string.motion_event_activate_scene, scene.name),
            metadata = logicRuleMetadata,
            triggers = triggers.toTypedArray(),
            conditions = conditions.toTypedArray(),
            actions = actions.toTypedArray()
        ).toJSONObject()

        if (existingRule == null) {
            try {
                val id = DevelcoHandler
                    .postLogicRule(gateway, logicCollection.id, rule)
                    .optJSONObject("body")
                    ?.optInt("id") ?: 0

                if (startTime != null
                    && endTime != null
                    && (startTime.trigger == SunriseSunsetType.SUNRISE
                            || startTime.trigger == SunriseSunsetType.SUNSET
                            || endTime.trigger == SunriseSunsetType.SUNSET
                            || endTime.trigger == SunriseSunsetType.SUNSET)) {
                    setupSunriseSunset(
                        startTime = startTime,
                        endTime = endTime,
                        conditions = conditions,
                        triggers = triggers,
                        logicRuleId = id
                    )
                }
            } catch (err: VolleyError) {
                App.actionFailed()
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
            }
        } else {
            try {
                DevelcoHandler.putLogicRule(gateway, logicCollection.id, existingRule.id, rule)

                if (startTime != null
                    && endTime != null
                    && (startTime.trigger == SunriseSunsetType.SUNRISE
                            || startTime.trigger == SunriseSunsetType.SUNSET
                            || endTime.trigger == SunriseSunsetType.SUNSET
                            || endTime.trigger == SunriseSunsetType.SUNSET)) {
                    setupSunriseSunset(
                        startTime = startTime,
                        endTime = endTime,
                        conditions = conditions,
                        triggers = triggers,
                        logicRuleId = existingRule.id
                    )
                }
            } catch (err: VolleyError) {
                App.actionFailed()
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                err.printStackTrace()
                try {
                    DevelcoHandler.deleteLogicRule(gateway, logicCollection.id, existingRule.id)
                } catch (err: VolleyError) {
                    App.actionFailed()
                    err.printStackTrace()
                }
                createLogicRule()
            }
        }

        return false
    }

    private fun createMetadata(scene: Scene, startTime: TriggerTime?, endTime: TriggerTime?, day: EventDay?): RuleMetadata {
        val logicRuleMetadata = RuleMetadata()
        val event = EventMetadata(days = day?.name?.lowercase())
        if (isAllDay.value == false && startTime != null && endTime != null) {
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
            val startOffset = if (startTime.trigger != SunriseSunsetType.TIME && startTime.offset != 0) {
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
        event.scene = SceneEventMetadata(id = scene.id, group = scene.groupId)
        logicRuleMetadata.event = event

        return logicRuleMetadata
    }

    private fun createTriggers(startTime: TriggerTime?): ArrayList<Trigger> {
        val triggers = ArrayList<Trigger>()
        val groupOnTrigger =
            ResourceUpdateTrigger(groupDatapoint(group = group, datapoint = "/onoff"))

        triggers.add(groupOnTrigger)

        if (isAllDay.value == false && startTime != null) {
            val timeOfDayTrigger = TimeOfDayTrigger(hour = startTime.hour, min = startTime.minute)
            triggers.add(timeOfDayTrigger)
        }

        return triggers
    }

    private fun createConditions(
        day: EventDay?,
        startTime: TriggerTime?,
        endTime: TriggerTime?
    ): ArrayList<Condition> {
        val conditions = ArrayList<Condition>()
        val groupOnCondition = ResourceValueCondition(
            path = groupDatapoint(group = group, datapoint = "/onoff.value"),
            rule = "##INVAL## == true"
        )
        conditions.add(groupOnCondition)
        day?.let {
            val dayOfWeekCondition =
                DayOfWeekCondition(gson.fromJson(day.days.toString(), Array<DayOfWeek>::class.java))
            conditions.add(dayOfWeekCondition)
        }
        if (isAllDay.value == false && startTime != null && endTime != null) {
            val timeIntervalCondition = TimeIntervalCondition(
                startHour = startTime.hour,
                startMin = startTime.minute,
                endHour = endTime.hour,
                endMin = endTime.minute
            )
            conditions.add(timeIntervalCondition)
        }

        return conditions
    }

    private fun createActions(scene: Scene): ArrayList<Action> {
        val actions = ArrayList<Action>()

        val sceneAction = UpdateResourceAction(
            path = groupScenes(group = group),
            LogicData(id = scene.id)
        )
        actions.add(sceneAction)

        return actions
    }


    private suspend fun setupSunriseSunset(
        startTime: TriggerTime,
        endTime: TriggerTime,
        conditions: ArrayList<Condition>,
        triggers: ArrayList<Trigger>,
        logicRuleId: Int
    ) {
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
                                App.context.getString(
                                    R.string.sunrisePositiveOffsetHours,
                                    startTime.offset
                                )
                            startMinute =
                                App.context.getString(
                                    R.string.sunrisePositiveOffsetMinutes,
                                    startTime.offset
                                )
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
                                App.context.getString(
                                    R.string.sunsetPositiveOffsetHours,
                                    startTime.offset
                                )
                            startMinute =
                                App.context.getString(
                                    R.string.sunsetPositiveOffsetMinutes,
                                    startTime.offset
                                )
                        }
                        startTime.offset < 0 -> {
                            startHour =
                                App.context.getString(
                                    R.string.sunsetNegativeOffsetHours,
                                    abs(startTime.offset)
                                )
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
                            endHour = App.context.getString(
                                R.string.sunrisePositiveOffsetHours,
                                endTime.offset
                            )
                            endMinute =
                                App.context.getString(
                                    R.string.sunrisePositiveOffsetMinutes,
                                    endTime.offset
                                )
                        }
                        endTime.offset < 0 -> {
                            endHour =
                                App.context.getString(
                                    R.string.sunriseNegativeOffsetHours,
                                    abs(endTime.offset)
                                )
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
                            endHour = App.context.getString(
                                R.string.sunsetPositiveOffsetHours,
                                endTime.offset
                            )
                            endMinute =
                                App.context.getString(
                                    R.string.sunsetPositiveOffsetMinutes,
                                    endTime.offset
                                )
                        }
                        endTime.offset < 0 -> {
                            endHour =
                                App.context.getString(
                                    R.string.sunsetNegativeOffsetHours,
                                    abs(endTime.offset)
                                )
                            endMinute =
                                App.context.getString(
                                    R.string.sunsetNegativeOffsetMinutes,
                                    abs(endTime.offset)
                                )
                        }
                        else -> {
                            endHour = getString(R.string.sunsetHour)
                            endMinute = getString(R.string.sunsetMinute)
                        }
                    }
                }
            }
        }

        conditions.removeAll { it is TimeIntervalCondition }

        val sunriseSunsetCondition = TimeIntervalCondition(
            startHour,
            startMinute,
            endHour,
            endMinute
        )
        conditions.add(sunriseSunsetCondition)

        val data = LogicData(conditions = conditions.toTypedArray())

        if (startTime.trigger == SunriseSunsetType.SUNRISE || startTime.trigger == SunriseSunsetType.SUNSET) {
            triggers.removeAll { it is TimeOfDayTrigger }

            val sunriseSunsetTrigger = TimeOfDayTrigger(
                startHour,
                startMinute
            )
            triggers.add(sunriseSunsetTrigger)

            data.triggers = triggers.toTypedArray()
        }

        val offsets: Array<SunriseSunsetOffsetWrapper> = arrayOf(startTime, endTime).mapNotNull {
            if (it.offset != 0) {
                SunriseSunsetOffsetWrapper(offset = it.offset, sunriseSunset = it.trigger)
            } else {
                null
            }
        }.toTypedArray()

        val action = UpdateResourceAction(logicRule(logicCollectionId = logicCollection.id, logicRuleId = logicRuleId), data)

        SunriseSunsetHandler.addSunriseSunsetAction(actions = arrayOf(SunriseSunsetActionWrapper(action, offsets)))
    }

    suspend fun deleteLogicRule() {
        val gateway = NabtoHandler.selectedGateway ?: return
        val logicRule = logicRule.value ?: return
        DevelcoHandler.deleteLogicRule(gateway, logicCollectionId = logicCollection.id, logicRuleId = logicRule.id)
    }

    override fun setIsAllDay(isAllDay: Boolean) {
        (this.isAllDay as MutableLiveData).postValue(isAllDay)
    }

    override fun updateStartTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        (startTime as MutableLiveData).postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }

    override fun updateEndTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        (endTime as MutableLiveData).postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }

    companion object {
        private val TAG = this::class.simpleName
    }
}

class DynamicEventViewModelFactory(private val logicCollection: LogicCollection, val logicRule: LogicRule?, val group: Group): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DynamicEventViewModel::class.java)) {
            return DynamicEventViewModel(logicCollection = logicCollection, logicRule = logicRule, group = group) as T
        }

        throw IllegalArgumentException("Unknown ViewModelClass")
    }

}