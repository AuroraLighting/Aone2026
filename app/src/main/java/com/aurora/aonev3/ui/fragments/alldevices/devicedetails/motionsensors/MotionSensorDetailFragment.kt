package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.logic.EventMetadata
import com.aurora.aonev3.logic.ResourceValueCondition
import com.aurora.aonev3.logic.TimeEventMetadata
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.data.logic.timers.LogicTimer
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.DeviceDetailFragment
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.aurora.aonev3.ui.fragments.schedules.EventTarget
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.google.android.material.card.MaterialCardView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

open class MotionSensorDetailFragment : DeviceDetailFragment(), PopupMenu.OnMenuItemClickListener {

    private val motionSensorViewModel: MotionSensorDetailViewModel by activityViewModels()
    private val motionSensorEventViewModel: MotionSensorEventViewModel by activityViewModels()

    private var mLogicCollections: List<LogicCollection> = emptyList()
    protected var mLogicRules: List<LogicRule> = emptyList()
    protected var mLogicTimers: List<LogicTimer> = emptyList()
    protected var mLogicCollectionMenu: LogicCollection? = null
    protected lateinit var listAdapter: EventSelectorViewAdapter

    protected val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        val device = viewModel.selectedDevice ?: return
        val activity = activity ?: throw Exception("Invalid activity")

//        sensorSwitchLayout.visibility = View.VISIBLE
        eventsOuterLayout.visibility = View.VISIBLE

        listAdapter = EventSelectorViewAdapter(activity).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val item = getItem(position) ?: return
                    if (item.section == MotionEventSection.EVENT) {
                        val logicCollection = item.logicCollection ?: return

                        NabtoHandler.selectedGateway?.let { gateway ->
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    DevelcoHandler.putLogicCollection(
                                        gateway,
                                        logicCollection.id,
                                        JSONObject().put("enabled", !logicCollection.isEnabled)
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
                                    err.printStackTrace()
                                }
                            }
                        }
                    } else {
                        val action = MotionSensorDetailFragmentDirections.actionMotionSensorDetailFragmentToMotionSensorEventFragment()
                        findNavController().navigate(action)
                    }
                }
            }

            onItemLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val item = getItem(position) ?: return false
                    if (item.section == MotionEventSection.EVENT) {
                        val logicCollection = item.logicCollection ?: return false

                        setUpViewModelEdit(logicCollection)

                        val action = MotionSensorDetailFragmentDirections.actionGlobalEditMotionSensorEventFragment()
                        findNavController().navigate(action)
                    }
                    return true
                }
            }

            onMenuClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val logicCollection = getItem(position)?.logicCollection ?: return

                    mLogicCollectionMenu = logicCollection

                    val popup = PopupMenu(requireContext(), view)
                    if (logicCollection.isEnabled) {
                        popup.menuInflater.inflate(R.menu.enabled_schedule_menu, popup.menu)
                    } else {
                        popup.menuInflater.inflate(R.menu.disabled_schedule_menu, popup.menu)
                    }
                    popup.setOnMenuItemClickListener(this@MotionSensorDetailFragment)
                    popup.show()
                }
            }
        }

        NabtoHandler.selectedGateway?.let { gateway ->
            motionSensorViewModel.getLogicCollections(gateway)
                .observe(viewLifecycleOwner) { logicCollections ->
                    mLogicCollections =
                        logicCollections.toList().filter { it.metadata.triggerId == device.id }
                    motionSensorViewModel.logicCollections = mLogicCollections

                    enableSwitch.isChecked =
                        mLogicCollections.isNotEmpty() && mLogicCollections.any { it.isEnabled }

                    if (motionSensorViewModel.initialEnabled == null) {
                        motionSensorViewModel.initialEnabled = enableSwitch.isChecked
                    }

                    val items = mLogicCollections.map {
                        MotionEventData(it, MotionEventSection.EVENT)
                    }.toMutableList()
                    if (allowEditing()) {
                        items.add(MotionEventData(section = MotionEventSection.NEW))
                    }
                    listAdapter.setLogicCollections(items)

                    enableSwitch.isEnabled = mLogicCollections.isNotEmpty()
                }
            motionSensorViewModel.getLogicRules(gateway)
                .observe(viewLifecycleOwner) { logicRules ->
                    mLogicRules = logicRules
                }
            motionSensorViewModel.getLogicTimers(gateway)
                .observe(viewLifecycleOwner) { logicTimers ->
                    mLogicTimers = logicTimers
                }
        }

        enableSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.text = if (isChecked) {
                getText(R.string.sensor_is_active)
            } else {
                getText(R.string.sensor_is_inactive)
            }
        }

        with(rvMotionSensorEvents) {
            adapter = listAdapter
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(
                context,
                1,
                RecyclerView.VERTICAL,
                false
            )

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(
                GridItemDecoration(margin, margin, margin * 2, margin * 2)
            )
        }
    }

    override fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val device = viewModel.selectedDevice ?: return@OnClickListener
            binding.btnSave.isEnabled = false
            activity?.layoutGreyOut?.visibility = View.VISIBLE
            val enabled = enableSwitch.isChecked
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                NabtoHandler.selectedGateway?.let { gateway ->
                    if (gateway.isConnected) {
                        if (viewModel.deviceName.isNotBlank() && viewModel.deviceName != device.name) {
                            try {
                                DevelcoHandler.putDevice(
                                    gateway,
                                    device.id,
                                    JSONObject().put("name", viewModel.deviceName)
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
                                err.printStackTrace()
                            }
                        }

                        if (motionSensorViewModel.initialEnabled != enabled) {
                            mLogicCollections.forEach { logicCollection ->
                                try {
                                    DevelcoHandler.putLogicCollection(
                                        gateway,
                                        logicCollection.id,
                                        JSONObject().put("enabled", enabled)
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
                                    err.printStackTrace()
                                }
                            }
                        }
                    }

                    activity?.runOnUiThread {
                        binding.btnSave.isEnabled = true
                        activity?.layoutGreyOut?.visibility = View.GONE
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun btnDeleteClickListener() {
        val device = viewModel.selectedDevice ?: return
        activity?.layoutGreyOut?.visibility = View.VISIBLE
        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val logicCollectionsToDelete = ArrayList<Int>()
                SyncHandler
                    .logicCollectionsList
                    .filter { it.parentGateway == gateway.serial }.forEach { logicCollection ->
                    val metadata = logicCollection.metadata

                    if (metadata.triggerId == device.id) {
                        logicCollectionsToDelete.add(logicCollection.id)
                    }
                }

                logicCollectionsToDelete.forEach {
                    try {
                        DevelcoHandler.deleteLogicCollection(
                            gateway,
                            it
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

                    }
                }

                SyncHandler
                    .groupsList
                    .filter {
                        it.parentGateway == gateway.serial
                    }
                    .forEach { group ->
                    val metadata = group.metadata
                    val virtualMembers = metadata.optJSONArray("virtual_members") ?: JSONArray()
                    val newVirtualMembers = JSONArray()
                    var updateGroup = false

                    for (i in virtualMembers.indices()) {
                        val virtualMember = virtualMembers.optJSONObject(i)

                        if (virtualMember.optInt("id") != device.id) {
                            newVirtualMembers.put(virtualMember)
                        } else {
                            updateGroup = true
                        }
                    }

                    if (updateGroup) {
                        metadata.put("virtual_members", newVirtualMembers)
                        try {
                            DevelcoHandler.putGroup(
                                gateway,
                                group.id,
                                JSONObject()
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

                        }
                    }
                }

                try {
                    DevelcoHandler.deleteDevice(gateway, device.id)
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

                }

                activity?.runOnUiThread {
                    activity?.layoutGreyOut?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    protected open fun setUpViewModelEdit(logicCollection: LogicCollection) {
        motionSensorEventViewModel.apply {
            val event = logicCollection.metadata.event ?: EventMetadata()
            this.logicCollection = logicCollection
            val logicTimers =
                mLogicTimers.filter { it.logicCollectionId == logicCollection.id }
            val logicRules =
                mLogicRules.filter { it.logicCollectionId == logicCollection.id }

            timeout.postValue(logicTimers.firstOrNull()?.timeout)

            var lux = -1
            for (logicRule in logicRules) {
                val conditions = logicRule.conditions ?: emptyArray()

                for (condition in conditions) {
                    if (condition !is ResourceValueCondition) continue
                    if (condition.path.endsWith("illuminance.value")) {
                        val rule = condition.rule
                        lux = try {
                            rule.replace("[^0-9]".toRegex(), "").toInt()
                        } catch (ex: NumberFormatException) {
                            -1
                        }
                        break
                    }
                }

                if (lux != -1) {
                    targetLux.postValue(lux)
                    break
                }
            }

            if (event.time != null) {
                val time = event.time ?: TimeEventMetadata()
                val start = SunriseSunsetType.fromMetadata(time.start) ?: SunriseSunsetType.TIME
                val startOffset = time.startOffset ?: 0
                val startHour = time.startHour ?: 0
                val startMinute = time.startMinute ?: 0
                val end = SunriseSunsetType.fromMetadata(time.end) ?: SunriseSunsetType.TIME
                val endOffset = time.endOffset ?: 0
                val endHour = time.endHour ?: 0
                val endMinute = time.endMinute ?: 0

                motionSensorEventViewModel.updateStartTime(startHour, startMinute, start, startOffset)
                motionSensorEventViewModel.updateEndTime(endHour, endMinute, end, endOffset)

                isAllDay.postValue(false)
            } else {
                motionSensorEventViewModel.setIsAllDay(true)
            }
            when {
                event.group != null -> {
                    val groupId = event.group?.id ?: -1
                    val group = SyncHandler
                        .groupsList
                        .find {
                            it.parentGateway == logicCollection.parentGateway && it.id == groupId
                        }

                    eventTarget.postValue(EventTarget.SPACE)

                    targetGroup.postValue(group)
                    device.postValue(null)
                    scene.postValue(null)
                }
                event.device != null -> {
                    val deviceId = event.device?.id ?: -1
                    val deviceLdev =
                        event.device?.ldev ?: ""
                    val targetDevice = SyncHandler
                        .devicesList
                        .find { it.parentGateway == logicCollection.parentGateway && it.id == deviceId }
                    targetDevice?.let {
                        val groupMembers = SyncHandler.groupMembersList.filter { groupMember -> groupMember.parentGateway == targetDevice.parentGateway && groupMember.deviceId == targetDevice.id }
                        val groups = SyncHandler.groupsList.filter { group -> group.parentGateway == targetDevice.parentGateway && groupMembers.any { gm -> gm.groupId == group.id } }
                        val group = NestedGroupTree(groups).getChild()?.group

                        targetGroup.value = group
                        eventTarget.value = EventTarget.DEVICE
                        device.value = Pair(targetDevice, deviceLdev)
                    }
                    scene.value = null
                }
                event.scene != null -> {
                    val sceneId = event.scene?.id ?: -1
                    val groupId = event.scene?.group ?: -1
                    val targetScene =
                        SyncHandler
                            .scenesList
                            .find { it.parentGateway == logicCollection.parentGateway && it.id == sceneId && it.groupId == groupId }
                    val group = SyncHandler
                        .groupsList
                        .find {
                            it.parentGateway == logicCollection.parentGateway
                                    && it.id == groupId
                        }

                    eventTarget.postValue(EventTarget.SCENE)

                    targetGroup.postValue(group)
                    device.postValue(null)
                    scene.postValue(targetScene)
                }
            }
            try {
                eventDay.postValue(
                    EventDay.valueOf(
                        event.days?.toUpperCase() ?: ""
                    )
                )
            } catch (ex: IllegalArgumentException) {
                crashlytics.log("E/$TAG: ${event.days}")
                crashlytics.recordException(ex)
            }

        }
    }

    inner class EventSelectorViewAdapter internal constructor(val context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var logicCollectionList = emptyList<MotionEventData>()
        var onItemClickListener: ItemClickListener? = null
        var onItemLongClickListener: ItemLongClickListener? = null
        var onMenuClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == MotionEventSection.EVENT.ordinal) {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_motion_event_selector_tile, parent, false)
                EventCardViewHolder(layoutView)
            } else {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_new_motion_event_selector_tile, parent, false)
                NewEventCardViewHolder(layoutView)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position) ?: return

            if (item.section == MotionEventSection.EVENT) {
                item.logicCollection ?: return
                (holder as? EventCardViewHolder)?.setLogicCollection(item.logicCollection)
            }
        }

        override fun getItemViewType(position: Int): Int = getItem(position)?.section?.ordinal ?: -1

        override fun getItemCount() = logicCollectionList.size

        fun getItem(position: Int): MotionEventData? =
            if (position in logicCollectionList.indices) logicCollectionList[position] else null

        internal fun setLogicCollections(logicCollections: List<MotionEventData>) {
            this.logicCollectionList = logicCollections
            notifyDataSetChanged()
        }

        inner class EventCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener, View.OnLongClickListener {
            var cardView: MaterialCardView = itemView.cardView
            var name: TextView = itemView.tvName
            var days: TextView = itemView.tvDays
            var menu: ImageView = itemView.menu

            init {
                if (allowEditing()) {
                    cardView.setOnClickListener(this)
                    cardView.setOnLongClickListener(this)
                    menu.setOnClickListener {
                        onMenuClickListener?.onItemClick(menu, adapterPosition)
                    }
                } else {
                    menu.visibility = View.GONE
                }
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(cardView, adapterPosition)
            }

            override fun onLongClick(v: View?): Boolean {
                return onItemLongClickListener?.onItemLongClick(cardView, adapterPosition) ?: false
            }

            fun setLogicCollection(logicCollection: LogicCollection) {
                val event = logicCollection.metadata.event ?: EventMetadata()

                val timeString = if (event.time != null) {
                    val time = event.time ?: TimeEventMetadata()
                    val startTimeString = if (time.startHour != null) {
                        String.format(
                            "%02d:%02d",
                            time.startHour,
                            time.startMinute
                        )
                    } else {
                        val offset = time.startOffset ?: 0
                        when {
                            offset > 0 -> {
                                "${time.start?.toCapitalisedLowerCase()} (+ $offset)"
                            }
                            offset < 0 -> {
                                "${time.start?.toCapitalisedLowerCase()} (- ${abs(offset)})"
                            }
                            else -> {
                                time.start?.toCapitalisedLowerCase()
                            }
                        }
                    }
                    val endTimeString = if (time.endHour != null) {
                        String.format(
                            "%02d:%02d",
                            time.endHour,
                            time.endMinute
                        )
                    } else {
                        val offset = time.endOffset ?: 0
                        when {
                            offset > 0 -> {
                                "${time.end?.toCapitalisedLowerCase()} (+ $offset)"
                            }
                            offset < 0 -> {
                                "${time.end?.toCapitalisedLowerCase()} (- ${abs(offset)})"
                            }
                            else -> {
                                time.end?.toCapitalisedLowerCase()
                            }
                        }
                    }

                    arrayOf(startTimeString, endTimeString).joinToString(" - ")
                } else {
                    context.getString(R.string.all_day)
                }
                val actionString = when {
                    event.group != null -> {
                        val groupId = event.group?.id ?: -1
                        val group = SyncHandler
                            .groupsList
                            .find {
                                it.parentGateway == logicCollection.parentGateway
                                        && it.id == groupId
                            }
                        String.format(context.getString(R.string.event_turn_on), group?.name ?: "")
                    }
                    event.device != null -> {
                        val deviceId = event.device?.id ?: -1
                        val device = SyncHandler
                            .devicesList
                            .find { it.parentGateway == logicCollection.parentGateway && it.id == deviceId }
                        String.format(context.getString(R.string.event_turn_on), device?.name ?: "")
                    }
                    event.scene != null -> {
                        val sceneId = event.scene?.id ?: -1
                        val groupId = event.scene?.group ?: -1
                        val scene = SyncHandler
                            .scenesList
                            .find { it.parentGateway == logicCollection.parentGateway && it.id == sceneId && it.groupId == groupId }
                        String.format(
                            context.getString(R.string.motion_event_activate_scene),
                            scene?.name ?: ""
                        )
                    }
                    else -> {
                        ""
                    }
                }
                val nameString = "$timeString | $actionString"
                val dayString = try {
                    EventDay.valueOf(event.days?.toUpperCase() ?: "").displayName
                } catch (ex: IllegalArgumentException) {
                    ""
                }

                name.text = nameString
                days.text = dayString
                if (logicCollection.isEnabled) {
                    cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    name.setTextColor(context.getColor(R.color.colorPrimary))
                    days.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    name.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                    days.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                }
            }
        }

        inner class NewEventCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var cardView: MaterialCardView = itemView.cardView
            var name: TextView = itemView.tvName

            init {
                cardView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(cardView, adapterPosition)
            }
        }
    }

    data class MotionEventData(val logicCollection: LogicCollection? = null, val section: MotionEventSection)

    enum class MotionEventSection {
        EVENT,
        NEW
    }

    companion object {
        private const val TAG = "MotionSensorDetailFragment"
        fun newInstance() =
            MotionSensorDetailFragment()
    }

    override fun onDetach() {
        motionSensorViewModel.clearViewModel()
        motionSensorEventViewModel.clearViewModel()
        super.onDetach()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.enable_event -> {
                NabtoHandler.selectedGateway?.let { gateway ->
                    if (!gateway.isConnected) return@let

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val logicCollection = mLogicCollectionMenu ?: return@launch
                        try {
                            DevelcoHandler.putLogicCollection(
                                gateway,
                                logicCollection.id,
                                JSONObject().put("enabled", true)
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
                        val logicCollection = mLogicCollectionMenu ?: return@launch
                        try {
                            DevelcoHandler.putLogicCollection(
                                gateway,
                                logicCollection.id,
                                JSONObject().put("enabled", false)
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
                            err.printStackTrace()
                            throw Exception("Failed to disable")
                        }
                    }
                }
                true
            }
            R.id.edit_event -> {
                mLogicCollectionMenu?.let { logicCollection ->
                    setUpViewModelEdit(logicCollection)

                    val action = MotionSensorDetailFragmentDirections.actionGlobalEditMotionSensorEventFragment()
                    findNavController().navigate(action)
                }
                true
            }
            R.id.delete_event -> {
                NabtoHandler.selectedGateway?.let { gateway ->
                    if (!gateway.isConnected) return@let

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val logicCollection = mLogicCollectionMenu ?: return@launch
                        try {
                            DevelcoHandler.deleteLogicCollection(
                                gateway,
                                logicCollection.id
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
                            err.printStackTrace()
                            if (err.networkResponse.statusCode != 404) {
                                throw Exception("Failed to delete")
                            }
                        }
                    }
                }
                true
            }
            else -> false
        }
    }
}
