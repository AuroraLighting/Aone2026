package com.aurora.aonev3.network.handlers

import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.R.string.*
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.timers.LogicTimer
import com.aurora.aonev3.debug
import com.aurora.aonev3.getString
import com.aurora.aonev3.gson
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.logicRule
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

object SunriseSunsetHandler {
    private val gateway
        get() = NabtoHandler.selectedGateway

    private val sunriseSunsetLogicCollection: LogicCollection?
    get() = SyncHandler
        .logicCollectionsList
        .find {
            it.parentGateway == gateway?.serial && it.metadata.collectionType == CollectionType.SUNRISE_SUNSET
        }
    private val sunriseSunsetRule: LogicRule?
        get() = SyncHandler
        .logicRulesList
        .find { it.parentGateway == gateway?.serial && it.logicCollectionId == sunriseSunsetLogicCollection?.id }
    private val sunriseSunsetTimer: LogicTimer?
        get() = SyncHandler
        .logicTimersList
        .find { it.parentGateway == gateway?.serial && it.logicCollectionId == sunriseSunsetLogicCollection?.id }

    suspend fun removeSunriseSunsetAction() {
        val gateway = gateway ?: return
        val sunriseSunsetLogicCollection = sunriseSunsetLogicCollection ?: return
        val sunriseSunsetRule = sunriseSunsetRule ?: return
        val sunriseSunsetTimer = sunriseSunsetTimer ?: return

        var existingActions = ArrayList<Action>()
        existingActions.addAll(sunriseSunsetTimer.actions ?: emptyArray())
        if (existingActions.isEmpty()) return

        existingActions = checkExistingActions(existingActions = existingActions)

        val newSunriseSunsetActions = JSONArray(gson.toJson(existingActions))

        try {
            DevelcoHandler.putLogicTimer(
                gateway,
                sunriseSunsetLogicCollection.id,
                sunriseSunsetTimer.id,
                JSONObject().put("actions", newSunriseSunsetActions)
            )
            DevelcoHandler.putLogicTimer(
                gateway,
                sunriseSunsetLogicCollection.id,
                sunriseSunsetTimer.id,
                JSONObject().put("command", "start")
            )
        } catch (err: VolleyError) {
            err.printStackTrace()
        }
    }

    suspend fun addSunriseSunsetAction(actions: Array<SunriseSunsetActionWrapper>) {
        val gateway = gateway ?: return

        if (sunriseSunsetLogicCollection == null || sunriseSunsetRule == null || sunriseSunsetTimer == null) {
            SyncHandler.syncLogicCollectionsCached(gateway, force = true)
            SyncHandler.syncLogicRulesAndTimersCached(gateway, force = true)
        }
        val sunriseSunsetLogicCollection = sunriseSunsetLogicCollection ?: return
        val sunriseSunsetRule = sunriseSunsetRule ?: return
        val sunriseSunsetTimer = sunriseSunsetTimer ?: return

        val newActions = actions.map { it.action }
        var existingActions = ArrayList<Action>()
        val setUpActions = sunriseSunsetTimer.actions?.filter { it !is UpdateResourceAction } ?: emptyList()
        val updateResourceActions = sunriseSunsetTimer
            .actions
            ?.filterIsInstance<UpdateResourceAction>()
            ?.distinctBy { it.path } ?: emptyList()
        existingActions.addAll(setUpActions)
        existingActions.addAll(updateResourceActions)
        if (existingActions.isEmpty()) return

        existingActions = addOffsets(actions, existingActions)

        val actionsDidRemove = removeMissingRules(existingActions)
        val newActionsDidRemove = checkToRemoveNewActions(actionsDidRemove.actions, newActions)
        val newActionsToAdd = checkToAddNewActions(newActionsDidRemove.actions, newActions)

        val allActions = ArrayList(newActionsDidRemove.actions)
        allActions.addAll(newActionsToAdd.actions)


        if (!actionsDidRemove.didChange && !newActionsDidRemove.didChange && !newActionsToAdd.didChange) {
            return
        }

        val newSunriseSunsetActions = JSONArray(gson.toJson(allActions))

        try {
            DevelcoHandler.putLogicTimer(
                gateway,
                sunriseSunsetLogicCollection.id,
                sunriseSunsetTimer.id,
                JSONObject().put("actions", newSunriseSunsetActions)
            )
            DevelcoHandler.putLogicTimer(
                gateway,
                sunriseSunsetLogicCollection.id,
                sunriseSunsetTimer.id,
                JSONObject().put("command", "start")
            )
        } catch (err: VolleyError) {
            err.printStackTrace()
        }
    }

    private fun removeMissingRules(
        existingActions: List<Action>
    ): ActionsDidChange {
        var didChange = false
        val filteredActions = existingActions.filter { action ->
            var keep = false

            if (action !is UpdateResourceAction) return@filter true
            if (!action.path.contains("rule")) return@filter true

            val ids = action.path.split("/").mapNotNull { it.toIntOrNull() }
            if (ids.count() != 2) return@filter true

            val rules = SyncHandler.logicRulesList.filter { it.parentGateway == gateway?.id }

            if (rules.any { it.logicCollectionId == ids.first() && it.id == ids.last() }) {
                keep = true
            }

            didChange = didChange || !keep

            keep
        }

        return ActionsDidChange(ArrayList(filteredActions), didChange)
    }

    private fun checkToRemoveNewActions(
        existingActions: List<Action>,
        newActions: List<Action>
    ): ActionsDidChange {
        var didChange = false
        val filteredActions = existingActions.filter { action ->
            val keep: Boolean

            if (action !is UpdateResourceAction) return@filter true
            if (!action.path.contains("rule")) return@filter true

            val newAction: UpdateResourceAction = newActions.find { it is UpdateResourceAction && it.path == action.path } as? UpdateResourceAction ?: return@filter true

            keep = action == newAction

            didChange = didChange || !keep

            keep
        }

        return ActionsDidChange(ArrayList(filteredActions), didChange)
    }

    private fun checkToAddNewActions(
        existingActions: List<Action>,
        newActions: List<Action>
    ): ActionsDidChange {
        var didChange = false
        val filteredActions = newActions.filter { action ->
            val keep: Boolean

            if (action !is UpdateResourceAction) return@filter true
            if (!action.path.contains("rule")) return@filter true

            val newAction = existingActions.find { it is UpdateResourceAction && it.path == action.path }
            if (newAction == null) {
                didChange = true
                return@filter true
            }

            keep = action != newAction

            didChange = didChange || keep

            keep
        }

        return ActionsDidChange(ArrayList(filteredActions), didChange)
    }

    private fun checkExistingActions(
        actionsBeingAdded: List<Action> = emptyList(),
        existingActions: List<Action>
    ): ArrayList<Action> {
        val filteredActions = existingActions.filter { action ->
            var keep = false

            if (action !is UpdateResourceAction) return@filter true

            val ids = action.path.split("/").mapNotNull { it.toIntOrNull() }
            if (ids.count() != 2) return@filter true

            val rules = SyncHandler.logicRulesList.filter { it.parentGateway == gateway?.id }

            if (rules.any { it.logicCollectionId == ids.first() && it.id == ids.last() }) {
                keep = true
            }

            keep =
                keep && !actionsBeingAdded.any { (it as? UpdateResourceAction)?.path == action.path }

            keep
        }

        return ArrayList(filteredActions)
    }

    private fun addOffsets(
        newActions: Array<SunriseSunsetActionWrapper>,
        existingActions: ArrayList<Action>
    ): ArrayList<Action> {
        if (newActions.isEmpty()) return existingActions

        val actionIndex = existingActions.indexOfFirst { it is CalculateValueAction }

        val calculateValueAction = existingActions.getOrNull(actionIndex) as? CalculateValueAction
            ?: return existingActions
        val expressions = ArrayList<Expression>()
        expressions.addAll(calculateValueAction.expressions)

        for (action in newActions) {
            for ((offset, sunriseSunset) in action.offsets) {
                if (offset == 0) continue

                val newExpressionTimeOffsetString: String
                val newExpressionTimeOffsetHourString: String
                val newExpressionTimeOffsetMinutesString: String

                if (offset > 0) {
                    if (sunriseSunset == SunriseSunsetType.SUNRISE) {
                        // ##SUNRISE_P_OFFSET_30##=##SUNRISE_AS_MINUTES##+30
                        newExpressionTimeOffsetString = App.context.getString(
                            expressionTimePositiveOffset,
                            App.context.getString(sunrisePositiveOffset, offset),
                            App.context.getString(sunriseAsMinutes),
                            offset
                        )

                        // ##SUNRISE_P_OFFSET_30_H##=floor(##SUNRISE_P_OFFSET_30##/60)
                        newExpressionTimeOffsetHourString = App.context.getString(
                            expressionHourOffset,
                            App.context.getString(sunrisePositiveOffsetHours, offset),
                            App.context.getString(sunrisePositiveOffset, offset)
                        )

                        // ##SUNRISE_P_OFFSET_30_M##=##SUNRISE_P_OFFSET_30##%60
                        newExpressionTimeOffsetMinutesString = App.context.getString(
                            expressionMinutesOffset,
                            App.context.getString(sunrisePositiveOffsetMinutes, offset),
                            App.context.getString(sunrisePositiveOffset, offset)
                        )
                    } else {
                        // ##SUNSET_P_OFFSET_30##=##SUNSET_AS_MINUTES##+30
                        newExpressionTimeOffsetString = App.context.getString(
                            expressionTimePositiveOffset,
                            App.context.getString(sunsetPositiveOffset, offset),
                            App.context.getString(sunsetAsMinutes),
                            offset
                        )

                        // ##SUNSET_P_OFFSET_30_H##=floor(##SUNSET_P_OFFSET_30##/60)
                        newExpressionTimeOffsetHourString = App.context.getString(
                            expressionHourOffset,
                            App.context.getString(sunsetPositiveOffsetHours, offset),
                            App.context.getString(sunsetPositiveOffset, offset)
                        )

                        // ##SUNSET_P_OFFSET_30_M##=##SUNSET_P_OFFSET_30##%60
                        newExpressionTimeOffsetMinutesString = App.context.getString(
                            expressionMinutesOffset,
                            App.context.getString(sunsetPositiveOffsetMinutes, offset),
                            App.context.getString(sunsetPositiveOffset, offset)
                        )
                    }
                } else {
                    val offsetAbs = abs(offset)
                    if (sunriseSunset == SunriseSunsetType.SUNRISE) {
                        // ##SUNRISE_N_OFFSET_30##=##SUNRISE_AS_MINUTES##+30
                        newExpressionTimeOffsetString = App.context.getString(
                            expressionTimeNegativeOffset,
                            App.context.getString(sunriseNegativeOffset, offsetAbs),
                            App.context.getString(sunriseAsMinutes),
                            offsetAbs
                        )

                        // ##SUNRISE_N_OFFSET_30_H##=floor(##SUNRISE_N_OFFSET_30##/60)
                        newExpressionTimeOffsetHourString = App.context.getString(
                            expressionHourOffset,
                            App.context.getString(sunriseNegativeOffsetHours, offsetAbs),
                            App.context.getString(sunriseNegativeOffset, offsetAbs)
                        )

                        // ##SUNRISE_N_OFFSET_30_M##=##SUNRISE_N_OFFSET_30##%60
                        newExpressionTimeOffsetMinutesString = App.context.getString(
                            expressionMinutesOffset,
                            App.context.getString(sunriseNegativeOffsetMinutes, offsetAbs),
                            App.context.getString(sunriseNegativeOffset, offsetAbs)
                        )
                    } else {
                        // ##SUNSET_N_OFFSET_30##=##SUNSET_AS_MINUTES##+30
                        newExpressionTimeOffsetString = App.context.getString(
                            expressionTimeNegativeOffset,
                            App.context.getString(sunsetNegativeOffset, offsetAbs),
                            App.context.getString(sunsetAsMinutes),
                            offsetAbs
                        )

                        // ##SUNSET_N_OFFSET_30_H##=floor(##SUNSET_N_OFFSET_30##/60)
                        newExpressionTimeOffsetHourString = App.context.getString(
                            expressionHourOffset,
                            App.context.getString(sunsetNegativeOffsetHours, offsetAbs),
                            App.context.getString(sunsetNegativeOffset, offsetAbs)
                        )

                        // ##SUNSET_N_OFFSET_30_M##=##SUNSET_N_OFFSET_30##%60
                        newExpressionTimeOffsetMinutesString = App.context.getString(
                            expressionMinutesOffset,
                            App.context.getString(sunsetNegativeOffsetMinutes, offsetAbs),
                            App.context.getString(sunsetNegativeOffset, offsetAbs)
                        )
                    }
                }

                if (!calculateValueAction.expressions.any { it.expression == newExpressionTimeOffsetString } &&
                    !expressions.any { it.expression == newExpressionTimeOffsetString }) {
                    val newExpressionTimeOffset = Expression(newExpressionTimeOffsetString)
                    val newExpressionTimeOffsetHour = Expression(newExpressionTimeOffsetHourString)
                    val newExpressionTimeOffsetMinutes =
                        Expression(newExpressionTimeOffsetMinutesString)

                    expressions.addAll(
                        arrayOf(
                            newExpressionTimeOffset,
                            newExpressionTimeOffsetHour,
                            newExpressionTimeOffsetMinutes
                        )
                    )
                }
            }
        }
        val newAction = CalculateValueAction(expressions.toTypedArray())
        existingActions[actionIndex] = newAction

        return existingActions
    }

    suspend fun validator() {
        val gateway = gateway ?: return
        if (sunriseSunsetLogicCollection == null) return

        val collections = SyncHandler.logicCollectionsList.filter { it.parentGateway == gateway.serial }
        val rules = SyncHandler.logicRulesList.filter { it.parentGateway == gateway.serial }
        val actions = ArrayList<SunriseSunsetActionWrapper>()

        for (collection in collections) {
            val metadata = collection.metadata
            val event = metadata.event ?: continue
            val time = event.time ?: continue

            if (metadata.collectionType == CollectionType.SENSOR && time.usesSunriseSunset) {
                val startTimeType = SunriseSunsetType.fromMetadata(time.start)
                val endTimeType = SunriseSunsetType.fromMetadata(time.end)
                val startHour: Any
                val startMinute: Any
                val endHour: Any
                val endMinute: Any
                val collectionRules = rules.filter { it.logicCollectionId == collection.id }
                val offsets = ArrayList<SunriseSunsetOffsetWrapper>()

                if (startTimeType == SunriseSunsetType.SUNRISE
                    || startTimeType == SunriseSunsetType.SUNSET) {
                        startHour = getSunriseSunsetHourString(
                            if (startTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                            else TriggerEnum.SUNSET, time.startOffset ?: 0
                        )
                        startMinute = getSunriseSunsetMinuteString(
                            if (startTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                            else TriggerEnum.SUNSET, time.startOffset ?: 0
                        )

                    offsets.add(
                        SunriseSunsetOffsetWrapper(
                            time.startOffset ?: 0,
                            sunriseSunset = startTimeType
                        )
                    )
                } else {
                    startHour = time.startHour ?: 0
                    startMinute = time.startMinute ?: 0
                }

                if (endTimeType == SunriseSunsetType.SUNRISE
                    || endTimeType == SunriseSunsetType.SUNSET) {
                    endHour = getSunriseSunsetHourString(
                        if (endTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET, time.endOffset ?: 0
                    )
                    endMinute = getSunriseSunsetMinuteString(
                        if (endTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET, time.endOffset ?: 0
                    )

                    offsets.add(
                        SunriseSunsetOffsetWrapper(
                            time.endOffset ?: 0,
                            sunriseSunset = endTimeType
                        )
                    )
                } else {
                    endHour = time.endHour ?: 0
                    endMinute = time.endMinute ?: 0
                }


                for (rule in collectionRules) {
                    var newTriggers = ArrayList<Trigger>()

                    if ((SunriseSunsetType.fromMetadata(time.end) == SunriseSunsetType.SUNRISE
                        || SunriseSunsetType.fromMetadata(time.end) == SunriseSunsetType.SUNSET)
                        && rule.triggers?.any { it is TimeOfDayTrigger } == true) {
                        newTriggers = ArrayList(
                            rule.triggers?.filter { it !is TimeOfDayTrigger }
                                ?: emptyList<Trigger>()
                        )

                        newTriggers.add(TimeOfDayTrigger(endHour, endMinute))
                    }

                    val newConditions: ArrayList<Condition> = ArrayList(
                        rule.conditions?.filter { it !is TimeIntervalCondition }
                            ?: emptyList<Condition>()
                    )

                    newConditions.add(TimeIntervalCondition(startHour, startMinute, endHour, endMinute))

                    val data = if (newTriggers.isNotEmpty()) {
                        LogicData(triggers = newTriggers.toTypedArray(), conditions = newConditions.toTypedArray())
                    } else {
                        LogicData(conditions = newConditions.toTypedArray())
                    }

                    val action = UpdateResourceAction(
                        logicRule(logicCollectionId = collection.id, logicRuleId = rule.id), data
                    )

                    actions.add(SunriseSunsetActionWrapper(action, offsets = offsets.toTypedArray()))
                }
            }
        }

        for (rule in rules) {
            val collection = collections.find { it.id == rule.logicCollectionId } ?: continue
            val collectionMetadata = collection.metadata
            val metadata = rule.metadata
            val event = metadata.event ?: continue
            val time = event.time

            if (event.trigger == TriggerEnum.SUNRISE || event.trigger == TriggerEnum.SUNSET) {
                val timeType: SunriseSunsetType = if (event.trigger == TriggerEnum.SUNRISE) SunriseSunsetType.SUNRISE else SunriseSunsetType.SUNSET
                val hour: Any
                val minute: Any
                val offsets = ArrayList<SunriseSunsetOffsetWrapper>()
                var newTriggers: ArrayList<Trigger>

                if (timeType == SunriseSunsetType.SUNRISE
                    || timeType == SunriseSunsetType.SUNSET) {
                    hour = getSunriseSunsetHourString(
                        if (timeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET,
                        event.triggerOffset ?: 0
                    )
                    minute = getSunriseSunsetMinuteString(
                        if (timeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET,
                        event.triggerOffset ?: 0
                    )

                    offsets.add(
                        SunriseSunsetOffsetWrapper(
                            event.triggerOffset ?: 0,
                            sunriseSunset = timeType
                        )
                    )

                    newTriggers = ArrayList(
                        rule
                            .triggers
                            ?.filter { it !is TimeOfDayTrigger } ?: emptyList<Trigger>()
                    )

                    newTriggers.add(TimeOfDayTrigger(hour, minute))

                    val data = LogicData(triggers = newTriggers.toTypedArray())

                    val action = UpdateResourceAction(
                        logicRule(logicCollectionId = collection.id, logicRuleId = rule.id), data
                    )

                    actions.add(SunriseSunsetActionWrapper(action, offsets = offsets.toTypedArray()))
                }
            }

            if (time?.usesSunriseSunset == true) {
                val startTimeType = SunriseSunsetType.fromMetadata(time.start)
                val endTimeType = SunriseSunsetType.fromMetadata(time.end)
                val startHour: Any
                val startMinute: Any
                val endHour: Any
                val endMinute: Any
                val offsets = ArrayList<SunriseSunsetOffsetWrapper>()

                if (startTimeType == SunriseSunsetType.SUNRISE
                    || startTimeType == SunriseSunsetType.SUNSET) {
                    startHour = getSunriseSunsetHourString(
                        if (startTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET, time.startOffset ?: 0
                    )
                    startMinute = getSunriseSunsetMinuteString(
                        if (startTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET, time.startOffset ?: 0
                    )

                    offsets.add(
                        SunriseSunsetOffsetWrapper(
                            time.startOffset ?: 0,
                            sunriseSunset = startTimeType
                        )
                    )
                } else {
                    startHour = time.startHour ?: 0
                    startMinute = time.startMinute ?: 0
                }

                if (endTimeType == SunriseSunsetType.SUNRISE
                    || endTimeType == SunriseSunsetType.SUNSET) {
                    endHour = getSunriseSunsetHourString(
                        if (endTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET, time.startOffset ?: 0
                    )
                    endMinute = getSunriseSunsetMinuteString(
                        if (endTimeType == SunriseSunsetType.SUNRISE) TriggerEnum.SUNRISE
                        else TriggerEnum.SUNSET, time.startOffset ?: 0
                    )

                    offsets.add(
                        SunriseSunsetOffsetWrapper(
                            time.endOffset ?: 0,
                            sunriseSunset = endTimeType
                        )
                    )
                } else {
                    endHour = time.endHour ?: 0
                    endMinute = time.endMinute ?: 0
                }


                var newTriggers = ArrayList<Trigger>()
                val newConditions: ArrayList<Condition> = ArrayList(
                    rule.conditions?.filter { it !is TimeIntervalCondition }
                        ?: emptyList<Condition>()
                )

                newConditions.add(TimeIntervalCondition(startHour, startMinute, endHour, endMinute))

                if (collectionMetadata.collectionType == CollectionType.DYNAMIC_EVENT) {
                    if ((SunriseSunsetType.fromMetadata(time.start) == SunriseSunsetType.SUNRISE
                                || SunriseSunsetType.fromMetadata(time.start) == SunriseSunsetType.SUNSET)
                        && rule.triggers?.any { it is TimeOfDayTrigger } == true) {
                        newTriggers = ArrayList(
                            rule.triggers?.filter { it !is TimeOfDayTrigger }
                                ?: emptyList<Trigger>()
                        )

                        newTriggers.add(TimeOfDayTrigger(startHour, startMinute))
                    }
                }

                val data = if (newTriggers.isNotEmpty()) {
                    LogicData(triggers = newTriggers.toTypedArray(), conditions = newConditions.toTypedArray())
                } else {
                    LogicData(conditions = newConditions.toTypedArray())
                }

                val action = UpdateResourceAction(
                    logicRule(logicCollectionId = collection.id, logicRuleId = rule.id), data
                )

                actions.add(SunriseSunsetActionWrapper(action, offsets = offsets.toTypedArray()))
            }
        }

        addSunriseSunsetAction(actions.toTypedArray())
    }

    fun getSunriseSunsetHourString(type: TriggerEnum, offset: Int = 0): String {
        return if (type == TriggerEnum.SUNRISE) {
            when {
                offset > 0 -> {
                    App.context.getString(sunrisePositiveOffsetHours, offset)
                }
                offset < 0 -> {
                    App.context.getString(sunriseNegativeOffsetHours, abs(offset))
                }
                else -> {
                    getString(sunriseHour)
                }
            }
        } else {
            when {
                offset > 0 -> {
                    App.context.getString(sunsetPositiveOffsetHours, offset)
                }
                offset < 0 -> {
                    App.context.getString(sunsetNegativeOffsetHours, abs(offset))
                }
                else -> {
                    getString(sunsetHour)
                }
            }
        }
    }

    fun getSunriseSunsetMinuteString(type: TriggerEnum, offset: Int = 0): String {
        return if (type == TriggerEnum.SUNRISE) {
            when {
                offset > 0 -> {
                    App.context.getString(sunrisePositiveOffsetMinutes, offset)
                }
                offset < 0 -> {
                    App.context.getString(sunriseNegativeOffsetMinutes, abs(offset))
                }
                else -> {
                    getString(sunriseMinute)
                }
            }
        } else {
            when {
                offset > 0 -> {
                    App.context.getString(sunsetPositiveOffsetMinutes, offset)
                }
                offset < 0 -> {
                    App.context.getString(sunsetNegativeOffsetMinutes, abs(offset))
                }
                else -> {
                    getString(sunsetMinute)
                }
            }
        }
    }

    private data class ActionsDidChange(val actions: ArrayList<Action>, val didChange: Boolean)
}

data class SunriseSunsetActionWrapper(
    val action: Action,
    val offsets: Array<SunriseSunsetOffsetWrapper>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SunriseSunsetActionWrapper

        if (action != other.action) return false
        if (!offsets.contentEquals(other.offsets)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + offsets.contentHashCode()
        return result
    }
}

data class SunriseSunsetOffsetWrapper(val offset: Int, val sunriseSunset: SunriseSunsetType)