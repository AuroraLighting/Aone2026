package com.aurora.aonev3.ui.fragments.schedules.eventselectors

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.R
import com.aurora.aonev3.debug
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.TimeConditionFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventViewModel
import kotlinx.android.synthetic.main.fragment_offset.*
import kotlinx.android.synthetic.main.fragment_offset.btnCancel
import kotlinx.android.synthetic.main.fragment_offset.btnSave
import kotlinx.android.synthetic.main.fragment_timeout.*
import kotlin.math.abs

class OffsetFragment : Fragment() {

    private lateinit var viewModel: IScheduleTimeViewModel
    private val args: OffsetFragmentArgs by navArgs()
    private val sender: String by lazy { args.sender }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()

        viewModel = when (sender) {
            "${TimeConditionFragment::class.simpleName}_start" -> {
                ViewModelProvider(activity).get(ConditionStartTimeViewModel::class.java)
            }
            "${TimeConditionFragment::class.simpleName}_end" -> {
                ViewModelProvider(activity).get(ConditionEndTimeViewModel::class.java)
            }
            else -> {
                ViewModelProvider(activity).get(ScheduleEventViewModel::class.java)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_offset, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        offsetPicker.minValue = 0
        offsetPicker.maxValue = 36

        offsetPicker.displayedValues = Array(offsetPicker.maxValue + 1) {
            val value = (it - 18) * 5

            val string = when {
                value > 0 -> getString(R.string.offset_after, value)
                value < 0 -> getString(R.string.offset_before, abs(value))
                else -> getString(R.string.no_offset)
            }

            string
        }

        offsetPicker.wrapSelectorWheel = false
        offsetPicker.displayedValues

        viewModel.trigger.observe(viewLifecycleOwner, { trigger ->
            val offset = trigger.offset

            val calculatedOffset = (offset / 5) + 18

            activity?.runOnUiThread {
                offsetPicker.value = calculatedOffset
            }
        })

        btnSave.setOnClickListener {

            val calculatedOffset = (offsetPicker.value - 18) * 5
            viewModel.updateTrigger(offset = calculatedOffset)

            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        view.rootView.requestFocus()
    }
}
