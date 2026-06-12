package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.remotes

import com.aurora.aonev3.synthetic.*
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
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
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.DeviceDetailFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class RemoteDetailFragment : DeviceDetailFragment() {

    private val remoteViewModel: RemoteDetailViewModel by activityViewModels()

    private var mTargetGroup: Pair<Group, String>? = null
    private var mTargetDevice: Pair<Device, String>? = null
    private var mTargetRecall: RecallMode? = null
    private var mLogicCollection: LogicCollection? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        val device = viewModel.selectedDevice ?: return

        viewModel.group.observe(viewLifecycleOwner, {
            controlLayout.isClickable = allowEditing() && it != null
            setControlColour()
            remoteViewModel.selectedGroup = it
            if (remoteViewModel.targetGroup.value != null && it != null) {
                remoteViewModel.targetGroup.postValue(Pair(it, it.ldevs.first()))
            }
        })

        btnIdentify.visibility = View.GONE

        controlOuterLayout.visibility = View.VISIBLE
        recallOuterLayout.visibility = View.VISIBLE

        controlLayout.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                RemoteDetailFragmentDirections
                    .actionRemoteDetailFragmentToRemoteDeviceSelectorFragment()
            )
        )
        controlLayout.isClickable = allowEditing() && viewModel.group.value != null

        recallLayout.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                RemoteDetailFragmentDirections
                    .actionRemoteDetailFragmentToRemoteRecallSelectorFragment()
            )
        )
        recallLayout.isClickable = false

        remoteViewModel.targetGroup.observe(viewLifecycleOwner, { group ->
            mTargetGroup = group
            group?.let {
                tvControl.text = getString(R.string.control_entire_space)
                recallLayout.isClickable = allowEditing() && true
            }

            if (mTargetDevice == null && mTargetGroup == null) {
                tvControl.text = getString(R.string.select_a_device_to_control_or_the_entire_space)
            }

            setControlColour()
            setRecallColour()
        })

        remoteViewModel.targetDevice.observe(viewLifecycleOwner, { targetDevice ->
            mTargetDevice = targetDevice
            targetDevice?.let {
                tvControl.text = targetDevice.first.name
                if (it.first.deviceClass in arrayOf(
                        Device.DeviceClass.AURORATWBULB,
                        Device.DeviceClass.AURORARGBWBULB
                    )
                ) {
                    recallLayout.isClickable = allowEditing() && true
                } else {
                    recallLayout.isClickable = false
                    remoteViewModel.targetRecall.postValue(RecallMode.SET_TO_50)
                }
            }

            setControlColour()
            setRecallColour()
        })

        remoteViewModel.targetRecall.observe(viewLifecycleOwner, { recallMode ->
            mTargetRecall = recallMode

            when (recallMode) {
                RecallMode.CYCLE_SCENES -> tvRecall.text = getString(R.string.scene_cycle)
                RecallMode.STEP_COLOUR_TEMPERATURE -> tvRecall.text =
                    getString(R.string.step_colour_temperature)
                RecallMode.SET_TO_50 -> tvRecall.text = getString(R.string.set_to_50)
                null -> tvRecall.text = getString(R.string.set_the_recall_button_functionality)
            }

            setRecallColour()
        })

        NabtoHandler.selectedGateway?.let { gateway ->
            remoteViewModel.getLogicCollections(gateway)
                .observe(viewLifecycleOwner, Observer { logicCollections ->
                    if (mTargetRecall != null) return@Observer
                    logicCollections?.forEach { logicCollection ->
                        val metadata = logicCollection.metadata

                        if (metadata.collectionType == CollectionType.REMOTE &&
                            metadata.triggerId == device.id
                        ) {
                            mLogicCollection = logicCollection

                            try {
                                val targetRecall = RecallMode.valueOf(metadata.subType?.name ?: "")
                                remoteViewModel.targetRecall.postValue(targetRecall)
                                remoteViewModel.previousTargetRecall = targetRecall

                            } catch (ex: IllegalArgumentException) {
                                ex.printStackTrace()
                            }

                            if (metadata.actionDevices != null) {
                                val actionDevice =
                                    metadata.actionDevices?.first()
                                val deviceId = actionDevice?.id ?: -1
                                val ldev = actionDevice?.ldev ?: ""
                                val targetDevice = SyncHandler
                                    .devicesList
                                    .find { it.parentGateway == gateway.serial && it.id == deviceId }

                                if (targetDevice != null) {
                                    remoteViewModel.targetDevice.postValue(Pair(targetDevice, ldev))
                                    remoteViewModel.previousTargetDevice = Pair(targetDevice, ldev)
                                }
                            }

                            if (metadata.actionGroups != null) {
                                val groupId =
                                    metadata.actionGroups?.first()
                                        ?.id ?: 1
                                val group = SyncHandler
                                    .groupsList
                                    .find {
                                        it.parentGateway == gateway.serial
                                                && it.id == groupId
                                    }
                                group?.let {
                                    val targetGroup = Pair(it, it.ldevs.first())
                                    remoteViewModel.targetGroup.postValue(targetGroup)
                                    remoteViewModel.previousTargetGroup = targetGroup
                                }
                            }

                            if (metadata.actionDevices == null
                                && metadata.actionGroups == null
                            ) {
                                val allLogicRules = SyncHandler
                                    .logicRulesList
                                    .filter { logicRule -> logicCollection.parentGateway == gateway.serial && logicRule.logicCollectionId == logicCollection.id }

                                allLogicRules.forEach { logicRule ->
                                    val actions = logicRule.actions ?: emptyArray()
                                    for (action in actions) {
                                        if (action !is UpdateResourceAction) continue
                                        val path = action.path
                                        val pathSplit = path.split("/")
                                        val isGroup = path.contains("grp/grps")
                                        val id = pathSplit.mapNotNull { it.toIntOrNull() }.firstOrNull() ?: 0
                                        val ldev = try {
                                            pathSplit[4]
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                            ""
                                        }

                                        if (isGroup) {
                                            val targetGroup =SyncHandler
                                                .groupsList
                                                .find {
                                                    it.parentGateway == gateway.serial
                                                            && it.id == id
                                                }

                                            if (targetGroup != null) {
                                                remoteViewModel.targetGroup.postValue(
                                                    Pair(
                                                        targetGroup,
                                                        ldev
                                                    )
                                                )
                                                remoteViewModel.previousTargetGroup =
                                                    Pair(targetGroup, ldev)
                                            }
                                        } else {
                                            val targetDevice = SyncHandler
                                                .devicesList
                                                .find { it.parentGateway == gateway.serial && it.id == id }

                                            if (targetDevice != null) {
                                                remoteViewModel.targetDevice.postValue(
                                                    Pair(
                                                        targetDevice,
                                                        ldev
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            return@forEach
                        }
                    }
                })
        }

        if (!allowEditing()) {
            controlLayout.isClickable = false
            recallLayout.isClickable = false
        }
    }

    override fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val device = viewModel.selectedDevice ?: return@OnClickListener
            binding.btnSave.isEnabled = false
            activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.VISIBLE
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val gateway = NabtoHandler.selectedGateway ?: return@launch

                if (viewModel.deviceName.isNotBlank() && viewModel.deviceName != device.name) {
                    if (gateway.isConnected) {
                        try {
                            DevelcoHandler.putDevice(
                                gateway,
                                device.id,
                                JSONObject().put("name", viewModel.deviceName)
                            )
                        } catch (err: VolleyError) {
                            App.actionFailed()
                            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                gateway.isConnected = false
                                val credentials = CloudHandler.getCredentials()
                                if (credentials.first.isEmpty()) {
                                    activity?.finishAffinity()
                                    startActivity(Intent(context, MainActivity::class.java))
                                }
                                NabtoHandler.openTunnel(gateway, credentials.first)
                            }
                            err.printStackTrace()
                        }

                        viewModel.deviceName = ""

                    }
                }
                if (mTargetRecall != remoteViewModel.previousTargetRecall ||
                    mTargetDevice != remoteViewModel.previousTargetDevice ||
                    mTargetGroup != remoteViewModel.previousTargetGroup
                ) {
                    if (mTargetRecall != null) {
                        SyncHandler
                            .logicCollectionsList
                            .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
                            val metadata = logicCollection.metadata

                            if (metadata.collectionType == CollectionType.REMOTE &&
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
                                        val credentials = CloudHandler.getCredentials()
                                        if (credentials.first.isEmpty()) {
                                            activity?.finishAffinity()
                                            startActivity(Intent(context, MainActivity::class.java))
                                        }
                                        NabtoHandler.openTunnel(gateway, credentials.first)
                                    }

                                }
                            }
                        }
                        val metadata = JSONObject()
                            .put("collection_type", "remote")
                            .put("sub_type", mTargetRecall?.name?.toLowerCase())
                            .put("trigger_id", device.id)

                        var onConditions = emptyArray<Condition>()
                        var onActions = emptyArray<Action>()
                        var offConditions = emptyArray<Condition>()
                        var offActions = emptyArray<Action>()
                        var stepUpActions = emptyArray<Action>()
                        var stepDownActions = emptyArray<Action>()
                        var moveUpActions = emptyArray<Action>()
                        var moveDownActions = emptyArray<Action>()
                        var stopActions = emptyArray<Action>()
                        var recallConditions: Array<Condition>? = null
                        var recallActions = emptyArray<Action>()
                        var recallResetConditions: Array<Condition>? = null
                        var recallResetActions: Array<Action>? = null

                        if (mTargetGroup != null) {
                            val groupMemberIds = SyncHandler
                                .groupMembersList
                                .filter { it.parentGateway == gateway.serial && it.groupId == mTargetGroup?.first?.id }
                                .map { it.deviceId }
                            val datapoints = SyncHandler
                                .deviceDatapointsList
                                .filter { it.parentGateway == gateway.serial && it.id in groupMemberIds }
                            val colourTemperatureMins = datapoints
                                .filter { it.key == "colourtempmin" }
                                .mapNotNull { it.value as? Int }
                            val colourTemperatureMaxes = datapoints
                                .filter { it.key == "colourtempmax" }
                                .mapNotNull { it.value as? Int }
                            val colourTemperatureMin = colourTemperatureMins.maxOrNull() ?: 153
                            val colourTemperatureMax = colourTemperatureMaxes.minOrNull() ?: 454

                            metadata.put(
                                "action_groups",
                                JSONArray().put(JSONObject().put("id", mTargetGroup?.first?.id))
                            )

                            onConditions = arrayOf(buildCondition(
                                groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/onoff.value"),
                                false
                            ))
                            onActions = onActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/onoff"), LogicData(value = true)))
                            offConditions = offConditions.plus(buildCondition(
                                groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/onoff.value"),
                                true
                            ))
                            offActions = offActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/onoff"), LogicData(value = false)))
                            stepUpActions = stepUpActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/stepup"), LogicData(value = 25)))
                            stepDownActions = stepDownActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/stepdown"), LogicData(value = 25)))
                            moveUpActions = moveUpActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/moveup"), LogicData(value = 25)))
                            moveDownActions = moveDownActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/movedown"), LogicData(value = 25)))
                            stopActions = stopActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/stop"), LogicData(value = true)))

                            recallActions = when (mTargetRecall) {
                                RecallMode.CYCLE_SCENES -> recallActions.plus(UpdateResourceAction(groupScenes(mTargetGroup?.first?.id ?: 0), LogicData(cycle = "next")))
                                RecallMode.STEP_COLOUR_TEMPERATURE -> {
                                    recallConditions = arrayOf(buildCondition(
                                        groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/mired.value"),
                                        "##INVAL## >= ${colourTemperatureMin + 60}"
                                    ))

                                    recallResetConditions = arrayOf(buildCondition(
                                        groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/mired.value"),
                                        "##INVAL## < ${colourTemperatureMin + 60}"
                                    ))

                                    recallResetActions = arrayOf(
                                        UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/mired"), LogicData(value = colourTemperatureMax)),
                                                UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/reportcolourtemperature"), LogicData(value = true)),
                                    )
                                    recallActions.plus(arrayOf(
                                        UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/miredstep"), LogicData(value = -60)),
                                        UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/reportcolourtemperature"), LogicData(value = true)),
                                    ))
                                }
                                RecallMode.SET_TO_50,
                                null -> recallActions.plus(UpdateResourceAction(groupDatapoint(mTargetGroup?.first?.id ?: 0, datapoint = "/level"), LogicData(value = 50)))
                            }
                        }
                        if (mTargetDevice != null) {
                            val datapoints = SyncHandler
                                .deviceDatapointsList
                                .filter { it.parentGateway == gateway.serial && it.id == mTargetDevice?.first?.id }
                            val colourTemperatureMin = datapoints
                                .find { it.key == "colourtempmin" }
                                ?.value as? Int ?: 153
                            val colourTemperatureMax = datapoints
                                .find { it.key == "colourtempmax" }
                                ?.value as? Int  ?: 454

                            metadata.put(
                                "action_devices",
                                JSONArray().put(
                                    JSONObject().put(
                                        "id",
                                        mTargetDevice?.first?.id
                                    )
                                )
                            )

                            onConditions = onConditions.plus(buildCondition(deviceDatapoint(
                                mTargetDevice?.first?.id ?: 0,
                                mTargetDevice?.first?.ldevs?.first() ?: "",
                                "/onoff.value"
                            ), false))
                            onActions = onActions.plus(UpdateResourceAction(
                                deviceDatapoint(
                                    mTargetDevice?.first?.id ?: 0,
                                    mTargetDevice?.first?.ldevs?.first() ?: "",
                                    "/onoff"
                                ),
                                LogicData(value = true)))
                            offConditions = offConditions.plus(buildCondition(deviceDatapoint(
                                mTargetDevice?.first?.id ?: 0,
                                mTargetDevice?.first?.ldevs?.first() ?: "",
                                "/onoff.value"
                            ), true))
                            offActions = offActions.plus(UpdateResourceAction(
                                deviceDatapoint(
                                    mTargetDevice?.first?.id ?: 0,
                                    mTargetDevice?.first?.ldevs?.first() ?: "",
                                    "/onoff"
                                ),
                                LogicData(value = false)))
                            stepUpActions = stepUpActions.plus(UpdateResourceAction(
                                deviceDatapoint(
                                    mTargetDevice?.first?.id ?: 0,
                                    mTargetDevice?.first?.ldevs?.first() ?: "",
                                    "/stepup"
                                ),
                                LogicData(value = 25)))
                            stepDownActions = stepDownActions.plus(UpdateResourceAction(
                                deviceDatapoint(
                                    mTargetDevice?.first?.id ?: 0,
                                    mTargetDevice?.first?.ldevs?.first() ?: "",
                                    "/stepdown"
                                ),
                                LogicData(value = 25)))
                            moveUpActions = moveUpActions.plus(UpdateResourceAction(
                                deviceDatapoint(
                                    mTargetDevice?.first?.id ?: 0,
                                    mTargetDevice?.first?.ldevs?.first() ?: "",
                                    "/moveup"
                                ),
                                LogicData(value = 25)))
                            moveDownActions = moveDownActions.plus(UpdateResourceAction(
                                deviceDatapoint(
                                    mTargetDevice?.first?.id ?: 0,
                                    mTargetDevice?.first?.ldevs?.first() ?: "",
                                    "/movedown"
                                ),
                                LogicData(value = 25)))
                            stopActions = stopActions.plus(UpdateResourceAction(
                                deviceDatapoint(
                                    mTargetDevice?.first?.id ?: 0,
                                    mTargetDevice?.first?.ldevs?.first() ?: "",
                                    "/stop"
                                ),
                                LogicData(value = true)))

                            if (mTargetRecall == RecallMode.SET_TO_50) {
                                recallActions = recallActions.plus(UpdateResourceAction(
                                    deviceDatapoint(
                                        mTargetDevice?.first?.id ?: 0,
                                        mTargetDevice?.first?.ldevs?.first() ?: "",
                                        "/level"
                                    ),
                                    LogicData(value = 50)))
                            } else if (mTargetRecall == RecallMode.STEP_COLOUR_TEMPERATURE) {
                                recallConditions = arrayOf(buildCondition(
                                    deviceDatapoint(
                                        mTargetDevice?.first?.id ?: 0,
                                        mTargetDevice?.first?.ldevs?.first() ?: "",
                                        "/mired.value"
                                    ),
                                    "##INVAL## >= ${colourTemperatureMin + 60}"
                                ))

                                recallResetConditions = arrayOf(buildCondition(
                                    deviceDatapoint(
                                        mTargetDevice?.first?.id ?: 0,
                                        mTargetDevice?.first?.ldevs?.first() ?: "",
                                        "/mired.value"
                                    ),
                                    "##INVAL## < ${colourTemperatureMin + 60}"
                                ))

                                recallResetActions = arrayOf(
                                    UpdateResourceAction(deviceDatapoint(
                                        mTargetDevice?.first?.id ?: 0,
                                        mTargetDevice?.first?.ldevs?.first() ?: "",
                                        "/mired"
                                    ),
                                        LogicData(value = colourTemperatureMax)),
                                    UpdateResourceAction(deviceDatapoint(
                                        mTargetDevice?.first?.id ?: 0,
                                        mTargetDevice?.first?.ldevs?.first() ?: "",
                                        "/reportcolourtemperature"
                                    ),
                                        LogicData(value = true))
                                )
                                recallActions = arrayOf(
                                    UpdateResourceAction(deviceDatapoint(
                                        mTargetDevice?.first?.id ?: 0,
                                        mTargetDevice?.first?.ldevs?.first() ?: "",
                                        "/miredstep"
                                    ),
                                        LogicData(value = -60)),
                                    UpdateResourceAction(deviceDatapoint(
                                        mTargetDevice?.first?.id ?: 0,
                                        mTargetDevice?.first?.ldevs?.first() ?: "",
                                        "/reportcolourtemperature"
                                    ),
                                        LogicData(value = true))
                                )
                            }
                        }

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
                                    val credentials = CloudHandler.getCredentials()
                                    if (credentials.first.isEmpty()) {
                                        activity?.finishAffinity()
                                        startActivity(Intent(context, MainActivity::class.java))
                                    }
                                    NabtoHandler.openTunnel(gateway, credentials.first)
                                }
                                return@launch
                            }
                        val collectionId =
                            collectionResponse.optJSONObject("body")?.optInt("id") ?: return@launch

                        val onOffTriggers = arrayOf(buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/power")))
                        val stepUpTriggers = arrayOf(buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/stepup")))
                        val stepDownTriggers = arrayOf(buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/stepdown")))
                        val moveUpTriggers = arrayOf(buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/moveup")))
                        val moveDownTriggers = arrayOf(buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/movedown")))
                        val stopTriggers = arrayOf(buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/stop")))
                        val recallTriggers = arrayOf(buildTrigger(deviceDatapoint(device, device.ldevs.first(), "/recall")))

                        val rules: ArrayList<JSONObject> = arrayListOf(
                            JSONObject()
                                .put("name", "On rule")
                                .put("triggers", JSONArray(gson.toJson(onOffTriggers)))
                                .put("conditions", JSONArray(gson.toJson(onConditions)))
                                .put("actions", JSONArray(gson.toJson(onActions))),
                            JSONObject()
                                .put("name", "Off rule")
                                .put("triggers", JSONArray(gson.toJson(onOffTriggers)))
                                .put("conditions", JSONArray(gson.toJson(offConditions)))
                                .put("actions", JSONArray(gson.toJson(offActions))),
                            JSONObject()
                                .put("name", "Step up rule")
                                .put("triggers", JSONArray(gson.toJson(stepUpTriggers)))
                                .put("actions", JSONArray(gson.toJson(stepUpActions))),
                            JSONObject()
                                .put("name", "Step down rule")
                                .put("triggers", JSONArray(gson.toJson(stepDownTriggers)))
                                .put("actions", JSONArray(gson.toJson(stepDownActions))),
                            JSONObject()
                                .put("name", "Move up rule")
                                .put("triggers", JSONArray(gson.toJson(moveUpTriggers)))
                                .put("actions", JSONArray(gson.toJson(moveUpActions))),
                            JSONObject()
                                .put("name", "Move up rule")
                                .put("triggers", JSONArray(gson.toJson(moveUpTriggers)))
                                .put("actions",JSONArray(gson.toJson(moveUpActions)) ),
                            JSONObject()
                                .put("name", "Move up rule")
                                .put("triggers", JSONArray(gson.toJson(moveDownTriggers)))
                                .put("actions", JSONArray(gson.toJson(moveDownActions))),
                            JSONObject()
                                .put("name", "Stop rule")
                                .put("triggers", JSONArray(gson.toJson(stopTriggers)))
                                .put("actions", JSONArray(gson.toJson(stopActions)))
                        )

                        if (mTargetRecall != RecallMode.STEP_COLOUR_TEMPERATURE) {
                            rules.add(
                                JSONObject()
                                    .put("name", "Recall rule")
                                    .put("triggers", JSONArray(gson.toJson(recallTriggers)))
                                    .put("actions", JSONArray(gson.toJson(recallActions)))
                            )
                        } else {
                            rules.add(
                                JSONObject()
                                    .put("name", "Recall rule")
                                    .put("triggers", JSONArray(gson.toJson(recallTriggers)))
                                    .put("conditions", recallConditions?.let { JSONArray(gson.toJson(recallConditions)) } ?: JSONArray())
                                    .put("actions", JSONArray(gson.toJson(recallActions)))
                            )
                            rules.add(
                                JSONObject()
                                    .put("name", "Recall reset rule")
                                    .put("triggers", JSONArray(gson.toJson(recallTriggers)))
                                    .put("conditions", recallResetConditions?.let { JSONArray(gson.toJson(recallResetConditions)) } ?: JSONArray())
                                    .put("actions", recallResetActions?.let { JSONArray(gson.toJson(recallResetActions)) } ?: JSONArray())
                            )
                        }

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
                                val credentials = CloudHandler.getCredentials()
                                if (credentials.first.isEmpty()) {
                                    activity?.finishAffinity()
                                    startActivity(Intent(context, MainActivity::class.java))
                                }
                                NabtoHandler.openTunnel(gateway, credentials.first)
                            }
                            return@launch
                        }

                    }
                }
                activity?.runOnUiThread {
                    binding.btnSave.isEnabled = true
                    activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun btnDeleteClickListener() {
        val device = viewModel.selectedDevice ?: return
        activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.VISIBLE
        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val logicCollectionsToDelete = ArrayList<Int>()
                SyncHandler
                    .logicCollectionsList
                    .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
                    val metadata = logicCollection.metadata

                    if (metadata.collectionType == CollectionType.REMOTE &&
                        metadata.triggerId == device.id
                    ) {
                        logicCollectionsToDelete.add(logicCollection.id)
                    }
                }

                logicCollectionsToDelete.forEach {
                    try {
                        DevelcoHandler.deleteLogicCollection(
                            gateway,
                            it
                        )
                    } catch (err: VolleyError) {
                        App.actionFailed()
                        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                            gateway.isConnected = false
                            val credentials = CloudHandler.getCredentials()
                            if (credentials.first.isEmpty()) {
                                activity?.finishAffinity()
                                startActivity(Intent(context, MainActivity::class.java))
                            }
                            NabtoHandler.openTunnel(gateway, credentials.first)
                        }

                    }
                }

                SyncHandler
                    .groupsList
                    .filter {
                        it.parentGateway == gateway.serial
                    }.forEach { group ->
                    val metadata = group.metadata
                    val virtualMembers = metadata.optJSONArray("virtual_members") ?: JSONArray()
                    val newVirtualMembers = JSONArray()
                    var updateGroup = false

                    for (i in virtualMembers.indices()) {
                        val virtualMember = virtualMembers.optJSONObject(i)

                        if (virtualMember.optInt("id") != device.id) {
                            newVirtualMembers.put(virtualMember)
                        } else {
                            updateGroup = true
                        }
                    }

                    if (updateGroup) {
                        metadata.put("virtual_members", newVirtualMembers)
                        try {
                            DevelcoHandler.putGroup(
                                gateway,
                                group.id,
                                JSONObject()
                                    .put("metadata", metadata.toString())
                            )
                        } catch (err: VolleyError) {
                            App.actionFailed()
                            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                gateway.isConnected = false
                                val credentials = CloudHandler.getCredentials()
                                if (credentials.first.isEmpty()) {
                                    activity?.finishAffinity()
                                    startActivity(Intent(context, MainActivity::class.java))
                                }
                                NabtoHandler.openTunnel(gateway, credentials.first)
                            }

                        }
                    }
                }

                try {
                    DevelcoHandler.deleteDevice(gateway, device.id)
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        val credentials = CloudHandler.getCredentials()
                        if (credentials.first.isEmpty()) {
                            activity?.finishAffinity()
                            startActivity(Intent(context, MainActivity::class.java))
                        }
                        NabtoHandler.openTunnel(gateway, credentials.first)
                    }

                }

                activity?.runOnUiThread {
                    activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun setControlColour() {
        val group = viewModel.group.value
        when {
            group == null -> {
                controlLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvControl.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimaryBackground
                    )
                )
            }
            mTargetDevice == null && mTargetGroup == null -> {
                controlLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvControl.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorTextPrimary
                    )
                )
            }
            else -> {
                controlLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                tvControl.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimary
                    )
                )
            }
        }
    }

    private fun setRecallColour() {
        val isControlSet = mTargetDevice != null || mTargetGroup != null
        when {
            !isControlSet -> {
                recallLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvRecall.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimaryBackground
                    )
                )
            }
            remoteViewModel.targetRecall.value == null -> {
                recallLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvRecall.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorTextPrimary
                    )
                )
            }
            else -> {
                recallLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                tvRecall.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorPrimary
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "RemoteDetailFragment"
        fun newInstance() =
            RemoteDetailFragment()
    }

    override fun onDetach() {
        remoteViewModel.clearViewModel()
        super.onDetach()
    }
}

enum class RecallMode(val displayName: String) {
    CYCLE_SCENES(App.context.getString(R.string.scene_cycle)),
    STEP_COLOUR_TEMPERATURE(App.context.getString(R.string.step_colour_temperature)),
    SET_TO_50(App.context.getString(R.string.set_to_50))
}
