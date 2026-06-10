package com.aurora.aonev3

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.timers.LogicTimer
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.deviceDatapoint
import com.aurora.aonev3.network.groupDatapoint
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.logicTimer
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.aurora.aonev3.ui.fragments.schedules.EventTarget
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.collections.ArrayList

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.aurora.aonev3.debug", appContext.packageName)
    }

    @Test
    fun testDoorEventSensor() {
        val response = createLogicCollection()

//        val response2 = createTimer(response.optInt("id"), true, false)
//        val response3 = createTimer(response.optInt("id"), false, true)

//        val response4 = createStartTimerRule(response.optInt("id"), response2.optInt("id"), response3.optInt("id"))

        assert(true)
    }

    private fun createLogicCollection(): JSONObject {

        val gateway = NabtoHandler.NabtoGateway(
            "02000001000014DE",
            "02000001000014DE",
            "02000001000014DE",
            "02000001000014DE",
            "02000001000014DE",
            ArrayList(),
            NabtoHandler.GatewayAccessLevel.OWNER
        )
        val triggerDevice = Device(
            parentGateway = "02000001000014DE",
            eui = "0015BC001E007B73",
            id = 13,
            name = "Window Sensor",
            defaultName = "Window Sensor",
            metadata = JSONObject(),
            online = false
        )
        val day: EventDay? = EventDay.EVERYDAY
        val isAllDay = false
        val startTime: Pair<Int,Int>? = Pair(2,0)
        val endTime: Pair<Int,Int>? = Pair(5,0)
        val target = EventTarget.DEVICE
        val scene: Scene? = null
        val group: Group? = null
        val device = Pair(
            Device(
                parentGateway = "02000001000014DE",
                eui = "00158D000361F0C1",
                id = 2,
                name = "Bedroom",
                defaultName = "Aurora Smart Lamp (RGBW)",
                metadata = JSONObject(),
                online = true
            ), "bulb")


        val metadata = CollectionMetadata()
        metadata.collectionType = CollectionType.SENSOR
        metadata.triggerId = triggerDevice.id

        val event = EventMetadata()
        event.days = day?.name?.toLowerCase(Locale.UK)
        if (!isAllDay && startTime != null && endTime != null) {
            event.time = TimeEventMetadata(startHour = startTime.first, startMinute =  startTime.second, endHour = endTime.first, endMinute = endTime.second)
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
        metadata.event = event

        val logicCollection = LogicCollection(gateway.serial, -1, triggerDevice.name, metadata, true).toJSONObject()
        logicCollection.remove("id")

        return FakeDevelcoHandler.postLogicCollection(gateway, logicCollection).optJSONObject("body")!!
    }

//    fun createTimer(collectionId: Int, onoff: Boolean, override: Boolean): JSONObject {
//
//        val gateway = NabtoHandler.NabtoGateway(
//            "02000001000014DE",
//            "02000001000014DE",
//            "02000001000014DE",
//            "02000001000014DE",
//            "02000001000014DE",
//            ArrayList(),
//            NabtoHandler.GatewayAccessLevel.OWNER
//        )
//        val triggerDevice = Device(
//            parentGateway = "02000001000014DE",
//            eui = "0015BC001E007B73",
//            id = 13,
//            name = "Window Sensor",
//            defaultName = "Window Sensor",
//            metadata = JSONObject(),
//            online = false
//        )
//        val day: EventDay? = EventDay.EVERYDAY
//        val isAllDay = false
//        val startTime: Pair<Int,Int>? = Pair(2,0)
//        val endTime: Pair<Int,Int>? = Pair(5,0)
//        val timeout = 240
//        val target = EventTarget.DEVICE
//        val scene: Scene? = null
//        val group: Group = Group("02000001000014DE", 1, "group", JSONObject(), "generic")
//
//        val actions = if (onoff) {
//            val action = when (target) {
//                EventTarget.SCENE -> {
//                    buildAction(groupDatapoint(group, datapoint = "/onoff"), value = false)
//                }
//                EventTarget.SPACE -> {
//                    buildAction(groupDatapoint(group, datapoint = "/onoff"), value = false)
//                }
//                EventTarget.DEVICE -> {
//                    val device = Pair(
//                        Device(
//                            parentGateway = "02000001000014DE",
//                            eui = "00158D000361F0C1",
//                            id = 2,
//                            name = "Bedroom",
//                            defaultName = "Aurora Smart Lamp (RGBW)",
//                            metadata = JSONObject(),
//                            online = true
//                        ), "bulb")
//                    buildAction(deviceDatapoint(device.first, device.second, datapoint = "/onoff"), value = false)
//                }
//            }
//            JSONArray().put(JSONObject(gson.toJson(action)))
//        } else {
//            null
//        }
//
//        val logicTimer = LogicTimer(gateway.serial, collectionId, -1, "", JSONObject(), timeout, actions, 0, "idle").toJSONObject()
//        logicTimer.remove("id")
//        logicTimer.remove("status")
//
//        return FakeDevelcoHandler.postLogicTimer(gateway, collectionId, logicTimer).optJSONObject("body")!!
//    }

    fun createStartTimerRule(collectionId: Int, offTimerId: Int, overrideTimerId: Int): JSONObject {
        val gateway = NabtoHandler.NabtoGateway(
            "02000001000014DE",
            "02000001000014DE",
            "02000001000014DE",
            "02000001000014DE",
            "02000001000014DE",
            ArrayList(),
            NabtoHandler.GatewayAccessLevel.OWNER
        )
        val triggerDevice = Device(
            parentGateway = "02000001000014DE",
            eui = "00158D000361F0C1",
            id = 2,
            name = "Bedroom",
            defaultName = "Aurora Smart Lamp (RGBW)",
            metadata = JSONObject(),
            online = true
        )
        val day = EventDay.EVERYDAY

        val trigger = buildTrigger(deviceDatapoint(triggerDevice, "alarm", "/alarm"))
        val triggers = arrayOf(trigger)

        val triggerCondition = buildCondition(deviceDatapoint(triggerDevice, "alarm", "/alarm.value"), false)
        val dayOfWeekCondition = DayOfWeekCondition(gson.fromJson(day.days.toString(), Array<DayOfWeek>::class.java))
        val conditions = arrayOf(triggerCondition, dayOfWeekCondition)

        val offTimerAction = buildAction(logicTimer(collectionId, offTimerId), dataKey = "command", value = "start")
        val overrideTimerAction = buildAction(logicTimer(collectionId, overrideTimerId), dataKey = "command", value = "start")
        val actions = arrayOf(offTimerAction, overrideTimerAction)

        val metadata = RuleMetadata(RuleMetadataType.START_TIMER)

        val logicRule = LogicRule(gateway.serial, collectionId, -1, "", metadata, true, triggers, conditions, actions).toJSONObject()
        logicRule.remove("id")
        logicRule.remove("name")

        return FakeDevelcoHandler.postLogicRule(gateway, collectionId, logicRule)
    }

    object FakeDevelcoHandler {
        fun postLogicCollection(gateway: NabtoHandler.NabtoGateway, logicCollection: JSONObject): JSONObject {
            return JSONObject()
                .put("status", 201)
                .put("body", logicCollection.put("id", 1))
        }
        fun postLogicTimer(gateway: NabtoHandler.NabtoGateway, collectionId: Int, logicTimer: JSONObject): JSONObject {
            return JSONObject()
                .put("status", 201)
                .put("body", logicTimer.put("id", 1))
        }
        fun postLogicRule(gateway: NabtoHandler.NabtoGateway, collectionId: Int, logicRule: JSONObject): JSONObject {
            return JSONObject()
                .put("status", 201)
                .put("body", logicRule.put("id", 1))
        }
    }
}
