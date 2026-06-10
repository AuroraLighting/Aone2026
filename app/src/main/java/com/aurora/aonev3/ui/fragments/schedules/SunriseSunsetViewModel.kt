package com.aurora.aonev3.ui.fragments.schedules

import android.location.Location
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.rules.LogicRuleRepository
import com.aurora.aonev3.data.logic.rules.NewLogicRule
import com.aurora.aonev3.data.logic.timers.LogicTimer
import com.aurora.aonev3.data.logic.timers.LogicTimerRepository
import com.aurora.aonev3.data.logic.timers.NewLogicTimer
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.logicTimer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.security.auth.login.LoginException

class SunriseSunsetViewModel: ViewModel() {
    private val collectionRepository = LogicCollectionRepository()
    private val ruleRepository = LogicRuleRepository()
    private val timerRepository = LogicTimerRepository()
    var sunriseSunsetLogicCollection: LogicCollection? = null
    var sunriseSunsetRules: List<LogicRule>? = null
    var sunriseSunsetTimers: List<LogicTimer>? = null
    var rules: List<LogicRule> = emptyList()
    var timers: List<LogicTimer> = emptyList()

    fun getCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> = collectionRepository.getLogicCollections(gateway)
    fun getRules(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicRule> = ruleRepository.getAllLogicRules(gateway)
    fun getTimers(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicTimer> = timerRepository.getAllLogicTimers(gateway)

    @Throws(VolleyError::class)
    suspend fun createSunriseSunsetCollection(location: Location) {
        val gateway = NabtoHandler.selectedGateway ?: return
        if (!gateway.isConnected) throw VolleyError()

        val metadata = CollectionMetadata(collectionType = CollectionType.SUNRISE_SUNSET)
        val collection = JSONObject()
            .put("name", "Sunrise / sunset")
            .put("metadata", gson.toJson(metadata))

        val response = DevelcoHandler.postLogicCollection(gateway, collection)
        val collectionId = response.optJSONObject("body")?.optInt("id") ?: throw VolleyError()


        val getTzOffsetAction = GetResourceAction(
            path = "config/time",
            mappings = arrayOf(
                Mapping(xpath = "$.tzoffset:integer", variable = "##TZ_OFFSET_S##"),
                Mapping(xpath = "$.dst:boolean", variable = "##IS_DST##")
            )
        )

        val getSunriseSunsetTimeAction = ExternalApiRequestAction(
            uri = "https://api.sunrise-sunset.org/json?lat=${location.latitude}&lng=${location.longitude}&formatted=0",
            method = "GET",
            mappings = arrayOf(
                Mapping(
                    xpath = "$.results.sunrise:string",
                    variable = App.context.getString(R.string.sunriseVar)
                ),
                Mapping(
                    xpath = "$.results.sunset:string",
                    variable = App.context.getString(R.string.sunsetVar)
                ),
            )
        )

        val expressions = arrayOf(
            Expression(
                expression = App.context.getString(
                    R.string.expressionVarToHour,
                    App.context.getString(R.string.sunriseHour),
                    App.context.getString(R.string.sunriseVar)
                )
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionHourToHourTimezone,
                    App.context.getString(R.string.sunriseHour),
                    App.context.getString(R.string.sunriseHour)
                ),
                rule = "##IS_DST##==false"
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionHourToHourTimezoneDst,
                    App.context.getString(R.string.sunriseHour),
                    App.context.getString(R.string.sunriseHour)
                ),
                rule = "##IS_DST##==true"
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionVarToMinutes,
                    App.context.getString(R.string.sunriseMinute),
                    App.context.getString(R.string.sunriseVar)
                )
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionTimeToMinutes,
                    App.context.getString(R.string.sunriseAsMinutes),
                    App.context.getString(R.string.sunriseHour),
                    App.context.getString(R.string.sunriseMinute)
                )
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionVarToHour,
                    App.context.getString(R.string.sunsetHour),
                    App.context.getString(R.string.sunsetVar)
                )
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionHourToHourTimezone,
                    App.context.getString(R.string.sunsetHour),
                    App.context.getString(R.string.sunsetHour)
                ),
                rule = "##IS_DST##==false"
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionHourToHourTimezoneDst,
                    App.context.getString(R.string.sunsetHour),
                    App.context.getString(R.string.sunsetHour)
                ),
                rule = "##IS_DST##==true"
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionVarToMinutes,
                    App.context.getString(R.string.sunsetMinute),
                    App.context.getString(R.string.sunsetVar)
                )
            ),
            Expression(
                expression = App.context.getString(
                    R.string.expressionTimeToMinutes,
                    App.context.getString(R.string.sunsetAsMinutes),
                    App.context.getString(R.string.sunsetHour),
                    App.context.getString(R.string.sunsetMinute)
                )
            )
        )
        val calculateValueAction = CalculateValueAction(expressions)

        val actions = arrayOf(getTzOffsetAction, getSunriseSunsetTimeAction, calculateValueAction)

        val timer = NewLogicTimer(name = "Update sunrise / sunset", timeout = 0, actions = actions)

        val timerResponse =
            DevelcoHandler.postLogicTimer(gateway, collectionId, timer.toJSONObject())
        val timerId = timerResponse.optJSONObject("body")?.optInt("id") ?: throw VolleyError()

        val triggers: Array<Trigger> = arrayOf(TimeOfDayTrigger(hour = 12, min = 0))
        val ruleActions: Array<Action> = arrayOf(
            UpdateResourceAction(
                path = logicTimer(logicCollectionId = collectionId, logicTimerId = timerId),
                LogicData(command = "start")
            )
        )

        val rule = NewLogicRule(
            name = "Update sunrise / sunset",
            triggers = triggers,
            actions = ruleActions
        )

        DevelcoHandler.postLogicRule(gateway, collectionId, rule.toJSONObject())

    }
}