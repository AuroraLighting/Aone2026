package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentDoorSensorEventBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.UnknownApiException
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.toCapitalisedLowerCase
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.fragments.alldevices.AllDevicesViewModel
import com.aurora.aonev3.ui.fragments.schedules.EventAction
import com.aurora.aonev3.ui.fragments.schedules.EventTarget
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

open class DoorSensorEventFragment : Fragment() {

    private var _binding: FragmentDoorSensorEventBinding? = null
    private val binding get() = _binding!!


    private val viewModel: DoorSensorEventViewModel by activityViewModels()
    private var mGroup: Group? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()

        val triggerDevice = ViewModelProvider(activity).get(AllDevicesViewModel::class.java).selectedDevice ?: return

        viewModel.setTriggerDevice(triggerDevice)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentDoorSensorEventBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sender = DoorSensorEventFragment::class.simpleName ?: return

        viewModel.targetGroup.observe(viewLifecycleOwner) { group ->
            if (group != null) {
                setButtonActive(btnGroup, group.name)
                viewModel.eventTargbinding.et.value = viewModel.eventTargbinding.et.value
            } else {
                setButtonClickable(btnGroup, getString(R.string.select_a_target_space))
                viewModel.eventTargbinding.et.value = null
            }

            if (mGroup != null && mGroup != group) {
                viewModel.device.value = null
                viewModel.scene.value = null
            }

            mGroup = group
        }

        viewModel.eventTargbinding.et.observe(viewLifecycleOwner) { target ->
            if (target != null) {
                setButtonActive(btnTarget, targbinding.et.displayName)

                when (target) {
                    EventTargbinding.et.SCENE -> {
                        layoutSceneDeviceOuter.visibility = View.VISIBLE
                        layoutScene.visibility = View.VISIBLE
                        layoutDevice.visibility = View.GONE

                        viewModel.device.value = null
                        viewModel.scene.value = viewModel.scene.value
                    }
                    EventTargbinding.et.SPACE -> {
                        layoutSceneDeviceOuter.visibility = View.GONE
                        layoutScene.visibility = View.GONE
                        layoutDevice.visibility = View.GONE

                        viewModel.device.value = null
                        viewModel.scene.value = null

                        viewModel.trigger.value = viewModel.trigger.value
                    }
                    EventTargbinding.et.DEVICE -> {
                        layoutSceneDeviceOuter.visibility = View.VISIBLE
                        layoutScene.visibility = View.GONE
                        layoutDevice.visibility = View.VISIBLE

                        viewModel.device.value = viewModel.device.value
                        viewModel.scene.value = null

                        setButtonClickable(btnDevice, "")
                    }
                }
            } else {
                if (viewModel.targetGroup.value != null) {
                    setButtonClickable(btnTarget, getString(R.string.scene_space_or_device))
                } else {
                    setButtonNotClickable(btnTarget, getString(R.string.scene_space_or_device))
                }

                viewModel.device.value = null
                viewModel.scene.value = null
                viewModel.trigger.value = null
            }
        }

        viewModel.scene.observe(viewLifecycleOwner) { scene ->
            if (scene != null) {
                setButtonActive(btnScene, scene.name)

                viewModel.trigger.value = viewModel.trigger.value
            } else {
                if (viewModel.eventTargbinding.et.value == EventTargbinding.et.SCENE) {
                    setButtonClickable(btnScene, getString(R.string.scene))

                    viewModel.trigger.value = null
                } else {
                    setButtonNotClickable(btnScene, getString(R.string.scene))
                }
            }
        }

        viewModel.device.observe(viewLifecycleOwner) { device ->
            if (device != null) {
                setButtonActive(btnDevice, device.first.name)

                viewModel.trigger.value = viewModel.trigger.value
            } else {
                if (viewModel.eventTargbinding.et.value == EventTargbinding.et.DEVICE) {
                    setButtonClickable(btnDevice, getString(R.string.device))

                    viewModel.trigger.value = null
                } else {
                    setButtonNotClickable(btnDevice, getString(R.string.device))
                }
            }
        }

        viewModel.trigger.observe(viewLifecycleOwner) { trigger ->
            if (trigger != null) {
                setButtonActive(btnTrigger, trigger.name.toCapitalisedLowerCase())

                if (viewModel.eventTargbinding.et.value != EventTargbinding.et.SCENE) {
                    viewModel.eventAction.value = viewModel.eventAction.value
                } else {
                    viewModel.eventAction.value = EventAction.ON
                }
            } else {
                if (viewModel.eventTargbinding.et.value == EventTargbinding.et.SPACE ||
                    (viewModel.eventTargbinding.et.value == EventTargbinding.et.SCENE && viewModel.scene.value != null) ||
                    (viewModel.eventTargbinding.et.value == EventTargbinding.et.DEVICE && viewModel.device.value != null)) {
                    setButtonClickable(btnTrigger, getString(R.string.open_close))
                } else {
                    setButtonNotClickable(btnTrigger, getString(R.string.open_close))
                }

                viewModel.eventAction.value = null
            }
        }

        viewModel.eventAction.observe(viewLifecycleOwner) { action ->
            if (action != null) {
                if (viewModel.eventTargbinding.et.value != EventTargbinding.et.SCENE) {
                    setButtonActive(btnEvent, action.displayName)
                } else {
                    setButtonActive(btnEvent, getString(R.string.activate_scene))
                    btnEvent.isClickable = false
                }

                if (action == EventAction.ON) {
                    binding.timeoutOuterLayout.visibility = View.VISIBLE
                    viewModel.timeout.value = viewModel.timeout.value
                }

                viewModel.setIsAllDay(viewModel.isAllDay.value ?: false)
                val startTime = viewModel.startTime.value
                val endTime = viewModel.endTime.value

                startTime?.let {
                    viewModel.updateStartTime(startTime.hour, startTime.minute, startTime.trigger, startTime.offset)
                }
                endTime?.let {
                    viewModel.updateEndTime(endTime.hour, endTime.minute, endTime.trigger, endTime.offset)
                }
                viewModel.eventDay.value = viewModel.eventDay.value
            } else {
                if (viewModel.trigger.value != null) {
                    setButtonClickable(btnEvent, getString(R.string.on_off))
                } else {
                    setButtonNotClickable(btnEvent, getString(R.string.on_off))
                }

                viewModel.timeout.value = null
                viewModel.setIsAllDay(false)
                viewModel.isAllDay.value = null
                viewModel.updateStartTime(hour = 0, minute = 0, trigger = SunriseSunsetType.TIME, offset = 0)
                viewModel.updateEndTime(hour = 0, minute = 0, trigger = SunriseSunsetType.TIME, offset = 0)
                viewModel.eventDay.value = null
            }
        }

        viewModel.timeout.observe(viewLifecycleOwner) { timeout ->
            if (timeout != null) {
                setButtonActive(btnTimeout, getString(R.string.minutes_seconds, timeout / 60, timeout % 60))
            } else {
                if (viewModel.eventAction.value != null) {
                    setButtonActive(btnTimeout, getString(R.string.minutes_seconds, 0, 0))
                } else {
                    setButtonNotClickable(btnTimeout, getString(R.string.minutes_seconds, 0, 0))
                }
            }
        }

        viewModel.isAllDay.observe(viewLifecycleOwner) {
            handleTimeCondition()
        }

        viewModel.startTime.observe(viewLifecycleOwner) {
            handleTimeCondition()
        }

        viewModel.endTime.observe(viewLifecycleOwner) {
            handleTimeCondition()
        }

        viewModel.eventDay.observe(viewLifecycleOwner) { day ->
            if (day != null) {
                setButtonActive(btnDays, day.displayName)
            } else {
                if (viewModel.eventAction.value != null) {
                    setButtonActive(btnDays, getString(R.string.everyday))
                } else {
                    setButtonNotClickable(btnDays, getString(R.string.everyday))
                }
            }
        }

        btnGroup.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToEventGroupSelectorFragment(sender)
            findNavController().navigate(action)
        }

        btnTargbinding.et.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToEventTargetSelectorFragment(sender)
            findNavController().navigate(action)
        }

        btnScene.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToEventSceneSelectorFragment(sender)
            findNavController().navigate(action)
        }

        btnDevice.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToEventDeviceSelectorFragment(sender)
            findNavController().navigate(action)
        }

        btnTrigger.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToEventTriggerSelectorFragment(sender)
            findNavController().navigate(action)
        }

        btnEvent.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToEventActionSelectorFragment(false, sender)
            findNavController().navigate(action)
        }

        btnTimeout.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToTimeoutFragment(sender)
            findNavController().navigate(action)
        }

        btnTime.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToTimeConditionFragment(sender)
            findNavController().navigate(action)
        }

        btnDays.setOnClickListener {
            val action = DoorSensorEventFragmentDirections.actionDoorSensorEventFragmentToEventDaySelectorFragment(sender)
            findNavController().navigate(action)
        }

        btnTargbinding.et.isClickable = false
        btnScene.isClickable = false
        btnDevice.isClickable = false
        btnTrigger.isClickable = false
        btnEvent.isClickable = false
        btnTime.isClickable = false
        btnDays.isClickable = false

        btnSave.setOnClickListener {
            it.isEnabled = false

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    viewModel.createLogic()

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }

                    activity?.runOnUiThread {
                        findNavController().popBackStack()
                    }
                } catch(ex: UnknownApiException) {
                    Toast.makeText(activity, "Failed to create Event", Toast.LENGTH_SHORT).show()
                } catch (err: VolleyError) {
                    App.actionFailed()
                    val gateway = NabtoHandler.selectedGateway ?: return@launch

                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        val credentials = CloudHandler.getCredentials()
                        if (credentials.first.isEmpty()) {
                            activity?.finishAffinity()
                            startActivity(Intent(context, SplashscreenActivity::class.java))
                        }
                        NabtoHandler.openTunnel(gateway, credentials.first)
                    }
                    err.printStackTrace()
                }
            }
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun handleTimeCondition() {
        val isAllDay = viewModel.isAllDay.value
        val startTime = viewModel.startTime.value
        val endTime = viewModel.endTime.value

        if (isAllDay == true) {
            setButtonActive(btnTime, getString(R.string.all_day))

            return
        }

        if (startTime != null && endTime != null) {
            val startTimeString = when (startTime.trigger) {
                SunriseSunsetType.SUNRISE,
                SunriseSunsetType.SUNSET -> {
                    when {
                        startTime.offset > 0 -> {
                            "${startTime.trigger.displayName} + ${startTime.offset}"
                        }
                        startTime.offset < 0 -> {
                            "${startTime.trigger.displayName} - ${abs(startTime.offset)}"
                        }
                        else -> {
                            startTime.trigger.displayName
                        }
                    }
                }
                else -> getString(R.string.event_time, startTime.hour, startTime.minute)
            }
            val endTimeString = when (endTime.trigger) {
                SunriseSunsetType.SUNRISE,
                SunriseSunsetType.SUNSET -> {
                    when {
                        endTime.offset > 0 -> {
                            "${endTime.trigger.displayName} + ${endTime.offset}"
                        }
                        endTime.offset < 0 -> {
                            "${endTime.trigger.displayName} - ${abs(endTime.offset)}"
                        }
                        else -> {
                            endTime.trigger.displayName
                        }
                    }
                }
                else -> getString(R.string.event_time, endTime.hour, endTime.minute)
            }
            setButtonActive(btnTime, getString(R.string.time_condition, startTimeString, endTimeString))

            return
        }

        if (viewModel.eventAction.value != null) {
            setButtonActive(btnTime, getString(R.string.time_condition_placeholder))
        } else {
            setButtonNotClickable(btnTime, getString(R.string.time_condition_placeholder))
        }
    }

    override fun onDetach() {
        viewModel.clearViewModel()
        super.onDetach()
    }

    private fun setButtonActive(button: Button, text: String) {
        val activity = activity ?: return

        button.isClickable = true
        button.text = text
        button.setTextColor(activity.getColor(R.color.colorPrimary))
        button.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
    }

    private fun setButtonClickable(button: Button, text: String) {
        val activity = activity ?: return

        button.isClickable = true
        button.text = text
        button.setTextColor(activity.getColor(R.color.colorTextPrimary))
        button.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
    }

    private fun setButtonNotClickable(button: Button, text: String) {
        val activity = activity ?: return

        button.isClickable = false
        button.text = text
        button.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
        button.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
    }

    companion object {
        private const val TAG = "DoorSensorEventFragmen…"
        fun newInstance() =
            DoorSensorEventFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
