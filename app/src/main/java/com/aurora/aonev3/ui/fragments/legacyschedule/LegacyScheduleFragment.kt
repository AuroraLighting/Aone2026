package com.aurora.aonev3.ui.fragments.legacyschedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.R
import com.aurora.aonev3.logic.DayOfWeek
import com.aurora.aonev3.logic.DayOfWeekCondition
import com.aurora.aonev3.logic.TimeOfDayTrigger
import com.aurora.aonev3.logic.UpdateResourceAction
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.fragment_legacy_schedule.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LegacyScheduleFragment : Fragment() {

    private val viewModel: LegacyScheduleViewModel by viewModels()
    private val args: LegacyScheduleFragmentArgs by navArgs()
    private val collectionId: Int by lazy { args.collectionId }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_legacy_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val gateway = NabtoHandler.selectedGateway ?: return
        val logicCollection = SyncHandler
            .logicCollectionsList
            .find { it.parentGateway == gateway.serial && it.id == collectionId } ?: return
        viewModel.logicCollection = logicCollection

        btnScheduleName.text = logicCollection.name

        viewModel.getLogicRules(logicCollection).observe(viewLifecycleOwner) {
            val rules = it.toList()
            if (rules.isEmpty()) return@observe

            var onRule: LogicRule? = null
            var offRule: LogicRule? = null
            var name = ""
            var daysString = ""
            var onTime = ""
            var offTime = ""

            rules.forEach { rule ->
                val actions = rule.actions ?: emptyArray()

                for (action in actions) {
                    if (action !is UpdateResourceAction) continue
                    val path = action.path
                    val data = action.data
                    val value = data.getValue() as? Boolean ?: false

                    if (DevelcoHandler.Endpoints.GROUP_SCENES.regex.matches(path) ||
                        (path.endsWith("onoff") && value)
                    ) {
                        onRule = rule
                    } else if (path.endsWith("onoff") && !value) {
                        offRule = rule
                    }
                }
            }

            val targetRule = onRule ?: offRule
            targetRule?.let { rule ->
                val actions = rule.actions ?: emptyArray()
                val conditions = rule.conditions ?: emptyArray()

                for (action in actions) {
                    if (action !is UpdateResourceAction) continue
                    val path = action.path
                    val id = path.split("/").mapNotNull { it.toIntOrNull() }.first()
                    val data = action.data
                    val sceneId = data.id

                    if (sceneId == null) {
                        name = if (DevelcoHandler.Endpoints.GROUP_DATAPOINT.regex.matches(path)) {
                            val group = SyncHandler
                                .groupsList
                                .find { it.parentGateway == gateway.serial && it.id == id } ?: continue

                            group.name
                        } else {
                            val device = SyncHandler
                                .devicesList
                                .find { it.parentGateway == gateway.serial && it.id == id } ?: continue

                            device.name
                        }
                    } else {
                        val scene = SyncHandler
                            .scenesList
                            .find { it.parentGateway ==gateway.serial && it.groupId == id && it.id == sceneId } ?: continue

                        name = scene.name
                    }
                }

                for (condition in conditions) {
                    if (condition !is DayOfWeekCondition) continue

                    val days = condition.days

                    val daysStrings = ArrayList<String>()

                    if (days.contains(DayOfWeek.MONDAY)) daysStrings.add("Mon")
                    if (days.contains(DayOfWeek.TUESDAY)) daysStrings.add("Tue")
                    if (days.contains(DayOfWeek.WEDNESDAY)) daysStrings.add("Wed")
                    if (days.contains(DayOfWeek.THURSDAY)) daysStrings.add("Thu")
                    if (days.contains(DayOfWeek.FRIDAY)) daysStrings.add("Fri")
                    if (days.contains(DayOfWeek.SATURDAY)) daysStrings.add("Sat")
                    if (days.contains(DayOfWeek.SUNDAY)) daysStrings.add("Sun")

                    daysString = daysStrings.joinToString(", ")
                }
            }

            onRule?.let { rule ->
                val triggers = rule.triggers ?: emptyArray()

                triggers.forEach triggers@{ trigger ->
                    if (trigger !is TimeOfDayTrigger) return@triggers
                    val time = arrayListOf(trigger.hour, trigger.min)

                    onTime = time.joinToString(":")
                }
            }

            offRule?.let { rule ->
                val triggers = rule.triggers ?: emptyArray()

                triggers.forEach triggers@{ trigger ->
                    if (trigger !is TimeOfDayTrigger) return@triggers
                    val time = arrayListOf(trigger.hour, trigger.min)

                    offTime = time.joinToString(":")
                }
            }

            activity.runOnUiThread {
                btnTarget.text = name
                btnDays.text = daysString

                if (onTime.isNotBlank()) {
                    btnOn.text = onTime
                    layoutOn.visibility = View.VISIBLE
                }
                if (offTime.isNotBlank()) {
                    btnOff.text = offTime
                    layoutOff.visibility = View.VISIBLE
                }
            }
        }

        btnHelp.setOnClickListener {
            if (!activity.isFinishing) {
                AlertDialog.Builder(activity)
                    .setTitle(R.string.legacy_schedule)
                    .setMessage(getString(R.string.legacy_schedule_info))
                    .setPositiveButton(R.string.ok, null)
                    .create()
                    .show()
            }
        }

        btnDelete.setOnClickListener {
            if (!activity.isFinishing) {
                AlertDialog.Builder(activity)
                    .setMessage(getString(R.string.delete_schedule_confirmation))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            viewModel.deleteCollection()
                            activity.runOnUiThread {
                                try {
                                    findNavController().popBackStack()
                                } catch (ex: IllegalStateException) {
                                    ex.printStackTrace()
                                }
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

    companion object {
        fun newInstance() = LegacyScheduleFragment()
    }
}