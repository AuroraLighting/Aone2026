package com.aurora.aonev3.ui.fragments.alldevices

import com.aurora.aonev3.synthetic.*
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.logic.LogicCollection
import com.google.android.material.card.MaterialCardView

class AllDevicesRecyclerViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mGatewayList = ArrayList<AllDevicesData>()
    private var mDeviceList = ArrayList<AllDevicesData>()
    private var mSchedulesList = ArrayList<AllDevicesData>()
    private var mGroupMembers = emptyList<GroupMember>()
    private var mGroups = emptyList<Group>()
    var onItemClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            AllDevicesDataType.GATEWAY.ordinal -> {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_hub_tile, parent, false)
                GatewayCardViewHolder(layoutView)
            }
            AllDevicesDataType.DEVICE.ordinal -> {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_all_device_tile, parent, false)
                DeviceCardViewHolder(layoutView)
            }
            AllDevicesDataType.LEGACY_SCHEDULE.ordinal -> {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_hub_tile, parent, false)
                LegacyScheduleCardViewHolder(layoutView)
            }
            else -> {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_section_header, parent, false)
                SectionHeaderViewHolder(layoutView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (item.type) {
            AllDevicesDataType.GATEWAY -> {
                val gateway = item.gateway ?: return

                (holder as? GatewayCardViewHolder)?.setGateway(gateway)
            }
            AllDevicesDataType.DEVICE -> {
                val device = item.device ?: return
                val groupMembers = mGroupMembers.filter { it.parentGateway == device.parentGateway && it.deviceId == device.id }
                val groups = mGroups.filter { group -> group.parentGateway == device.parentGateway && groupMembers.any { it.groupId == group.id } }
                val group = NestedGroupTree(groups).getChild()?.group

                (holder as? DeviceCardViewHolder)?.setTile(device, group)
            }
            AllDevicesDataType.LEGACY_SCHEDULE -> {
                val collection = item.collection ?: return

                (holder as? LegacyScheduleCardViewHolder)?.setCollection(collection)
            }
            else -> {
                val section = item.section ?: return

                (holder as? SectionHeaderViewHolder)?.setSectionHeader(section)
            }
        }
    }

    override fun getItemCount() = mGatewayList.size + mDeviceList.size + mSchedulesList.size

    override fun getItemViewType(position: Int): Int {
        val devicePosition = position - mGatewayList.size
        val schedulePosition = devicePosition - mDeviceList.size

        return when {
            position in mGatewayList.indices -> {
                mGatewayList[position].type.ordinal
            }
            devicePosition in mDeviceList.indices -> {
                mDeviceList[devicePosition].type.ordinal
            }
            schedulePosition in mSchedulesList.indices -> {
                mSchedulesList[schedulePosition].type.ordinal
            }
            else -> {
                AllDevicesDataType.SECTION.ordinal
            }
        }
    }

    fun getItem(position: Int): AllDevicesData? {
        val devicePosition = position - mGatewayList.size
        val schedulePosition = devicePosition - mDeviceList.size

        return when {
            position in mGatewayList.indices -> {
                mGatewayList[position]
            }
            devicePosition in mDeviceList.indices -> {
                mDeviceList[devicePosition]
            }
            schedulePosition in mSchedulesList.indices -> {
                mSchedulesList[schedulePosition]
            }
            else -> {
                null
            }
        }
    }

    fun setGateway(gateway: NabtoHandler.NabtoGateway) {
        this.mGatewayList.clear()
        this.mGatewayList.add(AllDevicesData(
            section = Device.DeviceCategory.HUB.name.toCapitalisedLowerCase(),
            type = AllDevicesDataType.SECTION
        ))
        this.mGatewayList.add(AllDevicesData(
            gateway = gateway,
            type = AllDevicesDataType.GATEWAY
        ))
        notifyDataSetChanged()
    }

    fun setDevices(devices: List<Pair<String, List<Device>>>) {
        this.mDeviceList.clear()
        devices.forEach {
            this.mDeviceList.add(AllDevicesData(section = it.first, type = AllDevicesDataType.SECTION))

            it.second.forEach { device ->
                this.mDeviceList.add((AllDevicesData(device = device, type = AllDevicesDataType.DEVICE)))
            }
        }
        notifyDataSetChanged()
    }

    fun setSchedules(collections: List<LogicCollection>) {
        this.mSchedulesList.clear()

        if (collections.isNotEmpty()) {
            this.mSchedulesList.add(
                AllDevicesData(
                    section = "Legacy Schedules",
                    type = AllDevicesDataType.SECTION
                )
            )

            collections.forEach { collection ->
                    this.mSchedulesList.add(
                        (AllDevicesData(
                            collection = collection,
                            type = AllDevicesDataType.LEGACY_SCHEDULE
                        ))
                    )
            }
        }
        notifyDataSetChanged()
    }

    fun setGroupMembers(groupMembers: List<GroupMember>) {
        this.mGroupMembers = groupMembers
        notifyDataSetChanged()
    }

    fun setGroups(groups: List<Group>) {
        this.mGroups = groups
        notifyDataSetChanged()
    }

    inner class DeviceCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var cardView: MaterialCardView = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardView)
        var tvName: TextView = itemView.findViewById<android.widget.TextView>(R.id.tvName)
        private var tvGroup : TextView = itemView.findViewById<android.widget.TextView>(R.id.tvGroup)
        var tvUpdating: TextView = itemView.findViewById<android.widget.TextView>(R.id.tvUpdating)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }

        fun setTile(device: Device, group: Group?) {
            tvName.text = device.name
            tvGroup.text = group?.name ?: context.getString(R.string.unassigned)
            tvUpdating.text = if (device.otaStatus == "updating") context.getString(R.string.updating) else ""
        }
    }

    inner class GatewayCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var cardView: MaterialCardView = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardView)
        var name: TextView = itemView.findViewById<android.widget.TextView>(R.id.tvName)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }

        fun setGateway(gateway: NabtoHandler.NabtoGateway) {
            name.text = gateway.name
        }
    }

    inner class LegacyScheduleCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var cardView: MaterialCardView = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardView)
        var name: TextView = itemView.findViewById<android.widget.TextView>(R.id.tvName)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }

        fun setCollection(collection: LogicCollection) {
            name.text = collection.name
        }
    }

    companion object {
        private const val TAG = "AllDevicesRecyclerViewAdapter"
    }
}
