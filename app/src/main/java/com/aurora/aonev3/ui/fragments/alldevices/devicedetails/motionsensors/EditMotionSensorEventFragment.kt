package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.timers.LogicTimer
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.fragments.schedules.EventTarget
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class EditMotionSensorEventFragment : MotionSensorEventFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle.text = getString(R.string.edit_event)
        
        if (allDeviceViewModel.selectedDevice?.deviceClass == Device.DeviceClass.DOORWINDOW
            || allDeviceViewModel.selectedDevice?.deviceClass == Device.DeviceClass.WINDOW) {
            daylightOuterLayout.visibility = View.GONE
        }
    }

    override fun createLogicCollection(gateway: NabtoHandler.NabtoGateway) {
        binding.btnSave.isEnabled = false
        activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.VISIBLE
        val logicCollection = viewModel.logicCollection ?: return

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val startTime = mStartTime
            val endTime = mEndTime
            val metadata = CollectionMetadata(collectionType = CollectionType.SENSOR, triggerId = allDeviceViewModel.selectedDevice?.id)

            val event = EventMetadata()
            event.days = mDay.name.lowercase()

            if (startTime != null && endTime != null) {
                val startHour = if (startTime.trigger == SunriseSunsetType.TIME) {
                    startTime.hour
                } else {
                    null
                }
                val startMinute = if (startTime.trigger == SunriseSunsetType.TIME) {
                    startTime.minute
                } else {
                    null
                }
                val start = if (startTime.trigger != SunriseSunsetType.TIME) {
                    startTime.trigger.name.lowercase()
                } else {
                    null
                }
                val startOffset =
                    if (startTime.trigger != SunriseSunsetType.TIME && startTime.offset != 0) {
                        startTime.offset
                    } else {
                        null
                    }
                val endHour = if (endTime.trigger == SunriseSunsetType.TIME) {
                    endTime.hour
                } else {
                    null
                }
                val endMinute = if (endTime.trigger == SunriseSunsetType.TIME) {
                    endTime.minute
                } else {
                    null
                }
                val end = if (endTime.trigger != SunriseSunsetType.TIME) {
                    endTime.trigger.name.lowercase()
                } else {
                    null
                }
                val endOffset =
                    if (endTime.trigger != SunriseSunsetType.TIME && endTime.offset != 0) {
                        endTime.offset
                    } else {
                        null
                    }

                event.time = TimeEventMetadata(
                    start = start,
                    startOffset = startOffset,
                    startHour = startHour,
                    startMinute = startMinute,
                    end = end,
                    endOffset = endOffset,
                    endHour = endHour,
                    endMinute = endMinute
                )
            }
            if (mIsAllDay == true) {
                event.time = null
            }

            when (mEventTarget) {
                EventTarget.SCENE -> {
                    mScene?.let {
                        event.scene = SceneEventMetadata(it.id, it.groupId)
                    }
                }
                EventTarget.SPACE -> {
                    mGroup?.let {
                        event.group = GroupEventMetadata(it.id)
                    }
                }
                EventTarget.DEVICE -> {
                    mSelectedDevice?.let {
                        event.device = DeviceEventMetadata(it.first.id, it.second, mGroup?.id)
                    }
                }
                null -> {
                }
            }

            metadata.event = event

            val newLogicCollection = LogicCollection(gateway.serial, logicCollection.id, allDeviceViewModel.selectedDevice?.name ?: "", metadata, true).toJSONObject()

            JSONObject() //Clear response before asynchronous api call
            try {
                DevelcoHandler.putLogicCollection(
                    gateway,
                    logicCollection.id,
                    newLogicCollection
                )

                val logicTimers = SyncHandler
                    .logicTimersList
                    .filter { it.parentGateway == logicCollection.parentGateway && it.logicCollectionId == logicCollection.id }
                for (i in logicTimers.size - 1 downTo 0) {
                    val timer = logicTimers[i]
                    DevelcoHandler.deleteLogicTimer(gateway, timer.logicCollectionId, timer.id)
                }
                val logicRules = SyncHandler
                    .logicRulesList
                    .filter { it.parentGateway == logicCollection.parentGateway && it.logicCollectionId == logicCollection.id }
                for (i in logicRules.size - 1 downTo 0) {
                    val rule = logicRules[i]
                    DevelcoHandler.deleteLogicRule(gateway, rule.logicCollectionId, rule.id)
                }

                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val offLogicTimerId = createLogicTimer(gateway, logicCollection, isOverride = false)
                    val overrideLogicTimerId = createLogicTimer(gateway, logicCollection, isOverride = true)
                    var offLogicTimer: LogicTimer? = null
                    var overrideLogicTimer: LogicTimer? = null

                    viewModel.viewModelScope.launch(Dispatchers.Main) mainLaunch@{

                        val offLogicTimerResponse = viewModel.getLogicTimers(gateway)
                        offLogicTimerResponse.observe(viewLifecycleOwner) offObserve@{ logicTimers ->
                            val logicTimer = logicTimers.toList().find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollection.id && it.id == offLogicTimerId }
                            offLogicTimer = logicTimer
                            val offTimer = offLogicTimer ?: return@offObserve
                            offLogicTimerResponse.removeObservers(viewLifecycleOwner)

                            val overrideTimer = overrideLogicTimer ?: return@offObserve
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                createLogicRules(gateway, logicCollection, offTimer, overrideTimer)

                                activity?.runOnUiThread {
                                    binding.btnSave.isEnabled = true
                                    activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.GONE
                                    findNavController().popBackStack()
                                }
                            }
                        }

                        val overrideLogicTimerResponse = viewModel.getLogicTimers(gateway)
                        overrideLogicTimerResponse.observe(viewLifecycleOwner) offObserve@{ logicTimers ->
                            val logicTimer = logicTimers.toList().find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollection.id && it.id == overrideLogicTimerId }
                            overrideLogicTimer = logicTimer
                            val overrideTimer = overrideLogicTimer ?: return@offObserve
                            overrideLogicTimerResponse.removeObservers(viewLifecycleOwner)

                            val offTimer = offLogicTimer ?: return@offObserve
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                createLogicRules(gateway, logicCollection, offTimer, overrideTimer)

                                activity?.runOnUiThread {
                                    binding.btnSave.isEnabled = true
                                    activity?.findViewById<android.view.View>(R.id.layoutGreyOut)?.visibility = View.GONE
                                    findNavController().popBackStack()
                                }
                            }
                        }
                    }
                }
            } catch (err: VolleyError) {
                App.actionFailed()
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                    val credentials = CloudHandler.getCredentials()
                    if (credentials.first.isEmpty()) {
                        activity?.finishAffinity()
                        startActivity(Intent(context, SplashscreenActivity::class.java))
                    }
                    NabtoHandler.openTunnel(gateway, credentials.first)
                }
            }
        }
    }

    companion object {
        private const val TAG = "EditMotionSensorEventF…"
        fun newInstance() =
            EditMotionSensorEventFragment()
    }
}
