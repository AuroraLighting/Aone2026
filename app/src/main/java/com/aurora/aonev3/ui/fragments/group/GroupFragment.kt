package com.aurora.aonev3.ui.fragments.group

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.ui.ColourScenes
import com.aurora.aonev3.ui.IconsScenes
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.activities.TourActivity
import com.aurora.aonev3.ui.fragments.dynamicevents.DynamicEventViewModel
import com.aurora.aonev3.ui.fragments.schedules.ScheduleViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.aurora.aonev3.synthetic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val ARG_GROUP_ID = "groupId"

class GroupFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    companion object {
        private const val TAG = "GroupFragment"
        fun newInstance(groupId: Int) = GroupFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_GROUP_ID, groupId)
            }
        }
    }

    private val crashlytics = FirebaseCrashlytics.getInstance()

    private val viewModel: GroupViewModel by activityViewModels()

    private lateinit var mGroup: Group
    private var mGroupOnOff = false
    private var mIsGroupRgb = false
    private var mIsGroupCt = false
    private var mGroupColourTemperatureMax = 0
    private var mGroupColourTemperatureMin = 999
    private var mScenes: List<Scene> = ArrayList()
    private var mSchedules: List<LogicCollection> = ArrayList()

    private var mIsScenes = false

    private val args: GroupFragmentArgs by navArgs()
    private val showScenes: Boolean by lazy { args.showScenes }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gateway = NabtoHandler.selectedGateway ?: return
        if (!this::mGroup.isInitialized) {
            findNavController().popBackStack(R.id.groupsFragment, false)
            return
        }

        mIsScenes = false

        refreshSpaceDevices(gateway)

        if (mGroup.metadata.optBoolean("is_virtual_group") || !showScenes) {
            fabScenes.visibility = View.GONE
        }

        if (!showScenes) {
            fabHome.visibility = View.GONE
        }

        if (!SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean(
                "groupTourDone",
                false
            )
        ) {
            val intent = Intent(activity, TourActivity::class.java)
            intent.putExtra("tour", "group")
            startActivity(intent)
        }

        menu.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.group_menu, popup.menu)
            if (mGroup.metadata.optBoolean("is_virtual_group") || mScenes.size >= 5) {
                popup.menu.removeItem(R.id.create_scene)
            }
            if (mGroup.metadata.optBoolean("is_virtual_group")) {
                popup.menu.findItem(R.id.renameGroup).title = getString(R.string.rename_group)
                popup.menu.findItem(R.id.add_nested_groups).title =
                    getString(R.string.manage_nested_groups)
                popup.menu.findItem(R.id.delete_group).title = getString(R.string.delete_group)
            }
            if (!OtaHandler.isDynamicEventsAvailable) {
                popup.menu.removeItem(R.id.create_dynamic_event)
            }
            popup.setOnMenuItemClickListener(this@GroupFragment)
            popup.show()
        }

        val lightsAdapter = GroupRecyclerViewAdapter(requireActivity()).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val item = getItem(position) ?: return

                    val device = item.device
                    val group = item.group
                    val schedule = item.schedule

                    when {
                        device != null -> {
                            if (item.type != GroupRecyclerViewAdapter.GroupRecyclerViewType.SWITCHES && item.type != GroupRecyclerViewAdapter.GroupRecyclerViewType.SENSORS) {
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    viewModel.toggleOnOff(device)

                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    } else {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    }
                                }
                            } else {
                                val activity = activity ?: return

                                if (!activity.isFinishing) {
                                    AlertDialog.Builder(activity)
                                        .setMessage(getString(R.string.set_up_devices_warning))
                                        .setPositiveButton(R.string.ok, null)
                                        .create()
                                        .show()
                                }
                            }
                        }
                        group != null -> {
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                viewModel.toggleOnOff(group)

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                            }
                        }
                        schedule != null -> {
                            val action =
                                GroupFragmentDirections.actionGroupFragmentToScheduleFragment(
                                    schedule,
                                    mGroup
                                )
                            findNavController().navigate(action)
                        }
                    }
                }
            }

            onItemLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val device = getItem(position)?.device
                    val group = getItem(position)?.group

                    if (device != null) {

                        if (device.getDeviceCategory() == Device.DeviceCategory.LIGHTS ||
                            device.deviceClass == Device.DeviceClass.AURORAWALLDIMMER
                        ) {
                            val action =
                                GroupFragmentDirections.actionGlobalControlsFragment(deviceId = device.id)
                            findNavController().navigate(action)
                        } else if (device.getDeviceCategory() == Device.DeviceCategory.POWER) {
                            val action =
                                GroupFragmentDirections.actionGroupFragmentToPowerFragment(device.id)
                            findNavController().navigate(action)
                        } else if (device.getDeviceCategory() == Device.DeviceCategory.SOCKETS) {
                            val action =
                                GroupFragmentDirections.actionGroupFragmentToDoubleSocketPowerFragment(
                                    device.id
                                )
                            findNavController().navigate(action)
                        }

                        return true
                    } else if (group != null) {
                        val action = GroupFragmentDirections.actionGroupFragmentSelf(group.id)
                        findNavController().navigate(action)
                    }

                    return false
                }
            }

            onOfflineClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val activity = activity ?: return
                    if (!activity.isFinishing) {
                        AlertDialog.Builder(activity)
                            .setTitle(getString(R.string.device_unavailable))
                            .setMessage(getString(R.string.device_unavailable_body))
                            .setPositiveButton(getString(R.string.ok), null)
                            .create()
                            .show()
                    }
                }
            }

            onLeftSocketClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val device = getItem(position)?.device ?: return
                    if (device.deviceClass != Device.DeviceClass.AURORADUALSOCKET) return

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        viewModel.toggleOnOff(device, "socket1")

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                    }
                }
            }

            onRightSocketClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val device = getItem(position)?.device ?: return
                    if (device.deviceClass != Device.DeviceClass.AURORADUALSOCKET) return

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        viewModel.toggleOnOff(device, "socket2")

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                    }
                }
            }

            onLeftSocketLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val device = getItem(position)?.device ?: return false
                    if (device.deviceClass != Device.DeviceClass.AURORADUALSOCKET) return false

                    viewModel.toggleLock(device, "lock")

                    return true
                }
            }

            onRightSocketLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val device = getItem(position)?.device ?: return false
                    if (device.deviceClass != Device.DeviceClass.AURORADUALSOCKET) return false

                    viewModel.toggleLock(device, "lock2")

                    return true
                }
            }
        }

        with(recyclerView) {
            adapter = lightsAdapter
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 12, RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = GroupSpanSizeLookup(lightsAdapter, spanCount, requireContext())
            }

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        var nestedGroups = emptyList<Group>()
        viewModel.getAllGroups(gateway).observe(viewLifecycleOwner) { groups ->
            mGroup =
                groups?.toList()?.find { it.parentGateway == gateway.serial && it.id == mGroup.id }
                    ?: return@observe
            tvTitle.text = mGroup.name

            val nestedGroupIds =
                mGroup.metadata.optJSONArray("nested_groups")?.toIntArray() ?: intArrayOf()
            nestedGroups = SyncHandler
                .groupsList
                .filter { group ->
                    group.parentGateway == gateway.serial
                            && group.id in nestedGroupIds
                }
                .sortedBy { it.name }

            lightsAdapter.setGroups(nestedGroups, mGroup)
        }
        val membersLiveData = viewModel.getMembers(gateway)
        val scenesLiveData = viewModel.getScenes(gateway, mGroup)
        val groupDatapointsLiveData = viewModel.getGroupDatapoints(gateway)
        val memberDatapointsLiveData = viewModel.getAllDeviceDatapoints(gateway)
        val logicCollectionsLiveData = viewModel.getLogicCollections(gateway)
        val logicRulesLiveData = viewModel.getLogicRules(gateway)
        var memberIds = emptyList<Int>()

        var isControlsOut = false

        membersLiveData.observe(viewLifecycleOwner) {
            memberIds = it
                ?.toList()
                ?.filter { member ->
                    member.parentGateway == mGroup.parentGateway && member.groupId == mGroup.id
                }
                ?.map { member ->
                    member.deviceId
                } ?: return@observe
            val devices =
                SyncHandler
                    .devicesList
                    .filter { device -> device.parentGateway == mGroup.parentGateway && device.id in memberIds }
                    .sortedBy { device -> device.name }

            if (devices.isEmpty()) {
                refreshSpaceDevices(gateway)
                return@observe
            }
            val nestedDevices = findNestedDevices(nestedGroups)

            memberDatapointsLiveData.postValue(memberDatapointsLiveData.value)

            val lights = devices
                .filter { device ->
                    device !in nestedDevices && device.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                }
            val sensors = devices
                .filter { device ->
                    device !in nestedDevices && device.getDeviceCategory() == Device.DeviceCategory.SENSORS
                }
            val power = devices
                .filter { device ->
                    device !in nestedDevices && device.getDeviceCategory() == Device.DeviceCategory.POWER
                }
            val sockets = devices
                .filter { device ->
                    device !in nestedDevices && device.getDeviceCategory() == Device.DeviceCategory.SOCKETS
                }
                .distinctBy { socket -> socket.id }
            val switches = devices
                .filter { device ->
                    device !in nestedDevices && device.getDeviceCategory() == Device.DeviceCategory.SWITCHES
                }
            val devicesList =
                ArrayList<Pair<GroupRecyclerViewAdapter.GroupDataType, List<Device>>>()

            if (lights.isNotEmpty()) {
                devicesList.add(Pair(GroupRecyclerViewAdapter.GroupDataType.LIGHTS, lights))
            }
            if (power.isNotEmpty()) {
                devicesList.add(Pair(GroupRecyclerViewAdapter.GroupDataType.POWER, power))
            }
            if (sockets.isNotEmpty()) {
                devicesList.add(Pair(GroupRecyclerViewAdapter.GroupDataType.SOCKETS, sockets))
            }
            if (sensors.isNotEmpty()) {
                devicesList.add(Pair(GroupRecyclerViewAdapter.GroupDataType.SENSORS, sensors))
            }
            if (switches.isNotEmpty()) {
                devicesList.add(Pair(GroupRecyclerViewAdapter.GroupDataType.SWITCHES, switches))
            }

            lightsAdapter.setDevices(devicesList)
        }

        memberDatapointsLiveData.observe(viewLifecycleOwner) { dp ->
            val datapoints = dp.toList().filter { datapoint ->
                datapoint.parentGateway == gateway.serial
                        && datapoint.id in memberIds
                        && datapoint.key in arrayOf(
                    "onoff",
                    "level",
                    "mired",
                    "hue",
                    "colourtempmin",
                    "colourtempmax",
                    "lock",
                    "lock2"
                )
            }
            lightsAdapter.setDeviceDatapoints(datapoints)

            if (datapoints.any { datapoint -> datapoint.key == "hue" }) {
                mIsGroupRgb = true
            }

            if (datapoints.any { datapoint -> datapoint.key == "mired" }) {
                mIsGroupCt = true
            }

            mGroupColourTemperatureMax = datapoints
                .map { datapoint ->
                    if (datapoint.key == "colourtempmax") datapoint.value as? Int ?: 0 else 0
                }
                .fold(0) { max, element -> if (element > max) element else max }
            mGroupColourTemperatureMin = datapoints
                .map { datapoint ->
                    if (datapoint.key == "colourtempmin") datapoint.value as? Int ?: 999 else 999
                }
                .fold(999) { min, element -> if (element < min) element else min }
        }

        val sceneFabs = arrayOf(fabScene1, fabScene2, fabScene3, fabScene4, fabScene5)

        scenesLiveData.observe(viewLifecycleOwner) {
            val activity = activity ?: return@observe
            it?.let {
                mScenes = it
                    .toList()
                    .filter { scene ->
                        scene.parentGateway == mGroup.parentGateway
                                && scene.groupId == mGroup.id
                    }
                    .sortedBy { scene -> scene.id }

                if (mScenes.isEmpty() && !allowEditing()) {
                    fabScenes.visibility = View.GONE
                }

                for ((index, scene) in mScenes.withIndex()) {
                    if (index !in sceneFabs.indices) continue
                    val icon = IconsScenes.fromString(scene.metadata.optString("icon"))
                    var colour = scene.metadata.optString("icon_colour")

                    if (colour == "") {
                        colour = ColourScenes.DEFAULT.stringValue
                    } else if (!colour.startsWith("#")) {
                        colour = "#$colour"
                    }

                    val colourStateList = try {
                        ColorStateList.valueOf(Color.parseColor(colour))
                    } catch (ex: Exception) {
                        ColorStateList.valueOf(Color.parseColor(ColourScenes.DEFAULT.stringValue))
                    }

                    sceneFabs[index].setImageDrawable(
                        if (icon != IconsScenes.NULL) {
                            ContextCompat.getDrawable(activity, icon.resourceValue)
                        } else {
                            null
                        }
                    )

                    icon.let {
                        sceneFabs[index].backgroundTintList = colourStateList
                        sceneFabs[index].imageTintList = null
                    }
                }
            }
        }

        groupDatapointsLiveData.observe(viewLifecycleOwner) {
            val activity = activity ?: return@observe
            val allDatapoints = it.toList()

            lightsAdapter.setGroupDatapoints(allDatapoints)

            val datapoints = allDatapoints.filter { datapoint ->
                datapoint.parentGateway == mGroup.parentGateway
                        && datapoint.id == mGroup.id
                        && datapoint.key == "onoff"
            }
            if (datapoints.isNotEmpty()) {
                mGroupOnOff = datapoints[0].value as? Boolean ?: false

                activity.runOnUiThread {
                    if (!mGroupOnOff) {
                        fabPower.backgroundTintList =
                            resources.getColorStateList(R.color.colorTileInactive, null)
                        fabPower.setImageDrawable(
                            ContextCompat.getDrawable(
                                activity,
                                R.drawable.ic_power_active
                            )
                        )

                        if (!isControlsOut) {
                            isControlsOut = true
                            showOutY(fabControls, 1)
                        }
                    } else {
                        fabPower.backgroundTintList =
                            resources.getColorStateList(R.color.colorTileActive, null)
                        fabPower.setImageDrawable(
                            ContextCompat.getDrawable(
                                activity,
                                R.drawable.ic_power_inactive
                            )
                        )

                        if (isControlsOut) {
                            isControlsOut = false
                            showInY(fabControls, 1)
                        }
                    }
                }
            }
        }

        logicCollectionsLiveData.observe(viewLifecycleOwner) {
            it?.let {
                mSchedules = it.toList().filter { logicCollection ->
                    (logicCollection.metadata.collectionType == CollectionType.SCHEDULE
                            || logicCollection.metadata.collectionType == CollectionType.DYNAMIC_EVENT)
                            && logicCollection.metadata.parentSpace == mGroup.id
                }

                lightsAdapter.setLogicCollections(mSchedules)
                lightsAdapter.setLogicRules(SyncHandler
                    .logicRulesList
                    .filter { logicRule -> logicRule.parentGateway == gateway.serial })
            }
        }

        logicRulesLiveData.observe(viewLifecycleOwner) { logicRules ->
            logicRules?.let { _ ->
                val rules =
                    logicRules.toList()
                        .filter { it.parentGateway == mGroup.parentGateway && it.logicCollectionId in mSchedules.map { logicCollection -> logicCollection.id } }

                lightsAdapter.setLogicRules(rules)
            }
        }

        sceneFabs.forEachIndexed { i, v ->
            init(v, i + 1)
        }

        with(fabScenes) {
            val r = requireActivity().resources
            val dp = 16 * r.displayMetrics.density

            setOnClickListener {
                if (mScenes.isNotEmpty()) {
                    mIsScenes = !mIsScenes

                    if (mIsScenes) {
                        backgroundTintList =
                            resources.getColorStateList(R.color.colorTileActive, null)
                        foreground = ContextCompat.getDrawable(
                            requireActivity(),
                            R.drawable.ic_scenes_active
                        )

                        animate()
                            .setDuration(400)
                            .translationX(this.height.toFloat() + dp)
                            .setListener(null)
                            .start()
                        fabPower.animate()
                            .setDuration(400)
                            .translationX(fabPower.height.toFloat() + dp)
                            .setListener(null)
                            .start()
                        fabControls.animate()
                            .setDuration(400)
                            .translationX(fabPower.height.toFloat() + dp)
                            .setListener(null)
                            .start()

                        showInXY(fabScene1, 1)
                        if (mScenes.size > 1 || (mScenes.size == 1 && allowEditing())) {
                            showInXY(fabScene2, 2)
                            if (mScenes.size > 2 || (mScenes.size == 2 && allowEditing())) {
                                showInXY(fabScene3, 3)
                                if (mScenes.size > 3 || (mScenes.size == 3 && allowEditing())) {
                                    showInXY(fabScene4, 4)
                                    if (mScenes.size > 4 || (mScenes.size == 4 && allowEditing())) {
                                        showInXY(fabScene5, 5)
                                    }
                                }
                            }
                        }
                    } else {
                        backgroundTintList =
                            resources.getColorStateList(R.color.colorTileInactive, null)
                        foreground =
                            ContextCompat.getDrawable(requireActivity(), R.drawable.ic_scenes)

                        animate()
                            .setDuration(400)
                            .translationX(0f)
                            .setListener(null)
                            .start()
                        fabPower.animate()
                            .setDuration(400)
                            .translationX(0f)
                            .setListener(null)
                            .start()
                        fabControls.animate()
                            .setDuration(400)
                            .translationX(0f)
                            .setListener(null)
                            .start()

                        showOutXY(fabScene1, 1)
                        showOutXY(fabScene2, 2)
                        if (mScenes.size > 1) {
                            showOutXY(fabScene3, 3)
                            if (mScenes.size > 2) {
                                showOutXY(fabScene4, 4)
                                if (mScenes.size > 3) {
                                    showOutXY(fabScene5, 5)
                                }
                            }
                        }

                    }
                } else {
                    if (allowEditing()) {
                        triggerCreateNewScene()
                    }
                }
            }
        }

        with(fabPower) {
            setOnClickListener {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    viewModel.toggleOnOff(mGroup, mGroupOnOff)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
            }
        }

        fabControls.setOnClickListener {
            val action = GroupFragmentDirections.actionGlobalControlsFragment(
                groupId = mGroup.id,
                isRgb = mIsGroupRgb,
                isCt = mIsGroupCt,
                ctMax = mGroupColourTemperatureMax,
                ctMin = mGroupColourTemperatureMin
            )
            findNavController().navigate(action)
        }

        with(fabHome) {
            setOnClickListener {
                findNavController().popBackStack(R.id.groupsFragment, false)
            }
        }

        sceneFabs.forEachIndexed { i, fab ->
            with(fab) {
                setOnClickListener {
                    val scene = mScenes.sortedBy { it.id }.getOrNull(i)
                    if (scene == null) {
                        if (allowEditing()) {
                            triggerCreateNewScene()
                        }
                    } else {
                        try {
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                viewModel.triggerScene(scene)

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            crashlytics.recordException(ex)
                        }
                    }
                }
                setOnLongClickListener {
                    val scene = mScenes.sortedBy { it.id }.getOrNull(i)
                    if (scene == null) {
                        triggerCreateNewScene()
                    } else {
                        try {
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                viewModel.triggerScene(scene)
                                triggerCreateNewScene(scene)

                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            crashlytics.recordException(ex)
                        }
                    }

                    true
                }
            }
        }

        swipeLayout.setOnRefreshListener {
            swipeLayout.isRefreshing = true

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
                    SyncHandler.reportGroupStates(gateway, mGroup)
                    SyncHandler.syncDevices(gateway, force = true)
                    SyncHandler.syncGroupMembers(gateway, mGroup, force = true)
                    SyncHandler.syncGroups(gateway, force = true)
                    SyncHandler.syncDeviceDatapoints(gateway, force = true)
                    SyncHandler.syncGroupDatapoints(gateway, mGroup, force = true)
                    SyncHandler.syncLogicCollectionsCached(gateway, force = true)
                    SyncHandler.syncLogicRulesAndTimersCached(gateway, force = true)
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

                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    swipeLayout?.isRefreshing = false
                }
            }
        }

        if (!allowEditing()) {
            menu.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var group: Group? = null
        arguments?.let {
            NabtoHandler.selectedGateway?.let { gateway ->
                group = SyncHandler
                    .groupsList
                    .find { g -> g.parentGateway == gateway.serial && g.id == it.getInt(ARG_GROUP_ID) }
            }
        }
        if (group == null) {
            return
        }
        mGroup = group ?: throw Exception("Invalid group")
        viewModel.selectedGroup.postValue(mGroup)

        viewModel.reportStates()
    }

    private fun refreshSpaceDevices(gateway: NabtoHandler.NabtoGateway) {
        if (!this::mGroup.isInitialized) return

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!gateway.isConnected) {
                    val credentials = CloudHandler.getCredentials()
                    if (credentials.first.isNotEmpty()) {
                        NabtoHandler.openTunnel(gateway, credentials.first)
                    }
                }

                if (!gateway.isConnected) return@launch

                SyncHandler.syncGroups(gateway, force = true)
                SyncHandler.syncDevices(gateway, force = true)
                SyncHandler.syncGroupMembers(gateway, mGroup, force = true)
                SyncHandler.syncDeviceDatapoints(gateway, force = true)
                SyncHandler.syncGroupDatapoints(gateway, mGroup, force = true)
                SyncHandler.syncLogicCollectionsCached(gateway, force = true)
                SyncHandler.syncLogicRulesAndTimersCached(gateway, force = true)
            } catch (err: VolleyError) {
                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                    gateway.isConnected = false
                }
                err.printStackTrace()
                crashlytics.recordException(err)
            } catch (ex: Exception) {
                ex.printStackTrace()
                crashlytics.recordException(ex)
            }
        }
    }

    private fun findNestedDevices(groups: List<Group>): List<Device> {
        val gateway = NabtoHandler.selectedGateway ?: return emptyList()
        val groupIds = groups.toList().map { it.id }
        val groupMembers = SyncHandler
            .groupMembersList
            .filter {
                it.parentGateway == gateway.serial && !it.isVirtualMember && it.groupId in groupIds
            }
        val deviceIds = groupMembers.map { it.deviceId }
        return SyncHandler
            .devicesList
            .filter { it.parentGateway == gateway.serial && it.id in deviceIds }
    }

    private fun showInXY(v: View, i: Int) {
        val r = activity?.resources
        val dp = 16 * (r?.displayMetrics?.density ?: 0f)

        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.translationY = i * v.height.toFloat()
        v.animate()
            .setDuration(400)
            .translationX(fabPower.height.toFloat() + dp)
            .translationY(0f)
            .setListener(object : AnimatorListenerAdapter() {})
            .alpha(1f)
            .start()
    }

    private fun showOutXY(v: View, i: Int) {
        v.visibility = View.VISIBLE
        v.alpha = 1f
        v.translationY = 0f
        v.animate()
            .setDuration(400)
            .translationY(i * v.height.toFloat())
            .translationX(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.GONE
                    super.onAnimationEnd(animation)
                }
            }).alpha(0f)
            .start()
    }

    private fun showInY(v: View, i: Int) {
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.translationY = i * v.height.toFloat()
        v.animate()
            .setDuration(400)
            .translationY(0f)
            .setListener(object : AnimatorListenerAdapter() {})
            .alpha(1f)
            .start()
    }

    private fun showOutY(v: View, i: Int) {
        v.visibility = View.VISIBLE
        v.alpha = 1f
        v.translationY = 0f
        v.animate()
            .setDuration(400)
            .translationY(i * v.height.toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.GONE
                    super.onAnimationEnd(animation)
                }
            }).alpha(0f)
            .start()
    }

    private fun init(v: View, i: Int) {
        v.translationY = 0f
        v.animate()
            .setDuration(1)
            .translationY(i * v.height.toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.GONE
                    super.onAnimationEnd(animation)
                }
            }).alpha(0f)
            .start()
    }

    var triggeredCreateNewScene = false
    private fun triggerCreateNewScene(scene: Scene? = null) {
        if (triggeredCreateNewScene) return
        triggeredCreateNewScene = true

        val action = GroupFragmentDirections.actionGroupFragmentToNewSceneFragment(
            mGroup.id,
            scene?.id ?: -1,
            mIsGroupRgb,
            mIsGroupCt,
            mGroupColourTemperatureMax,
            mGroupColourTemperatureMin
        )
        activity?.runOnUiThread {
            try {
                findNavController().navigate(action)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
            }
            triggeredCreateNewScene = false
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.renameGroup -> {
                val action =
                    GroupFragmentDirections.actionGroupFragmentToRenameGroupFragment(mGroup.id)
                findNavController().navigate(action)
                true
            }
            R.id.addDevicesFragment -> {
                val action =
                    GroupFragmentDirections.actionGroupFragmentToAddDevicesFragment(mGroup.id)
                findNavController().navigate(action)
                true
            }
            R.id.add_nested_groups -> {
                val action =
                    GroupFragmentDirections.actionGroupFragmentToAddNestedGroupsFragment(mGroup.id)
                findNavController().navigate(action)
                true
            }
            R.id.create_scene -> {
                if (mScenes.size < 5) {
                    triggerCreateNewScene()
                } else {
                    Toast.makeText(context, getString(R.string.max_scenes), Toast.LENGTH_SHORT)
                        .show()
                }
                true
            }
            R.id.create_schedule -> {
                menu.isClickable = false
                NabtoHandler.selectedGateway?.let { gateway ->
                    val existingLogicCollections = SyncHandler
                        .logicCollectionsList
                        .filter {
                            it.parentGateway == gateway.serial &&
                                    it.metadata.parentSpace == mGroup.id &&
                                    it.metadata.collectionType == CollectionType.SCHEDULE
                        }

                    if (existingLogicCollections.isNotEmpty()) {
                        val action = GroupFragmentDirections.actionGroupFragmentToScheduleFragment(
                            existingLogicCollections.first(),
                            mGroup
                        )
                        menu.isClickable = true
                        findNavController().navigate(action)
                    } else {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            val response = try {
                                DevelcoHandler.postLogicCollection(
                                    gateway, JSONObject()
                                        .put(
                                            "name",
                                            String.format(
                                                getString(R.string.schedule_name),
                                                mGroup.name
                                            )
                                        )
                                        .put(
                                            "metadata",
                                            JSONObject().put("collection_type", "schedule")
                                                .put("parent_space", mGroup.id).toString()
                                        )
                                )
                            } catch (err: VolleyError) {
                                App.actionFailed()
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
                                JSONObject()
                            }


                            viewModel.viewModelScope.launch(Dispatchers.Main) mainLaunch@{
                                val logicCollectionId =
                                    response.optJSONObject("body")?.optInt("id")
                                        ?: return@mainLaunch

                                val logicCollections = viewModel.getLogicCollections(gateway)
//                                    viewModel.getLogicCollection(gateway, logicCollectionId)
                                view?.let {
                                    logicCollections.observe(viewLifecycleOwner) { collections ->
                                        val logicCollection = collections?.toList()
                                            ?.find { it.parentGateway == gateway.serial && it.id == logicCollectionId }
                                        logicCollection?.let {
                                            logicCollections.removeObservers(viewLifecycleOwner)
                                            activity?.runOnUiThread {
                                                val action =
                                                    GroupFragmentDirections.actionGroupFragmentToScheduleFragment(
                                                        it,
                                                        mGroup
                                                    )
                                                try {
                                                    menu.isClickable = true
                                                    findNavController().navigate(action)
                                                } catch (ex: IllegalArgumentException) {
                                                    ex.printStackTrace()
                                                    Log.e(
                                                        TAG,
                                                        "Tried to navigate from incorrect destination"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                true
            }
            R.id.create_dynamic_event -> {
                menu.isClickable = false
                val gateway = NabtoHandler.selectedGateway ?: return false

                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val logicCollectionId = try {
                        viewModel.createDynamicEventCollection()
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
                        menu.isClickable = true
                        null
                    } ?: return@launch

                    withContext(Dispatchers.Main) {
                        val logicCollections = viewModel.getLogicCollections(gateway)

                        logicCollections.observe(viewLifecycleOwner) { collections ->
                            val logicCollection = collections
                                ?.toList()
                                ?.firstOrNull {
                                    it.parentGateway == gateway.serial
                                            && it.id == logicCollectionId
                                } ?: return@observe

                            logicCollections.removeObservers(viewLifecycleOwner)

                            activity?.runOnUiThread {
                                val action = GroupFragmentDirections
                                    .actionGroupFragmentToScheduleFragment(logicCollection, mGroup)
                                try {
                                    menu.isClickable = true
                                    findNavController().navigate(action)
                                } catch (ex: IllegalArgumentException) {
                                    ex.printStackTrace()
                                    Log.e(TAG, "Tried to navigate from incorrect destination")
                                }
                            }
                        }
                    }
                }

                true
            }
            R.id.delete_group -> {
                activity?.let {
                    if (!it.isFinishing) {
                        AlertDialog.Builder(it)
                            .setMessage(getString(R.string.delete_confirmation, mGroup.name))
                            .setPositiveButton(R.string.yes) { _, _ ->
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    viewModel.deleteGroup(mGroup)
                                    activity?.runOnUiThread {
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

                true
            }
            else -> false
        }
    }
}

class GroupSpanSizeLookup(
    private val adapter: GroupRecyclerViewAdapter,
    private val spanCount: Int,
    private val context: Context
) : GridLayoutManager.SpanSizeLookup() {
    override fun getSpanSize(position: Int): Int {
        val isTablet = context.resources.getBoolean(R.bool.isTablet)
        val isLandscape = context.resources.getBoolean(R.bool.isLandscape)
        return if (adapter.getItemViewType(position) == GroupRecyclerViewAdapter.GroupRecyclerViewType.SECTION.ordinal ||
            adapter.getItemViewType(position) == GroupRecyclerViewAdapter.GroupRecyclerViewType.SCHEDULES.ordinal ||
            adapter.getItemViewType(position) == GroupRecyclerViewAdapter.GroupRecyclerViewType.DYNAMIC_EVENTS.ordinal
        ) {
            spanCount / 1
        } else if (adapter.getItemViewType(position) == GroupRecyclerViewAdapter.GroupRecyclerViewType.SOCKETS.ordinal ||
            adapter.getItemViewType(position) == GroupRecyclerViewAdapter.GroupRecyclerViewType.NESTED_GROUPS.ordinal
        ) {
            if (!isTablet) {
                spanCount / 1
            } else if (isLandscape) {
                spanCount / 2
            } else {
                spanCount / 1
            }
        } else {
            if (!isTablet) {
                spanCount / 2
            } else if (isLandscape) {
                spanCount / 4
            } else {
                spanCount / 3
            }
        }
    }
}