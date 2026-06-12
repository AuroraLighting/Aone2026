package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.walldimmer

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.deviceDatapoint
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import com.aurora.aonev3.logic.LogicData
import com.aurora.aonev3.logic.UpdateResourceAction
import org.json.JSONArray
import org.json.JSONObject

class WallDimmerControlViewModel : ViewModel() {
    private val logicCollectionRepository = LogicCollectionRepository()
    var existingTarget: Device? = null
    var target: MutableLiveData<Device?> = MutableLiveData(null)

    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> =
        logicCollectionRepository.getLogicCollections(gateway)

    suspend fun saveLogic(device: Device, target: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return

        SyncHandler
            .logicCollectionsList
            .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
            val metadata = logicCollection.metadata

            if (metadata.collectionType == CollectionType.WALLDIMMER &&
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
            .put("collection_type", "walldimmer")
            .put("trigger_id", device.id)
        val onOffTrigger = buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/power"))
        val stepUpTrigger = buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/stepup"))
        val stepDownTrigger = buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/stepdown"))
        val onOffAction = UpdateResourceAction(deviceDatapoint(target, target.ldevs.first(), "/toggle"), LogicData(value = true))
        val stepUpAction = UpdateResourceAction(deviceDatapoint(target, target.ldevs.first(), "/stepup"), LogicData(value = 10))
        val stepDownAction = UpdateResourceAction(deviceDatapoint(target, target.ldevs.first(), "/stepdown"), LogicData(value = 10))

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

        val rules: ArrayList<JSONObject> = arrayListOf(
            JSONObject()
                .put("name", "Toggle rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(onOffTrigger))))
                .put("actions", JSONArray(gson.toJson(arrayOf(onOffAction)))),
            JSONObject()
                .put("name", "Step up rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(stepUpTrigger))))
                .put("actions", JSONArray(gson.toJson(arrayOf(stepUpAction)))),
            JSONObject()
                .put("name", "Step down rule")
                .put("triggers", JSONArray(gson.toJson(arrayOf(stepDownTrigger))))
                .put("actions", JSONArray(gson.toJson(arrayOf(stepDownAction)))),
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
        }
    }

}
