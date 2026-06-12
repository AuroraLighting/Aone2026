package com.aurora.aonev3.ui.fragments.schedules

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.databinding.FragmentScheduleEventBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.logic.DayOfWeekCondition
import com.aurora.aonev3.logic.EventMetadata
import com.aurora.aonev3.logic.TimeOfDayTrigger
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.logic.TriggerEnum
import com.aurora.aonev3.toCapitalisedLowerCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.math.abs

class ScheduleEventFragment : Fragment() {

    private var _binding: FragmentScheduleEventBinding? = null
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = ScheduleEventFragment()
    }

    private val viewModel: ScheduleEventViewModel by activityViewModels()
    private var mLogicRule: LogicRule? = null
    private var mEventTarget: EventTarget? = null
    private var mDevice: Pair<Device, String>? = null
    private var mScene: Scene? = null
    private var mEvent: EventAction? = null
    private var mDay: EventDay? = null

    private var edit = false

    private val args: ScheduleEventFragmentArgs by navArgs()
    private val mGroup: Group by lazy { args.group }
    private val mLogicCollection: LogicCollection by lazy { args.logicCollection }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.logicRule.postValue(args.logicRule)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentScheduleEventBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        val activity = activity ?: throw Exception("Invalid activity")

        viewModel.targetGroup.value = mGroup

        viewModel.logicRule.observe(viewLifecycleOwner, Observer { logicRule ->
            if (edit) return@Observer
            mLogicRule = logicRule

            if (logicRule == null) {
                binding.btnDelete.visibility = View.GONE
                return@Observer
            }

            val metadataEvent = logicRule.metadata.event ?: EventMetadata()
            val triggers = logicRule.triggers ?: emptyArray()
            val conditions = logicRule.conditions ?: emptyArray()

            when {
                metadataEvent.group != null -> viewModel.eventTarget.postValue(EventTarget.SPACE)
                metadataEvent.device != null -> {
                    val group = viewModel.targetGroup.value ?: return@Observer
                    val deviceMeta = metadataEvent.device ?: return@Observer
                    val device = SyncHandler
                        .devicesList
                        .find { device -> device.parentGateway == group.parentGateway && device.id == deviceMeta.id } ?: return@Observer
                    viewModel.eventTarget.postValue(EventTarget.DEVICE)
                    viewModel.device.postValue(Pair(device, deviceMeta.ldev ?: ""))
                }
                metadataEvent.scene != null -> {
                    val group = viewModel.targetGroup.value ?: return@Observer
                    val sceneMeta = metadataEvent.scene ?: return@Observer
                    val scene = SyncHandler
                        .scenesList
                        .find { scene -> scene.parentGateway == group.parentGateway && scene.id == sceneMeta.id && scene.groupId == group.id }
                        ?: return@Observer
                    viewModel.eventTarget.postValue(EventTarget.SCENE)
                    viewModel.scene.postValue(scene)
                }
            }

            try {
                viewModel.eventAction.postValue(
                    EventAction.valueOf(
                        metadataEvent.action?.name ?: "ON"
                    )
                )
            } catch (ex: IllegalArgumentException) {

            }

            val triggerType = when (metadataEvent.trigger) {
                TriggerEnum.SUNRISE -> SunriseSunsetType.SUNRISE
                TriggerEnum.SUNSET -> SunriseSunsetType.SUNSET
                else -> SunriseSunsetType.TIME
            }

            val offset = metadataEvent.triggerOffset ?: 0

            var hour = 0
            var minute = 0
            for (trigger in triggers) {
                if (trigger is TimeOfDayTrigger) {
                    val (h, m) = trigger
                    if (h is Number && m is Number) {
                        hour = h.toInt()
                        minute = m.toInt()
                    }
                }
            }

            viewModel.updateTrigger(hour, minute, triggerType, offset)

            for (condition in conditions) {
                if (condition is DayOfWeekCondition) {
                    val days = condition.days

                    val day = when {
                        days.count() == 7 -> EventDay.EVERYDAY
                        days.count() == 5 -> EventDay.WEEKDAYS
                        days.count() == 2 -> EventDay.WEEKEND
                        days.count() == 1 -> {
                            try {
                                EventDay.valueOf(days[0].name)
                            } catch (ex: IllegalArgumentException) {
                                null
                            }
                        }
                        else -> null
                    }

                    viewModel.eventDay.postValue(day)
                    break
                }
            }

            activity?.runOnUiThread {
                binding.btnDelete.visibility = View.VISIBLE
            }

            edit = true
        })

        viewModel.eventTarget.observe(viewLifecycleOwner) { eventTarget ->
            mEventTarget = eventTarget

            activity?.runOnUiThread {
                val activity = activity ?: return@runOnUiThread
                if (eventTarget != null) {
                    binding.cardTarget.backgroundTintList =
                        ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                    binding.tvSelectTarget.setTextColor(activity.getColor(R.color.colorPrimary))
                } else {
                    binding.cardTarget.backgroundTintList =
                        ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                    binding.tvSelectTarget.setTextColor(activity.getColor(R.color.colorTextPrimary))
                }
                if (eventTarget == null) {
                    binding.cardEvent.isClickable = false
                    binding.cardDevice.isClickable = false
                    binding.cardScene.isClickable = false
                    binding.tvSelectTarget.text = getString(R.string.scene_space_or_device)
                }

                eventTarget?.let {
                    when (eventTarget) {
                        EventTarget.SPACE -> {
                            viewModel.device.postValue(null)
                            viewModel.scene.postValue(null)

                            binding.tvSelectTarget.text = getString(R.string.this_space)
                            binding.layoutScene.visibility = View.GONE
                            binding.layoutDevice.visibility = View.GONE

                            binding.cardEvent.isClickable = true
                            if (mEvent != null) {
                                binding.cardEvent.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                                binding.tvEvent.setTextColor(activity.getColor(R.color.colorPrimary))
                            } else {
                                binding.cardEvent.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                                binding.tvEvent.setTextColor(activity.getColor(R.color.colorTextPrimary))
                            }
                        }
                        EventTarget.DEVICE -> {
                            viewModel.scene.postValue(null)

                            binding.tvSelectTarget.text = getString(R.string.device_in_space)

                            binding.layoutDevice.visibility = View.VISIBLE
                            binding.cardDevice.isClickable = true

                            if (viewModel.device.value == null) {
                                binding.cardDevice.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))

                                binding.tvDevice.setTextColor(activity.getColor(R.color.colorTextPrimary))

                                binding.tvDevice.text = getString(R.string.select_device)
                                binding.cardEvent.isClickable = false
                                binding.cardEvent.backgroundTintList =
                                    activity.resources.getColorStateList(
                                        R.color.colorTileTextActive,
                                        null
                                    )
                                binding.cardTime.isClickable = false
                                binding.cardTime.backgroundTintList =
                                    activity.resources.getColorStateList(
                                        R.color.colorTileTextActive,
                                        null
                                    )
                                binding.cardDays.isClickable = false
                                binding.cardDays.backgroundTintList =
                                    activity.resources.getColorStateList(
                                        R.color.colorTileTextActive,
                                        null
                                    )
                                binding.tvEvent.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                binding.tvTime.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                binding.tvDays.setTextColor(activity.getColor(R.color.colorPrimaryBackground))

//                                viewModel.eventAction.postValue(null)
                            } else {
                                binding.cardDevice.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))

                                binding.tvDevice.setTextColor(activity.getColor(R.color.colorPrimary))
                            }
                        }
                        EventTarget.SCENE -> {
                            viewModel.device.postValue(null)
                            binding.tvSelectTarget.text = getString(R.string.scene)

                            binding.layoutScene.visibility = View.VISIBLE

                            binding.cardScene.isClickable = true

                            viewModel.eventAction.postValue(EventAction.ON)
                            binding.cardEvent.isClickable = false
                            binding.cardEvent.backgroundTintList =
                                activity.resources.getColorStateList(
                                    R.color.colorTileTextActive,
                                    null
                                )

                            binding.tvEvent.setTextColor(activity.getColor(R.color.colorPrimaryBackground))

                            if (viewModel.scene.value == null) {
                                binding.cardScene.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))

                                binding.tvScene.setTextColor(activity.getColor(R.color.colorTextPrimary))

                                binding.tvScene.text = getString(R.string.select_scene)

                                binding.cardTime.isClickable = false
                                binding.cardTime.backgroundTintList =
                                    activity.resources.getColorStateList(
                                        R.color.colorTileTextActive,
                                        null
                                    )
                                binding.cardDays.isClickable = false
                                binding.cardDays.backgroundTintList =
                                    activity.resources.getColorStateList(
                                        R.color.colorTileTextActive,
                                        null
                                    )
                                binding.tvTime.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                                binding.tvDays.setTextColor(activity.getColor(R.color.colorPrimaryBackground))

//                                viewModel.eventAction.postValue(null)
                            } else {
                                binding.cardScene.backgroundTintList =
                                    ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))

                                binding.tvScene.setTextColor(activity.getColor(R.color.colorPrimary))
                            }
                        }
                    }
                }
            }
        }

        viewModel.device.observe(viewLifecycleOwner, Observer { device ->
            val activity = activity ?: return@Observer
            mDevice = device

            if (viewModel.eventTarget.value != EventTarget.DEVICE) return@Observer

            if (device != null) {
                binding.cardDevice.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                binding.tvDevice.setTextColor(activity.getColor(R.color.colorPrimary))
            } else {
                binding.cardDevice.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                binding.tvDevice.setTextColor(activity.getColor(R.color.colorTextPrimary))
            }

            device?.let {
                activity.runOnUiThread {
                    binding.tvDevice.text =
                        if (it.first.deviceClass != Device.DeviceClass.AURORADUALSOCKET) {
                            device.first.name
                        } else {
                            when (it.second) {
                                "socket1" -> {
                                    getString(R.string.double_socket_left_socket, device.first.name)
                                }
                                "socket2" -> {
                                    getString(
                                        R.string.double_socket_right_socket,
                                        device.first.name
                                    )
                                }
                                else -> {
                                    device.first.name
                                }
                            }
                        }

                    binding.cardEvent.isClickable = true
                    if (mEvent != null) {
                        binding.cardEvent.backgroundTintList =
                            ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                        binding.tvEvent.setTextColor(activity.getColor(R.color.colorPrimary))
                    } else {
                        binding.cardEvent.backgroundTintList =
                            ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                        binding.tvEvent.setTextColor(activity.getColor(R.color.colorTextPrimary))
                    }
                    when (mEvent) {
                        EventAction.ON -> getString(R.string.on)
                        EventAction.OFF -> getString(R.string.off)
                        EventAction.LOCK -> getString(R.string.lock)
                        EventAction.UNLOCK -> getString(R.string.unlock)
                        else -> {
                            if (it.second != "lock") {
                                binding.tvEvent.text = getString(R.string.on_off)
                            } else {
                                binding.tvEvent.text = getString(R.string.lock_unlock)
                            }
                        }
                        }

                    if (viewModel.eventAction.value != null) {
                        binding.cardTime.isClickable = true
                        binding.cardTime.backgroundTintList =
                            ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))

                        binding.cardDays.isClickable = true
                        binding.cardDays.backgroundTintList =
                            ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))

                        binding.tvTime.setTextColor(activity.getColor(R.color.colorPrimary))
                        binding.tvDays.setTextColor(activity.getColor(R.color.colorPrimary))
                    }
                }
            }
        })

        viewModel.scene.observe(viewLifecycleOwner, Observer { scene ->
            mScene = scene

            if (viewModel.eventTarget.value != EventTarget.SCENE) return@Observer

            scene?.let {
                activity?.runOnUiThread {
                    binding.tvScene.text = scene.name
                }

                viewModel.eventAction.postValue(EventAction.ON)
            }
        })

        viewModel.eventAction.observe(viewLifecycleOwner) { action ->
            val activity = activity ?: return@observe
            mEvent = action
            activity.runOnUiThread {
                if (mEvent != null) {
                    binding.cardEvent.backgroundTintList =
                        ColorStateList.valueOf(activity.getColor(R.color.colorTileActive))
                    binding.tvEvent.setTextColor(activity.getColor(R.color.colorPrimary))
                } else {
                    binding.cardEvent.backgroundTintList =
                        ColorStateList.valueOf(activity.getColor(R.color.colorTileInactive))
                    if (mEventTarget == EventTarget.SPACE || mDevice != null || mScene != null) {
                        binding.tvEvent.setTextColor(activity.getColor(R.color.colorTextPrimary))
                    } else {
                        binding.tvEvent.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                    }
                }

                when (action) {
                    EventAction.ON,
                    EventAction.OFF,
                    EventAction.LOCK,
                    EventAction.UNLOCK -> {
                        binding.tvEvent.text = when (action) {
                            EventAction.ON -> {
                                getString(R.string.on)
                            }
                            EventAction.OFF -> {
                                getString(R.string.off)
                            }
                            EventAction.LOCK -> {
                                getString(R.string.lock)
                            }
                            else -> {
                                getString(R.string.unlock)
                            }
                        }

                        if ((viewModel.eventTarget.value == EventTarget.SCENE && viewModel.scene.value != null)
                            || (viewModel.eventTarget.value == EventTarget.DEVICE && viewModel.device.value != null)
                            || viewModel.eventTarget.value == EventTarget.SPACE
                        ) {
                            binding.cardTime.isClickable = true
                            binding.cardTime.backgroundTintList =
                                activity.resources.getColorStateList(
                                    R.color.colorTileActive,
                                    null
                                )

                            binding.cardDays.isClickable = true
                            binding.cardDays.backgroundTintList =
                                activity.resources.getColorStateList(
                                    R.color.colorTileActive,
                                    null
                                )

                            binding.tvTime.setTextColor(activity.getColor(R.color.colorPrimary))
                            binding.tvDays.setTextColor(activity.getColor(R.color.colorPrimary))
                        }
                    }
                    null -> {
                        binding.tvEvent.text = getString(R.string.on_off)

                        binding.cardTime.isClickable = false
                        binding.cardTime.backgroundTintList =
                            activity.resources.getColorStateList(R.color.colorTileTextActive, null)

                        binding.cardDays.isClickable = false
                        binding.cardDays.backgroundTintList =
                            activity.resources.getColorStateList(R.color.colorTileTextActive, null)

                        binding.tvTime.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                        binding.tvDays.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                    }
                }
            }
        }

        viewModel.trigger.observe(viewLifecycleOwner) { trigger ->
            activity?.runOnUiThread {
                when (trigger.trigger) {
                    SunriseSunsetType.SUNRISE,
                    SunriseSunsetType.SUNSET -> {
                        binding.offsetOuterLayout.visibility = View.VISIBLE
                        binding.tvTime.text = trigger.trigger.displayName.toCapitalisedLowerCase()
                    }
                    else -> {
                        binding.offsetOuterLayout.visibility = View.GONE
                        val hour = viewModel.trigger.value?.hour ?: 0
                        val minute = viewModel.trigger.value?.minute ?: 0
                        binding.tvTime.text = String.format("%02d:%02d", hour, minute)
                    }
                }

                when {
                    trigger.offset > 0 -> {
                        binding.tvOffset.text = getString(
                            R.string.offset_after_time,
                            trigger.offset,
                            trigger.trigger.displayName
                        )
                    }
                    trigger.offset < 0 -> {
                        binding.tvOffset.text = getString(
                            R.string.offset_before_time,
                            abs(trigger.offset),
                            trigger.trigger.displayName
                        )
                    }
                    else -> {
                        binding.tvOffset.text = getString(R.string.no_offset)
                    }
                }
            }
        }

        viewModel.eventDay.observe(viewLifecycleOwner) {
            val day = it ?: return@observe

            mDay = day
            activity?.runOnUiThread {
                binding.tvDays.text = day.displayName
            }
        }

        binding.cardTarget.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                ScheduleEventFragmentDirections
                    .actionGlobalEventTargetSelectorFragment(ScheduleEventFragment::class.simpleName)
            )
        )

        binding.cardDevice.setOnClickListener {
            val action = ScheduleEventFragmentDirections.actionGlobalEventDeviceSelectorFragment(
                ScheduleEventFragment::class.simpleName!!
            )
            findNavController().navigate(action)
        }

        binding.cardScene.setOnClickListener {
            val sender = ScheduleEventFragment::class.simpleName ?: return@setOnClickListener
            val action = ScheduleEventFragmentDirections.actionGlobalEventSceneSelectorFragment(sender, mGroup)
            findNavController().navigate(action)
        }

        binding.cardEvent.setOnClickListener {
            val action = ScheduleEventFragmentDirections
                .actionScheduleEventFragmentToEventActionSelectorFragment(viewModel.device.value?.second == "lock", ScheduleEventFragment::class.simpleName ?: "")
            findNavController().navigate(action)
        }

        binding.cardTime.setOnClickListener {
            val action = ScheduleEventFragmentDirections.actionScheduleEventFragmentToScheduleTimeTriggerFragment(ScheduleEventFragment::class.simpleName ?: "")
            findNavController().navigate(action)
        }

        binding.cardOffset.setOnClickListener {
            val action = ScheduleEventFragmentDirections.actionScheduleEventFragmentToOffsetFragment(ScheduleEventFragment::class.simpleName ?: "")
            findNavController().navigate(action)
        }

        binding.cardDays.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                ScheduleEventFragmentDirections.actionGlobalEventDaySelectorFragment(
                    ScheduleEventFragment::class.simpleName
                )
            )
        )

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener { button ->
            button.isEnabled = false
            activity?.layoutGreyOut?.visibility = View.VISIBLE

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val isSuccessful = mLogicCollection.let {
                    viewModel.createLogicRule(it)
                }
                val activity = activity ?: return@launch

                activity.runOnUiThread {
                    if (isSuccessful) {
                        App.requestReviewIfAppropriate(activity)
                    } else {
                        App.actionFailed()
                    }
//    TODO                if (isSuccessful) {
                        binding.btnSave.isEnabled = true
                        activity.layoutGreyOut?.visibility = View.GONE
                        findNavController().popBackStack()
//                    }
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            val activity = activity ?: return@setOnClickListener
            if (!activity.isFinishing) {
                mLogicCollection.let { logicCollection ->
                    mLogicRule?.let { rule ->
                        AlertDialog.Builder(activity)
                            .setMessage(getString(R.string.delete_event_confirmation))
                            .setPositiveButton(R.string.yes) { _, _ ->
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    viewModel.deleteLogicRule(logicCollection.id, rule.id)
                                    activity.runOnUiThread {
                                        findNavController().popBackStack()
                                    }
                                }
                            }
                            .setNegativeButton(R.string.no) { _, _ ->

                            }
                            .create()
                            .show()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        viewModel.clearViewModel()

        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class SunriseSunsetType {
    SUNRISE,
    SUNSET,
    TIME;

    val displayName: String
    get() {
        return when (this) {
            SUNRISE,
            SUNSET -> name.lowercase()
            TIME -> "00:00"
        }
    }

    companion object {
        fun fromMetadata(value: String?): SunriseSunsetType? {
            if (value.isNullOrBlank()) return null

            values().forEach {
                if (it.name.equals(value, ignoreCase = true)) {
                    return it
                }
            }

            return null
        }
    }
}

enum class EventTarget(val displayName: String) {
    SCENE(App.context.getString(R.string.scene)),
    SPACE(App.context.getString(R.string.this_space)),
    DEVICE(App.context.getString(R.string.device_in_space))
}

enum class EventAction(val displayName: String) {
    ON(App.context.getString(R.string.on)),
    OFF(App.context.getString(R.string.off)),
    LOCK(App.context.getString(R.string.lock)),
    UNLOCK(App.context.getString(R.string.unlock))
}

enum class EventDay(val displayName: String, val days: JSONArray) {
    EVERYDAY(
        App.context.getString(R.string.everyday),
        JSONArray()
            .put("MONDAY")
            .put("TUESDAY")
            .put("WEDNESDAY")
            .put("THURSDAY")
            .put("FRIDAY")
            .put("SATURDAY")
            .put("SUNDAY")
    ),
    WEEKDAYS(
        App.context.getString(R.string.weekdays),
        JSONArray()
            .put("MONDAY")
            .put("TUESDAY")
            .put("WEDNESDAY")
            .put("THURSDAY")
            .put("FRIDAY")
    ),
    WEEKEND(
        App.context.getString(R.string.weekends),
        JSONArray()
            .put("SATURDAY")
            .put("SUNDAY")
    ),
    MONDAY(
        App.context.getString(R.string.monday),
        JSONArray()
            .put("MONDAY")
    ),
    TUESDAY(
        App.context.getString(R.string.tuesday),
        JSONArray()
            .put("TUESDAY")
    ),
    WEDNESDAY(
        App.context.getString(R.string.wednesday),
        JSONArray()
            .put("WEDNESDAY")
    ),
    THURSDAY(
        App.context.getString(R.string.thursday),
        JSONArray()
            .put("THURSDAY")
    ),
    FRIDAY(
        App.context.getString(R.string.friday),
        JSONArray()
            .put("FRIDAY")
    ),
    SATURDAY(
        App.context.getString(R.string.saturday),
        JSONArray()
            .put("SATURDAY")
    ),
    SUNDAY(
        App.context.getString(R.string.sunday),
        JSONArray()
            .put("SUNDAY")
    )
}
