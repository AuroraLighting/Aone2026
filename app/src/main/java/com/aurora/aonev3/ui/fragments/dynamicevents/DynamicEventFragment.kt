package com.aurora.aonev3.ui.fragments.dynamicevents

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.aurora.aonev3.databinding.FragmentDynamicEventBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragmentDirections
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class DynamicEventFragment : Fragment() {

    private var _binding: FragmentDynamicEventBinding? = null
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = DynamicEventFragment()
    }

    private val args: DynamicEventFragmentArgs by navArgs()

    private val viewModel: DynamicEventViewModel by navGraphViewModels(R.id.dynamicEventFragment) { DynamicEventViewModelFactory(args.logicCollection, args.logicRule, args.group) }

    private val edit: Boolean
    get() = viewModel.logicRule.value != null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentDynamicEventBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.scene.observe(viewLifecycleOwner) {
            setUpUI()
        }

        viewModel.isAllDay.observe(viewLifecycleOwner) {
            setUpUI()
        }

        viewModel.startTime.observe(viewLifecycleOwner) {
            setUpUI()
        }

        viewModel.endTime.observe(viewLifecycleOwner) {
            setUpUI()
        }

        viewModel.eventDay.observe(viewLifecycleOwner) {
            setUpUI()
        }

        binding.btnScene.setOnClickListener {
            val sender = DynamicEventFragment::class.simpleName ?: return@setOnClickListener
            val action = DynamicEventFragmentDirections.actionGlobalEventSceneSelectorFragment(sender, viewModel.group)
            findNavController().navigate(action)
        }

        binding.btnTime.setOnClickListener {
            val sender = DynamicEventFragment::class.simpleName ?: return@setOnClickListener
            val action = DynamicEventFragmentDirections.actionDynamicEventFragmentToTimeConditionFragment(sender)
            findNavController().navigate(action)
        }

        binding.btnDays.setOnClickListener {
            val sender = DynamicEventFragment::class.simpleName ?: return@setOnClickListener
            val action = DynamicEventFragmentDirections.actionGlobalEventDaySelectorFragment(sender)
            findNavController().navigate(action)
        }

        binding.ivHelp.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.dynamic_events)
                .setMessage(R.string.dynamic_events_help)
                .setPositiveButton(R.string.ok, null)
                .create()
                .show()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener { button ->
            button.isEnabled = false
            activity?.layoutGreyOut?.visibility = View.VISIBLE

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val isSuccessful = viewModel.createLogicRule()
                val activity = activity ?: return@launch

                activity.runOnUiThread {
                    if (isSuccessful) {
                        App.requestReviewIfAppropriate(activity)
                    } else {
                        App.actionFailed()
                    }
//    TODO                if (isSuccessful) {
                    binding.binding.btnSave.isEnabled = true
                    activity.layoutGreyOut?.visibility = View.GONE
                    findNavController().popBackStack()
//                    }
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            val activity = activity ?: return@setOnClickListener
            if (!activity.isFinishing) {
                AlertDialog.Builder(activity)
                    .setMessage(getString(R.string.delete_event_confirmation))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            viewModel.deleteLogicRule()
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

        binding.btnDelete.visibility = if (!edit) {
            View.GONE
        } else {
            View.VISIBLE
        }

        setUpUI()
    }

    private fun setUpUI() {
        val scene = viewModel.scene.value
        val isAllDay = viewModel.isAllDay.value
        val startTime = viewModel.startTime.value
        val endTime = viewModel.endTime.value
        val eventDay = viewModel.eventDay.value

        if (scene != null) {
            setButtonActive(btnScene, scene.name)

            if (isAllDay != null
                || (startTime != null
                        && endTime != null)) {
                if (isAllDay == true) {
                    setButtonActive(btnTime, getString(R.string.all_day))
                } else {
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
                    }
                }
            } else {
                setButtonActive(btnTime, getString(R.string.time_condition_placeholder))
            }

            setButtonActive(btnDays, eventDay?.displayName ?: EventDay.EVERYDAY.displayName)
        } else {
            setButtonClickable(btnScene, getString(R.string.scene))
            setButtonNotClickable(btnTime, getString(R.string.time_condition_placeholder))
            setButtonNotClickable(btnDays, eventDay?.displayName ?: EventDay.EVERYDAY.displayName)
        }
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
