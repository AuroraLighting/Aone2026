package com.aurora.aonev3.ui.fragments.group

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.ItemLongClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.rules.LogicRule
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.ui.IconsDevices
import com.google.android.material.card.MaterialCardView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.layout_device_tile.view.*
import kotlinx.android.synthetic.main.layout_device_tile.view.cardView
import kotlinx.android.synthetic.main.layout_device_tile.view.tvName
import kotlinx.android.synthetic.main.layout_double_socket_tile.view.*
import kotlinx.android.synthetic.main.layout_schedule_tile.view.*
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("NotifyDataSetChanged")
class GroupRecyclerViewAdapter internal constructor(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allGroupMembers = emptyList<GroupMember>()
    private var deviceList = ArrayList<GroupData>()
    private var groupList = ArrayList<GroupData>()
    private var scheduleList = ArrayList<GroupData>()
    private var dynamicEventsList = ArrayList<GroupData>()
    private var deviceDatapointList = emptyList<DeviceDatapoint>()
    private var groupDatapointList = emptyList<GroupDatapoint>()
    private var logicRulesList = emptyList<LogicRule>()
    var onItemClickListener: ItemClickListener? = null
    var onItemLongClickListener: ItemLongClickListener? = null
    var onOfflineClickListener: ItemClickListener? = null
    var onLeftSocketClickListener : ItemClickListener? = null
    var onRightSocketClickListener : ItemClickListener? = null
    var onLeftSocketLongClickListener : ItemLongClickListener? = null
    var onRightSocketLongClickListener : ItemLongClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            GroupRecyclerViewType.SECTION.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_section_header, parent, false)
                return SectionHeaderViewHolder(layoutView)
            }
            GroupRecyclerViewType.LIGHTS.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_device_tile, parent, false)
                return LightCardViewHolder(layoutView)
            }
            GroupRecyclerViewType.POWER.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_power_device_tile, parent, false)
                return PowerCardViewHolder(layoutView)
            }
            GroupRecyclerViewType.SOCKETS.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_double_socket_tile, parent, false)
                return SocketCardViewHolder(layoutView)
            }
            GroupRecyclerViewType.SENSORS.ordinal,
            GroupRecyclerViewType.SWITCHES.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_sensor_device_tile, parent, false)
                return SensorCardViewHolder(layoutView)
            }
            GroupRecyclerViewType.SWITCHES_DIMMER.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_device_tile, parent, false)
                return LightCardViewHolder(layoutView)
            }
            GroupRecyclerViewType.NESTED_GROUPS.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_nested_group_tile, parent, false)
                return GroupCardViewHolder(layoutView)
            }
            GroupRecyclerViewType.SCHEDULES.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_schedule_tile, parent, false)
                return ScheduleCardViewHolder(layoutView)
            }
            GroupRecyclerViewType.DYNAMIC_EVENTS.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_schedule_tile, parent, false)
                return DynamicEventsCardViewHolder(layoutView)
            }
            else -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_schedule_tile, parent, false)
                return LightCardViewHolder(layoutView)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (item.type) {
            GroupRecyclerViewType.SECTION -> {
                if (holder !is SectionHeaderViewHolder) return

                item.section?.let { holder.setSectionHeader(it.displayName) }
            }
            GroupRecyclerViewType.LIGHTS -> {
                if (holder !is LightCardViewHolder) return

                item.device?.let { holder.setDevice(it) }
            }
            GroupRecyclerViewType.POWER -> {
                if (holder !is PowerCardViewHolder) return

                item.device?.let { holder.setDevice(it) }
            }
            GroupRecyclerViewType.SOCKETS -> {
                if (holder !is SocketCardViewHolder) return

                item.device?.let { holder.setDevice(it) }
            }
            GroupRecyclerViewType.SENSORS,
            GroupRecyclerViewType.SWITCHES -> {
                if (holder !is SensorCardViewHolder) return

                item.device?.let { holder.setDevice(it) }
            }
            GroupRecyclerViewType.SWITCHES_DIMMER -> {
                if (holder !is LightCardViewHolder) return

                item.device?.let { holder.setDevice(it) }
            }
            GroupRecyclerViewType.NESTED_GROUPS -> {
                if (holder !is GroupCardViewHolder) return

                item.group?.let { holder.setGroup(it) }
            }
            GroupRecyclerViewType.SCHEDULES -> {
                if (holder !is ScheduleCardViewHolder) return

                item.schedule?.let { holder.setLogicCollection(it) }
            }
            GroupRecyclerViewType.DYNAMIC_EVENTS -> {
                if (holder !is DynamicEventsCardViewHolder) return

                item.schedule?.let { holder.setLogicCollection(it) }
            }
        }
    }

    override fun getItemCount() = deviceList.size + groupList.size + scheduleList.size + dynamicEventsList.size

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return item?.type?.ordinal ?: 0
    }

    fun getItem(position: Int): GroupData? {
        val devicePosition = position - groupList.size
        val schedulePosition = devicePosition - deviceList.size
        val dynamicEventsPosition = schedulePosition - scheduleList.size

        return when {
            position in groupList.indices -> {
                groupList[position]
            }
            devicePosition in deviceList.indices -> {
                deviceList[devicePosition]
            }
            schedulePosition in scheduleList.indices -> {
                scheduleList[schedulePosition]
            }
            dynamicEventsPosition in dynamicEventsList.indices -> {
                dynamicEventsList[dynamicEventsPosition]
            }
            else -> {
                null
            }
        }
    }

    internal fun setDevices(devices: List<Pair<GroupDataType, List<Device>>>) {
        this.deviceList.clear()
        devices.forEach {
            this.deviceList.add(GroupData(section = it.first, type = GroupRecyclerViewType.SECTION))

            it.second.forEach { device ->
                when (it.first) {
                    GroupDataType.LIGHTS -> this.deviceList.add(GroupData(device = device, type = GroupRecyclerViewType.LIGHTS))
                    GroupDataType.SENSORS -> this.deviceList.add(GroupData(device = device, type = GroupRecyclerViewType.SENSORS))
                    GroupDataType.POWER -> this.deviceList.add(GroupData(device = device, type = GroupRecyclerViewType.POWER))
                    GroupDataType.SOCKETS -> this.deviceList.add(GroupData(device = device, type = GroupRecyclerViewType.SOCKETS))
                    GroupDataType.SWITCHES -> {
                        if (device.deviceClass != Device.DeviceClass.AURORAWALLDIMMER) {
                            this.deviceList.add(
                                GroupData(
                                    device = device,
                                    type = GroupRecyclerViewType.SWITCHES
                                )
                            )
                        } else {
                            this.deviceList.add(
                                GroupData(
                                    device = device,
                                    type = GroupRecyclerViewType.SWITCHES_DIMMER
                                )
                            )
                        }
                    }
                    else -> {}
                }

            }
        }
        notifyDataSetChanged()
    }

    internal fun setDeviceDatapoints(datapoints: List<DeviceDatapoint>) {
        this.deviceDatapointList = datapoints
        notifyDataSetChanged()
    }

    internal fun setGroups(groups: List<Group>, group: Group) {
        this.groupList.clear()
        if (groups.isEmpty()) return

        this.groupList.add(
            GroupData(section = if (!group.metadata.optBoolean("is_virtual_group")) GroupDataType.NESTED_SPACES else GroupDataType.NESTED_GROUPS, type = GroupRecyclerViewType.SECTION)
        )

        groups.toList()
            .sortedBy { it.name }
            .forEach {
            this.groupList.add(
                GroupData(group = it, type = GroupRecyclerViewType.NESTED_GROUPS)
            )
        }

        notifyDataSetChanged()
    }

    internal fun setGroupDatapoints(datapoints: List<GroupDatapoint>) {
        this.groupDatapointList = datapoints
        notifyDataSetChanged()
    }

    internal fun setLogicCollections(logicCollections: List<LogicCollection>) {
        this.scheduleList.clear()
        this.dynamicEventsList.clear()

        val schedules = logicCollections.filter { it.metadata.collectionType == CollectionType.SCHEDULE }
        val dynamicEvents = logicCollections.filter { it.metadata.collectionType == CollectionType.DYNAMIC_EVENT }

        if (schedules.isNotEmpty()) {
            this.scheduleList.add(
                GroupData(section = GroupDataType.SCHEDULES, type = GroupRecyclerViewType.SECTION)
            )

            schedules.forEach { schedule ->
                this.scheduleList.add(
                    GroupData(schedule = schedule, type = GroupRecyclerViewType.SCHEDULES)
                )
            }
        }

        if (dynamicEvents.isNotEmpty()) {
            this.dynamicEventsList.add(
                GroupData(section = GroupDataType.DYNAMIC_EVENTS, type = GroupRecyclerViewType.SECTION)
            )

            dynamicEvents.forEach { schedule ->
                this.dynamicEventsList.add(
                    GroupData(schedule = schedule, type = GroupRecyclerViewType.DYNAMIC_EVENTS)
                )
            }
        }

        notifyDataSetChanged()
    }

    internal fun setLogicRules(logicRules: List<LogicRule>) {
        this.logicRulesList = logicRules
        notifyDataSetChanged()
    }

    inner class LightCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val nameTv: TextView = itemView.tvName
        private val levelTv: TextView = itemView.tvLevel
        private val colourTemperatureTv: TextView = itemView.tvColourTemperature
        private val iconIv: ImageView = itemView.ivIcon
        private val ivOfflineIv: ImageView = itemView.ivOffline

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            ivOfflineIv.setOnClickListener {
                onOfflineClickListener?.onItemClick(ivOfflineIv, bindingAdapterPosition)
            }
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        override fun onLongClick(p0: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(cardView, bindingAdapterPosition) ?: true
        }

        fun setDevice(device: Device) {
            val deviceDatapoints =
                deviceDatapointList.filter { it.parentGateway == device.parentGateway && it.id == device.id }
            val onOffValue =
                deviceDatapoints.find { it.key.lowercase() == "onoff" }?.value as? Boolean
                    ?: false
            var levelValue =
                deviceDatapoints.find { it.key.lowercase() == "level" }?.value as? Int ?: 0
            var miredValue =
                deviceDatapoints.find { it.key.lowercase() == "mired" }?.value as? Int ?: -1

            if (miredValue == 0) {
                miredValue = 454
            }

            nameTv.text = device.name

            val icon = IconsDevices.fromDefaultName(device.defaultName)

            levelTv.visibility = View.VISIBLE


            ivOfflineIv.visibility = if (device.online) View.GONE else View.VISIBLE

            if (onOffValue && device.online) {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                nameTv.setTextColor(context.getColor(R.color.colorTextPrimary))
                levelTv.setTextColor(0xCCFFFFFF.toInt())
                iconIv.setImageDrawable(ContextCompat.getDrawable(context, icon.onResId))

                when {
                    levelValue % 10 == 1 && levelValue != 1 -> {
                        levelValue -= 1
                    }
                    levelValue % 10 == 9 -> {
                        levelValue += 1
                    }
                    levelValue % 10 == 4 -> {
                        levelValue += 1
                    }
                    levelValue % 10 == 6 -> {
                        levelValue += 1
                    }
                }
                levelTv.text = context.getString(R.string.percentString, levelValue)

                if (miredValue != -1) {
                    colourTemperatureTv.text = context.getString(
                        R.string.kelvinString,
                        ((1000000 / miredValue) / 100) * 100
                    )
                    colourTemperatureTv.visibility = View.VISIBLE
                } else {
                    colourTemperatureTv.visibility = View.GONE
                }
            } else {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                nameTv.setTextColor(context.getColor(R.color.colorTextPrimary))
                levelTv.setTextColor(0x80FFFFFF.toInt())
                iconIv.setImageDrawable(ContextCompat.getDrawable(context, icon.offResId))

                if (device.online) {
                    levelTv.text = context.getString(R.string.off)
                } else {
                    levelTv.text = context.getString(R.string.unavailable)
                }

                colourTemperatureTv.visibility = View.GONE
            }
        }
    }

    inner class SensorCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val nameTv: TextView = itemView.tvName
        private val iconIv: ImageView = itemView.ivIcon

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        fun setDevice(device: Device) {
            val icon = IconsDevices.fromDefaultName(device.defaultName)
            nameTv.text = device.name

            cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
            nameTv.setTextColor(context.getColor(R.color.colorTextPrimary))
            iconIv.setImageDrawable(ContextCompat.getDrawable(context, icon.onResId))
        }
    }

    inner class PowerCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val nameTv: TextView = itemView.tvName
        private val levelTv: TextView = itemView.tvLevel
        private val iconIv: ImageView = itemView.ivIcon
        private val ivOfflineIv: ImageView = itemView.ivOffline

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            ivOfflineIv.setOnClickListener {
                onOfflineClickListener?.onItemClick(ivOfflineIv, bindingAdapterPosition)
            }
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        override fun onLongClick(p0: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(cardView, bindingAdapterPosition) ?: true
        }

        fun setDevice(device: Device) {
            val deviceDatapoints =
                deviceDatapointList.filter { it.parentGateway == device.parentGateway && it.id == device.id }
            val onOffValue =
                deviceDatapoints.find { it.key.lowercase() == "onoff" }?.value as? Boolean
                    ?: false

            nameTv.text = device.name

            val icon = IconsDevices.fromDefaultName(device.defaultName)

            levelTv.visibility = View.VISIBLE
            ivOfflineIv.visibility = View.GONE

            if (onOffValue && device.online) {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                nameTv.setTextColor(context.getColor(R.color.colorTextPrimary))
                levelTv.setTextColor(0xCCFFFFFF.toInt())
                iconIv.setImageDrawable(ContextCompat.getDrawable(context, icon.onResId))

                levelTv.text = context.getString(R.string.on)

            } else {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                nameTv.setTextColor(context.getColor(R.color.colorTextPrimary))
                levelTv.setTextColor(0x80FFFFFF.toInt())
                iconIv.setImageDrawable(ContextCompat.getDrawable(context, icon.offResId))

                if (device.online) {
                    levelTv.text = context.getString(R.string.off)
                } else {
                    levelTv.text = context.getString(R.string.unavailable)
                    ivOfflineIv.visibility = View.VISIBLE
                }
            }
        }
    }

    inner class SocketCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnLongClickListener {
        private val nameTv: TextView = itemView.tvName
        private val ibLeftSocket: ImageButton = itemView.ibLeftSocket
        private val ibRightSocket: ImageButton = itemView.ibRightSocket
        private val ivLeftLock: ImageView = itemView.ivLeftLock
        private val ivRightLock: ImageView = itemView.ivRightLock

        init {
            itemView.setOnLongClickListener(this)
            ibLeftSocket.setOnClickListener {
                onLeftSocketClickListener?.onItemClick(it, bindingAdapterPosition)
            }
            ibRightSocket.setOnClickListener {
                onRightSocketClickListener?.onItemClick(it, bindingAdapterPosition)
            }
            ibLeftSocket.setOnLongClickListener {
                onLeftSocketLongClickListener?.onItemLongClick(it, bindingAdapterPosition) ?: false
            }
            ibRightSocket.setOnLongClickListener {
                onRightSocketLongClickListener?.onItemLongClick(it, bindingAdapterPosition) ?: false
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(itemView, bindingAdapterPosition) ?: true
        }

        fun setDevice(device: Device) {
            val deviceDatapoints =
                deviceDatapointList.filter { it.parentGateway == device.parentGateway && it.id == device.id }
            val leftOnOffValue =
                deviceDatapoints.find { it.ldev == "socket1" && it.key.lowercase() == "onoff" }?.value as? Boolean
                    ?: false
            val rightOnOffValue =
                deviceDatapoints.find { it.ldev == "socket2" && it.key.lowercase() == "onoff" }?.value as? Boolean
                    ?: false
            val leftLockValue =
                deviceDatapoints.find { it.key.lowercase() == "lock" }?.value as? Boolean
                    ?: false
            val rightLockValue =
                deviceDatapoints.find { it.key.lowercase() == "lock2" }?.value as? Boolean
                    ?: false

            nameTv.text = device.name

            if (leftOnOffValue) {
                ibLeftSocket.background = ContextCompat
                    .getDrawable(context, R.drawable.left_socket_background_on)
                ibLeftSocket.setImageDrawable(ContextCompat
                    .getDrawable(context, R.drawable.socket_on_left))
            } else {
                ibLeftSocket.background = ContextCompat
                    .getDrawable(context, R.drawable.left_socket_background_off)
                ibLeftSocket.setImageDrawable(ContextCompat
                    .getDrawable(context, R.drawable.socket_off_left))
            }

            if (rightOnOffValue) {
                ibRightSocket.background = ContextCompat
                    .getDrawable(context, R.drawable.right_socket_background_on)
                ibRightSocket.setImageDrawable(ContextCompat
                    .getDrawable(context, R.drawable.socket_on_right))
            } else {
                ibRightSocket.background = ContextCompat
                    .getDrawable(context, R.drawable.right_socket_background_off)
                ibRightSocket.setImageDrawable(ContextCompat
                    .getDrawable(context, R.drawable.socket_off_right))
            }

            ivLeftLock.visibility = if (!leftLockValue) View.GONE else View.VISIBLE
            ivRightLock.visibility = if (!rightLockValue) View.GONE else View.VISIBLE
        }
    }

    inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val nameTv: TextView = itemView.tvName
        private val levelTv: TextView = itemView.tvLevel
        private val colourTemperatureTv: TextView = itemView.tvColourTemperature
        private val offlineIv: ImageView = itemView.ivOffline

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        override fun onLongClick(p0: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(cardView, bindingAdapterPosition) ?: true
        }

        fun setGroup(group: Group) {
            val groupDatapoints =
                groupDatapointList.filter { it.parentGateway == group.parentGateway && it.id == group.id }
            val onOffValue =
                groupDatapoints.find { it.key.lowercase() == "onoff" }?.value as? Boolean
                    ?: false
            var levelValue =
                groupDatapoints.find { it.key.lowercase() == "level" }?.value as? Int ?: 0
            var miredValue =
                groupDatapoints.find { it.key.lowercase() == "mired" }?.value as? Int ?: -1
            val groupMemberIds = SyncHandler.groupMembersList.filter { it.parentGateway == group.parentGateway && it.groupId == group.id && !it.isVirtualMember }.map { it.deviceId }
            val groupDevices = SyncHandler.devicesList.filter { it.parentGateway == group.parentGateway && it.id in groupMemberIds }
            val devicesOffline = groupDevices.any { !it.online }

            val offline = groupDevices.isNotEmpty() && groupDevices.all { !it.online }

            if (miredValue == 0) {
                miredValue = 454
            }

            nameTv.text = group.name

            levelTv.visibility = View.VISIBLE

            if (onOffValue && !offline) {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                nameTv.setTextColor(context.getColor(R.color.colorTextPrimary))
                levelTv.setTextColor(0xCCFFFFFF.toInt())

                when {
                    levelValue % 10 == 1 && levelValue != 1 -> {
                        levelValue -= 1
                    }
                    levelValue % 10 == 9 -> {
                        levelValue += 1
                    }
                    levelValue % 10 == 4 -> {
                        levelValue += 1
                    }
                    levelValue % 10 == 6 -> {
                        levelValue += 1
                    }
                }
                levelTv.text = context.getString(R.string.percentString, levelValue)

                if (miredValue != -1) {
                    colourTemperatureTv.text = context.getString(
                        R.string.percentString,
                        ((1000000 / miredValue) / 100) * 100
                    )
                    colourTemperatureTv.visibility = View.VISIBLE
                } else {
                    colourTemperatureTv.visibility = View.GONE
                }
            } else {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                nameTv.setTextColor(context.getColor(R.color.colorTextPrimary))
                levelTv.setTextColor(0x80FFFFFF.toInt())

                levelTv.text = context.getString(R.string.off)

                colourTemperatureTv.visibility = View.GONE
            }

            if (devicesOffline) {
                offlineIv.visibility = View.VISIBLE

                offlineIv.setOnClickListener {
                    try {
                        AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.device_unavailable))
                            .setMessage(context.getString(
                                if (!group.metadata.optBoolean("is_virtual_group"))
                                    R.string.space_unavailable_body
                                else
                                    R.string.group_unavailable_body
                            ))
                            .setPositiveButton(context.getString(R.string.ok), null)
                            .create()
                            .show()
                    } catch (ex: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            } else {
                offlineIv.visibility = View.GONE
            }
        }
    }

    inner class ScheduleCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val numberOfEvents: TextView = itemView.tvNumberOfEvents

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        fun setLogicCollection(logicCollection: LogicCollection) {
            val logicRules = logicRulesList.filter { it.parentGateway == logicCollection.parentGateway && it.logicCollectionId == logicCollection.id }
            val text = when {
                logicRules.isNotEmpty() -> {
                    context.resources.getQuantityString(R.plurals.multiple_events, logicRules.size, logicRules.size)
                }
                else -> {
                    context.getString(R.string.no_events)
                }
            }

            numberOfEvents.text = text
        }
    }

    inner class DynamicEventsCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val numberOfEvents: TextView = itemView.tvNumberOfEvents

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, bindingAdapterPosition)
        }

        fun setLogicCollection(logicCollection: LogicCollection) {
            val logicRules = logicRulesList.filter { it.parentGateway == logicCollection.parentGateway && it.logicCollectionId == logicCollection.id }
            val text = when {
                logicRules.isNotEmpty() -> {
                    context.resources.getQuantityString(R.plurals.multiple_dynamic_events, logicRules.size, logicRules.size)
                }
                else -> {
                    context.getString(R.string.no_dynamic_events)
                }
            }

            numberOfEvents.text = text
        }
    }

    enum class GroupRecyclerViewType {
        SECTION,
        LIGHTS,
        SENSORS,
        POWER,
        SOCKETS,
        SWITCHES,
        SWITCHES_DIMMER,
        NESTED_GROUPS,
        SCHEDULES,
        DYNAMIC_EVENTS
    }

    enum class GroupDataType(val displayName: String) {
        NESTED_SPACES("Spaces / Groups"),
        NESTED_GROUPS("Groups"),
        LIGHTS("Lights"),
        SENSORS("Sensors"),
        POWER("Power"),
        SOCKETS("Sockets"),
        SWITCHES("Switches"),
        SCHEDULES("Schedules"),
        DYNAMIC_EVENTS("Dynamic Events")
    }

    data class GroupData(val device: Device? = null, val group: Group? = null, val schedule: LogicCollection? = null, val section: GroupDataType? = null, val type: GroupRecyclerViewType)
}