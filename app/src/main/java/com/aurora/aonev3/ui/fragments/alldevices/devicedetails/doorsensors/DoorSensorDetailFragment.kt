package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorDetailFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorDetailFragmentDirections
import com.aurora.aonev3.ui.fragments.schedules.*
import kotlinx.android.synthetic.main.fragment_device_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class DoorSensorDetailFragment : MotionSensorDetailFragment() {
    private val doorSensorEventViewModel: DoorSensorEventViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
//        val activity = activity ?: throw Exception("Invalid activity")

        listAdapter.run {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val item = getItem(position) ?: return
                    if (item.section == MotionEventSection.EVENT) {
                        val logicCollection = item.logicCollection ?: return

                        NabtoHandler.selectedGateway?.let { gateway ->
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    DevelcoHandler.putLogicCollection(
                                        gateway,
                                        logicCollection.id,
                                        JSONObject().put("enabled", !logicCollection.isEnabled)
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
                            }
                        }
                    } else {
                        val action = DoorSensorDetailFragmentDirections.actionDoorSensorDetailFragmentToDoorSensorEventFragment()
                        findNavController().navigate(action)
                    }
                }
            }

            onItemLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val logicCollection = getItem(position)?.logicCollection ?: return false

                    setUpViewModelEdit(logicCollection)

                    val action = DoorSensorDetailFragmentDirections.actionDoorSensorDetailFragmentToDoorSensorEventFragment()
                    findNavController().navigate(action)

                    return true
                }
            }
        }

        tvEventsTitle.text = getString(R.string.door_sensor_events)
    }

    override fun setUpViewModelEdit(logicCollection: LogicCollection) {
        with(doorSensorEventViewModel) {
            clearViewModel()
            val event = logicCollection.metadata.event ?: EventMetadata()
            existingLogicCollection = logicCollection
            val logicTimers =
                mLogicTimers.filter { it.logicCollectionId == logicCollection.id }

            timeout.value = logicTimers.firstOrNull()?.timeout

            when (event.trigger) {
                TriggerEnum.OPEN -> trigger.value = TriggerEnum.OPEN
                TriggerEnum.CLOSE -> trigger.value = TriggerEnum.CLOSE
                null -> trigger.value = TriggerEnum.OPEN
                else -> trigger.value = null
            }

            if (event.action == RuleMetadataType.OFF) {
                eventAction.value = EventAction.OFF
            } else {
                eventAction.value = EventAction.ON
            }

            if (event.time != null) {
                val time = event.time ?: TimeEventMetadata()
                val start = SunriseSunsetType.fromMetadata(time.start) ?: SunriseSunsetType.TIME
                val startOffset = time.startOffset ?: 0
                val startHour = time.startHour ?: 0
                val startMinute = time.startMinute ?: 0
                val end = SunriseSunsetType.fromMetadata(time.end) ?: SunriseSunsetType.TIME
                val endOffset = time.endOffset ?: 0
                val endHour = time.endHour ?: 0
                val endMinute = time.endMinute ?: 0
                
                updateStartTime(hour = startHour, minute = startMinute, trigger = start, offset = startOffset)
                updateEndTime(hour = endHour, minute = endMinute, trigger = end, offset = endOffset)
                isAllDay.value = false
            } else {
                isAllDay.value = true
            }

            when {
                event.group != null -> {
                    val groupId = event.group?.id ?: -1
                    val group = SyncHandler
                        .groupsList
                        .find {
                            it.parentGateway == logicCollection.parentGateway && it.id == groupId
                        }

                    eventTarget.value = EventTarget.SPACE

                    targetGroup.value = group
                    device.value = null
                    scene.value = null
                }
                event.device != null -> {
                    val deviceId = event.device?.id ?: -1
                    val deviceLdev =
                        event.device?.ldev ?: ""
                    val targetDevice = SyncHandler
                        .devicesList
                        .find { it.parentGateway == logicCollection.parentGateway && it.id == deviceId }
                    targetDevice?.let {
                        val groupMembers = SyncHandler.groupMembersList.filter { groupMember -> groupMember.parentGateway == targetDevice.parentGateway && groupMember.deviceId == targetDevice.id }
                        val groups = SyncHandler.groupsList.filter { group -> group.parentGateway == targetDevice.parentGateway && groupMembers.any { gm -> gm.groupId == group.id } }
                        val group = NestedGroupTree(groups).getChild()?.group

                        targetGroup.value = group
                        eventTarget.value = EventTarget.DEVICE
                        device.value = Pair(targetDevice, deviceLdev)
                    }
                    scene.value = null
                }
                event.scene != null -> {
                    val sceneId = event.scene?.id ?: -1
                    val groupId = event.scene?.group ?: -1
                    val targetScene =
                        SyncHandler
                            .scenesList
                            .find { it.parentGateway == logicCollection.parentGateway && it.id == sceneId && it.groupId == groupId }
                    val group = SyncHandler
                        .groupsList
                        .find {
                            it.parentGateway == logicCollection.parentGateway
                                    && it.id == groupId
                        }

                    eventTarget.value = EventTarget.SCENE

                    targetGroup.value = group
                    device.value = null
                    scene.value = targetScene
                }
            }
            try {
                eventDay.value = EventDay.valueOf(event.days?.toUpperCase() ?: "")
            } catch (ex: IllegalArgumentException) {
                crashlytics.log("E/${TAG}: ${event.days}")
                crashlytics.recordException(ex)
            }
        }
    }

    companion object {
        private const val TAG = "DoorSensorDetailFragment"
        fun newInstance() =
            DoorSensorDetailFragment()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.enable_event,
            R.id.disable_event,
            R.id.delete_event -> super.onMenuItemClick(item)
            R.id.edit_event -> {
                mLogicCollectionMenu?.let { logicCollection ->
                    setUpViewModelEdit(logicCollection)

                    val action = DoorSensorDetailFragmentDirections.actionDoorSensorDetailFragmentToDoorSensorEventFragment()
                    findNavController().navigate(action)
                }
                true
            }
            else -> false
        }
    }
}
