package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.aurora.aonev3.databinding.FragmentTimeConditionBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.debug
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventViewModel
import com.aurora.aonev3.ui.fragments.dynamicevents.DynamicEventFragment
import com.aurora.aonev3.ui.fragments.dynamicevents.DynamicEventViewModel
import com.aurora.aonev3.ui.fragments.groups.eventgroupselector.EventGroupSelectorFragmentArgs
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.ConditionEndTimeViewModel
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.ConditionStartTimeViewModel
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.IScheduleTimeViewModel
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.ScheduleTimeViewModel
import kotlin.math.abs

class TimeConditionFragment : Fragment() {

    private var _binding: FragmentTimeConditionBinding? = null
    private val binding get() = _binding!!


    private lateinit var senderEventViewModel: ITimeConditionViewModel
    private val viewModel: ITimeConditionViewModel by viewModels<TimeConditionViewModel>()
    private val startConditionViewModel: IScheduleTimeViewModel by activityViewModels<ConditionStartTimeViewModel>()
    private val endConditionViewModel: IScheduleTimeViewModel  by activityViewModels<ConditionEndTimeViewModel>()
    private var mStartTime: TriggerTime? = null
    private var mEndTime: TriggerTime? = null
    private var mIsAllDay: Boolean? = null

    private val args: TimeConditionFragmentArgs by navArgs()
    private val sender: String by lazy { args.sender }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()
        senderEventViewModel = when (sender) {
            DynamicEventFragment::class.simpleName -> {
                navGraphViewModels<DynamicEventViewModel>(R.id.dynamicEventFragment).value
            }
            MotionSensorEventFragment::class.simpleName -> {
                ViewModelProvider(activity).get(MotionSensorEventViewModel::class.java)
            }
            else -> {
                ViewModelProvider(activity).get(DoorSensorEventViewModel::class.java)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentTimeConditionBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")
        debug(sender)

        senderEventViewModel.startTime.observe(viewLifecycleOwner) {
            if (mStartTime == null) {
                viewModel.updateStartTime(it.hour, it.minute, it.trigger, it.offset)
            }
        }

        senderEventViewModel.endTime.observe(viewLifecycleOwner) {
            if (mEndTime == null) {
                viewModel.updateEndTime(it.hour, it.minute, it.trigger, it.offset)
            }
        }

        senderEventViewModel.isAllDay.observe(viewLifecycleOwner) {
            if (mIsAllDay == null) {
                viewModel.setIsAllDay(it)
            }
        }

        viewModel.startTime.observe(viewLifecycleOwner) {
            it ?: return@observe
            mStartTime = it

            if (startConditionViewModel.trigger.value == null) {
                startConditionViewModel.updateTrigger(it.hour, it.minute, it.trigger, it.offset)
            }

            mStartTime?.let { trigger ->
                binding.tvStartTime.text = when (trigger.trigger) {
                    SunriseSunsetType.SUNRISE -> {
                        binding.startOffsetLayout.visibility = View.VISIBLE
                        getString(R.string.sunrise)
                    }
                    SunriseSunsetType.SUNSET -> {
                        binding.startOffsetLayout.visibility = View.VISIBLE
                        getString(R.string.sunset)
                    }
                    SunriseSunsetType.TIME -> {
                        binding.startOffsetLayout.visibility = View.GONE
                        getString(R.string.event_time, trigger.hour, trigger.minute)
                    }
                }

                when {
                    trigger.offset > 0 -> {
                        tvStartOffset.text = getString(
                            R.string.offset_after_time,
                            trigger.offset,
                            trigger.trigger.displayName
                        )
                    }
                    trigger.offset < 0 -> {
                        tvStartOffset.text = getString(
                            R.string.offset_before_time,
                            abs(trigger.offset),
                            trigger.trigger.displayName
                        )
                    }
                    else -> {
                        tvStartOffset.text = getString(R.string.no_offset)
                    }
                }
            }
        }

        viewModel.endTime.observe(viewLifecycleOwner) {
            it ?: return@observe
            mEndTime = it

            if (endConditionViewModel.trigger.value == null) {
                endConditionViewModel.updateTrigger(it.hour, it.minute, it.trigger, it.offset)
            }

            mEndTime?.let { trigger ->
                binding.tvEndTime.text = when (trigger.trigger) {
                    SunriseSunsetType.SUNRISE -> {
                        binding.endOffsetLayout.visibility = View.VISIBLE
                        getString(R.string.sunrise)
                    }
                    SunriseSunsetType.SUNSET -> {
                        binding.endOffsetLayout.visibility = View.VISIBLE
                        getString(R.string.sunset)
                    }
                    SunriseSunsetType.TIME -> {
                        binding.endOffsetLayout.visibility = View.GONE
                        getString(R.string.event_time, trigger.hour, trigger.minute)
                    }
                }

                when {
                    trigger.offset > 0 -> {
                        tvEndOffset.text = getString(
                            R.string.offset_after_time,
                            trigger.offset,
                            trigger.trigger.displayName
                        )
                    }
                    trigger.offset < 0 -> {
                        tvEndOffset.text = getString(
                            R.string.offset_before_time,
                            abs(trigger.offset),
                            trigger.trigger.displayName
                        )
                    }
                    else -> {
                        tvEndOffset.text = getString(R.string.no_offset)
                    }
                }
            }
        }

        viewModel.isAllDay.observe(viewLifecycleOwner) {
            mIsAllDay = it

            if (mIsAllDay != false) {
                binding.allDayCard.backgroundTintList =
                    activity.resources.getColorStateList(R.color.colorTileActive, null)
                binding.tvAllDay.setTextColor(activity.getColor(R.color.colorTileTextActive))

                binding.startTimeCard.backgroundTintList =
                    activity.resources.getColorStateList(R.color.colorTileTextActive, null)
                binding.tvStartTime.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                binding.endTimeCard.backgroundTintList =
                    activity.resources.getColorStateList(R.color.colorTileTextActive, null)
                binding.tvEndTime.setTextColor(activity.getColor(R.color.colorPrimaryBackground))
                binding.startOffsetLayout.visibility = View.GONE
                binding.endOffsetLayout.visibility = View.GONE
            } else {
                binding.allDayCard.backgroundTintList =
                    activity.resources.getColorStateList(R.color.colorTileTextActive, null)
                binding.tvAllDay.setTextColor(activity.getColor(R.color.colorPrimaryBackground))

                binding.startTimeCard.backgroundTintList =
                    activity.resources.getColorStateList(R.color.colorTileActive, null)
                binding.tvStartTime.setTextColor(activity.getColor(R.color.colorTileTextActive))
                binding.endTimeCard.backgroundTintList =
                    activity.resources.getColorStateList(R.color.colorTileActive, null)
                binding.tvEndTime.setTextColor(activity.getColor(R.color.colorTileTextActive))
            }
        }

        startConditionViewModel.trigger.observe(viewLifecycleOwner) {
            viewModel.updateStartTime(it.hour, it.minute, it.trigger, it.offset)
        }

        endConditionViewModel.trigger.observe(viewLifecycleOwner) {
            viewModel.updateEndTime(it.hour, it.minute, it.trigger, it.offset)
        }

        binding.allDayCard.setOnClickListener {
            viewModel.setIsAllDay(true)
        }

        binding.startTimeCard.setOnClickListener {
            viewModel.setIsAllDay(false)

            val action = TimeConditionFragmentDirections.actionTimeConditionFragmentToScheduleTimeTriggerFragment("${TimeConditionFragment::class.simpleName}_start")
            findNavController().navigate(action)
        }

        binding.startOffsetLayout.setOnClickListener {
            val action = TimeConditionFragmentDirections.actionTimeConditionFragmentToOffsetFragment("${TimeConditionFragment::class.simpleName}_start")
            findNavController().navigate(action)
        }

        binding.endTimeCard.setOnClickListener {
            viewModel.setIsAllDay(false)

            val action = TimeConditionFragmentDirections.actionTimeConditionFragmentToScheduleTimeTriggerFragment("${TimeConditionFragment::class.simpleName}_end")
            findNavController().navigate(action)
        }

        binding.endOffsetLayout.setOnClickListener {
            val action = TimeConditionFragmentDirections.actionTimeConditionFragmentToOffsetFragment("${TimeConditionFragment::class.simpleName}_end")
            findNavController().navigate(action)
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            val startTime = viewModel.startTime.value ?: TriggerTime(0, 0, SunriseSunsetType.TIME, 0)
            val endTime = viewModel.endTime.value ?: TriggerTime(0, 0, SunriseSunsetType.TIME, 0)

            if (mIsAllDay != false) {
                senderEventViewModel.setIsAllDay(true)
            } else {
                senderEventViewModel.updateStartTime(startTime.hour, startTime.minute, startTime.trigger, startTime.offset)
                senderEventViewModel.updateEndTime(endTime.hour, endTime.minute, endTime.trigger, endTime.offset)
                senderEventViewModel.setIsAllDay(false)
            }

            findNavController().popBackStack()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (startConditionViewModel as? ConditionStartTimeViewModel)?.clearViewModel()
        (endConditionViewModel as? ConditionEndTimeViewModel)?.clearViewModel()
    }

    companion object {
        fun newInstance() =
            TimeConditionFragment()
    }
}

interface ITimeConditionViewModel {
    val isAllDay: LiveData<Boolean>
    val startTime: LiveData<TriggerTime>
    val endTime: LiveData<TriggerTime>
    
    fun setIsAllDay(isAllDay: Boolean)
    fun updateStartTime(hour: Int = startTime.value?.hour ?: 0, minute: Int = startTime.value?.minute ?: 0, trigger: SunriseSunsetType = startTime.value?.trigger ?: SunriseSunsetType.TIME, offset: Int = startTime.value?.offset ?: 0)
    fun updateEndTime(hour: Int = endTime.value?.hour ?: 0, minute: Int = endTime.value?.minute ?: 0, trigger: SunriseSunsetType = endTime.value?.trigger ?: SunriseSunsetType.TIME, offset: Int = endTime.value?.offset ?: 0)


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
