package com.aurora.aonev3.ui.fragments.scenes

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.ItemLongClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.ui.IconsDevices
import com.google.android.material.card.MaterialCardView

class SceneDevicesRecyclerViewAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var deviceList = ArrayList<SceneDeviceData>()
    private var groupList = ArrayList<SceneDeviceData>()
    private var datapointList = emptyList<DeviceDatapoint>()
    private var groupDatapointList = emptyList<GroupDatapoint>()
    var onItemClickListener: ItemClickListener? = null
    var onItemLongClickListener: ItemLongClickListener? = null
    var onOfflineClickListener: ItemClickListener? = null
    var onLeftSocketClickListener: ItemClickListener? = null
    var onRightSocketClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            SceneDeviceDataType.SECTION.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_section_header, parent, false)
                return SectionHeaderViewHolder(layoutView)
            }
            SceneDeviceDataType.DEVICE.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_device_tile, parent, false)
                return LightCardViewHolder(layoutView)
            }
            SceneDeviceDataType.SOCKET.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_double_socket_tile, parent, false)
                return SocketCardViewHolder(layoutView)
            }
            SceneDeviceDataType.GROUP.ordinal -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_nested_group_tile, parent, false)
                return GroupCardViewHolder(layoutView)
            }
            else -> {
                val layoutView =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_device_tile, parent, false)
                return LightCardViewHolder(layoutView)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (item.type) {
            SceneDeviceDataType.DEVICE -> {
                val device = item.device ?: return

                (holder as? LightCardViewHolder)?.setDevice(device)
            }
            SceneDeviceDataType.SOCKET -> {
                val device = item.device ?: return

                (holder as? SocketCardViewHolder)?.setDevice(device)
            }
            SceneDeviceDataType.GROUP -> {
                val group = item.group ?: return

                (holder as? GroupCardViewHolder)?.setGroup(group)
            }
            SceneDeviceDataType.SECTION -> {
                val section = item.section ?: return

                (holder as? SectionHeaderViewHolder)?.setSectionHeader(section)
            }
        }
    }

    override fun getItemCount() = deviceList.size + groupList.size

    fun getItem(position: Int): SceneDeviceData? =
        when {
            position in groupList.indices -> groupList[position]
            position - groupList.size in deviceList.indices -> deviceList[position - groupList.size]
            else -> null
        }
    
    override fun getItemViewType(position: Int): Int {
        val devicePosition = position - groupList.size
        return when {
            position in groupList.indices -> groupList[position].type.ordinal
            devicePosition in deviceList.indices -> deviceList[devicePosition].type.ordinal
            else -> SceneDeviceDataType.SECTION.ordinal
        }
    }

    fun setDevices(devices: List<Pair<String, List<Device>>>) {
        this.deviceList.clear()
        devices.forEach {
            this.deviceList.add(SceneDeviceData(section = it.first, type = SceneDeviceDataType.SECTION))

            it.second.forEach { device ->
                if (device.deviceClass != Device.DeviceClass.AURORADUALSOCKET) {
                    this.deviceList
                        .add(SceneDeviceData(device = device, type = SceneDeviceDataType.DEVICE))
                } else {
                    this.deviceList
                        .add(SceneDeviceData(device = device, type = SceneDeviceDataType.SOCKET))
                }
            }
        }
        notifyDataSetChanged()
    }

    fun setGroups(groups: List<Group>) {
        this.groupList.clear()
        this.groupList.add(SceneDeviceData(section = "Spaces / Groups", type = SceneDeviceDataType.SECTION))

        this.groupList.addAll(groups.toList().map { SceneDeviceData(group = it, type = SceneDeviceDataType.GROUP) })
        notifyDataSetChanged()
    }

    internal fun setDatapoints(datapoints: List<DeviceDatapoint>) {
        this.datapointList = datapoints
        notifyDataSetChanged()
    }

    internal fun setGroupDatapoints(datapoints: List<GroupDatapoint>) {
        this.groupDatapointList = datapoints
        notifyDataSetChanged()
    }

    inner class LightCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        var cardView: MaterialCardView = itemView.cardView
        var tvName: TextView = itemView.tvName
        var tvLevel: TextView = itemView.tvLevel
        var tvColourTemperature: TextView = itemView.tvColourTemperature
        var ivIcon: ImageView = itemView.ivIcon
        var ivOffline: ImageView = itemView.ivOffline

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            ivOffline.setOnClickListener {
                onOfflineClickListener?.onItemClick(ivOffline, adapterPosition)
            }
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }

        override fun onLongClick(p0: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(cardView, adapterPosition) ?: true
        }

        fun setDevice(device: Device) {
            val deviceDatapoints = datapointList.filter { it.parentGateway == device.parentGateway && it.id == device.id }
            val onOff = deviceDatapoints.find { it.key.toLowerCase() == "onoff" }?.value as? Boolean ?: false
            var level = deviceDatapoints.find { it.key.toLowerCase() == "level" }?.value as? Int ?: 0
            var mired = deviceDatapoints.find { it.key.toLowerCase() == "mired" }?.value as? Int ?: -1

            if (mired == 0) {
                mired = 454
            }

            tvName.text = device.name

            val icon = IconsDevices.fromDefaultName(device.defaultName)

            if (device.getDeviceCategory() == Device.DeviceCategory.LIGHTS ||
                device.getDeviceCategory() == Device.DeviceCategory.POWER) {
                tvLevel.visibility = View.VISIBLE
                ivOffline.visibility = View.GONE

                if (onOff && device.online) {
                    cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    tvName.setTextColor(context.getColor(R.color.colorPrimary))
                    tvLevel.setTextColor(context.getColor(R.color.colorPrimary))
                    ivIcon.setImageDrawable(ContextCompat.getDrawable(context, icon.onResId))

                    if (device.getDeviceCategory() != Device.DeviceCategory.POWER) {
                        when {
                            level % 10 == 1 && level != 1 -> {
                                level -= 1
                            }
                            level % 10 == 9 -> {
                                level += 1
                            }
                            level % 10 == 4 -> {
                                level += 1
                            }
                            level % 10 == 6 -> {
                                level += 1
                            }
                        }
                        tvLevel.text = context.getString(R.string.percentString, level)
                    } else {
                        tvLevel.text = context.getString(R.string.on)
                    }
                    if (mired != -1) {
                        val colourTemperature = ((1000000 / mired) / 100) * 100
                        tvColourTemperature.text = context.getString(R.string.kelvinString, colourTemperature)
                        tvColourTemperature.visibility = View.VISIBLE
                    } else {
                        tvColourTemperature.visibility = View.GONE
                    }
                } else {
                    cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    tvName.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                    tvLevel.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                    ivIcon.setImageDrawable(ContextCompat.getDrawable(context, icon.offResId))

                    if (device.online) {
                        tvLevel.text = context.getString(R.string.off)
                    } else {
                        tvLevel.text = context.getString(R.string.unavailable)
                        ivOffline.visibility = View.VISIBLE
                    }
                    tvColourTemperature.visibility = View.GONE
                }
            } else {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                tvName.setTextColor(context.getColor(R.color.colorPrimary))
                tvLevel.visibility = View.GONE
                tvColourTemperature.visibility = View.GONE
                ivIcon.setImageDrawable(ContextCompat.getDrawable(context, icon.onResId))
            }
        }
    }

    inner class SocketCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTv: TextView = itemView.findViewById(R.id.tvName)
        private val ibLeftSocket: ImageButton = itemView.findViewById(R.id.ibLeftSocket)
        private val ibRightSocket: ImageButton = itemView.findViewById(R.id.ibRightSocket)

        init {
            ibLeftSocket.setOnClickListener {
                onLeftSocketClickListener?.onItemClick(it, adapterPosition)
            }
            ibRightSocket.setOnClickListener {
                onRightSocketClickListener?.onItemClick(it, adapterPosition)
            }
        }

        fun setDevice(device: Device) {
            val deviceDatapoints =
                datapointList.filter { it.parentGateway == device.parentGateway && it.id == device.id }
            val leftOnOffValue =
                deviceDatapoints.find { it.ldev == "socket1" && it.key.toLowerCase() == "onoff" }?.value as? Boolean
                    ?: false
            val rightOnOffValue =
                deviceDatapoints.find { it.ldev == "socket2" && it.key.toLowerCase() == "onoff" }?.value as? Boolean
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
        }
    }

    inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val nameTv: TextView = itemView.findViewById(R.id.tvName)
        private val levelTv: TextView = itemView.findViewById(R.id.tvLevel)
        private val colourTemperatureTv: TextView = itemView.findViewById(R.id.tvColourTemperature)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }

        override fun onLongClick(p0: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(cardView, adapterPosition) ?: true
        }

        fun setGroup(group: Group) {
            val groupDatapoints =
                groupDatapointList.filter { it.parentGateway == group.parentGateway && it.id == group.id }
            val onOffValue =
                groupDatapoints.find { it.key.toLowerCase() == "onoff" }?.value as? Boolean
                    ?: false
            var levelValue =
                groupDatapoints.find { it.key.toLowerCase() == "level" }?.value as? Int ?: 0
            var miredValue =
                groupDatapoints.find { it.key.toLowerCase() == "mired" }?.value as? Int ?: -1

            if (miredValue == 0) {
                miredValue = 454
            }

            nameTv.text = group.name

            levelTv.visibility = View.VISIBLE

            if (onOffValue) {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                nameTv.setTextColor(context.getColor(R.color.colorPrimary))
                levelTv.setTextColor(context.getColor(R.color.colorPrimary))

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
                nameTv.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                levelTv.setTextColor(context.getColor(R.color.colorPrimaryBackground))

                levelTv.text = context.getString(R.string.off)

                colourTemperatureTv.visibility = View.GONE
            }
        }
    }
}

data class SceneDeviceData(val device: Device? = null, val group: Group? = null, val section: String? = null, val type: SceneDeviceDataType)

enum class SceneDeviceDataType {
    GROUP,
    DEVICE,
    SOCKET,
    SECTION
}
