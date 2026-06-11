package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.kinetics

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.allowEditing
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.logic.UpdateResourceAction
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.DeviceDetailFragment
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class KineticDetailFragment : DeviceDetailFragment() {

    private val kineticViewModel: KineticDetailViewModel by activityViewModels()

    private var mTargetGroup: Group? = null
    private var mTargetMode: UpDownMode? = null
    private var mSecondaryMode: UpDownMode? = null
    private var mLogicCollection: LogicCollection? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        val device = viewModel.selectedDevice ?: return

        viewModel.group.observe(viewLifecycleOwner, {
            mTargetGroup = it
            kineticViewModel.selectedGroup = it
            upDownLayout.isClickable = allowEditing() && it != null
            kineticSecondaryModeLayout.isClickable = allowEditing() && it != null
            if (kineticViewModel.targetGroup.value != null && it != null) {
                kineticViewModel.targetGroup.postValue(it)
            }
            setModeColour(upDownLayout, tvUpDown, kineticViewModel.targetMode.value)
            setModeColour(kineticSecondaryModeLayout, tvKineticSecondaryMode, kineticViewModel.secondaryMode.value)
        })

        btnIdentify.visibility = View.GONE

        upDownOuterLayout.visibility = View.VISIBLE
        kineticSecondaryModeOuterLayout.visibility = View.VISIBLE

        tvOtaStatusLabel.visibility = View.GONE
        tvOtaStatusValue.visibility = View.GONE

        upDownLayout.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                KineticDetailFragmentDirections
                    .actionKineticDetailFragmentToKineticUpDownSelectorFragment(false)
            )
        )

        kineticSecondaryModeLayout.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                KineticDetailFragmentDirections
                    .actionKineticDetailFragmentToKineticUpDownSelectorFragment(true)
            )
        )
        upDownLayout.isClickable = allowEditing() && viewModel.group.value != null
        kineticSecondaryModeLayout.isClickable = allowEditing() && viewModel.group.value != null

        ivHelp.setOnClickListener {
            val activity = requireActivity()
            activity.runOnUiThread {
                if (!activity.isFinishing) {
                    AlertDialog.Builder(activity)
                        .setTitle(getString(R.string.secondary_mode))
                        .setMessage(getString(R.string.kinetic_secondary_mode_help))
                        .setPositiveButton(R.string.ok, null)
                        .create()
                        .show()
                }
            }
        }

        kineticViewModel.targetMode.observe(viewLifecycleOwner, { mode ->
            mTargetMode = mode

            when (mTargetMode) {
                UpDownMode.CYCLE_SCENES -> tvUpDown.text = getString(R.string.scene_cycle)
                UpDownMode.STEP_COLOUR_TEMPERATURE -> tvUpDown.text = getString(R.string.step_colour_temperature_up_down)
                UpDownMode.STEP -> tvUpDown.text = getString(R.string.step_up_down)
                null -> tvUpDown.text =
                    getString(R.string.assign_functionality_of_the_up_down_buttons)
            }

            setModeColour(upDownLayout, tvUpDown, mode)
        })

        kineticViewModel.secondaryMode.observe(viewLifecycleOwner, { mode ->
            mSecondaryMode = mode

            when (mSecondaryMode) {
                UpDownMode.CYCLE_SCENES -> tvKineticSecondaryMode.text = getString(R.string.scene_cycle)
                UpDownMode.STEP_COLOUR_TEMPERATURE -> tvKineticSecondaryMode.text = getString(R.string.step_colour_temperature_up_down)
                UpDownMode.STEP -> tvKineticSecondaryMode.text = getString(R.string.step_up_down)
                null -> tvKineticSecondaryMode.text =
                    getString(R.string.assign_functionality_secondary_mode)
            }

            setModeColour(kineticSecondaryModeLayout, tvKineticSecondaryMode, mode)
        })

        val gateway = NabtoHandler.selectedGateway ?: return
        kineticViewModel.getLogicCollections(gateway)
            .observe(viewLifecycleOwner, Observer { logicCollections ->
                if (mTargetMode != null || mSecondaryMode != null) return@Observer
                val logicCollection = logicCollections.toList().firstOrNull { logicCollection ->
                    val metadata = logicCollection.metadata

                    metadata.collectionType == CollectionType.KINETIC &&
                            metadata.triggerId == device.id
                } ?: return@Observer

                val metadata = logicCollection.metadata

                mLogicCollection = logicCollection

                try {
                    val upDownMode = UpDownMode.valueOf(
                        metadata.subType?.name ?: ""
                    )
                    kineticViewModel.targetMode.postValue(upDownMode)
                    kineticViewModel.previousTargetMode = upDownMode
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                }

                if (metadata.subType2 != null) {
                    try {
                        val upDownMode = UpDownMode.valueOf(
                            metadata.subType2?.name ?: ""
                        )
                        kineticViewModel.secondaryMode.postValue(upDownMode)
                        kineticViewModel.previousSecondaryMode = upDownMode
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                    }
                }

                if (metadata.actionGroups != null) {
                    mGroup?.let {
                        val targetGroup = it
                        kineticViewModel.targetGroup.postValue(targetGroup)
                        kineticViewModel.previousTargetGroup = targetGroup
                    }
                } else {
                    val allLogicRules = SyncHandler
                        .logicRulesList
                        .filter { logicRule -> logicRule.parentGateway == gateway.serial && logicRule.logicCollectionId == logicCollection.id }

                    allLogicRules.forEach { logicRule ->
                        val actions = logicRule.actions ?: emptyArray()
                        for (action in actions) {
                            if (action !is UpdateResourceAction) continue
                            val path = action.path
                            if (!path.contains("grp/grps")) continue
                            val id = path.split("/")
                                .mapNotNull { it.toIntOrNull() }
                                .firstOrNull() ?: continue

                            val targetGroup = SyncHandler
                                .groupsList
                                .find {
                                    it.parentGateway == gateway.serial && it.id == id }

                            if (targetGroup != null) {
                                kineticViewModel.targetGroup.postValue(targetGroup)
                                kineticViewModel.previousTargetGroup = targetGroup
                            }
                        }
                    }
                }
            })

        if (!allowEditing()) {
            upDownLayout.isClickable = false
            kineticSecondaryModeLayout.isClickable = false
        }
    }

    private fun setModeColour(
        layout: MaterialCardView,
        textView: TextView,
        mode: UpDownMode?
    ) {
        when {
            mTargetGroup == null -> {
                layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                textView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryBackground)
                )
            }
            mode == null -> {
                layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                textView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
                )
            }
            else -> {
                layout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                textView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            }
        }
    }

    override fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val device = viewModel.selectedDevice ?: return@OnClickListener
            val group = mGroup ?: return@OnClickListener
            btnSave.isEnabled = false
            activity?.layoutGreyOut?.visibility = View.VISIBLE
            viewModel.viewModelScope.launch(Dispatchers.IO) {

                NabtoHandler.selectedGateway?.let { gateway ->
                    kineticViewModel.updateDevice(viewModel.deviceName, device)
                    viewModel.deviceName = ""

                    if (mTargetMode != kineticViewModel.previousTargetMode ||
                        mTargetGroup != kineticViewModel.previousTargetGroup ||
                        mSecondaryMode != kineticViewModel.previousSecondaryMode
                    ) {
                        if (mTargetMode != null) {
                            kineticViewModel.deleteLogicCollections(device)

                            val metadata = JSONObject()
                                .put("collection_type", "kinetic")
                                .put("sub_type", mTargetMode?.name?.toLowerCase())
                                .put("sub_type_2", mSecondaryMode?.name?.toLowerCase())
                                .put("trigger_id", device.id)

                            if (mTargetGroup != null) {
                                metadata.put(
                                    "action_groups",
                                    JSONArray().put(JSONObject().put("id", mTargetGroup?.id))
                                )
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
                                    return@let
                                }
                            val collectionId =
                                collectionResponse.optJSONObject("body")?.optInt("id") ?: return@let

                            kineticViewModel.buildRule(device, group, "switchA", collectionId, RuleType.ON)
                            kineticViewModel.buildRule(device, group, "switchA", collectionId, RuleType.OFF)

                            when (mTargetMode) {
                                UpDownMode.CYCLE_SCENES -> {
                                    kineticViewModel.buildRule(device, group, "switchB", collectionId, RuleType.CYCLE_UP)
                                    kineticViewModel.buildRule(device, group, "switchB", collectionId, RuleType.CYCLE_DOWN)
                                }
                                UpDownMode.STEP_COLOUR_TEMPERATURE -> {
                                    kineticViewModel.buildRule(device, group, "switchB", collectionId, RuleType.COLOUR_TEMPERATURE_UP)
                                    kineticViewModel.buildRule(device, group, "switchB", collectionId, RuleType.COLOUR_TEMPERATURE_DOWN)
                                }
                                else -> {
                                    kineticViewModel.buildRule(device, group, "switchB", collectionId, RuleType.UP)
                                    kineticViewModel.buildRule(device, group, "switchB", collectionId, RuleType.DOWN)
                                }
                            }
                            when (mSecondaryMode) {
                                UpDownMode.CYCLE_SCENES -> {
                                    kineticViewModel.buildRule(device, group, "switchAB", collectionId, RuleType.CYCLE_UP)
                                    kineticViewModel.buildRule(device, group, "switchAB", collectionId, RuleType.CYCLE_DOWN)
                                }
                                UpDownMode.STEP_COLOUR_TEMPERATURE -> {
                                    kineticViewModel.buildRule(device, group, "switchAB", collectionId, RuleType.COLOUR_TEMPERATURE_UP)
                                    kineticViewModel.buildRule(device, group, "switchAB", collectionId, RuleType.COLOUR_TEMPERATURE_DOWN)
                                }
                                UpDownMode.STEP -> {
                                    kineticViewModel.buildRule(device, group, "switchAB", collectionId, RuleType.UP)
                                    kineticViewModel.buildRule(device, group, "switchAB", collectionId, RuleType.DOWN)
                                }
                                null -> {}
                            }
                        }
                    }
                }

                activity?.runOnUiThread {
                    btnSave.isEnabled = true
                    activity?.layoutGreyOut?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun btnDeleteClickListener() {
        val device = viewModel.selectedDevice ?: return
        activity?.layoutGreyOut?.visibility = View.VISIBLE
        kineticViewModel.viewModelScope.launch(Dispatchers.IO) {
            kineticViewModel.deleteLogicCollections(device)

            kineticViewModel.deleteDevice(device)

            activity?.runOnUiThread {
                activity?.layoutGreyOut?.visibility = View.GONE
                findNavController().popBackStack()
            }
        }
    }

    override fun onDetach() {
        kineticViewModel.clearViewModel()
        super.onDetach()
    }

    companion object {
        private const val TAG = "KineticDetailFragment"
        fun newInstance() =
            KineticDetailFragment()
    }

    enum class RuleType {
        ON,
        OFF,
        UP,
        DOWN,
        COLOUR_TEMPERATURE_UP,
        COLOUR_TEMPERATURE_DOWN,
        CYCLE_UP,
        CYCLE_DOWN
    }
}

enum class UpDownMode(val displayName: String) {
    CYCLE_SCENES(App.context.getString(R.string.scene_cycle)),
    STEP_COLOUR_TEMPERATURE(App.context.getString(R.string.step_colour_temperature_up_down)),
    STEP(App.context.getString(R.string.step_up_down))
}