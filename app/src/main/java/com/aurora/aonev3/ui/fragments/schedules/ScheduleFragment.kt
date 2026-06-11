package com.aurora.aonev3.ui.fragments.schedules

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentScheduleBinding
import com.aurora.aonev3.*
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.logic.TimeOfDayTrigger
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.logic.TriggerEnum
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.fragments.group.adddevices.AddDevicesFragmentArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ScheduleFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!


    companion object {
        private const val TAG = "ScheduleFragment"
        fun newInstance() =
            ScheduleFragment()
    }

    private lateinit var viewModel: ScheduleViewModel

    private val args: ScheduleFragmentArgs by navArgs()
    private val group: Group by lazy { args.group }

    private var mLogicRuleMenu: LogicRule? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = ScheduleViewModelFactory(args.logicCollection)

        viewModel = ViewModelProvider(this, viewModelFactory).get(ScheduleViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentScheduleBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")

        if (viewModel.logicCollection.metadata.collectionType == CollectionType.SCHEDULE) {
            tvEventsTitle.text = getString(R.string.scheduled_events)
        } else if (viewModel.logicCollection.metadata.collectionType == CollectionType.DYNAMIC_EVENT) {
            tvEventsTitle.text = getString(R.string.dynamic_events)
        }

        val eventsAdapter = EventRecyclerViewAdapter(activity, viewModel.logicCollection).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val logicRule = getItem(position) ?: return

                    NabtoHandler.selectedGateway?.let { gateway ->
                        if (!gateway.isConnected) return@let

                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            try {
                                DevelcoHandler.putLogicRule(
                                    gateway,
                                    viewModel.logicCollection.id,
                                    logicRule.id,
                                    JSONObject().put("enabled", !logicRule.isEnabled)
                                )

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                            } catch (err: VolleyError) {
                                App.actionFailed()
                                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                    gateway.isConnected = false
                                    val credentials = CloudHandler.getCredentials()
                                    if (credentials.first.isEmpty()) {
                                        activity.finishAffinity()
                                        startActivity(Intent(context, SplashscreenActivity::class.java))
                                    }
                                    NabtoHandler.openTunnel(gateway, credentials.first)
                                }
                                err.printStackTrace()
                                if (err.networkResponse.statusCode != 404) {
                                    throw Exception("Failed to disable")
                                }
                            }
                        }
                    }
                }
            }

            onItemLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val logicRule = getItem(position) ?: return false

                    val action = if (logicCollection.metadata.collectionType == CollectionType.SCHEDULE) {
                        ScheduleFragmentDirections.actionScheduleFragmentToScheduleEventFragment(
                            group,
                            logicCollection,
                            logicRule
                        )
                    } else {
                        ScheduleFragmentDirections.actionScheduleFragmentToDynamicEventFragment(
                            group,
                            logicCollection,
                            logicRule
                        )
                    }

                    findNavController().navigate(action)

                    return true
                }
            }

            onMenuClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val logicRule = getItem(position) ?: return

                    mLogicRuleMenu = logicRule

                    val popup = PopupMenu(requireContext(), view)
                    if (logicRule.isEnabled) {
                        popup.menuInflater.inflate(R.binding.menu.enabled_schedule_menu, popup.menu)
                    } else {
                        popup.menuInflater.inflate(R.binding.menu.disabled_schedule_menu, popup.menu)
                    }
                    popup.setOnMenuItemClickListener(this@ScheduleFragment)
                    popup.show()
                }
            }
        }

        viewModel.logicRules
            .observe(viewLifecycleOwner) {

                activity.runOnUiThread {
                    val rules = viewModel.sortedLogicRules
                    if (rules.isNotEmpty()) {
                        eventsAdapter.setLogicRules(rules)
                        layoutEvent.visibility = View.VISIBLE
                    } else {
                        layoutEvent.visibility = View.GONE
                    }
                }
            }

        with(rvEvents) {
            adapter = eventsAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        binding.addEvent.setOnClickListener {
            val action = if (viewModel.logicCollection.metadata.collectionType == CollectionType.SCHEDULE) {
                ScheduleFragmentDirections.actionScheduleFragmentToScheduleEventFragment(
                    group,
                    viewModel.logicCollection
                )
            } else {
                ScheduleFragmentDirections.actionScheduleFragmentToDynamicEventFragment(
                    group,
                    viewModel.logicCollection
                )
            }
            findNavController().navigate(action)
        }

        btnSave.setOnClickListener {
            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        if (!allowEditing()) {
            btnSave.visibility = View.GONE
            btnCancel.visibility = View.GONE
            binding.addEvent.visibility = View.GONE
            binding.dividerTop.visibility = View.GONE
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        mLogicCollection =
//            viewModel.selectedLogicCollection ?: throw Exception("Invalid logic collection")
//    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.enable_event -> {
                NabtoHandler.selectedGateway?.let { gateway ->
                    if (!gateway.isConnected) return@let

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val logicRule = mLogicRuleMenu ?: return@launch
                        try {
                            DevelcoHandler.putLogicRule(
                                gateway,
                                viewModel.logicCollection.id,
                                logicRule.id,
                                JSONObject().put("enabled", true)
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
                            err.printStackTrace()
                            throw Exception("Failed to disable")
                        }
                    }
                }
                true
            }
            R.id.disable_event -> {
                NabtoHandler.selectedGateway?.let { gateway ->
                    if (!gateway.isConnected) return@let

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val logicRule = mLogicRuleMenu ?: return@launch
                        try {
                            DevelcoHandler.putLogicRule(
                                gateway,
                                viewModel.logicCollection.id,
                                logicRule.id,
                                JSONObject().put("enabled", false)
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
                            err.printStackTrace()
                            throw Exception("Failed to disable")
                        }
                    }
                }
                true
            }
            R.id.edit_event -> {
                val logicCollection = viewModel.logicCollection

                val action = if (logicCollection.metadata.collectionType == CollectionType.SCHEDULE) {
                    ScheduleFragmentDirections.actionScheduleFragmentToScheduleEventFragment(
                        group,
                        logicCollection,
                        mLogicRuleMenu
                    )
                } else {
                    ScheduleFragmentDirections.actionScheduleFragmentToDynamicEventFragment(
                        group,
                        logicCollection,
                        mLogicRuleMenu
                    )
                }

                findNavController().navigate(action)
                true
            }
            R.id.delete_event -> {
                activity?.let { activity ->
                    if (!activity.isFinishing) {
                        val logicRule = mLogicRuleMenu ?: return@let
                        AlertDialog.Builder(activity)
                            .setMessage(getString(R.string.delete_event_confirmation))
                            .setPositiveButton(R.string.yes) { _, _ ->
                                viewModel.deleteLogicRule(viewModel.logicCollection.id, logicRule.id)
                            }
                            .setNegativeButton(R.string.no) { _, _ ->

                            }
                            .create()
                            .show()
                    }
                }
                true
            }
            else -> false
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
