package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.deviceDatapoint
import com.aurora.aonev3.network.groupDatapoint
import com.aurora.aonev3.network.groupScenes
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import com.aurora.aonev3.logic.LogicData
import com.aurora.aonev3.logic.UpdateResourceAction
import org.json.JSONArray
import org.json.JSONObject

open class BatteryDimmer1GViewModel : ViewModel() {
    private val logicCollectionRepository = LogicCollectionRepository()
    val mode: MutableLiveData<BatteryDimmerMode> = MutableLiveData(BatteryDimmerMode.NONE)
    var existingMode = BatteryDimmerMode.NONE

    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> = logicCollectionRepository.getLogicCollections(gateway)

    open fun loadExistingLogic(device: Device, logicCollections: List<LogicCollection>) {
        if (existingMode != BatteryDimmerMode.NONE) return

        logicCollections.forEach { logicCollection ->
            val metadata = logicCollection.metadata

            if (metadata.collectionType == CollectionType.BATTERY_DIMMER &&
                metadata.triggerId == device.id
            ) {
                try {
                    existingMode = BatteryDimmerMode.fromSubType(metadata.subType?.displayName ?: "")
                    mode.postValue(existingMode)

                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
                return@forEach
            }

        }
    }

    suspend fun saveLogic(device: Device, group: Group) {
        val gateway = NabtoHandler.selectedGateway ?: return

        SyncHandler
            .logicCollectionsList
            .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
            val metadata = logicCollection.metadata

            if (metadata.collectionType == CollectionType.BATTERY_DIMMER &&
                metadata.triggerId == device.id
            ) {
                try {
                    DevelcoHandler.deleteLogicCollection(
                        gateway,
                        logicCollection.id
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

                }
            }
        }

        val metadata = JSONObject()
            .put("collection_type", "battery_dimmer")
            .put("trigger_id", device.id)
            .put("sub_type", mode.value?.subType)
            .put("target_space", group.id)

        val collectionResponse =
            try {
                DevelcoHandler
                    .postLogicCollection(
                        gateway,
                        JSONObject()
                            .put("name", device.name)
                            .put("metadata", metadata.toString())
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
                return
            }
        val collectionId =
            collectionResponse.optJSONObject("body")?.optInt("id") ?: return

        saveBasicRules(group, device, "remote", gateway, collectionId)

        when (mode.value) {
            BatteryDimmerMode.COLOUR_TEMPERATURE -> saveMiredRules(
                group,
                device,
                "remote",
                gateway,
                collectionId
            )
            BatteryDimmerMode.COLOUR -> saveHueRules(
                group,
                device,
                "remote",
                gateway,
                collectionId
            )
            BatteryDimmerMode.SCENES -> saveSceneRules(
                group,
                device,
                "remote",
                gateway,
                collectionId
            )
            else -> {
            }
        }
    }

    protected suspend fun saveBasicRules(
        group: Group,
        device: Device,
        ldev: String,
        gateway: NabtoHandler.NabtoGateway,
        collectionId: Int
    ) {
        val onCondition = buildCondition(groupDatapoint(group, datapoint = "/onoff.value"), false)
        val onAction = UpdateResourceAction(groupDatapoint(group, datapoint = "/onoff"), LogicData(value = true))
        val offCondition = buildCondition(groupDatapoint(group, datapoint = "/onoff.value"), true)
        val offAction = UpdateResourceAction(groupDatapoint(group, datapoint = "/onoff"), LogicData(value = false))
        val stepUpAction = UpdateResourceAction(groupDatapoint(group, datapoint = "/stepup"), LogicData(value = 8))
        val stepDownAction = UpdateResourceAction(groupDatapoint(group, datapoint = "/stepdown"), LogicData(value = 8))
        val onOffTrigger = buildTrigger(deviceDatapoint(device, ldev, "/power"))
        val stepUpTrigger = buildTrigger(deviceDatapoint(device, ldev, "/stepup"))
        val stepDownTrigger = buildTrigger(deviceDatapoint(device, ldev, "/stepdown"))

        val rules: ArrayList<JSONObject> = arrayListOf(
            JSONObject()
                .put("name", "On rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(onOffTrigger))))
                .put("conditions", JSONArray(gson.toJson(arrayOf(onCondition))))
                .put("actions", JSONArray(gson.toJson(arrayOf(onAction)))),
            JSONObject()
                .put("name", "Off rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(onOffTrigger))))
                .put("conditions", JSONArray(gson.toJson(arrayOf(offCondition))))
                .put("actions", JSONArray(gson.toJson(arrayOf(offAction)))),
            JSONObject()
                .put("name", "Step up rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(stepUpTrigger))))
                .put("actions", JSONArray(gson.toJson(arrayOf(stepUpAction)))),
            JSONObject()
                .put("name", "Step down rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(stepDownTrigger))))
                .put("actions", JSONArray(gson.toJson(arrayOf(stepDownAction))))
        )

        try {
            rules.forEach { rule ->
                DevelcoHandler.postLogicRule(
                    gateway,
                    collectionId,
                    rule
                )
            }
        } catch (err: VolleyError) {
            App.actionFailed()
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(
                    gateway,
                    CloudHandler.getCredentials().first
                )
            }
            return
        }
    }

    protected suspend fun saveMiredRules(
        group: Group,
        device: Device,
        ldev: String,
        gateway: NabtoHandler.NabtoGateway,
        collectionId: Int
    ) {
        val stepUpAction = UpdateResourceAction(groupDatapoint(group, datapoint = "/stepmired"), LogicData(value = 1))
        val stepDownAction = UpdateResourceAction(groupDatapoint(group, datapoint = "/stepmired"), LogicData(value = 3))
        val stepUpTrigger = buildTrigger(deviceDatapoint(device, ldev, "/stepmiredup"))
        val stepDownTrigger = buildTrigger(deviceDatapoint(device, ldev, "/stepmireddown"))

        val rules: ArrayList<JSONObject> = arrayListOf(
            JSONObject()
                .put("name", "Step mired up rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(stepUpTrigger))))
                .put("actions", JSONArray(gson.toJson(arrayOf(stepUpAction)))),
            JSONObject()
                .put("name", "Step mired down rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(stepDownTrigger))))
                .put("actions", JSONArray(gson.toJson(arrayOf(stepDownAction))))
        )

        try {
            rules.forEach { rule ->
                DevelcoHandler.postLogicRule(
                    gateway,
                    collectionId,
                    rule
                )
            }
        } catch (err: VolleyError) {
            App.actionFailed()
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(
                    gateway,
                    CloudHandler.getCredentials().first
                )
            }
            return
        }
    }

    protected suspend fun saveHueRules(
        group: Group,
        device: Device,
        ldev: String,
        gateway: NabtoHandler.NabtoGateway,
        collectionId: Int
    ) {
        val rules: ArrayList<JSONObject> = arrayListOf(
            buildHueRule(group, device, ldev, 5, 0),
            buildHueRule(group, device, ldev, 10, 5),
            buildHueRule(group, device, ldev, 15, 10),
            buildHueRule(group, device, ldev, 25, 15),
            buildHueRule(group, device, ldev, 35, 25),
            buildHueRule(group, device, ldev, 60, 35),
            buildHueRule(group, device, ldev, 100, 60),
            buildHueRule(group, device, ldev, 120, 100),
            buildHueRule(group, device, ldev, 135, 120),
            buildHueRule(group, device, ldev, 150, 135),
            buildHueRule(group, device, ldev, 175, 150),
            buildHueRule(group, device, ldev, 200, 175),
            buildHueRule(group, device, ldev, 230, 200),
            buildHueRule(group, device, ldev, 240, 230),
            buildHueRule(group, device, ldev, 246, 240),
            buildHueRule(group, device, ldev, 250, 246),
            buildHueRule(group, device, ldev, 255, 250),
            buildHueRule(group, device, ldev, 295, 255),
            buildHueRule(group, device, ldev, 345, 295)
        )

        try {
            rules.forEach { rule ->
                DevelcoHandler.postLogicRule(
                    gateway,
                    collectionId,
                    rule
                )
            }
        } catch (err: VolleyError) {
            App.actionFailed()
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(
                    gateway,
                    CloudHandler.getCredentials().first
                )
            }
            return
        }
    }


    private fun buildHueRule(
        group: Group,
        device: Device,
        ldev: String,
        hue: Int,
        lowerHue: Int
    ): JSONObject {
        val actions = if (hue != 240) {
            arrayOf(
                UpdateResourceAction(groupDatapoint(group, datapoint = "/sat"), LogicData(value = 254)),
                UpdateResourceAction(groupDatapoint(group, datapoint = "/hue"), LogicData(value = hue)),
            )
        } else {
            arrayOf(
                UpdateResourceAction(groupDatapoint(group, datapoint = "/mired"), LogicData(value = 370)),
            )
        }
        val conditions = arrayOf(
            buildCondition(deviceDatapoint(device, ldev, "/hue.value"), "##INVAL## <= $hue"),
            buildCondition(deviceDatapoint(device, ldev, "/hue.value"), "##INVAL## > $lowerHue"),
        )
        val triggers = arrayOf(
            buildTrigger(deviceDatapoint(device, ldev, "/hue"))
        )

        return JSONObject()
            .put("name", if (hue != 240) "Step hue rule" else "White")
            .put("triggers", JSONArray(gson.toJson(triggers)))
            .put("conditions", JSONArray(gson.toJson(conditions)))
            .put("actions", JSONArray(gson.toJson(actions)))
    }

    protected suspend fun saveSceneRules(
        group: Group,
        device: Device,
        ldev: String,
        gateway: NabtoHandler.NabtoGateway,
        collectionId: Int
    ) {
        val stepUpActions = arrayOf(
            UpdateResourceAction(groupScenes(group), LogicData(cycle = "next"))
        )
        val stepDownActions = arrayOf(
            UpdateResourceAction(groupScenes(group), LogicData(cycle = "prev"))
        )
        val stepUpTriggers = arrayOf(
            buildTrigger(deviceDatapoint(device, ldev, "/stepmiredup"))
        )
        val stepDownTriggers = arrayOf(
            buildTrigger(deviceDatapoint(device, ldev, "/stepmireddown"))
        )


        val rules: ArrayList<JSONObject> = arrayListOf(
            JSONObject()
                .put("name", "Step Scenes up rule")
                .put("triggers", JSONArray(gson.toJson(stepUpTriggers)))
                .put("actions", JSONArray(gson.toJson(stepUpActions))),
            JSONObject()
                .put("name", "Step Scenes down rule")
                .put("triggers", JSONArray(gson.toJson(stepDownTriggers)))
                .put("actions", JSONArray(gson.toJson(stepDownActions)))
        )

        try {
            rules.forEach { rule ->
                DevelcoHandler.postLogicRule(
                    gateway,
                    collectionId,
                    rule
                )
            }
        } catch (err: VolleyError) {
            App.actionFailed()
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(
                    gateway,
                    CloudHandler.getCredentials().first
                )
            }
            return
        }
    }
}

enum class BatteryDimmerMode(val displayName: String, val subType: String) {
    NONE("None", "none"),
    COLOUR_TEMPERATURE("Colour temperature (cool to warm)", "tunable"),
    COLOUR("Select from 20 predefined colours", "rgb"),
    SCENES("Trigger your Scenes", "scene");

    companion object {
        fun fromSubType(subType: String): BatteryDimmerMode {
            values().forEach {
                if (it.subType == subType.toLowerCase()) {
                    return it
                }
            }

            throw IllegalArgumentException("No enum constant with subType $subType")
        }
    }
}