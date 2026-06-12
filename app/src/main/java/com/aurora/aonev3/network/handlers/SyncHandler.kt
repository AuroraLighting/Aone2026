package com.aurora.aonev3.network.handlers

import com.aurora.aonev3.synthetic.*
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.aurora.aonev3.*
import com.aurora.aonev3.logic.*
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
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object SyncHandler {
    private const val TAG = "SyncHandler"

    private val identitiesDao = AppDatabase.getDatabase().identitiesDao()

    val devices: MutableLiveDataArrayList<Device> = MutableLiveDataArrayList()
    val devicesList
        get() = devices.value?.toList() ?: emptyList()
    val deviceDatapoints: MutableLiveDataArrayList<DeviceDatapoint> = MutableLiveDataArrayList()
    val deviceDatapointsList
        get() = deviceDatapoints.value?.toList() ?: emptyList()
    val groups: MutableLiveDataArrayList<Group> = MutableLiveDataArrayList()
    val groupsList
        get() = groups.value?.toList() ?: emptyList()
    val groupDatapoints: MutableLiveDataArrayList<GroupDatapoint> = MutableLiveDataArrayList()
    val groupDatapointsList
        get() = groupDatapoints.value?.toList() ?: emptyList()
    val groupMembers: MutableLiveDataArrayList<GroupMember> = MutableLiveDataArrayList()
    val groupMembersList
        get() = groupMembers.value?.toList() ?: emptyList()
    val scenes: MutableLiveDataArrayList<Scene> = MutableLiveDataArrayList()
    val scenesList
        get() = scenes.value?.toList() ?: emptyList()
    val logicCollections: MutableLiveDataArrayList<LogicCollection> = MutableLiveDataArrayList()
    val logicCollectionsList
        get() = logicCollections.value?.toList() ?: emptyList()
    val logicRules: MutableLiveDataArrayList<LogicRule> = MutableLiveDataArrayList()
    val logicRulesList
        get() = logicRules.value?.toList() ?: emptyList()
    val logicTimers: MutableLiveDataArrayList<LogicTimer> = MutableLiveDataArrayList()
    val logicTimersList
        get() = logicTimers.value?.toList() ?: emptyList()

    var syncHandlerCoroutineScope = CoroutineScope(Dispatchers.IO)
        get() {
            if (!field.isActive)
                field = CoroutineScope(Dispatchers.IO)

            return field
        }

    fun getGroupCount() = groupsList.size

    private val crashlytics = FirebaseCrashlytics.getInstance()

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncDevices(gateway: NabtoHandler.NabtoGateway, force: Boolean = false): List<Device> {
        if (devicesList.any { it.parentGateway == gateway.serial } && !force) return devicesList
        if (force) devices.removeAll { it.parentGateway == gateway.serial }

        val response = DevelcoHandler.getDevices(gateway)
        val body = response.optJSONArray("body") ?: JSONArray()

        for (i in body.indices()) {
            val deviceJson = body.optJSONObject(i) ?: continue

            val deviceClassName = if (!deviceJson.optString("defaultName").equals("squidzigbee", ignoreCase = true)) {
                AppDatabase.getDatabase().identitiesDao()
                    .getDeviceClassForDefaultName(deviceJson.optString("defaultName"))
                    ?: "unknown"
            } else {
                "gateway"
            }

            val deviceClass = try {
                Device.DeviceClass.valueOf(deviceClassName.uppercase())
            } catch (ex: IllegalArgumentException) {
                crashlytics.recordException(
                    Exception(
                        "No such enum: defaultName - ${deviceJson.optString("defaultName")}, deviceClass - $deviceClassName"
                    )
                )

                Log.e(
                    TAG,
                    "No such enum: defaultName - ${deviceJson.optString("defaultName")}, deviceClass - $deviceClassName"
                )
                continue
            }
            val ldevs = deviceClass.ldevs

            val metadata = try {
                JSONObject(deviceJson.optString("metadata", ""))
            } catch (ex: JSONException) {
                JSONObject()
            }

            val device = Device(
                gateway.serial,
                deviceJson.optString("eui"),
                deviceJson.optInt("id"),
                deviceJson.optString("name"),
                deviceJson.optString("defaultName"),
                metadata,
                deviceJson.optBoolean("online")
            )

            device.deviceClass = deviceClass
            device.ldevs = ldevs

            if (devices.value?.contains(device) != true) {
                devices.value?.add(device)
            }
        }

        devices.postValue(devices.value)

        if (!force) {
            syncHandlerCoroutineScope.launch(Dispatchers.IO) {
                try {
                    devicesList.forEach { device ->
                        if (device.online) {
                            try {
                                reportDeviceStates(gateway, device)
                            } catch (err: VolleyError) {

                            }
                        }
                    }
                } catch (ex: ConcurrentModificationException) {
                    ex.printStackTrace()
                }
            }
        }

        return devicesList
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncDeviceDatapoints(gateway: NabtoHandler.NabtoGateway, device: Device, force: Boolean = false): List<DeviceDatapoint> {
        if (deviceDatapointsList.any { it.parentGateway == gateway.serial && it.id == device.id} && !force) return deviceDatapointsList

        return syncDeviceDatapoints(gateway, force)
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncDeviceDatapoints(gateway: NabtoHandler.NabtoGateway, force: Boolean = false): List<DeviceDatapoint> {
        if (deviceDatapointsList.any { it.parentGateway == gateway.serial} && !force) return deviceDatapointsList
        if (force) deviceDatapoints.removeAll { it.parentGateway == gateway.serial }

        val response = DevelcoHandler.getAllDevicesDatapoints(gateway)
        val responseBody = response.optJSONArray("body") ?: JSONArray()

        for (i in responseBody.indices()) {
            val bodyJson = responseBody.optJSONObject(i) ?: JSONObject()
            val path = bodyJson.optString("path")
            val pathSplit = path.split("/")
            val data = bodyJson.optJSONArray("data") ?: JSONArray()
            val deviceId = pathSplit.getOrNull(2)?.toIntOrNull() ?: continue
            val deviceLdev = pathSplit.getOrNull(4) ?: continue

            for (j in data.indices()) {
                val datapoint = data.optJSONObject(j) ?: JSONObject()
                if (!datapoint.optString("access").contains("r")) continue

                val deviceDatapoint = DeviceDatapoint(
                    gateway.serial,
                    deviceId,
                    deviceLdev,
                    datapoint.optString("key"),
                    datapoint.opt("value"),
                    datapoint.optString("lastUpdated")
                )

                deviceDatapoints.value?.add(deviceDatapoint)
            }
        }

        deviceDatapoints.postValue(deviceDatapoints.value)

        return deviceDatapointsList
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncGroups(gateway: NabtoHandler.NabtoGateway, first: Boolean = false, force: Boolean = false) {
        syncGroupsCached(gateway, first, force)

        syncGroupDatapointsAndScenesCached(gateway, force)
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncGroupsCached(gateway: NabtoHandler.NabtoGateway, first: Boolean = false, force: Boolean = false): List<Group> {
        if (groupsList.any { it.parentGateway == gateway.serial } && !force) return groupsList
        if (force) groups.removeAll { it.parentGateway == gateway.serial }

        val response = DevelcoHandler.getGroups(gateway, first = first)
        val body = response.optJSONArray("body") ?: JSONArray()
        val virtualGroupMembers: ArrayList<GroupMember> = ArrayList()
        val tempGroups = ArrayList<Group>()

        for (i in body.indices()) {
            val groupJson = body.optJSONObject(i) ?: continue

            val metadata = try {
                JSONObject(groupJson.optString("metadata"))
            } catch (ex: JSONException) {
                JSONObject()
            }

            val group = Group(
                gateway.serial,
                groupJson.optInt("id"),
                groupJson.optString("name"),
                metadata,
                groupJson.optString("grpType")
            )

            val virtualMembers = metadata.optJSONArray("virtual_members") ?: JSONArray()

            for (j in virtualMembers.indices()) {
                val virtualMember = virtualMembers.optJSONObject(j)

                val groupMember = GroupMember(
                    gateway.serial,
                    group.id,
                    deviceId = virtualMember.optInt("id"),
                    deviceLdev = virtualMember.optString("ldev"),
                    id = -1 * virtualMember.optInt("id"),
                    isVirtualMember = true
                )

                virtualGroupMembers.add(groupMember)
            }

            tempGroups.add(group)
        }

        groups.postValue(tempGroups)
        groupMembers.add(virtualGroupMembers)

        return groupsList
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncGroupDatapointsAndScenesCached(gateway: NabtoHandler.NabtoGateway, force: Boolean = false) {
        if ((groupDatapointsList.any { it.parentGateway == gateway.serial }
            || scenesList.any { it.parentGateway == gateway.serial })
            && !force) return
        if (force) {
            groupDatapoints.removeAll { it.parentGateway == gateway.serial }
            scenes.removeAll { it.parentGateway == gateway.serial }
        }

        val response = DevelcoHandler.getAllGroupsDatapointsAndScenes(gateway)
        val groupsDatapointsAndScenes = response.optJSONArray("body") ?: JSONArray()

        for (i in groupsDatapointsAndScenes.indices()) {
            val groupDatapointsScenes = groupsDatapointsAndScenes.optJSONObject(i)
            val path = groupDatapointsScenes.optString("path")
            val data = groupDatapointsScenes.optJSONArray("data") ?: JSONArray()
            val pathSplit = path.split("/")
            val groupId = pathSplit.getOrNull(2)?.toIntOrNull() ?: continue
            val groupLdev = pathSplit.getOrNull(4) ?: continue
            val datapoints = ArrayList<GroupDatapoint>()
            val tempScenes = ArrayList<Scene>()

            if (DevelcoHandler.Endpoints.GROUP_DATA.regex.matchEntire(path) != null) {
                for (j in data.indices()) {
                    val datapoint = data.optJSONObject(j) ?: JSONObject()

                    val groupDatapoint =
                        GroupDatapoint(
                            gateway.serial,
                            groupId,
                            groupLdev,
                            datapoint.optString("key"),
                            datapoint.opt("value"),
                            datapoint.optString("lastUpdated")
                        )

                    datapoints.add(groupDatapoint)
                }

                groupDatapoints.add(datapoints)
            } else if (DevelcoHandler.Endpoints.GROUP_SCENES.regex.matchEntire(path) != null) {
                for (j in data.indices()) {
                    val scene = data.optJSONObject(j) ?: JSONObject()

                    val metadata = try {
                        JSONObject(scene.optString("metadata"))
                    } catch (ex: JSONException) {
                        JSONObject()
                    }

                    val groupScene = Scene(
                        gateway.serial,
                        groupId,
                        scene.optInt("id"),
                        scene.optString("name"),
                        metadata,
                        scene.optInt("transitionTime")
                    )

                    tempScenes.add(groupScene)
                }

                scenes.add(tempScenes)
            }
        }
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncGroupDatapoints(gateway: NabtoHandler.NabtoGateway, group: Group, force: Boolean = false): List<GroupDatapoint> {
        if (groupDatapointsList.any { it.parentGateway == group.parentGateway && it.id == group.id } && !force) return groupDatapointsList
        if (force) {
            groupDatapoints.removeAll { it.parentGateway == gateway.serial }
            scenes.removeAll { it.parentGateway == gateway.serial }
        }

        syncGroupDatapointsAndScenesCached(gateway, force)

        return groupDatapointsList
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncGroupScenes(gateway: NabtoHandler.NabtoGateway, group: Group, force: Boolean = false): List<Scene> {
        if (scenesList.any { it.parentGateway == group.parentGateway && it.id == group.id } && !force) return scenesList
        if (force) {
            groupDatapoints.removeAll { it.parentGateway == gateway.serial }
            scenes.removeAll { it.parentGateway == gateway.serial }
        }

        syncGroupDatapointsAndScenesCached(gateway, force)

        return scenesList
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncGroupMembers(gateway: NabtoHandler.NabtoGateway, group: Group, force: Boolean = false) {
        syncGroupMembers(gateway, group.id, force)
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncGroupMembers(gateway: NabtoHandler.NabtoGateway, group: Int, force: Boolean = false): List<GroupMember> {
        if (groupMembersList.any { it.parentGateway == gateway.serial && it.groupId == group && !it.isVirtualMember } && !force) return groupMembersList
        if (force) groupMembers.removeAll { it.parentGateway == gateway.serial }

        val response = DevelcoHandler.getAllGroupsMembers(gateway)
        val responseBody = response.optJSONArray("body") ?: JSONArray()
        val tempGroupMembers = ArrayList<GroupMember>()

        for (i in responseBody.indices()) {
            val bodyJson = responseBody.optJSONObject(i) ?: JSONObject()
            val path = bodyJson.optString("path")
            val pathSplit = path.split("/")
            val data = bodyJson.optJSONArray("data") ?: JSONArray()
            val groupId = pathSplit[2].toIntOrNull() ?: continue

            for (j in data.indices()) {
                val mbr = data.optJSONObject(j) ?: continue
                val mbrResourceSplit = mbr.optString("resource").split("/")
                val deviceId = mbrResourceSplit.getOrNull(2)?.toIntOrNull() ?: continue
                val deviceLdev = mbrResourceSplit.getOrNull(4) ?: continue

                val groupMember = GroupMember(
                    gateway.serial,
                    groupId,
                    deviceId,
                    deviceLdev,
                    mbr.optInt("id")
                )

                tempGroupMembers.add(groupMember)
            }
        }

        groupMembers.add(tempGroupMembers)

        return groupMembersList
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncLogicCollectionsCached(gateway: NabtoHandler.NabtoGateway, force: Boolean = false): List<LogicCollection> {
        if (logicCollectionsList.any { it.parentGateway == gateway.serial } && !force) return logicCollectionsList
        if (force) logicCollections.removeAll { it.parentGateway == gateway.serial }

        val response = DevelcoHandler.getLogicCollections(gateway)
        val body = response.optJSONArray("body") ?: JSONArray()
        val tempLogicCollections = ArrayList<LogicCollection>()

        for (i in body.indices()) {
            val logicCollectionJson = body.optJSONObject(i) ?: continue

            val metadata = try {
                gson.fromJson(logicCollectionJson.optString("metadata", ""), CollectionMetadata::class.java) ?: CollectionMetadata()
            } catch (ex: JSONException) {
                CollectionMetadata()
            } catch (ex: IllegalStateException) {
                CollectionMetadata()
            } catch (ex: JsonSyntaxException) {
                CollectionMetadata()
            }

            val logicCollection = LogicCollection(
                gateway.serial,
                logicCollectionJson.optInt("id"),
                logicCollectionJson.optString("name"),
                metadata,
                logicCollectionJson.optBoolean("enabled")
            )

            tempLogicCollections.add(logicCollection)
        }

        logicCollections.add(tempLogicCollections)
        return logicCollectionsList
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncLogicRulesAndTimersCached(gateway: NabtoHandler.NabtoGateway, force: Boolean = false) {
        if (logicRulesList.any { it.parentGateway == gateway.serial }
            && logicTimersList.any { it.parentGateway == gateway.serial }
            && !force) return
        if (force) {
            logicRules.removeAll { it.parentGateway == gateway.serial }
            logicTimers.removeAll { it.parentGateway == gateway.serial }
        }

        val response = DevelcoHandler.getAllLogicRulesAndTimers(gateway)

        val logicRulesTimers = response.optJSONArray("body") ?: JSONArray()
        val tempRules = ArrayList<LogicRule>()
        val tempTimers = ArrayList<LogicTimer>()

        for (i in logicRulesTimers.indices()) {
            val logicRuleTimer = logicRulesTimers.optJSONObject(i)
            val path = logicRuleTimer.optString("path")
            val data = logicRuleTimer.optJSONArray("data") ?: JSONArray()
            val pathSplit = path.split("/")
            val logicCollectionId = pathSplit.getOrNull(2)?.toIntOrNull() ?: continue

            if (DevelcoHandler.Endpoints.LOGIC_RULES.regex.matchEntire(path) != null) {
                for (j in data.indices()) {
                    val logicRule = data.optJSONObject(j) ?: JSONObject()

                    val metadata = try {
                        gson.fromJson(logicRule.optString("metadata"), RuleMetadata::class.java) ?: RuleMetadata()
                    } catch (ex: JSONException) {
                        RuleMetadata()
                    } catch (ex: IllegalStateException) {
                        RuleMetadata()
                    } catch (ex: JsonSyntaxException) {
                        RuleMetadata()
                    }

                    val triggers: Array<Trigger> = try {
                        gson.fromJson(logicRule.optJSONArray("triggers")?.toString(), Array<Trigger>::class.java) ?: emptyArray()
                    } catch (ex: JSONException) {
                        emptyArray()
                    } catch (ex: IllegalStateException) {
                        emptyArray()
                    } catch (ex: JsonSyntaxException) {
                        emptyArray()
                    }

                    val conditions: Array<Condition> = try {
                        gson.fromJson(logicRule.optJSONArray("conditions")?.toString(), Array<Condition>::class.java) ?: emptyArray()
                    } catch (ex: JSONException) {
                        emptyArray()
                    } catch (ex: IllegalStateException) {
                        emptyArray()
                    } catch (ex: JsonSyntaxException) {
                        emptyArray()
                    }

                    val actions: Array<Action> = try {
                        gson.fromJson(logicRule.optJSONArray("actions")?.toString(), Array<Action>::class.java) ?: emptyArray()
                    } catch (ex: JSONException) {
                        emptyArray()
                    } catch (ex: IllegalStateException) {
                        emptyArray()
                    } catch (ex: JsonSyntaxException) {
                        emptyArray()
                    }

                    val rule = LogicRule(
                        gateway.serial,
                        logicCollectionId,
                        logicRule.optInt("id"),
                        logicRule.optString("name"),
                        metadata,
                        logicRule.optBoolean("enabled"),
                        triggers,
                        conditions,
                        actions
                    )

                    tempRules.add(rule)
                }

                logicRules.add(tempRules)
            } else if (DevelcoHandler.Endpoints.LOGIC_TIMERS.regex.matchEntire(path) != null) {
                for (j in data.indices()) {
                    val logicTimer = data.optJSONObject(j) ?: JSONObject()

                    val metadata = try {
                        JSONObject(logicTimer.optString("metadata", ""))
                    } catch (ex: JSONException) {
                        JSONObject()
                    }

                    val actions: Array<Action> = try {
                        gson.fromJson(logicTimer.optJSONArray("actions")?.toString(), Array<Action>::class.java) ?: emptyArray()
                    } catch (ex: JSONException) {
                        emptyArray()
                    } catch (ex: IllegalStateException) {
                        emptyArray()
                    } catch (ex: JsonSyntaxException) {
                        emptyArray()
                    }

                    tempTimers.add(
                        LogicTimer(
                            gateway.serial,
                            logicCollectionId,
                            logicTimer.optInt("id"),
                            logicTimer.optString("name"),
                            metadata,
                            logicTimer.optInt("timeout"),
                            actions,
                            logicTimer.optInt("remaining"),
                            logicTimer.optString("status")
                        )
                    )
                }

                logicTimers.add(tempTimers)
            }
        }

        SunriseSunsetHandler.validator()
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncLogicRulesCached(
        gateway: NabtoHandler.NabtoGateway,
        logicCollection: LogicCollection
    ) {
        if (logicRulesList.any { it.parentGateway == logicCollection.parentGateway && it.logicCollectionId == logicCollection.id }) return


        syncLogicRulesAndTimersCached(gateway, true)
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncLogicTimersCached(
        gateway: NabtoHandler.NabtoGateway,
        logicCollection: LogicCollection
    ) {
        if (logicTimersList.any { it.parentGateway == logicCollection.parentGateway && it.logicCollectionId == logicCollection.id }) return

        syncLogicRulesAndTimersCached(gateway, true)
    }

    private var isSyncingFirmware = false
    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncZigbeeFirmware(gateway: NabtoHandler.NabtoGateway) {
        if (isSyncingFirmware) return
        isSyncingFirmware = true
        val response = DevelcoHandler.getFirmwareImages(gateway)
        val images = response.optJSONArray("body") ?: JSONArray()

        for (i in images.indices()) {
            val image = images.optJSONObject(i) ?: JSONObject()

            if (image.optString("technology").equals("zigbee", ignoreCase = true) &&
                !image.optString("storage").equals("permanent", ignoreCase = true)) {
                try {
                    DevelcoHandler.deleteFirmwareImage(gateway, image.optInt("id"))
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                    }
                }
            }
        }

        val bundleArray = OtaHandler.zigbeeFirmwareArray ?: return
        var error = false

        for (i in bundleArray.indices()) {
            val fw = bundleArray.optJSONObject(i) ?: JSONObject()
            try {
                DevelcoHandler.postFirmwareDownload(
                    gateway,
                    JSONObject()
                        .put("uri", fw.optString("uri"))
                        .put("hash", fw.optString("hash"))
                        .put("technology", fw.optString("technology"))
                        .put("algorithm", fw.optString("algorithm"))
                        .put("storage", "persisted")
                )
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                }
                error = true
            }
        }

        if (!error) {
            val gatewayDevice = devicesList.firstOrNull { it.deviceClass == Device.DeviceClass.GATEWAY }
            getHubMetadata(gateway)?.let { metadata ->
                metadata.put(
                    "zigbee_firmware_bundle",
                    OtaHandler.zigbeeFirmwareBundle.value?.optInt("fw_increment")
                )

                gatewayDevice?.let {
                    DevelcoHandler.putDevice(
                        gateway,
                        gatewayDevice.id,
                        JSONObject().put("metadata", metadata.toString())
                    )
                }
            }
        }
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncFirmwareStatus(gateway: NabtoHandler.NabtoGateway) {
        val response = DevelcoHandler.getFirmwareStatus(gateway)
        val body = response.optJSONArray("body") ?: JSONArray()

        for (i in body.indices()) {
            val statusJson = body.optJSONObject(i) ?: continue

            if (statusJson.optString("technology") == "zigbee") {
                val eui = statusJson.optString("id")

                val device = devicesList.firstOrNull { it.eui == eui } ?: continue

                device.otaStatus = statusJson.optString("status")
                device.firmwareVersion = statusJson.optString("version")

                devices.replace(device)
            } else if (
                statusJson.optString("technology") == "gateway" &&
                statusJson.optString("id") == "dp-release"
            ) {
                gateway.gwFirmware.postValue(statusJson.optString("version"))
            }
        }

    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun reportDeviceStates(gateway: NabtoHandler.NabtoGateway, device: Device) {
        val reports = JSONArray()

        if (device.deviceClass == Device.DeviceClass.AURORABULB) {
            reports.put(
                JSONObject()
                    .put("key", "reportlevel")
                    .put(
                        "data", JSONObject()
                            .put("value", true)
                    )
            )
        }

        if (device.deviceClass == Device.DeviceClass.AURORATWBULB) {
            reports
                .put(
                    JSONObject()
                        .put("key", "reportlevel")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                )
                .put(
                    JSONObject()
                        .put("key", "reportcolourtemperature")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                )
        }

        if (device.deviceClass == Device.DeviceClass.AURORARGBWBULB) {
            reports
                .put(
                    JSONObject()
                        .put("key", "reportlevel")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                )
                .put(
                    JSONObject()
                        .put("key", "reportcolourtemperature")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                )
                .put(
                    JSONObject()
                        .put("key", "reporthue")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                )
        }

        if (!reports.isEmpty()) {
            DevelcoHandler.putDeviceData(
                gateway,
                device.id,
                device.ldevs.firstOrNull() ?: "bulb",
                reports
            )
        }
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun reportGroupStates(gateway: NabtoHandler.NabtoGateway, group: Group) {
        DevelcoHandler.putGroupData(
            gateway,
            group.id,
            "generic",
            JSONArray()
                .put(
                    JSONObject()
                        .put("key", "reportlevel")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                )
                .put(
                    JSONObject()
                        .put("key", "reportcolourtemperature")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                )
                .put(
                    JSONObject()
                        .put("key", "reporthue")
                        .put(
                            "data", JSONObject()
                                .put("value", true)
                        )
                ),
            first = false,
            maxRetries = 0
        )
    }

    suspend fun getHubMetadata(gateway: NabtoHandler.NabtoGateway): JSONObject? {
        val response = try {
            DevelcoHandler.getDevices(gateway)
        } catch (err: VolleyError) {
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(
                    gateway,
                    CloudHandler.getCredentials().first
                )
            }
            err.printStackTrace()
            return null
        }
        val body = response.optJSONArray("body") ?: JSONArray()

        for (i in body.indices()) {
            val deviceJson = body.optJSONObject(i) ?: continue

            val deviceClassName = if (!deviceJson.optString("defaultName").equals("squidzigbee", ignoreCase = true)) {
                AppDatabase.getDatabase().identitiesDao()
                    .getDeviceClassForDefaultName(deviceJson.optString("defaultName"))
                    ?: "unknown"
            } else {
                "gateway"
            }

            val deviceClass = try {
                Device.DeviceClass.valueOf(deviceClassName.uppercase())
            } catch (ex: IllegalArgumentException) {
                crashlytics.recordException(
                    Exception("No such enum: defaultName - ${deviceJson.optString("defaultName")}, deviceClass - $deviceClassName")
                )

                Log.e("SyncHandler", "No such enum: defaultName - ${deviceJson.optString("defaultName")}, deviceClass - $deviceClassName")
                continue
            }
            try {
                if (deviceClass == Device.DeviceClass.GATEWAY) {
                    if (deviceJson.has("metadata")) {
                        val metadataString = deviceJson.optString("metadata", "")

                        return if (metadataString.isNotBlank()) {
                            JSONObject(deviceJson.optString("metadata"))
                        } else {
                            JSONObject()
                        }
                    }
                }
            } catch (ex: JSONException) {
                return null
            }
        }
        return null
    }

    fun signOut() {
        syncHandlerCoroutineScope.cancel()
        devices.clear()
        deviceDatapoints.clear()
        groups.clear()
        groupDatapoints.clear()
        groupMembers.clear()
        scenes.clear()
        logicCollections.clear()
        logicRules.clear()
        logicTimers.clear()
    }

    fun restartCoroutineScope() {
        if (syncHandlerCoroutineScope.isActive) return
        syncHandlerCoroutineScope = CoroutineScope(Dispatchers.IO)
    }
}
