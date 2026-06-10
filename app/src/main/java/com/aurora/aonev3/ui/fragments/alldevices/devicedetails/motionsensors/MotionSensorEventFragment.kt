package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.NewLogicRule
import com.aurora.aonev3.data.logic.timers.LogicTimer
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.*
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.fragments.alldevices.AllDevicesViewModel
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventFragmentDirections
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.aurora.aonev3.ui.fragments.schedules.EventTarget
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_motion_sensor_event.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.abs

open class MotionSensorEventFragment : Fragment() {

    protected val viewModel: MotionSensorEventViewModel by activityViewModels()
    protected val allDeviceViewModel: AllDevicesViewModel by activityViewModels()
    protected var mGroup: Group? = null
    protected var mSelectedDevice: Pair<Device, String>? = null
    protected var mScene: Scene? = null
    protected var mEventTarget: EventTarget? = null
    protected var mDay = EventDay.EVERYDAY
    protected var mStartTime: TriggerTime? = null
    protected var mEndTime: TriggerTime? = null
    protected var mIsAllDay: Boolean? = null
    protected var mLux: Int? = null
    protected var mTimeout: Int? = null

    protected val sunriseSunsetActions = ArrayList<SunriseSunsetActionWrapper>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_motion_sensor_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")

        viewModel.targetGroup.observe(viewLifecycleOwner) { group ->
            mGroup = group
            if (group != null) {
                btnGroup.text = group.name
                btnGroup.setTextColor(activity.getColor(R.color.colorPrimary))
                btnGroup.backgroundTintList =
                    ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))

                btnTarget.isClickable = true
                btnTarget.setTextColor(activity.getColor(R.color.colorTextPrimary))
            }
        }

        viewModel.eventTarget.observe(viewLifecycleOwner) { eventTarget ->
            mEventTarget = eventTarget
            activity.runOnUiThread {
                if (eventTarget == null) {
                    btnEvent.text = getString(R.string.turn_space_device_on_activate_scene)
                    btnDevice.isClickable = false
                    btnScene.isClickable = false
                    btnTarget.text = getString(R.string.scene_space_or_device)
                }

                eventTarget?.let {
                    btnTarget.setTextColor(activity.getColor(R.color.colorPrimary))
                    btnTarget.backgroundTintList =
                        ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                    when (eventTarget) {
                        EventTarget.SPACE -> {
                            viewModel.device.postValue(null)
                            viewModel.scene.postValue(null)

                            btnTarget.text = getString(R.string.this_space)
                            layoutScene.visibility = View.GONE
                            layoutDevice.visibility = View.GONE

                            btnEvent.setTextColor(activity.getColor(R.color.colorPrimary))
                            btnEvent.backgroundTintList =
                                ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                            btnEvent.text = getString(R.string.turn_space_on)

                            btnTime.isClickable = true
                            btnTime.setTextColor(activity.getColor(R.color.colorPrimary))
                            btnTime.backgroundTintList =
                                ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))

                            btnTimeout.isClickable = true
                            btnTimeout.backgroundTintList =
                                ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                            btnTimeout.setTextColor(activity.getColor(R.color.colorPrimary))

                            btnDays.isClickable = true
                            btnDays.setTextColor(activity.getColor(R.color.colorPrimary))
                            btnDays.backgroundTintList =
                                ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))

                            btnDaylight.isClickable = true
                            btnDaylight.setTextColor(activity.getColor(R.color.colorPrimary))
                            btnDaylight.backgroundTintList =
                                ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                        }
                        EventTarget.DEVICE -> {
                            viewModel.scene.postValue(null)

                            btnTarget.text = getString(R.string.device_in_space)

                            layoutDevice.visibility = View.VISIBLE

                            btnDevice.isClickable = true
                            btnDevice.backgroundTintList =
                                ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                            btnDevice.setTextColor(activity.getColor(R.color.colorTextPrimary))

                            btnEvent.text = getString(R.string.turn_device_on)

                            if (viewModel.device.value == null) {
                                btnDevice.text = getString(R.string.select_device)

                                btnEvent.isClickable = false
                                btnEvent.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                btnEvent.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))

                                btnTime.isClickable = false
                                btnTime.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                btnTime.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))

                                btnTimeout.isClickable = false
                                btnTimeout.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                                btnTimeout.setTextColor(activity.getColor(R.color.colorPrimaryBackground))

                                btnDays.isClickable = false
                                btnDays.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                btnDays.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))

                                btnDaylight.isClickable = false
                                btnDaylight.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                btnDaylight.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                            }
                        }
                        EventTarget.SCENE -> {
                            viewModel.device.postValue(null)
                            btnTarget.text = getString(R.string.scene)

                            layoutScene.visibility = View.VISIBLE

                            btnScene.isClickable = true
                            btnScene.backgroundTintList =
                                ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                            btnScene.setTextColor(activity.getColor(R.color.colorTextPrimary))

                            btnEvent.text = getString(R.string.activate_scene)

                            if (viewModel.scene.value == null) {
                                btnScene.text = getString(R.string.select_scene)

                                btnEvent.isClickable = false
                                btnEvent.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                                btnEvent.setTextColor(activity.getColor(R.color.colorPrimaryBackground))

                                btnTime.isClickable = false
                                btnTime.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                btnTime.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))

                                btnTimeout.isClickable = false
                                btnTimeout.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                                btnTimeout.setTextColor(activity.getColor(R.color.colorPrimaryBackground))

                                btnDays.isClickable = false
                                btnDays.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                btnDays.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))

                                btnDaylight.isClickable = false
                                btnDaylight.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                btnDaylight.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                            }
                        }
                    }
                }
            }
        }

        viewModel.device.observe(viewLifecycleOwner, Observer { device ->
            if (viewModel.eventTarget.value != EventTarget.DEVICE) return@Observer
            mSelectedDevice = device

            device?.let {
                btnDevice.text = device.first.name
                viewModel.scene.postValue(null)

                btnDevice.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnDevice.setTextColor(activity.getColor(R.color.colorPrimary))

                btnEvent.isClickable = true
                btnEvent.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnEvent.setTextColor(activity.getColor(R.color.colorPrimary))

                btnTime.isClickable = true
                btnTime.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnTime.setTextColor(activity.getColor(R.color.colorPrimary))

                btnTimeout.isClickable = true
                btnTimeout.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnTimeout.setTextColor(activity.getColor(R.color.colorPrimary))

                btnDays.isClickable = true
                btnDays.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnDays.setTextColor(activity.getColor(R.color.colorPrimary))

                btnDaylight.isClickable = true
                btnDaylight.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnDaylight.setTextColor(activity.getColor(R.color.colorPrimary))
            }
        })

        viewModel.scene.observe(viewLifecycleOwner, Observer { scene ->
            if (viewModel.eventTarget.value != EventTarget.SCENE) return@Observer
            mScene = scene

            scene?.let {
                btnScene.text = scene.name
                viewModel.device.postValue(null)

                btnScene.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnScene.setTextColor(activity.getColor(R.color.colorPrimary))

                btnEvent.isClickable = true
                btnEvent.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnEvent.setTextColor(activity.getColor(R.color.colorPrimary))

                btnTime.isClickable = true
                btnTime.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnTime.setTextColor(activity.getColor(R.color.colorPrimary))

                btnTimeout.isClickable = true
                btnTimeout.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnTimeout.setTextColor(activity.getColor(R.color.colorPrimary))

                btnDays.isClickable = true
                btnDays.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnDays.setTextColor(activity.getColor(R.color.colorPrimary))

                btnDaylight.isClickable = true
                btnDaylight.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                btnDaylight.setTextColor(activity.getColor(R.color.colorPrimary))
            }
        })

        viewModel.isAllDay.observe(viewLifecycleOwner) { isAllDay ->
            mIsAllDay = isAllDay
            if (viewModel.startTime.value == null && viewModel.endTime.value == null) {
                btnTime.text = getString(R.string.time_condition_placeholder)
                return@observe
            }

            isAllDay?.let {
                if (isAllDay) {
                    btnTime.text = getString(R.string.all_day)
                } else {
                    val startTime = mStartTime ?: TriggerTime(
                        hour = 0,
                        minute = 0,
                        trigger = SunriseSunsetType.TIME,
                        offset = 0
                    )
                    val endTime = mEndTime ?: TriggerTime(
                        hour = 0,
                        minute = 0,
                        trigger = SunriseSunsetType.TIME,
                        offset = 0
                    )
                    val startString = when (startTime.trigger) {
                        SunriseSunsetType.SUNRISE,
                        SunriseSunsetType.SUNSET -> {
                            when {
                                startTime.offset > 0 -> "${startTime.trigger.displayName} + ${startTime.offset}"
                                startTime.offset < 0 -> "${startTime.trigger.displayName} - ${
                                    abs(
                                        startTime.offset
                                    )
                                }"
                                else -> startTime.trigger.displayName
                            }
                        }
                        SunriseSunsetType.TIME -> getString(
                            R.string.event_time,
                            startTime.hour,
                            startTime.minute
                        )
                    }
                    val endString = when (endTime.trigger) {
                        SunriseSunsetType.SUNRISE,
                        SunriseSunsetType.SUNSET -> {
                            when {
                                endTime.offset > 0 -> "${endTime.trigger.displayName} + ${endTime.offset}"
                                endTime.offset < 0 -> "${endTime.trigger.displayName} - ${
                                    abs(
                                        endTime.offset
                                    )
                                }"
                                else -> endTime.trigger.displayName
                            }
                        }
                        SunriseSunsetType.TIME -> getString(
                            R.string.event_time,
                            endTime.hour,
                            endTime.minute
                        )
                    }
                    btnTime.text = String.format(
                        getString(R.string.time_condition),
                        startString,
                        endString
                    )
                }
            }
        }

        viewModel.startTime.observe(viewLifecycleOwner) { startTime ->
            startTime ?: return@observe
            mStartTime = startTime

            if (mIsAllDay == false) {
                val endTime = mEndTime ?: TriggerTime(
                    hour = 0,
                    minute = 0,
                    trigger = SunriseSunsetType.TIME,
                    offset = 0
                )
                val startString = when (startTime.trigger) {
                    SunriseSunsetType.SUNRISE,
                    SunriseSunsetType.SUNSET -> {
                        when {
                            startTime.offset > 0 -> "${startTime.trigger.displayName} + ${startTime.offset}"
                            startTime.offset < 0 -> "${startTime.trigger.displayName} - ${
                                abs(
                                    startTime.offset
                                )
                            }"
                            else -> startTime.trigger.displayName
                        }
                    }
                    SunriseSunsetType.TIME -> getString(
                        R.string.event_time,
                        startTime.hour,
                        startTime.minute
                    )
                }
                val endString = when (endTime.trigger) {
                    SunriseSunsetType.SUNRISE,
                    SunriseSunsetType.SUNSET -> {
                        when {
                            endTime.offset > 0 -> "${endTime.trigger.displayName} + ${endTime.offset}"
                            endTime.offset < 0 -> "${endTime.trigger.displayName} - ${abs(endTime.offset)}"
                            else -> endTime.trigger.displayName
                        }
                    }
                    SunriseSunsetType.TIME -> getString(
                        R.string.event_time,
                        endTime.hour,
                        endTime.minute
                    )
                }
                btnTime.text = String.format(
                    getString(R.string.time_condition),
                    startString,
                    endString
                )
            }
        }

        viewModel.endTime.observe(viewLifecycleOwner) { endTime ->
            endTime ?: return@observe
            mEndTime = endTime

            if (mIsAllDay == false) {
                val startTime = mStartTime ?: TriggerTime(
                    hour = 0,
                    minute = 0,
                    trigger = SunriseSunsetType.TIME,
                    offset = 0
                )
                val startString = when (startTime.trigger) {
                    SunriseSunsetType.SUNRISE,
                    SunriseSunsetType.SUNSET -> {
                        when {
                            startTime.offset > 0 -> "${startTime.trigger.displayName} + ${startTime.offset}"
                            startTime.offset < 0 -> "${startTime.trigger.displayName} - ${
                                abs(
                                    startTime.offset
                                )
                            }"
                            else -> startTime.trigger.displayName
                        }
                    }
                    SunriseSunsetType.TIME -> getString(
                        R.string.event_time,
                        startTime.hour,
                        startTime.minute
                    )
                }
                val endString = when (endTime.trigger) {
                    SunriseSunsetType.SUNRISE,
                    SunriseSunsetType.SUNSET -> {
                        when {
                            endTime.offset > 0 -> "${endTime.trigger.displayName} + ${endTime.offset}"
                            endTime.offset < 0 -> "${endTime.trigger.displayName} - ${abs(endTime.offset)}"
                            else -> endTime.trigger.displayName
                        }
                    }
                    SunriseSunsetType.TIME -> getString(
                        R.string.event_time,
                        endTime.hour,
                        endTime.minute
                    )
                }
                btnTime.text = String.format(
                    getString(R.string.time_condition),
                    startString,
                    endString
                )
            }
        }

        viewModel.eventDay.observe(viewLifecycleOwner) {
            val day = it ?: return@observe
            mDay = day
            btnDays.text = day.displayName
        }

        viewModel.targetLux.observe(viewLifecycleOwner) { lux ->
            mLux = lux

            btnDaylight.text = if (lux != null) {
                "$lux"
            } else {
                getString(R.string.always)
            }
        }

        viewModel.timeout.observe(viewLifecycleOwner, Observer { value ->
            mTimeout = value
            if (value == null) {
                btnTimeout.text = String.format(getString(R.string.minutes_seconds), 4, 0)
                return@Observer
            }
            val minutes = value / 60
            val seconds = value % 60

            if (viewModel.initialTimeout == null) {
                viewModel.initialTimeout = value
            }

            btnTimeout.text = String.format(getString(R.string.minutes_seconds), minutes, seconds)
        })

        btnGroup.setOnClickListener {
            try {
                val action = when (this) {
                    is EditMotionSensorEventFragment -> EditMotionSensorEventFragmentDirections.actionGlobalMotionSensorGroupSelectorFragment()
                    else -> MotionSensorEventFragmentDirections.actionMotionSensorEventFragmentToMotionSensorGroupSelectorFragment()
                }
                findNavController().navigate(action)
            } catch (ex: IllegalArgumentException) {
                findNavController().navigate(R.id.action_global_motionSensorGroupSelectorFragment)
            }
        }

        btnTarget.setOnClickListener(
            if (this !is DoorSensorEventFragment) {
                if (this !is EditMotionSensorEventFragment) {
                    Navigation.createNavigateOnClickListener(
                        MotionSensorEventFragmentDirections
                            .actionGlobalEventTargetSelectorFragment(MotionSensorEventFragment::class.simpleName)
                    )
                } else {
                    Navigation.createNavigateOnClickListener(
                        EditMotionSensorEventFragmentDirections
                            .actionGlobalEventTargetSelectorFragment(MotionSensorEventFragment::class.simpleName)
                    )
                }
            } else {
                Navigation.createNavigateOnClickListener(
                    DoorSensorEventFragmentDirections
                        .actionGlobalEventTargetSelectorFragment(MotionSensorEventFragment::class.simpleName)
                )
            }
        )
        btnTarget.isClickable = false

        btnDevice.setOnClickListener {
            val action =
                if (this !is DoorSensorEventFragment) {
                    if (this !is EditMotionSensorEventFragment) {
                        MotionSensorEventFragmentDirections
                            .actionGlobalEventDeviceSelectorFragment(
                                MotionSensorEventFragment::class.simpleName!!
                            )
                    } else {
                        EditMotionSensorEventFragmentDirections
                            .actionGlobalEventDeviceSelectorFragment(
                                MotionSensorEventFragment::class.simpleName!!
                            )
                    }
                } else {
                    DoorSensorEventFragmentDirections
                        .actionGlobalEventDeviceSelectorFragment(
                            DoorSensorEventFragment::class.simpleName!!
                        )
                }
            findNavController().navigate(action)
        }
        btnDevice.isClickable = false

        btnScene.setOnClickListener {
            val sender = MotionSensorEventFragment::class.simpleName ?: return@setOnClickListener
            val action =
                if (this !is EditMotionSensorEventFragment) {
                    MotionSensorEventFragmentDirections
                        .actionGlobalEventSceneSelectorFragment(sender)
                } else {
                    EditMotionSensorEventFragmentDirections
                        .actionGlobalEventSceneSelectorFragment(sender)
                }
            findNavController().navigate(action)
        }
        btnScene.isClickable = false

        btnTime.setOnClickListener {
            try {
                val action = when (this) {
                    is EditMotionSensorEventFragment -> EditMotionSensorEventFragmentDirections.actionGlobalTimeConditionFragment(MotionSensorEventFragment::class.simpleName ?: "")
                    else -> MotionSensorEventFragmentDirections.actionMotionSensorEventFragmentToTimeConditionFragment(MotionSensorEventFragment::class.simpleName ?: "")
                }
                findNavController().navigate(action)
            } catch (ex: IllegalArgumentException) {
                findNavController().navigate(R.id.action_global_timeConditionFragment)
            }
        }
        btnTime.isClickable = false

        btnDays.setOnClickListener(
            if (this !is DoorSensorEventFragment) {
                if (this !is EditMotionSensorEventFragment) {
                    Navigation.createNavigateOnClickListener(
                        MotionSensorEventFragmentDirections
                            .actionGlobalEventDaySelectorFragment(MotionSensorEventFragment::class.simpleName)
                    )
                } else {
                    Navigation.createNavigateOnClickListener(
                        EditMotionSensorEventFragmentDirections
                            .actionGlobalEventDaySelectorFragment(MotionSensorEventFragment::class.simpleName)
                    )
                }
            } else {
                Navigation.createNavigateOnClickListener(
                    DoorSensorEventFragmentDirections
                        .actionGlobalEventDaySelectorFragment(MotionSensorEventFragment::class.simpleName)
                )
            }
        )
        btnDays.isClickable = false

        btnDaylight.setOnClickListener(
            if (this !is EditMotionSensorEventFragment) {
                Navigation.createNavigateOnClickListener(
                    MotionSensorEventFragmentDirections
                        .actionMotionSensorEventFragmentToDaylightSensitivityFragment()
                )
            } else {
                Navigation.createNavigateOnClickListener(
                    EditMotionSensorEventFragmentDirections
                        .actionEditMotionSensorEventFragmentToDaylightSensitivityFragment()
                )
            }
        )
        btnDaylight.isClickable = false

        btnTimeout.setOnClickListener {
            val sender = MotionSensorEventFragment::class.simpleName ?: return@setOnClickListener
            val action =
                if (this !is EditMotionSensorEventFragment) {
                    MotionSensorEventFragmentDirections
                        .actionGlobalTimeoutFragment(sender)
                } else {
                    EditMotionSensorEventFragmentDirections
                        .actionGlobalTimeoutFragment(sender)
                }
            findNavController().navigate(action)
        }
        btnTimeout.isClickable = false

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            it.isEnabled = false
            activity.layoutGreyOut?.visibility = View.VISIBLE
            NabtoHandler.selectedGateway?.let { gateway ->
                createLogicCollection(gateway)
            }
        }
    }

    protected open fun createLogicCollection(gateway: NabtoHandler.NabtoGateway) {
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

            val newLogicCollection = LogicCollection(gateway.serial, -1, allDeviceViewModel.selectedDevice?.name ?: "", metadata, true).toJSONObject()
            newLogicCollection.remove("id")

            val response = try {
                DevelcoHandler.postLogicCollection(
                    gateway,
                    newLogicCollection
                )
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
                JSONObject()
            }

            viewModel.viewModelScope.launch(Dispatchers.Main) mainLaunch@{
                val logicCollectionId =
                    response.optJSONObject("body")?.optInt("id")
                        ?: return@mainLaunch
                val logicCollectionResponse = viewModel.getLogicCollections(gateway)//, logicCollectionId)
                logicCollectionResponse.observe(viewLifecycleOwner) { logicCollections ->
                    val logicCollection = logicCollections?.toList()
                        ?.find { it.parentGateway == gateway.serial && it.id == logicCollectionId }
                    logicCollection?.let {
                        logicCollectionResponse.removeObservers(viewLifecycleOwner)

                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            val offLogicTimerId =
                                createLogicTimer(gateway, logicCollection, isOverride = false)
                            val overrideLogicTimerId =
                                createLogicTimer(gateway, logicCollection, isOverride = true)
                            var offLogicTimer: LogicTimer? = null
                            var overrideLogicTimer: LogicTimer? = null

                            viewModel.viewModelScope.launch(Dispatchers.Main) mainLaunch@{

                                val offLogicTimerResponse = viewModel.getLogicTimers(gateway)
                                offLogicTimerResponse.observe(
                                    viewLifecycleOwner
                                ) offObserve@{ logicTimers ->
                                    val logicTimer = logicTimers.toList()
                                        .find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollection.id && it.id == offLogicTimerId }
                                    offLogicTimer = logicTimer
                                    val offTimer = offLogicTimer ?: return@offObserve
                                    offLogicTimerResponse.removeObservers(viewLifecycleOwner)

                                    val overrideTimer = overrideLogicTimer ?: return@offObserve
                                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                                        createLogicRules(
                                            gateway,
                                            logicCollection,
                                            offTimer,
                                            overrideTimer
                                        )

                                        activity?.runOnUiThread {
                                            btnSave.isEnabled = true
                                            activity?.layoutGreyOut?.visibility = View.GONE
                                            findNavController().popBackStack()
                                        }
                                    }
                                }

                                val overrideLogicTimerResponse = viewModel.getLogicTimers(gateway)
                                overrideLogicTimerResponse.observe(
                                    viewLifecycleOwner
                                ) offObserve@{ logicTimers ->
                                    val logicTimer = logicTimers.toList()
                                        .find { it.parentGateway == gateway.serial && it.logicCollectionId == logicCollection.id && it.id == overrideLogicTimerId }
                                    overrideLogicTimer = logicTimer
                                    val overrideTimer = overrideLogicTimer ?: return@offObserve
                                    overrideLogicTimerResponse.removeObservers(
                                        viewLifecycleOwner
                                    )

                                    val offTimer = offLogicTimer ?: return@offObserve
                                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                                        createLogicRules(
                                            gateway,
                                            logicCollection,
                                            offTimer,
                                            overrideTimer
                                        )

                                        activity?.runOnUiThread {
                                            btnSave.isEnabled = true
                                            activity?.layoutGreyOut?.visibility = View.GONE
                                            findNavController().popBackStack()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected open suspend fun createLogicTimer(
        gateway: NabtoHandler.NabtoGateway,
        logicCollection: LogicCollection,
        isOverride: Boolean
    ): Int {
        val timeout = mTimeout ?: 4 * 60

        val path =
            when (mEventTarget) {
                EventTarget.SCENE,
                EventTarget.SPACE -> {
                    mGroup?.let {
                        DevelcoHandler
                            .Endpoints
                            .GROUP_DATAPOINT
                            .url
                            .replace(
                                Pair("{ID}", it.id),
                                Pair("{LDEV}", "generic"),
                                Pair("{DATAPOINT}", "/onoff")
                            )
                    } ?: ""
                }
                EventTarget.DEVICE -> {
                    mSelectedDevice?.let {
                        DevelcoHandler
                            .Endpoints
                            .DEVICE_DATAPOINT
                            .url
                            .replace(
                                Pair("{ID}", it.first.id),
                                Pair("{LDEV}", it.second),
                                Pair("{DATAPOINT}", "/onoff")
                            )
                    } ?: ""
                }
                null -> {
                    ""
                }
            }

        val action = JSONObject()
            .put("type", "UpdateResource")
            .put("path", path)
            .put(
                "data", JSONObject()
                    .put("value", false)
            )
        val actions = JSONArray()
            .put(action)

        val timer = JSONObject()
        if (!isOverride) {
            timer.put("actions", actions)
        }
        timer.put("timeout", timeout)

        val response = try {
            DevelcoHandler.postLogicTimer(
                gateway,
                logicCollection.id,
                timer
            )
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
            JSONObject()
        }

        return response.optJSONObject("body")?.optInt("id") ?:  -1
    }

    protected suspend fun createLogicRules(
        gateway: NabtoHandler.NabtoGateway,
        logicCollection: LogicCollection,
        logicTimer: LogicTimer,
        overrideTimer: LogicTimer
    ) {
        createTimerRule(gateway, logicCollection, logicTimer, overrideTimer, true)
        createTimerRule(gateway, logicCollection, logicTimer, overrideTimer, false)
        createOnRule(gateway, logicCollection, overrideTimer)

        if (mIsAllDay == false && mStartTime != null && mEndTime != null) {
            createStopTimerRule(gateway, logicCollection, logicTimer, overrideTimer)

            SunriseSunsetHandler.addSunriseSunsetAction(sunriseSunsetActions.toTypedArray())
        }


    }

    protected open suspend fun createTimerRule(
        gateway: NabtoHandler.NabtoGateway,
        logicCollection: LogicCollection,
        logicTimer: LogicTimer,
        overrideTimer: LogicTimer,
        start: Boolean
    ) {
        val sensor = allDeviceViewModel.selectedDevice ?: return
        val timerPath = logicTimer(logicCollection.id, logicTimer.id)
        val overrideTimerPath = logicTimer(logicCollection.id, overrideTimer.id)
        val alarmPath = deviceDatapoint(sensor.id, "alarm", "/alarm")
        val startTime = mStartTime
        val endTime = mEndTime

        val metadata = RuleMetadata(
            action = if (start) RuleMetadataType.START_TIMER else RuleMetadataType.STOP_TIMER
        )
        val actions = arrayListOf(
            UpdateResourceAction(
                path = timerPath,
                LogicData(command = if (start) "start" else "stop")
            )
        )
        if (start) {
            actions.add(
                UpdateResourceAction(
                    path = overrideTimerPath,
                    LogicData(command = "start")
                )
            )
        }
        val triggers: Array<Trigger> = arrayOf(ResourceUpdateTrigger(alarmPath))
        val conditions = arrayListOf(
            ResourceValueCondition(path = "$alarmPath.value", rule = "##INVAL## == ${!start}"),
            DayOfWeekCondition(gson.fromJson(mDay.days.toString(), Array<DayOfWeek>::class.java))
        )
        if (mIsAllDay == false && startTime != null && endTime != null) {
            conditions.add(
                TimeIntervalCondition(
                    startTime.hour,
                    startTime.minute,
                    endTime.hour,
                    endTime.minute
                )
            )
        }

        val newLogicRule = NewLogicRule("Timer rule", metadata, triggers = triggers, conditions = conditions.toTypedArray(), actions = actions.toTypedArray()).toJSONObject()
        try {
            val ruleId = DevelcoHandler.postLogicRule(
                gateway,
                logicCollection.id,
                newLogicRule
            ).optJSONObject("body")?.optInt("id") ?: return

            if (startTime != null && endTime != null) {
                if (startTime.trigger == SunriseSunsetType.SUNRISE ||
                    startTime.trigger == SunriseSunsetType.SUNSET ||
                    endTime.trigger == SunriseSunsetType.SUNRISE ||
                    endTime.trigger == SunriseSunsetType.SUNSET
                ) {
                    val sunriseSunsetConditions =
                        ArrayList(conditions.filter { it !is TimeIntervalCondition })

                    val sunriseSunsetCondition = sunriseSunsetCondition(startTime, endTime)
                    sunriseSunsetConditions.add(sunriseSunsetCondition)

                    val sunriseSunsetAction = UpdateResourceAction(
                        path = logicRule(logicCollection.id, ruleId),
                        LogicData(conditions = sunriseSunsetConditions.toTypedArray())
                    )
                    val offsets: Array<SunriseSunsetOffsetWrapper> = arrayOf(startTime, endTime).mapNotNull {
                        if (it.offset != 0) {
                            SunriseSunsetOffsetWrapper(offset = it.offset, sunriseSunset = it.trigger)
                        } else {
                            null
                        }
                    }.toTypedArray()
                    sunriseSunsetActions.add(SunriseSunsetActionWrapper(action = sunriseSunsetAction, offsets))
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

    protected open suspend fun createOnRule(
        gateway: NabtoHandler.NabtoGateway,
        logicCollection: LogicCollection,
        overrideTimer: LogicTimer
    ) {
        val sensor = allDeviceViewModel.selectedDevice ?: return
        val timerPath = logicTimer(logicCollection.id, overrideTimer.id)
        val alarmPath = deviceDatapoint(sensor.id, "alarm", "/alarm")
        val startTime = mStartTime
        val endTime = mEndTime

        val metadata = RuleMetadata(action = RuleMetadataType.ON)
        val actions: Array<Action> = when (mEventTarget) {
            EventTarget.SCENE -> {
                val scene = mScene ?: return

                arrayOf(
                    UpdateResourceAction(
                        path = groupScenes(groupId = scene.groupId),
                        LogicData(id = scene.id)
                    )
                )
            }
            EventTarget.SPACE -> {
                val group = mGroup ?: return

                arrayOf(
                    UpdateResourceAction(
                        path = groupDatapoint(groupId = group.id, datapoint = "/onoff"),
                        LogicData(value = true)
                    )
                )
            }
            EventTarget.DEVICE -> {
                val device = mSelectedDevice ?: return

                arrayOf(
                    UpdateResourceAction(
                        path = deviceDatapoint(
                            deviceId = device.first.id,
                            ldev = device.second,
                            datapoint = "/onoff"
                        ),
                        LogicData(value = true)
                    )
                )
            }
            null -> {
                emptyArray()
            }
        }
        val triggers: Array<Trigger> = arrayOf(ResourceUpdateTrigger(alarmPath))
        val conditions: ArrayList<Condition> = arrayListOf(
            ResourceValueCondition(
                path = "$alarmPath.value",
                rule = "##INVAL## == true"
            ),
            ResourceValueCondition(
                path = "$timerPath.status",
                rule = "str_is_equal('##INVAL##', 'idle')"
            )
        )

        if (mIsAllDay == false && startTime != null && endTime != null) {
            conditions.add(
                TimeIntervalCondition(
                    startTime.hour,
                    startTime.minute,
                    endTime.hour,
                    endTime.minute
                )
            )
        }

        if (mLux != null) {
            conditions.add(
                ResourceValueCondition(
                    path = deviceDatapoint(
                        deviceId = sensor.id,
                        ldev = "light",
                        datapoint = "/illuminance.value"
                    ),
                    rule = "##INVAL## < $mLux"
                )
            )
        }

        conditions.add(
            DayOfWeekCondition(gson.fromJson(mDay.days.toString(), Array<DayOfWeek>::class.java))
        )

        val newLogicRule = NewLogicRule("On rule", metadata, triggers = triggers, conditions = conditions.toTypedArray(), actions = actions).toJSONObject()

        try {
            val ruleId = DevelcoHandler.postLogicRule(
                gateway,
                logicCollection.id,
                newLogicRule
            ).optJSONObject("body")?.optInt("id") ?: return

            if (startTime != null && endTime != null) {
                if (startTime.trigger == SunriseSunsetType.SUNRISE ||
                    startTime.trigger == SunriseSunsetType.SUNSET ||
                    endTime.trigger == SunriseSunsetType.SUNRISE ||
                    endTime.trigger == SunriseSunsetType.SUNSET
                ) {
                    val sunriseSunsetConditions =
                        ArrayList(conditions.filter { it !is TimeIntervalCondition })

                    val sunriseSunsetCondition = sunriseSunsetCondition(startTime, endTime)
                    sunriseSunsetConditions.add(sunriseSunsetCondition)

                    val sunriseSunsetAction = UpdateResourceAction(
                        path = logicRule(logicCollection.id, ruleId),
                        LogicData(conditions = sunriseSunsetConditions.toTypedArray())
                    )
                    val offsets: Array<SunriseSunsetOffsetWrapper> = arrayOf(startTime, endTime).mapNotNull {
                        if (it.offset != 0) {
                            SunriseSunsetOffsetWrapper(offset = it.offset, sunriseSunset = it.trigger)
                        } else {
                            null
                        }
                    }.toTypedArray()
                    sunriseSunsetActions.add(SunriseSunsetActionWrapper(action = sunriseSunsetAction, offsets))
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

    protected open suspend fun createStopTimerRule(
        gateway: NabtoHandler.NabtoGateway,
        logicCollection: LogicCollection,
        logicTimer: LogicTimer,
        overrideTimer: LogicTimer
    ) {
        val timerPath = logicTimer(logicCollection.id, logicTimer.id)
        val overrideTimerPath = logicTimer(logicCollection.id, overrideTimer.id)
        val endTime = mEndTime ?: return
        val metadata = RuleMetadata(action = RuleMetadataType.STOP_TIMER)
        val actions: Array<Action> = arrayOf(
            UpdateResourceAction(path = timerPath, LogicData(command = "stop")),
            UpdateResourceAction(path = overrideTimerPath, LogicData(command = "stop"))
        )
        val triggers: Array<Trigger> = arrayOf(TimeOfDayTrigger(hour = endTime.hour, min = endTime.minute))

        val newLogicRule = NewLogicRule("Stop rule", metadata, triggers = triggers, actions = actions).toJSONObject()
        try {
            val ruleId = DevelcoHandler.postLogicRule(
                gateway,
                logicCollection.id,
                newLogicRule
            ).optJSONObject("body")?.optInt("id") ?: return

            if (endTime.trigger == SunriseSunsetType.SUNRISE || endTime.trigger == SunriseSunsetType.SUNSET) {
                val endHour: String
                val endMinute: String
                when (endTime.trigger) {
                    SunriseSunsetType.SUNRISE -> {
                        when {
                            endTime.offset > 0 -> {
                                endHour = getString(R.string.sunrisePositiveOffsetHours, endTime.offset)
                                endMinute = getString(R.string.sunrisePositiveOffsetMinutes, endTime.offset)
                            }
                            endTime.offset < 0 -> {
                                endHour = getString(R.string.sunriseNegativeOffsetHours, abs(endTime.offset))
                                endMinute = getString(R.string.sunriseNegativeOffsetMinutes, abs(endTime.offset))
                            }
                            else -> {
                                endHour = getString(R.string.sunriseHour)
                                endMinute = getString(R.string.sunriseHour)
                            }
                        }
                    }
                    else -> {
                        when {
                            endTime.offset > 0 -> {
                                endHour = getString(R.string.sunsetPositiveOffsetHours, endTime.offset)
                                endMinute = getString(R.string.sunsetPositiveOffsetMinutes, endTime.offset)
                            }
                            endTime.offset < 0 -> {
                                endHour = getString(R.string.sunsetNegativeOffsetHours, abs(endTime.offset))
                                endMinute = getString(R.string.sunsetNegativeOffsetMinutes, abs(endTime.offset))
                            }
                            else -> {
                                endHour = getString(R.string.sunsetHour)
                                endMinute = getString(R.string.sunsetMinute)
                            }
                        }
                    }
                }
                val sunriseSunsetTriggers: Array<Trigger> = arrayOf(
                    TimeOfDayTrigger(
                        hour = endHour,
                        min = endMinute
                    )
                )

                sunriseSunsetActions.add(
                    SunriseSunsetActionWrapper(
                        UpdateResourceAction(
                            path = logicRule(logicCollection.id, ruleId),
                            LogicData(triggers = sunriseSunsetTriggers)
                        ),
                        arrayOf(endTime).mapNotNull {
                            if (it.offset != 0) {
                                SunriseSunsetOffsetWrapper(offset = it.offset, sunriseSunset = it.trigger)
                            } else {
                                null
                            }
                        }.toTypedArray()
                    )
                )
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

    private fun sunriseSunsetCondition(
        startTime: TriggerTime,
        endTime: TriggerTime
    ): TimeIntervalCondition {
        val startHour: Any
        val startMinute: Any
        val endHour: Any
        val endMinute: Any
        if (startTime.trigger == SunriseSunsetType.TIME) {
            startHour = startTime.hour
            startMinute = startTime.minute
        } else {
            when (startTime.trigger) {
                SunriseSunsetType.SUNRISE -> {
                    when {
                        startTime.offset > 0 -> {
                            startHour =
                                getString(R.string.sunrisePositiveOffsetHours, startTime.offset)
                            startMinute =
                                getString(R.string.sunrisePositiveOffsetMinutes, startTime.offset)
                        }
                        startTime.offset < 0 -> {
                            startHour = getString(
                                R.string.sunriseNegativeOffsetHours,
                                abs(startTime.offset)
                            )
                            startMinute = getString(
                                R.string.sunriseNegativeOffsetMinutes,
                                abs(startTime.offset)
                            )
                        }
                        else -> {
                            startHour = getString(R.string.sunriseHour)
                            startMinute = getString(R.string.sunriseHour)
                        }
                    }
                }
                else -> {
                    when {
                        startTime.offset > 0 -> {
                            startHour =
                                getString(R.string.sunsetPositiveOffsetHours, startTime.offset)
                            startMinute =
                                getString(R.string.sunsetPositiveOffsetMinutes, startTime.offset)
                        }
                        startTime.offset < 0 -> {
                            startHour =
                                getString(R.string.sunsetNegativeOffsetHours, abs(startTime.offset))
                            startMinute = getString(
                                R.string.sunsetNegativeOffsetMinutes,
                                abs(startTime.offset)
                            )
                        }
                        else -> {
                            startHour = getString(R.string.sunsetHour)
                            startMinute = getString(R.string.sunsetMinute)
                        }
                    }
                }
            }
        }
        if (endTime.trigger == SunriseSunsetType.TIME) {
            endHour = endTime.hour
            endMinute = endTime.minute
        } else {
            when (endTime.trigger) {
                SunriseSunsetType.SUNRISE -> {
                    when {
                        endTime.offset > 0 -> {
                            endHour = getString(R.string.sunrisePositiveOffsetHours, endTime.offset)
                            endMinute =
                                getString(R.string.sunrisePositiveOffsetMinutes, endTime.offset)
                        }
                        endTime.offset < 0 -> {
                            endHour =
                                getString(R.string.sunriseNegativeOffsetHours, abs(endTime.offset))
                            endMinute = getString(
                                R.string.sunriseNegativeOffsetMinutes,
                                abs(endTime.offset)
                            )
                        }
                        else -> {
                            endHour = getString(R.string.sunriseHour)
                            endMinute = getString(R.string.sunriseHour)
                        }
                    }
                }
                else -> {
                    when {
                        endTime.offset > 0 -> {
                            endHour = getString(R.string.sunsetPositiveOffsetHours, endTime.offset)
                            endMinute =
                                getString(R.string.sunsetPositiveOffsetMinutes, endTime.offset)
                        }
                        endTime.offset < 0 -> {
                            endHour =
                                getString(R.string.sunsetNegativeOffsetHours, abs(endTime.offset))
                            endMinute =
                                getString(R.string.sunsetNegativeOffsetMinutes, abs(endTime.offset))
                        }
                        else -> {
                            endHour = getString(R.string.sunsetHour)
                            endMinute = getString(R.string.sunsetMinute)
                        }
                    }
                }
            }
        }

        return TimeIntervalCondition(
            startHour,
            startMinute,
            endHour,
            endMinute
        )
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.clearViewModel()
    }

    companion object {
        private const val TAG = "MotionSensorEventFragm…"
        fun newInstance() =
            MotionSensorEventFragment()
    }
}
