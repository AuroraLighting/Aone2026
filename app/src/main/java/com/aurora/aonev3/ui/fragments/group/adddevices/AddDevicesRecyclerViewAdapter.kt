package com.aurora.aonev3.ui.fragments.group.adddevices

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.layout_all_device_tile.view.*

class AddDevicesRecyclerViewAdapter internal constructor(val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mDeviceList = ArrayList<AddDevicesData>()
    private var mGroupMembers = emptyList<GroupMember>()
    private var mGroups = emptyList<Group>()
    var inGroup: BooleanArray = booleanArrayOf()
    var inSelectedGroup: BooleanArray = booleanArrayOf()
    private var mGroup: Group? = null
    var onItemClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == AddDevicesDataType.DEVICE.ordinal) {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_all_device_tile, parent, false)
            GroupCardViewHolder(layoutView)
        } else {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_section_header, parent, false)
            SectionHeaderViewHolder(layoutView)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < mDeviceList.size) {
            val item = mDeviceList[position]
            if (item.type == AddDevicesDataType.DEVICE) {
                val device = item.device ?: return
                if (holder !is GroupCardViewHolder) return
                val groupMember =
                    mGroupMembers.find { it.parentGateway == device.parentGateway && it.deviceId == device.id }
                val group =
                    mGroups.find { it.parentGateway == groupMember?.parentGateway && it.id == groupMember.groupId }

                holder.setDevice(device, group)
            } else {
                if (holder !is SectionHeaderViewHolder) return

                item.section?.let { holder.setSectionHeader(it) }
            }
        }
    }

    override fun getItemCount() = mDeviceList.size

    override fun getItemViewType(position: Int): Int {
        return mDeviceList[position].type.ordinal
    }

    fun getItem(position: Int): Device? =
        if (position in mDeviceList.indices) mDeviceList[position].device else null

    internal fun setDevices(devices: List<Pair<String, List<Device>>>) {
        this.mDeviceList.clear()
        devices.forEach {
            this.mDeviceList.add(AddDevicesData(section = it.first, type = AddDevicesDataType.SECTION))

            it.second.forEach { device ->
                this.mDeviceList.add((AddDevicesData(device = device, type = AddDevicesDataType.DEVICE)))
            }
        }
        this.inGroup = BooleanArray(mDeviceList.size)
        this.inSelectedGroup = BooleanArray(mDeviceList.size)
        notifyDataSetChanged()
    }

    internal fun setGroupMembers(groupMembers: List<GroupMember>) {
        this.mGroupMembers = groupMembers
        notifyDataSetChanged()
    }

    internal fun setGroups(groups: List<Group>) {
        this.mGroups = groups
        notifyDataSetChanged()
    }

    internal fun setGroup(group: Group) {
        this.mGroup = group
        notifyDataSetChanged()
    }

    inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val name: TextView = itemView.tvName
        private val groupTv: TextView = itemView.tvGroup
        private val updating: TextView = itemView.tvUpdating

        init {
            updating.visibility = View.GONE
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }

        fun setDevice(device: Device, group: Group?) {
            name.text = device.name
            groupTv.text = if (group != null) {
                inGroup[adapterPosition] = true
                group.name
            } else {
                inGroup[adapterPosition] = false
                context.getString(R.string.unassigned)
            }

            if (group == mGroup) {
                inSelectedGroup[adapterPosition] = true
                name.setTextColor(context.getColor(R.color.colorPrimary))
                groupTv.setTextColor(context.getColor(R.color.colorPrimary))
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
            } else {
                inSelectedGroup[adapterPosition] = false
                name.setTextColor(context.getColor(R.color.colorTextPrimary))
                groupTv.setTextColor(context.getColor(R.color.colorTextPrimary))
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
            }
        }
    }
}

data class AddDevicesData(val device: Device? = null, val section: String? = null, val type: AddDevicesDataType)

enum class AddDevicesDataType {
    DEVICE,
    SECTION
}
