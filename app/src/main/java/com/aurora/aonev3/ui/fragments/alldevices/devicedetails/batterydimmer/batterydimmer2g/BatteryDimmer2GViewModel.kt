package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer2g

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g.BatteryDimmer1GViewModel
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g.BatteryDimmerMode
import org.json.JSONObject

class BatteryDimmer2GViewModel : BatteryDimmer1GViewModel() {
    val targetGroup: MutableLiveData<Group?> = MutableLiveData(null)
    var existingTargetGroup: Group? = null
    val targetGroup2: MutableLiveData<Group?> = MutableLiveData(null)
    var existingTargetGroup2: Group? = null
    val mode2: MutableLiveData<BatteryDimmerMode> = MutableLiveData(BatteryDimmerMode.NONE)
    var existingMode2 = BatteryDimmerMode.NONE

    override fun loadExistingLogic(device: Device, logicCollections: List<LogicCollection>) {
        val gateway = NabtoHandler.selectedGateway ?: return
        if (existingMode != BatteryDimmerMode.NONE) return

        logicCollections.forEach { logicCollection ->
            val metadata = logicCollection.metadata

            if (metadata.collectionType == CollectionType.BATTERY_DIMMER_2 &&
                metadata.triggerId == device.id
            ) {
                try {
                    existingTargetGroup = SyncHandler
                        .groupsList
                        .find {
                            it.parentGateway == gateway.serial
                                    && it.id == metadata.targetSpaceLeft
                        }
                    targetGroup.postValue(existingTargetGroup)
                    existingTargetGroup2 = SyncHandler
                        .groupsList
                        .find {
                            it.parentGateway == gateway.serial
                                    && it.id == metadata.targetSpaceRight
                        }
                    targetGroup2.postValue(existingTargetGroup2)
                    existingMode = BatteryDimmerMode
                        .fromSubType(
                            metadata.subTypeLeft?.displayName ?: ""
                        )
                    mode.postValue(existingMode)
                    existingMode2 = BatteryDimmerMode.fromSubType(
                        metadata.subTypeRight?.displayName ?: ""
                    )
                    mode2.postValue(existingMode2)

                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }
                return@forEach
            }

        }
    }

    suspend fun saveLogic(device: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return

        SyncHandler
            .logicCollectionsList
            .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
            val metadata = logicCollection.metadata

            if (metadata.collectionType == CollectionType.BATTERY_DIMMER_2 &&
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
            .put("collection_type", "battery_dimmer_2")
            .put("trigger_id", device.id)
            .put("sub_type_left", mode.value?.subType)
            .put("target_space_left", targetGroup.value?.id)
            .put("sub_type_right", mode2.value?.subType)
            .put("target_space_right", targetGroup2.value?.id)

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

        targetGroup.value?.let {
            saveBasicRules(it, device, "remote1", gateway, collectionId)

            when (mode.value) {
                BatteryDimmerMode.COLOUR_TEMPERATURE -> saveMiredRules(
                    it,
                    device,
                    "remote1",
                    gateway,
                    collectionId
                )
                BatteryDimmerMode.COLOUR -> saveHueRules(
                    it,
                    device,
                    "remote1",
                    gateway,
                    collectionId
                )
                BatteryDimmerMode.SCENES -> saveSceneRules(
                    it,
                    device,
                    "remote1",
                    gateway,
                    collectionId
                )
                else -> {
                }
            }
        }
        targetGroup2.value?.let {
            saveBasicRules(it, device, "remote2", gateway, collectionId)

            when (mode2.value) {
                BatteryDimmerMode.COLOUR_TEMPERATURE -> saveMiredRules(
                    it,
                    device,
                    "remote2",
                    gateway,
                    collectionId
                )
                BatteryDimmerMode.COLOUR -> saveHueRules(
                    it,
                    device,
                    "remote2",
                    gateway,
                    collectionId
                )
                BatteryDimmerMode.SCENES -> saveSceneRules(
                    it,
                    device,
                    "remote2",
                    gateway,
                    collectionId
                )
                else -> {
                }
            }
        }
    }
}
