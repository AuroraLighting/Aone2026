package com.aurora.aonev3.network.handlers

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import com.android.volley.VolleyError
import com.aurora.aonev3.network.volley.Request
import com.aurora.aonev3.replace
import org.json.JSONArray
import org.json.JSONObject

object DevelcoHandler {

    enum class Endpoints(val url: String, val regex: Regex) {
        BASE("http://127.0.0.1:{PORT}/ssapi", Regex("""http://127.0.0.1:\d+/ssapi""")),
        WEBSOCKET_BASE("ws://127.0.0.1:{PORT}/ssapi/ws", Regex("""ws://127.0.0.1:\d+/ssapi/ws""")),

        ZIGBEE("zb", Regex("""zb""")),

        DEVICES("zb/dev", Regex("""zb/dev""")),
        DEVICE("zb/dev/{ID}", Regex("""zb/dev/\d+""")),
        DEVICE_LDEV("zb/dev/{ID}/ldev/{LDEV}", Regex("""zb/dev/\d+/ldev/\w+""")),
        DEVICE_DATAPOINT("zb/dev/{ID}/ldev/{LDEV}/data{DATAPOINT}", Regex("""zb/dev/\d+/ldev/\w+/data/\w+""")),
        DEVICE_DATA("zb/dev/{ID}/ldev/{LDEV}/data", Regex("""zb/dev/\d+/ldev/\w+/data""")),

        GROUPS("grp/grps", Regex("""grp/grps""")),
        GROUP("grp/grps/{ID}", Regex("""grp/grps/\d+""")),
        GROUP_DATAPOINT("grp/grps/{ID}/ldev/{LDEV}/data{DATAPOINT}", Regex("""grp/grps/\d+/ldev/\w+/data/\w+""")),
        GROUP_DATA("grp/grps/{ID}/ldev/{LDEV}/data", Regex("""grp/grps/\d+/ldev/\w+/data""")),
        GROUP_DATAPOINT_AND_SCENES("grp/grps/{ID}/ldev/{LDEV}/+", Regex("""grp/grps/\d+/ldev/\w+/\w+/\w+""")),
        GROUP_SCENES("grp/grps/{ID}/ldev/{LDEV}/scene", Regex("""grp/grps/\d+/ldev/\w+/scene""")),
        GROUP_SCENE("grp/grps/{GROUP_ID}/ldev/{LDEV}/scene/{SCENE_ID}", Regex("""grp/grps/\d+/ldev/\w+/scene/\d+""")),
        GROUP_MEMBERS("grp/grps/{ID}/mbrs", Regex("""grp/grps/\d+/mbrs/\d+""")),
        GROUP_MEMBER("grp/grps/{GROUP_ID}/mbrs/{MEMBER_ID}", Regex("""grp/grps/\d+/mbrs/\d+""")),

        LOGIC_COLLECTIONS("logic/collection", Regex("""logic/collection""")),
        LOGIC_COLLECTION("logic/collection/{ID}", Regex("""logic/collection/\d+""")),
        LOGIC_RULES("logic/collection/{ID}/rule", Regex("""logic/collection/\d+/rule""")),
        LOGIC_RULE("logic/collection/{COLLECTION_ID}/rule/{RULE_ID}", Regex("""logic/collection/\d+/rule/\d+""")),
        LOGIC_TIMERS("logic/collection/{ID}/timer", Regex("""logic/collection/\d+/timer""")),
        LOGIC_TIMER("logic/collection/{COLLECTION_ID}/timer/{TIMER_ID}", Regex("""logic/collection/\d+/timer/\d+""")),
        LOGIC_RULES_AND_TIMERS("logic/collection/{ID}/+", Regex("""logic/collection/\d+/\w+""")),

        FIRMWARE_DOWNLOADS("fw/downloads", Regex("""fw/downloads""")),
        FIRMWARE_IMAGES("fw/images", Regex("""fw/images""")),
        FIRMWARE_IMAGE("fw/images/{ID}", Regex("""fw/images/\d+""")),
        FIRMWARE_STATUS("fw/status", Regex("""fw/status""")),

        MQTT_CONFIG("config/mqtt", Regex("""config/mqtt""")),
        MQTT_STATUS("config/mqtt/status", Regex("""config/mqtt/status""")),
        TIME("config/time", Regex("""config/time""")),
        TEMPLATES("config/template", Regex("""config/template""")),
        TEMPLATE("config/template/{HASH}", Regex("""config/template/\w+""")),
        EXTERNALS("config/external", Regex("""config/external""")),
        EXTERNAL("config/external/{KEY}", Regex("""config/external"""))
    }

    val prospects = MutableLiveData<ArrayList<JSONObject>>(ArrayList())

    @Throws(VolleyError::class)
    suspend fun putZigbee(gateway: NabtoHandler.NabtoGateway, autoAdd: Boolean = false, enableScan: Boolean, rejectUnknown: Boolean = true): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.ZIGBEE.url}"
        val body: JSONObject = JSONObject()
            .put("autoAdd", autoAdd)
            .put("enableScan", enableScan)
            .put("rejectUnknownDevices", rejectUnknown)
        return Request.put(url, body)
    }

    @Throws(VolleyError::class)
    suspend fun getDevices(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICES.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postDevice(gateway: NabtoHandler.NabtoGateway, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICES.url}"
        return Request.post(
            url,
            body,
            maxRetries,
            first)
    }

    @Throws(VolleyError::class)
    suspend fun putDevice(gateway: NabtoHandler.NabtoGateway, id: Int, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICE.url.replace(Pair("{ID}", id))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteDevice(gateway: NabtoHandler.NabtoGateway, id: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICE.url.replace(Pair("{ID}", id))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getDeviceDatapoint(gateway: NabtoHandler.NabtoGateway, id: Int, ldev: String, datapoint: String? = null, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICE_DATAPOINT.url.replace(Pair("{ID}", id), Pair("{LDEV}", ldev), Pair("{DATAPOINT}", if (datapoint != null) "/$datapoint" else ""))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getAllDevicesDatapoints(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICE_DATA.url.replace(Pair("{ID}", "+")).replace(Pair("{LDEV}", "+"))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putDeviceDatapoint(gateway: NabtoHandler.NabtoGateway, id: Int, ldev: String, datapoint: String, value: Any, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICE_DATAPOINT.url.replace(Pair("{ID}", id), Pair("{LDEV}", ldev), Pair("{DATAPOINT}", "/$datapoint"))}"
        val body: JSONObject = JSONObject()
            .put("value", value)
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putDeviceData(gateway: NabtoHandler.NabtoGateway, id: Int, ldev: String, data: JSONArray, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.DEVICE_DATA.url.replace(Pair("{ID}", id), Pair("{LDEV}", ldev))}"
        return Request.put(url, data, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getGroups(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUPS.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getGroup(gateway: NabtoHandler.NabtoGateway, id: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP.url.replace(Pair("{ID}", id))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postGroups(gateway: NabtoHandler.NabtoGateway, body: JSONObject, maxRetries: Int = 0, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUPS.url}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putGroup(gateway: NabtoHandler.NabtoGateway, id: Int, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP.url.replace(Pair("{ID}", id))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteGroup(gateway: NabtoHandler.NabtoGateway, id: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP.url.replace(Pair("{ID}", id))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getGroupDatapoint(gateway: NabtoHandler.NabtoGateway, id: Int, ldev: String, datapoint: String? = null, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_DATAPOINT.url.replace(Pair("{ID}", id), Pair("{LDEV}", ldev), Pair("{DATAPOINT}", if (datapoint != null) "/$datapoint" else ""))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getAllGroupsDatapoints(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_DATAPOINT.url.replace(Pair("{ID}", "+"), Pair("{LDEV}", "+"), Pair("{DATAPOINT}", ""))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getAllGroupsDatapointsAndScenes(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_DATAPOINT_AND_SCENES.url.replace(Pair("{ID}", "+"), Pair("{LDEV}", "+"))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putGroupData(gateway: NabtoHandler.NabtoGateway, id: Int, ldev: String, data: JSONArray, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_DATA.url.replace(Pair("{ID}", id), Pair("{LDEV}", ldev))}"
        return Request.put(url, data, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putGroupDatapoint(gateway: NabtoHandler.NabtoGateway, id: Int, ldev: String, datapoint: String, value: Any, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_DATAPOINT.url.replace(Pair("{ID}", id), Pair("{LDEV}", ldev), Pair("{DATAPOINT}", "/$datapoint"))}"
        val body: JSONObject = JSONObject()
            .put("value", value)
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getGroupScenes(gateway: NabtoHandler.NabtoGateway, groupId: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_SCENES.url.replace(Pair("{ID}", groupId), Pair("{LDEV}", "generic"))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postGroupScenes(gateway: NabtoHandler.NabtoGateway, groupId: Int, body: JSONObject, maxRetries: Int = 0, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_SCENES.url.replace(Pair("{ID}", groupId), Pair("{LDEV}", "generic"))}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putGroupScenes(gateway: NabtoHandler.NabtoGateway, groupId: Int, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_SCENES.url.replace(Pair("{ID}", groupId), Pair("{LDEV}", "generic"))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putGroupScene(gateway: NabtoHandler.NabtoGateway, groupId: Int, sceneId: Int, body: JSONObject?, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_SCENE.url.replace(Pair("{GROUP_ID}", groupId), Pair("{LDEV}", "generic"), Pair("{SCENE_ID}", sceneId))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteGroupScene(gateway: NabtoHandler.NabtoGateway, groupId: Int, sceneId: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_SCENE.url.replace(Pair("{GROUP_ID}", groupId), Pair("{LDEV}", "generic"), Pair("{SCENE_ID}", sceneId))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getGroupMembers(gateway: NabtoHandler.NabtoGateway, groupId: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_MEMBERS.url.replace(Pair("{ID}", groupId))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getAllGroupsMembers(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_MEMBERS.url.replace(Pair("{ID}", "+"))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postGroupMember(gateway: NabtoHandler.NabtoGateway, groupId: Int, deviceId: Int, deviceLdev: String, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_MEMBERS.url.replace(Pair("{ID}", groupId))}"
        val body: JSONObject = JSONObject()
            .put("resource", Endpoints.DEVICE_LDEV.url.replace(Pair("{ID}", deviceId), Pair("{LDEV}", deviceLdev)))
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteGroupMember(gateway: NabtoHandler.NabtoGateway, groupId: Int, memberId: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.GROUP_MEMBER.url.replace(Pair("{GROUP_ID}", groupId), Pair("{MEMBER_ID}", memberId))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getLogicCollections(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_COLLECTIONS.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postLogicCollection(gateway: NabtoHandler.NabtoGateway, body: JSONObject, maxRetries: Int = 0, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_COLLECTIONS.url}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putLogicCollection(gateway: NabtoHandler.NabtoGateway, logicCollectionId: Int, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_COLLECTION.url.replace(Pair("{ID}", logicCollectionId))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteLogicCollection(gateway: NabtoHandler.NabtoGateway, id: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_COLLECTION.url.replace(Pair("{ID}", id))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getAllLogicRulesAndTimers(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_RULES_AND_TIMERS.url.replace(Pair("{ID}", "+"))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getLogicRules(gateway: NabtoHandler.NabtoGateway, id: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_RULES.url.replace(Pair("{ID}", id))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postLogicRule(gateway: NabtoHandler.NabtoGateway, logicCollectionId: Int, body: JSONObject, maxRetries: Int = 0, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_RULES.url.replace(Pair("{ID}", logicCollectionId))}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putLogicRule(gateway: NabtoHandler.NabtoGateway, logicCollectionId: Int, logicRuleId: Int, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_RULE.url.replace(Pair("{COLLECTION_ID}", logicCollectionId), Pair("{RULE_ID}", logicRuleId))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteLogicRule(gateway: NabtoHandler.NabtoGateway, logicCollectionId: Int, logicRuleId: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_RULE.url.replace(Pair("{COLLECTION_ID}", logicCollectionId), Pair("{RULE_ID}", logicRuleId))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getLogicTimers(gateway: NabtoHandler.NabtoGateway, id: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_TIMERS.url.replace(Pair("{ID}", id))}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postLogicTimer(gateway: NabtoHandler.NabtoGateway, logicCollectionId: Int, body: JSONObject, maxRetries: Int =0, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_TIMERS.url.replace(Pair("{ID}", logicCollectionId))}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putLogicTimer(gateway: NabtoHandler.NabtoGateway, logicCollectionId: Int, logicTimerId: Int, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_TIMER.url.replace(Pair("{COLLECTION_ID}", logicCollectionId), Pair("{TIMER_ID}", logicTimerId))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteLogicTimer(gateway: NabtoHandler.NabtoGateway, logicCollectionId: Int, logicTimerId: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.LOGIC_TIMER.url.replace(Pair("{COLLECTION_ID}", logicCollectionId), Pair("{TIMER_ID}", logicTimerId))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getFirmwareDownloads(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.FIRMWARE_DOWNLOADS.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postFirmwareDownload(gateway: NabtoHandler.NabtoGateway, body:JSONObject, maxRetries: Int = 0, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.FIRMWARE_DOWNLOADS.url}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getFirmwareImages(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.FIRMWARE_IMAGES.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteFirmwareImage(gateway: NabtoHandler.NabtoGateway, id: Int, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.FIRMWARE_IMAGE.url.replace(Pair("{ID}", id))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getFirmwareStatus(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.FIRMWARE_STATUS.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getMqttConfig(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.MQTT_CONFIG.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putMqttConfig(gateway: NabtoHandler.NabtoGateway, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.MQTT_CONFIG.url}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getMqttStatus(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.MQTT_STATUS.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getTime(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.TIME.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putTime(gateway: NabtoHandler.NabtoGateway, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.TIME.url}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getTemplates(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.TEMPLATES.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postTemplate(gateway: NabtoHandler.NabtoGateway, body:JSONObject, maxRetries: Int = 1, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.TEMPLATES.url}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteTemplate(gateway: NabtoHandler.NabtoGateway, hash: String, maxRetries: Int = 1, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.TEMPLATE.url.replace(Pair("{HASH}", hash))}"
        return Request.delete(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun getExternal(gateway: NabtoHandler.NabtoGateway, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.EXTERNALS.url}"
        return Request.get(url, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun postExternal(gateway: NabtoHandler.NabtoGateway, body: JSONArray, maxRetries: Int = 1, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.EXTERNALS.url}"
        return Request.post(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun putExternal(gateway: NabtoHandler.NabtoGateway, key: String, body: JSONObject, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.EXTERNAL.url.replace(Pair("{KEY}", key))}"
        return Request.put(url, body, maxRetries, first)
    }

    @Throws(VolleyError::class)
    suspend fun deleteExternal(gateway: NabtoHandler.NabtoGateway, key: String, maxRetries: Int = 2, first: Boolean = false): JSONObject {
        val url = "${Endpoints.BASE.url.replace(Pair("{PORT}", gateway.port))}/${Endpoints.EXTERNAL.url.replace(Pair("{KEY}", key))}"
        return Request.delete(url, maxRetries, first)
    }
}
