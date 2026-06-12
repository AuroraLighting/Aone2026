package com.aurora.aonev3.ui.fragments.group

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.HapticFeedbackConstants.CONFIRM
import android.view.HapticFeedbackConstants.CONTEXT_CLICK
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.databinding.FragmentGroupsBinding
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.*
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.TourActivity
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.login.LoginActivity
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.groups.creategroups.NoGroupsFragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class GroupsFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!


    companion object {
        const val TAG = "GroupsFragment"
        fun newInstance() = GroupsFragment()
    }

    private val viewModel: GroupsViewModel by activityViewModels()
    private var groupsLiveData: MutableLiveDataArrayList<Group> = MutableLiveDataArrayList()
    private var datapointsLiveData: MutableLiveDataArrayList<GroupDatapoint> = MutableLiveDataArrayList()
    private var allDevicesLiveData: MutableLiveDataArrayList<Device> = MutableLiveDataArrayList()
    private var logicCollectionsLiveData: MutableLiveDataArrayList<LogicCollection> = MutableLiveDataArrayList()
    private var deviceDatapointsLiveData: MutableLiveDataArrayList<DeviceDatapoint> = MutableLiveDataArrayList()

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentGroupsBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NabtoHandler.connectingCallback = object : NabtoHandler.NabtoConnecting {
            override fun finish(success: Boolean) {
                if (!success) {
                    activity?.runOnUiThread {
                        val activity = activity ?: return@runOnUiThread
                        activity.tvConnecting?.text = getString(R.string.failed_to_connect)

                        if (!activity.isFinishing) {
                            AlertDialog.Builder(activity)
                                .setTitle(getString(R.string.failed_to_open_tunnel))
                                .setMessage("Error code: ${NabtoHandler.selectedGateway?.lastError}")
                                .setPositiveButton(R.string.ok) { _, _ ->
                                    val action =
                                        GroupsFragmentDirections.actionGroupsFragmentToGatewaySwitchFragment()
                                    try {
                                        findNavController().navigate(action)
                                    } catch (ex: IllegalArgumentException) {
                                        Log.e(TAG, ex.localizedMessage ?: "")
                                        ex.printStackTrace()
//                                        crashlytics.recordException(ex)
                                    }
                                }
                                .create()
                                .show()
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        checkTimezone()
                        (activity as? MainActivity)?.binding?.connectingLayout?.visibility = View.GONE
                    }
                    SyncHandler.syncHandlerCoroutineScope.launch(Dispatchers.IO) {
                        NabtoHandler.selectedGateway?.let { gateway ->
                            if (!gateway.isConnected) return@let

                            try {
                                SyncHandler.syncGroups(gateway, first = true)

                                if (SyncHandler.getGroupCount() == 0) {
                                    val action =
                                        GroupsFragmentDirections.actionGroupsFragmentToNoGroupsFragment()
                                    activity?.runOnUiThread {
                                        try {
                                            findNavController().navigate(action)
                                        } catch (ex: IllegalArgumentException) {
                                            ex.printStackTrace()
                                            Log.e(TAG, "Tried to navigate from incorrect destination")
                                        }
                                    }
                                }
                            } catch (err: VolleyError) {
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

                            TemplateHandler(gateway).check()
                        }
                    }
                }
            }
        }

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            if (viewModel.getGateways().isNotEmpty()) {
                val credentials = CloudHandler.getCredentials()
                if (credentials.first.isEmpty()) {
                    activity?.finishAffinity()
                    startActivity(Intent(context, SplashscreenActivity::class.java))
                }
                NabtoHandler.openTunnels(credentials.first)
                NabtoHandler.selectedGateway?.let { gateway ->
                    groupsLiveData = viewModel.getGroups(gateway)
                    datapointsLiveData =
                        SyncHandler.groupDatapoints
                    allDevicesLiveData = viewModel.getDevices(gateway)
                    deviceDatapointsLiveData = viewModel.getDeviceDatapoints(gateway)
                    logicCollectionsLiveData = viewModel.getLogicCollection(gateway)
                }
            }
        }

        if (!SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("homeTourDone", false)) {
            val intent = Intent(activity, TourActivity::class.java)
            intent.putExtra("tour", "home")
            startActivity(intent)
        }

        binding.tvTitle.setOnClickListener {
            val action = GroupsFragmentDirections.actionGroupsFragmentToGatewaySwitchFragment()
            try {
                findNavController().navigate(action)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
                Log.e(TAG, "Tried to navigate from incorrect destination")
            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
                Log.e(TAG, "Failed to navigate")
            }
        }

        binding.ivHub.setOnClickListener {
            val action = GroupsFragmentDirections.actionGroupsFragmentToGatewaySwitchFragment()
            try {
                findNavController().navigate(action)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
                Log.e(TAG, "Tried to navigate from incorrect destination")
            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
                Log.e(TAG, "Failed to navigate")
            }
        }

        binding.menu.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.groups_menu, popup.menu)
            if (!allowEditing()) {
                popup.binding.menu.removeItem(R.id.create_group)
            }
            popup.setOnMenuItemClickListener(this@GroupsFragment)
            popup.show()
        }

        if (NabtoHandler.selectedGateway == null) {
            val action = GroupsFragmentDirections.actionGroupsFragmentToGatewaySwitchFragment()
            try {
                findNavController().navigate(action)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
                Log.e(TAG, "Tried to navigate from incorrect destination")
            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
                Log.e(TAG, "Failed to navigate")
            }
        }

        val gridAdapter = GroupsRecyclerViewAdapter(requireActivity()).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val group = getItem(position) ?: return
                    viewModel.selectedGroup = group

                    val action =
                        GroupsFragmentDirections.actionGroupsFragmentToGroupFragment(group.id)
                    try {
                        findNavController().navigate(action)
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Tried to navigate from incorrect destination")
                    } catch (ex: IllegalStateException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Failed to navigate")
                    }
                }
            }

            onItemLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val group = getItem(position) ?: return false
                    val members = SyncHandler
                        .groupMembersList
                        .filter { it.parentGateway == group.parentGateway && it.groupId == group.id && !it.isVirtualMember }
                    val datapoints = SyncHandler
                        .deviceDatapointsList
                        .filter { members.any { member -> member.parentGateway == it.parentGateway && member.deviceId == it.id } }

                    val isGroupCt = datapoints.any { it.key == "mired" }
                    val isGroupRgb = datapoints.any { it.key == "hue" }

                    val groupColourTemperatureMax = datapoints
                        .map { if (it.key == "colourtempmax") it.value as? Int ?: 0 else 0 }
                        .fold(0) { max, element -> if (element > max) element else max }
                    val groupColourTemperatureMin = datapoints
                        .map {
                            if (it.key == "colourtempmin") it.value as? Int ?: 999 else 999
                        }
                        .fold(999) { min, element -> if (element < min) element else min }

                    val action = GroupsFragmentDirections.actionGlobalControlsFragment(
                        groupId = group.id,
                        isRgb = isGroupRgb,
                        isCt = isGroupCt,
                        ctMax = groupColourTemperatureMax,
                        ctMin = groupColourTemperatureMin
                    )
                    try {
                        findNavController().navigate(action)
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Tried to navigate from incorrect destination")
                    } catch (ex: IllegalStateException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Failed to navigate")
                    }

                    return true
                }
            }

            onStateClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val group = getItem(position) ?: return
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val activity = activity ?: return@launch
                        viewModel.toggleOnOff(group, activity)

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            view.performHapticFeedback(CONFIRM)
                        } else {
                            view.performHapticFeedback(CONTEXT_CLICK)
                        }
                    }
                }
            }
        }

        with(binding.rvGroups) {
            adapter = gridAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        NabtoHandler.selectedGatewayLive.observe(viewLifecycleOwner) {
            NabtoHandler.selectedGateway?.let { gateway ->
                binding.tvTitle.text = gateway.name.replace("Gateway Lite ", "")
                groupsLiveData = viewModel.getGroups(gateway)
                datapointsLiveData =
                    viewModel.getDatapoints(gateway)//, arrayOf("onoff", "level", "mired"))
                allDevicesLiveData = viewModel.getDevices(gateway)
                logicCollectionsLiveData = viewModel.getLogicCollection(gateway)
                deviceDatapointsLiveData = viewModel.getDeviceDatapoints(gateway)

                groupsLiveData.removeObservers(viewLifecycleOwner)
                groupsLiveData.observe(viewLifecycleOwner) { groups ->
                    groups?.let {
                        val nonVirtualGroups = groups.toList()
                            .filter { group -> !group.metadata.optBoolean("is_virtual_group") }
                            .sortedBy { it.name }
                        gridAdapter.setGroups(nonVirtualGroups)
                    }
                    viewModel.getGroupMembers(gateway).observe(viewLifecycleOwner) {
                        gridAdapter.setGroupMembers(it)
                    }
                }

                datapointsLiveData.removeObservers(viewLifecycleOwner)
                datapointsLiveData.observe(viewLifecycleOwner) {
                    gridAdapter.setDatapoints(it)
                }

                deviceDatapointsLiveData.removeObservers(viewLifecycleOwner)
                deviceDatapointsLiveData.observe(viewLifecycleOwner) datapointsObserver@{
                    val groupDatapoints = datapointsLiveData.value ?: return@datapointsObserver
                    if (groupDatapoints.isNotEmpty()) {
                        gridAdapter.setDatapoints(groupDatapoints)
                    }
                }

                allDevicesLiveData.removeObservers(viewLifecycleOwner)
                allDevicesLiveData.observe(
                    viewLifecycleOwner
                ) { d ->
                    val activity = activity ?: return@observe

                    val devices = d
                        .toList()
                        .filter { it.parentGateway == gateway.serial }
                    gridAdapter.setDevices(devices)
                    val lights = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                    }
                    val sensors = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.SENSORS
                    }
                    val power = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.POWER
                    }
                    val sockets = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.SOCKETS
                    }
                    val switches = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.SWITCHES
                    }
                    val legacyCollections = SyncHandler
                        .logicCollectionsList
                        .filter { collection ->
                            collection.parentGateway == gateway.serial &&
                                    collection.metadata.collectionType == CollectionType.SCHEDULE &&
                                    collection.metadata.parentSpace == null
                        }

                    val deviceStrings: ArrayList<String> = ArrayList()
                    if (lights.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_lights,
                                lights.size,
                                lights.size
                            )
                        )
                    }
                    if (power.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_power,
                                power.size,
                                power.size
                            )
                        )
                    }
                    if (sockets.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_sockets,
                                sockets.size,
                                sockets.size
                            )
                        )
                    }
                    if (sensors.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_sensors,
                                sensors.size,
                                sensors.size
                            )
                        )
                    }
                    if (switches.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_switches,
                                switches.size,
                                switches.size
                            )
                        )
                    }
                    if (legacyCollections.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_legacy_schedules,
                                legacyCollections.size,
                                legacyCollections.size
                            )
                        )
                    }

                    activity.runOnUiThread {
                        binding.tvDevices.text = deviceStrings.joinToString(" | ")
                    }
                }

                logicCollectionsLiveData.removeObservers(viewLifecycleOwner)
                logicCollectionsLiveData.observe(
                    viewLifecycleOwner
                ) { collections ->
                    val activity = activity ?: return@observe

                    val devices =
                        SyncHandler.devicesList.filter { it.parentGateway == gateway.serial }
                    val lights = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                    }
                    val sensors = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.SENSORS
                    }
                    val power = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.POWER
                    }
                    val sockets = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.SOCKETS
                    }
                    val switches = devices.filter {
                        it.getDeviceCategory() == Device.DeviceCategory.SWITCHES
                    }
                    val legacyCollections = collections.filter { collection ->
                        collection.metadata.collectionType == CollectionType.SCHEDULE &&
                                collection.metadata.parentSpace == null
                    }

                    val deviceStrings: ArrayList<String> = ArrayList()
                    if (lights.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_lights,
                                lights.size,
                                lights.size
                            )
                        )
                    }
                    if (sensors.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_sensors,
                                sensors.size,
                                sensors.size
                            )
                        )
                    }
                    if (power.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_power,
                                power.size,
                                power.size
                            )
                        )
                    }
                    if (sockets.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_sockets,
                                sockets.size,
                                sockets.size
                            )
                        )
                    }
                    if (switches.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_switches,
                                switches.size,
                                switches.size
                            )
                        )
                    }
                    if (legacyCollections.isNotEmpty()) {
                        deviceStrings.add(
                            activity.resources.getQuantityString(
                                R.plurals.number_legacy_schedules,
                                legacyCollections.size,
                                legacyCollections.size
                            )
                        )
                    }

                    activity.runOnUiThread {
                        binding.tvDevices.text = deviceStrings.joinToString(" | ")
                    }
                }
            }
        }

        binding.viewAllDevicesCard.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                GroupsFragmentDirections.actionGroupsFragmentToAllDevicesFragment()
            )
        )

        binding.swipeLayout.setOnRefreshListener {
            val gateway = NabtoHandler.selectedGateway ?: return@setOnRefreshListener
            binding.swipeLayout.isRefreshing = true

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                if (!gateway.isConnected) {
                    val credentials = CloudHandler.getCredentials()
                    if (credentials.first.isEmpty()) {
                        activity?.finishAffinity()
                        startActivity(Intent(context, SplashscreenActivity::class.java))
                    }
                    NabtoHandler.openTunnel(gateway, credentials.first)
                    return@launch
                }

                try {
                    SyncHandler.syncGroups(gateway, force = true)
                    SyncHandler.groupsList.firstOrNull()?.let { group ->
                        SyncHandler.syncGroupDatapoints(gateway, group, force = true)
                    }
                    SyncHandler.syncDevices(gateway, force = true)
                    SyncHandler.syncDeviceDatapoints(gateway, force = true)

                    viewModel.viewModelScope.launch(Dispatchers.Main) {
                        binding.swipeLayout?.isRefreshing = false
                    }
                } catch (err: VolleyError) {
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
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.create_group -> {
                val action = GroupsFragmentDirections.actionGroupsFragmentToNoGroupsFragment()
                findNavController().navigate(action)
                true
            }
            R.id.help -> {
                val action = GroupsFragmentDirections.actionGroupsFragmentToHelp()
                findNavController().navigate(action)
                true
            }
            R.id.sign_out -> {
                signOut()

                activity?.startActivity(Intent(activity, LoginActivity::class.java))
                activity?.finishAffinity()

                true
            }
            else -> false
        }
    }

    private fun checkTimezone() {
        val activity = activity ?: return
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            NabtoHandler.selectedGateway?.let { gateway ->
                gateway.isConnectedLiveData.observe(activity) {
                    if (!it) return@observe

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val metadata = SyncHandler
                            .getHubMetadata(gateway) ?: return@launch
                        val metadataTimeZone = metadata
                            .optString("timezone")
                        val phoneTimeZone = Calendar
                            .getInstance()
                            .timeZone
                            .id
                        val gwTimeZone = try {
                            DevelcoHandler
                                .getTime(gateway)
                                .optJSONObject("body")
                                ?.optString("timezone") ?: ""
                        } catch (ex: Exception) {
                            crashlytics.recordException(ex)
                            ""
                        }

                        if (!metadataTimeZone.isNullOrBlank()) {
                            if (metadataTimeZone != phoneTimeZone) {
                                val activity = activity ?: return@launch
                                activity.runOnUiThread {
                                    activity.runOnUiThread {
                                        if (!activity.isFinishing) {
                                            AlertDialog.Builder(activity)
                                                .setMessage(
                                                    getString(
                                                        R.string.different_timezone,
                                                        metadataTimeZone,
                                                        phoneTimeZone
                                                    )
                                                )
                                                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                                                        try {
                                                            DevelcoHandler.putTime(
                                                                gateway,
                                                                JSONObject().put(
                                                                    "timezone",
                                                                    phoneTimeZone
                                                                )
                                                            )
                                                            metadata.put("timezone", phoneTimeZone)
                                                            DevelcoHandler.putDevice(
                                                                gateway,
                                                                0,
                                                                JSONObject()
                                                                    .put(
                                                                        "metadata",
                                                                        metadata.toString()
                                                                    )
                                                            )
                                                        } catch (err: VolleyError) {
                                                            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                                                gateway.isConnected = false
                                                                val credentials =
                                                                    CloudHandler.getCredentials()
                                                                if (credentials.first.isEmpty()) {
                                                                    activity.finishAffinity()
                                                                    startActivity(
                                                                        Intent(
                                                                            context,
                                                                            SplashscreenActivity::class.java
                                                                        )
                                                                    )
                                                                }
                                                                NabtoHandler.openTunnel(
                                                                    gateway,
                                                                    credentials.first
                                                                )
                                                            }
                                                            err.printStackTrace()
                                                        }
                                                    }
                                                }
                                                .setNegativeButton(getString(R.string.no)) { _, _ ->
                                                    if (metadataTimeZone != gwTimeZone) {
                                                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                                                            try {
                                                                DevelcoHandler.putTime(
                                                                    gateway,
                                                                    JSONObject().put(
                                                                        "timezone",
                                                                        metadataTimeZone
                                                                    )
                                                                )
                                                            } catch (err: VolleyError) {
                                                                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                                                    gateway.isConnected = false
                                                                    val credentials =
                                                                        CloudHandler.getCredentials()
                                                                    if (credentials.first.isEmpty()) {
                                                                        activity.finishAffinity()
                                                                        startActivity(
                                                                            Intent(
                                                                                context,
                                                                                SplashscreenActivity::class.java
                                                                            )
                                                                        )
                                                                    }
                                                                    NabtoHandler.openTunnel(
                                                                        gateway,
                                                                        credentials.first
                                                                    )
                                                                }
                                                                err.printStackTrace()
                                                            }
                                                        }
                                                    }
                                                }
                                                .create()
                                                .show()
                                        }
                                    }
                                }
                            }

                            if (metadataTimeZone != gwTimeZone) {
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    try {
                                        DevelcoHandler.putTime(
                                            gateway,
                                            JSONObject().put("timezone", metadataTimeZone)
                                        )
                                    } catch (err: VolleyError) {
                                        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                            gateway.isConnected = false
                                            val credentials = CloudHandler.getCredentials()
                                            if (credentials.first.isEmpty()) {
                                                activity?.finishAffinity()
                                                startActivity(
                                                    Intent(
                                                        context,
                                                        SplashscreenActivity::class.java
                                                    )
                                                )
                                            }
                                            NabtoHandler.openTunnel(gateway, credentials.first)
                                        }
                                        err.printStackTrace()
                                    }
                                }
                            }
                        } else {
                            try {
                                DevelcoHandler.putTime(
                                    gateway,
                                    JSONObject().put("timezone", phoneTimeZone)
                                )
                                metadata.put("timezone", phoneTimeZone)
                                DevelcoHandler.putDevice(
                                    gateway,
                                    0,
                                    JSONObject()
                                        .put("metadata", metadata.toString())
                                )
                            } catch (err: VolleyError) {
                                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                    gateway.isConnected = false
                                    val credentials = CloudHandler.getCredentials()
                                    if (credentials.first.isEmpty()) {
                                        activity?.finishAffinity()
                                        startActivity(
                                            Intent(
                                                context,
                                                SplashscreenActivity::class.java
                                            )
                                        )
                                    }
                                    NabtoHandler.openTunnel(gateway, credentials.first)
                                }
                                err.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
