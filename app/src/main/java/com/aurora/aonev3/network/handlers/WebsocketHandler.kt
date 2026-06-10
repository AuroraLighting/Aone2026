package com.aurora.aonev3.network.handlers

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.aurora.aonev3.debug
import com.aurora.aonev3.gson
import com.aurora.aonev3.indices
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.replace
import com.aurora.aonev3.data.AppDatabase
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.timers.LogicTimer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonSyntaxException
import com.jackmills.queue.Queue
import com.nabto.api.NabtoCApiWrapper
import com.nabto.api.NabtoTunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URI
import java.util.*

class WebsocketHandler(private val gateway: NabtoHandler.NabtoGateway) : WebSocketClient(
    URI(DevelcoHandler.Endpoints.WEBSOCKET_BASE.url.replace(Pair("{PORT}", gateway.port)))
) {
    private val TAG = this::class.simpleName

    private var closedByUser = false

    private val queue = Queue<String>()

    private val crashlytics = FirebaseCrashlytics.getInstance()

    private var handlerThread =
        HandlerThread("WebsocketHandlerThread-${Calendar.getInstance().timeInMillis}").apply {
            start()
        }

    init {
        Handler(handlerThread.looper).apply {
            postDelayed(object : Runnable {
                override fun run() {
                    if (isOpen) {
                        Log.v(TAG, "Ping sent")
                        try {
                            sendPing()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        if (!closedByUser) {
                            reconnect()
                        }
                    }

                    postDelayed(this, (2 * 60) * 1000)
                }
            }, (2 * 60) * 1000)
        }

        queue.delay = 50
        queue.setQueueCallback(object : Queue.QueueCallback<String> {
            override fun onProcess(item: String) {
                handleMessage(item)
            }
        })
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.v(this@WebsocketHandler::class.simpleName, "WS open")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.e(TAG, "WS closed: code - $code, reason - $reason, remote - $remote")

        handlerThread.quitSafely()

        handlerThread =
            HandlerThread("WebsocketHandlerThread-${Calendar.getInstance().timeInMillis}").apply {
                start()
            }

        Handler(handlerThread.looper).postDelayed({
            if (!closedByUser) reconnect()
        }, 2000)
    }

    override fun onMessage(m: String?) {
        m ?: return
        Log.v(TAG, "$m")
        queue.pushTail(m)
        if (!queue.isProcessing) {
            queue.start()
        }
    }

    override fun onError(ex: Exception?) {
        Log.e(
            TAG, """WS exception
            ${ex?.cause}
            ${ex?.localizedMessage}
        """
        )
        crashlytics.log(
            """E/$TAG:WS exception
            ${ex?.cause}
            ${ex?.localizedMessage}"""
        )
        ex?.let {
            crashlytics.recordException(ex)
        }

//        handlerThread.quitSafely()
//
//        handlerThread =
//            HandlerThread("WebsocketHandlerThread-${Calendar.getInstance().timeInMillis}").apply {
//                start()
//            }
//
//        Handler(handlerThread.looper).postDelayed({
//            if (!closedByUser) reconnect()
//        }, 2000)
    }

    override fun close() {
        closedByUser = true
        super.close()
        handlerThread.quitSafely()
    }

    override fun connect() {
        gateway.nabtoTunnel?.let {
            val tunnelState = NabtoCApiWrapper.nabtoTunnelInfo(it).tunnelState
            debug(tunnelState.toString())
            when (tunnelState) {
                NabtoTunnelState.CLOSED,
                NabtoTunnelState.CONNECTING,
                NabtoTunnelState.READY_FOR_RECONNECT,
                NabtoTunnelState.UNKNOWN -> return
                else -> {}
            }
        }
        closedByUser = false
        if (!handlerThread.isAlive) {
            try {
                handlerThread.start()
            } catch (ex: IllegalThreadStateException) {
                Log.e(TAG, ex.toString())
                ex.printStackTrace()
            }
        }
        try {
            Handler(handlerThread.looper).post {
                try {
                    if (!isOpen) {
                        super.connect()
                    }
                } catch (ex: NullPointerException) {
                    reconnect()
                } catch (ex: IllegalStateException) {
                    reconnect()
                } catch (ex: IllegalThreadStateException) {
                    reconnect()
                }
            }
        } catch (ex: NullPointerException) {
            handlerThread.quitSafely()

            handlerThread =
                HandlerThread("WebsocketHandlerThread-${Calendar.getInstance().timeInMillis}").apply {
                    start()
                }
            connect()
        }
    }

    override fun reconnect() {
        gateway.nabtoTunnel?.let {
            when (NabtoCApiWrapper.nabtoTunnelInfo(it).tunnelState) {
                NabtoTunnelState.CLOSED,
                NabtoTunnelState.CONNECTING,
                NabtoTunnelState.READY_FOR_RECONNECT,
                NabtoTunnelState.UNKNOWN -> return
                else -> {}
            }
        }
        super.reconnect()
    }

    private fun handleMessage(m: String) {
        val job = Job()
        CoroutineScope(Dispatchers.IO + job).launch {
            val message = try {
                JSONObject(m)
            } catch (ex: JSONException) {
                Log.e(
                    TAG, """WS exception
                ${ex.cause}
                ${ex.localizedMessage}"""
                )
                return@launch
            }
            val path = message.optString("path")
            val type = message.optString("type")
            val json = try {
                JSONTokener(message.optString("data")).nextValue()
            } catch (ex: JSONException) {
                crashlytics.recordException(ex)
                ex.printStackTrace()
            }
            val data: JSONObject = if (json is JSONObject) {
                json
            } else {
                JSONObject()
            }

            val dataArray: JSONArray = if (json is JSONArray) {
                json
            } else {
                JSONArray()
            }

            when {
                DevelcoHandler.Endpoints.ZIGBEE.regex.matchEntire(path) != null -> {
                    val prospects = data.optJSONArray("prospects") ?: return@launch
                    val prospectsArrayList = ArrayList<JSONObject>()

                    for (i in prospects.indices()) {
                        val prospect = prospects.optJSONObject(i) ?: continue

                        prospectsArrayList.add(prospect)
                    }

                    DevelcoHandler.prospects.postValue(prospectsArrayList)
                }
                DevelcoHandler.Endpoints.DEVICE.regex.matchEntire(path) != null -> {
                    val id = data.optInt("id")
                    if (id == 0) return@launch

                    when (Type.valueOf(type.toUpperCase())) {
                        Type.ADD -> {
                            val metadata = try {
                                if (data.optString("metadata", "").isNotBlank()) {
                                    JSONObject(data.optString("metadata", ""))
                                } else {
                                    JSONObject()
                                }
                            } catch (ex: JSONException) {
                                ex.printStackTrace()
                                JSONObject()
                            }

                            val device = Device(
                                gateway.serial,
                                data.optString("eui"),
                                id,
                                data.optString("name"),
                                data.optString("defaultName"),
                                metadata,
                                data.optBoolean("online")
                            )

                            SyncHandler.devices.add(device)
                        }
                        Type.UPDATE -> {
                            val metadata = try {
                                if (data.optString("metadata", "").isNotBlank()) {
                                    JSONObject(data.optString("metadata", ""))
                                } else {
                                    JSONObject()
                                }
                            } catch (ex: JSONException) {
                                ex.printStackTrace()
                                JSONObject()
                            }

                            SyncHandler
                                .devicesList
                                .find { it.parentGateway == gateway.serial && it.id == id }
                                ?.let { device ->
                                    device.name = data.optString("name")
                                    device.defaultName = data.optString("defaultName")

                                    val deviceClassName =
                                        AppDatabase.getDatabase().identitiesDao()
                                            .getDeviceClassForDefaultName(
                                                data.optString("defaultName")
                                            ) ?: "unknown"

                                    try {
                                        device.deviceClass =
                                            Device.DeviceClass.valueOf(deviceClassName.toUpperCase())
                                        device.ldevs = device.deviceClass.ldevs
                                    } catch (ex: IllegalArgumentException) {
                                        crashlytics.recordException(
                                            Exception(
                                                "No such enum: defaultName - ${
                                                    data.optString(
                                                        "defaultName"
                                                    )
                                                }, deviceClass - $deviceClassName"
                                            )
                                        )
                                        Log.e(
                                            TAG,
                                            "No such enum: defaultName - ${data.optString("defaultName")}, deviceClass - $deviceClassName"
                                        )
                                    }
                                    device.metadata = metadata
                                    device.online = data.optBoolean("online")

                                    SyncHandler.devices.replace(device)
                                }
                        }
                        Type.REMOVE -> {
                            val device = SyncHandler
                                .devicesList
                                .find { it.parentGateway == gateway.serial && it.id == id } ?: return@launch
                            SyncHandler
                                .devices.remove(device)
                        }
                    }
                }
                DevelcoHandler.Endpoints.DEVICE_DATAPOINT.regex.matchEntire(path) != null -> {
                    val pathSplit = path.split("/")
                    val id = pathSplit[2].toInt()
                    val ldev = pathSplit[4]
                    val property = data.optString("key")

                    when (Type.valueOf(type.toUpperCase())) {
                        Type.ADD -> {
                            val datapoint = DeviceDatapoint(
                                gateway.serial,
                                id,
                                ldev,
                                property,
                                data.opt("value"),
                                data.optString("lastUpdated")
                            )
                            SyncHandler.deviceDatapoints.add(datapoint)
                        }
                        Type.UPDATE -> {
                            val datapoint = DeviceDatapoint(
                                gateway.serial,
                                id,
                                ldev,
                                property,
                                data.opt("value"),
                                data.optString("lastUpdated")
                            )
                            SyncHandler.deviceDatapoints.add(datapoint)
                        }
                        Type.REMOVE -> {

                        }
                    }
                }
                DevelcoHandler.Endpoints.GROUP.regex.matchEntire(path) != null -> {
                    val id = data.optInt("id")
                    if (id == 0) return@launch

                    when (Type.valueOf(type.toUpperCase())) {
                        Type.ADD -> {
                            val metadata = try {
                                JSONObject(data.optString("metadata"))
                            } catch (ex: JSONException) {
                                JSONObject()
                            }

                            val group = Group(
                                gateway.serial,
                                id,
                                data.optString("name"),
                                metadata,
                                data.optString("grpType")
                            )

                            SyncHandler.groups.add(group)
                        }
                        Type.UPDATE -> {
                            val metadata = try {
                                JSONObject(data.optString("metadata"))
                            } catch (ex: JSONException) {
                                JSONObject()
                            }

                            SyncHandler
                                .groupsList
                                .find { it.parentGateway == gateway.serial && it.id == id }
                                ?.let { group ->
                                    group.name = data.optString("name")
                                    group.metadata = metadata

                                    SyncHandler
                                        .groups
                                        .replace(group)

                                    val virtualGroupMembers = ArrayList<GroupMember>()
                                    val virtualMembers =
                                        metadata.optJSONArray("virtual_members") ?: JSONArray()
                                    val groupMembers = SyncHandler
                                        .groupMembersList
                                        .filter { it.parentGateway == gateway.serial && it.groupId == group.id && it.isVirtualMember }
                                    val membersToAdd = ArrayList<GroupMember>()

                                    for (i in virtualMembers.indices()) {
                                        val virtualMember = virtualMembers.optJSONObject(i)

                                        val groupMember = GroupMember(
                                            gateway.serial,
                                            group.id,
                                            deviceId = virtualMember.optInt("id"),
                                            deviceLdev = virtualMember.optString("ldev"),
                                            id = -1 * virtualMember.optInt("id"),
                                            isVirtualMember = true
                                        )

                                        virtualGroupMembers.add(groupMember)
                                        membersToAdd.add(groupMember)
                                    }

                                    val membersToDelete = groupMembers.filter { it !in virtualGroupMembers }

                                    SyncHandler.groupMembers.addRemove(membersToAdd, membersToDelete)
                                }

                        }
                        Type.REMOVE -> {
                            val group = SyncHandler
                                .groupsList
                                .find { it.parentGateway == gateway.serial && it.id == id } ?: return@launch
                            SyncHandler.groups.remove(group)
                        }
                    }
                }
                DevelcoHandler.Endpoints.GROUP_DATAPOINT.regex.matchEntire(path) != null -> {
                    val pathSplit = path.split("/")
                    val id = pathSplit[2].toInt()
                    val ldev = pathSplit[4]
                    val property = data.optString("key")

                    when (Type.valueOf(type.toUpperCase())) {
                        Type.ADD,
                        Type.UPDATE -> {
                            val datapoint = GroupDatapoint(
                                gateway.serial,
                                id,
                                ldev,
                                property,
                                data.opt("value"),
                                data.optString("lastUpdated")
                            )
                            SyncHandler.groupDatapoints.add(datapoint)
                        }
                        Type.REMOVE -> {
                        }
                    }
                }
                DevelcoHandler.Endpoints.GROUP_SCENE.regex.matchEntire(path) != null -> {
                    val id = data.optInt("id")
                    val pathSplit = path.split("/")
                    val groupId = pathSplit[2].toInt()
                    val metadata = if (data.optString("metadata", "").isNotBlank()) {
                        JSONObject(data.optString("metadata", ""))
                    } else {
                        JSONObject()
                    }

                    when (Type.valueOf(type.toUpperCase())) {
                        Type.ADD,
                        Type.UPDATE -> {
                            val scene = Scene(
                                gateway.serial,
                                groupId,
                                id,
                                data.optString("name"),
                                metadata,
                                data.optInt("transitionTime")
                            )

                            SyncHandler.scenes.add(scene)
                        }
                        Type.REMOVE -> {
                            val scene = SyncHandler
                                .scenesList
                                .find {
                                    it.parentGateway == gateway.serial
                                            && it.groupId == groupId && it.id == id
                                }
                                ?: return@launch
                            SyncHandler.scenes.remove(scene)
                        }
                    }
                }
                DevelcoHandler.Endpoints.GROUP_MEMBERS.regex.matchEntire(path) != null -> {
                    val id = data.optInt("id")
                    val pathSplit = path.split("/")
                    val groupId = pathSplit[2].toInt()
                    val resource = data.optString("resource")
                    val resourceSplit = resource.split("/")
                    val deviceId = resourceSplit[2].toInt()
                    val deviceLdev = resourceSplit[4]

                    when (Type.valueOf(type.toUpperCase())) {
                        Type.ADD -> {
                            SyncHandler.groupMembers.add(
                                GroupMember(
                                    gateway.serial,
                                    groupId,
                                    deviceId,
                                    deviceLdev,
                                    id
                                )
                            )
                        }
                        Type.UPDATE -> {
                        }
                        Type.REMOVE -> {
                            val member = SyncHandler
                                .groupMembersList
                                .find { it.parentGateway == gateway.serial && it.groupId == groupId && it.id == id} ?: return@launch
                            SyncHandler.groupMembers.remove(member)
                        }
                    }
                }
                DevelcoHandler.Endpoints.LOGIC_COLLECTION.regex.matchEntire(path) != null -> {
                    val id = data.optInt("id")
                    if (id == 0) return@launch
                    val metadata = if (data.optString("metadata", "").isNotBlank()) {
                        gson.fromJson(data.optString("metadata", ""), CollectionMetadata::class.java) ?: CollectionMetadata()
                    } else {
                        CollectionMetadata()
                    }

                        when (Type.valueOf(type.toUpperCase())) {
                            Type.ADD -> {
                                val logicCollection = LogicCollection(
                                    gateway.serial,
                                    id,
                                    data.optString("name"),
                                    metadata,
                                    data.optBoolean("enabled")
                                )

                                SyncHandler.logicCollections.add(logicCollection)
                            }
                            Type.UPDATE -> {
                                val logicCollection = SyncHandler.logicCollectionsList.find { it.parentGateway == gateway.serial && it.id == id } ?: return@launch

                                logicCollection.name = data.optString("name")
                                logicCollection.metadata = metadata
                                logicCollection.isEnabled = data.optBoolean("enabled")

                                SyncHandler.logicCollections.replace(logicCollection)
                            }
                            Type.REMOVE -> {
                                val logicCollection = SyncHandler.logicCollectionsList.find { it.parentGateway == gateway.serial && it.id == id } ?: return@launch
                                SyncHandler.logicCollections.remove(logicCollection)
                            }
                        }
                    }
                    DevelcoHandler.Endpoints.LOGIC_RULE.regex.matchEntire(path) != null -> {
                        val id = data.optInt("id")
                        if (id == 0) return@launch

                        val pathSplit = path.split("/")
                        val logicCollectionId = pathSplit[2].toInt()

                        val metadata = try {
                            gson.fromJson(data.optString("metadata"), RuleMetadata::class.java) ?: RuleMetadata()
                        } catch (ex: JSONException) {
                            RuleMetadata()
                        } catch (ex: IllegalStateException) {
                            RuleMetadata()
                        } catch (ex: JsonSyntaxException) {
                            RuleMetadata()
                        }

                        val triggers: Array<Trigger> = try {
                            gson.fromJson(data.optJSONArray("triggers")?.toString(), Array<Trigger>::class.java) ?: emptyArray()
                        } catch (ex: JSONException) {
                            emptyArray()
                        } catch (ex: IllegalStateException) {
                            emptyArray()
                        } catch (ex: JsonSyntaxException) {
                            emptyArray()
                        }

                        val conditions: Array<Condition> = try {
                            gson.fromJson(data.optJSONArray("conditions")?.toString(), Array<Condition>::class.java) ?: emptyArray()
                        } catch (ex: JSONException) {
                            emptyArray()
                        } catch (ex: IllegalStateException) {
                            emptyArray()
                        } catch (ex: JsonSyntaxException) {
                            emptyArray()
                        }

                        val actions: Array<Action> = try {
                            gson.fromJson(data.optJSONArray("actions")?.toString(), Array<Action>::class.java) ?: emptyArray()
                        } catch (ex: JSONException) {
                            emptyArray()
                        } catch (ex: IllegalStateException) {
                            emptyArray()
                        } catch (ex: JsonSyntaxException) {
                            emptyArray()
                        }

                        when (Type.valueOf(type.toUpperCase())) {
                            Type.ADD -> {
                                SyncHandler.logicRules.add(
                                    LogicRule(
                                        gateway.serial,
                                        logicCollectionId,
                                        id,
                                        data.optString("name"),
                                        metadata,
                                        data.optBoolean("enabled"),
                                        triggers,
                                        conditions,
                                        actions
                                    )
                                )
                            }
                            Type.UPDATE -> {
                                val logicRule = SyncHandler.logicRulesList.find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollectionId && it.id == id } ?: return@launch
                                logicRule.name = data.optString("name")
                                logicRule.metadata = metadata
                                logicRule.isEnabled = data.optBoolean("enabled")
                                logicRule.triggers = triggers
                                logicRule.conditions = conditions
                                logicRule.actions = actions

                                SyncHandler.logicRules.replace(logicRule)
                            }
                            Type.REMOVE -> {
                                val logicRule = SyncHandler.logicRulesList.find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollectionId && it.id == id } ?: return@launch

                                SyncHandler.logicRules.remove(logicRule)
                            }
                        }
                    }
                    DevelcoHandler.Endpoints.LOGIC_TIMER.regex.matchEntire(path) != null -> {
                        val id = data.optInt("id")
                        if (id == 0) {
                            return@launch
                        }
                        val pathSplit = path.split("/")
                        val logicCollectionId = pathSplit[2].toInt()
                        val metadata = if (data.optString("metadata", "").isNotBlank()) {
                            JSONObject(data.optString("metadata", ""))
                        } else {
                            JSONObject()
                        }

                        val actions: Array<Action> = try {
                            gson.fromJson(data.optJSONArray("actions")?.toString(), Array<Action>::class.java) ?: emptyArray()
                        } catch (ex: JSONException) {
                            emptyArray()
                        } catch (ex: IllegalStateException) {
                            emptyArray()
                        } catch (ex: JsonSyntaxException) {
                            emptyArray()
                        }

                        when (Type.valueOf(type.toUpperCase())) {
                            Type.ADD -> {
                                SyncHandler.logicTimers.add(
                                    LogicTimer(
                                        gateway.serial,
                                        logicCollectionId,
                                        id,
                                        data.optString("name"),
                                        metadata,
                                        data.optInt("timeout"),
                                        actions,
                                        data.optInt("remaining"),
                                        data.optString("status")
                                    )
                                )
                            }
                            Type.UPDATE -> {
                                val logicTimer = SyncHandler.logicTimersList.find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollectionId && it.id == id } ?: return@launch

                                logicTimer.name = data.optString("name")
                                logicTimer.metadata = metadata
                                logicTimer.timeout = data.optInt("timeout")
                                logicTimer.actions = actions
                                logicTimer.remaining = data.optInt("remaining")
                                logicTimer.status = data.optString("status")

                                SyncHandler.logicTimers.replace(logicTimer)
                            }
                            Type.REMOVE -> {
                                val logicTimer = SyncHandler.logicTimersList.find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollectionId && it.id == id } ?: return@launch

                                SyncHandler.logicTimers.remove(logicTimer)
                            }
                        }
                    }
                    Regex("""fw/downloads/\d+""").matchEntire(path) != null -> {
                        val technology = data.optString("technology")
                        if (technology.toLowerCase() != "gateway") return@launch
//                    var details = data.optString("details")

//                    when (Type.valueOf(type.toUpperCase())) {
//                        Type.ADD -> {
//                            gateway.update.status.postValue(UpdateStatus.valueOf(data.optString("status").toUpperCase()))
//                            if (details.isNotBlank()) {
//                                details = details.replace(Pair("%", ""))
//                                gateway.update.perCent.postValue(details.toInt())
//                            }
//                        }
//                        Type.UPDATE -> {
//                            gateway.update.status.postValue(UpdateStatus.valueOf(data.optString("status").toUpperCase()))
//                            if (details.isNotBlank()) {
//                                details = details.replace(Pair("%", ""))
//                                try {
//                                    gateway.update.perCent.postValue(details.toInt())
//                                } catch (ex: NumberFormatException) {
//                                    ex.printStackTrace()
//                                }
//                            }
//                        }
//                        Type.REMOVE -> {
//                            gateway.update.status.postValue(UpdateStatus.valueOf("idle".toUpperCase()))
//                            gateway.update.perCent.postValue(0)
//                        }
//                    }
                    }
                    path == "fw/status" -> {
                        when (Type.valueOf(type.toUpperCase())) {
                            Type.ADD,
                            Type.REMOVE -> {
                            }
                            Type.UPDATE -> {
                                val devices = SyncHandler
                                    .devicesList
                                    .filter { it.parentGateway == gateway.serial }
                                for (i in dataArray.indices()) {
                                    val fwStatus = dataArray.optJSONObject(i) ?: JSONObject()
                                    val eui = fwStatus.optString("id")

                                    val device = devices.firstOrNull { it.eui == eui } ?: continue

                                    device.otaStatus = fwStatus.optString("status")
                                    device.firmwareVersion = fwStatus.optString("version")

                                    SyncHandler.devices.replace(device)
                                }
                            }
                        }
                    }
                }

                job.complete()
            }
        }

    private enum class Type {
        ADD,
        UPDATE,
        REMOVE
    }
}