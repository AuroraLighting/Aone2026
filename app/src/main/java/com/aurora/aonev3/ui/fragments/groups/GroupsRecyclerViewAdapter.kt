package com.aurora.aonev3.ui.fragments.groups

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.ItemLongClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.replaceOrAdd
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.google.android.material.card.MaterialCardView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.layout_group_tile.view.*

class GroupsRecyclerViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<GroupsRecyclerViewAdapter.GroupCardViewHolder>() {

    private var groupList = emptyList<Group>()
    private var deviceList = emptyList<Device>()
    private var groupMembersList = emptyList<GroupMember>()
    private var datapointList = emptyList<GroupDatapoint>()
    var onItemClickListener: ItemClickListener? = null
    var onItemLongClickListener: ItemLongClickListener? = null
    var onStateClickListener: ItemClickListener? = null
//    var onOfflineClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupCardViewHolder {
        val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_group_tile, parent, false)
        return GroupCardViewHolder(layoutView)
    }

    override fun onBindViewHolder(holder: GroupCardViewHolder, position: Int) {
        val group = getItem(position) ?: return

        holder.setGroup(group)
    }

    override fun getItemCount() = groupList.size

    fun getItem(position: Int): Group? = if (position in groupList.indices) groupList[position] else null

    internal fun setGroups(groups: List<Group>) {
        this.groupList = groups
        notifyDataSetChanged()
    }

    internal fun setDevices(devices: List<Device>) {
        this.deviceList = devices
        notifyDataSetChanged()
    }

    internal fun setGroupMembers(members: List<GroupMember>) {
        this.groupMembersList = members
        notifyDataSetChanged()
    }

    internal fun setDatapoints(datapoints: List<GroupDatapoint>) {
        val gateway = this.groupList.firstOrNull()?.parentGateway ?: return
        val groupIds = this.groupList.toList().map { it.id }
        val newDatapoints = datapoints.toList().filter {
            it?.parentGateway == gateway
                    && it.id in groupIds
        }
        
        if (this.datapointList.isEmpty()) {
            this.datapointList = newDatapoints
            notifyDataSetChanged()
        } else {
            val existingDatapoints = ArrayList(this.datapointList)
            groupIds.forEach {  groupId ->
                val existingGroupDatapoints = existingDatapoints.filter { it.id == groupId }
                val newGroupDatapoints = newDatapoints.filter { it.id == groupId }
                val existingOnOff = existingGroupDatapoints.find { it.key == "onoff" }
                val existingLevel = existingGroupDatapoints.find { it.key == "level" }
                val existingMired = existingGroupDatapoints.find { it.key == "mired" }
                val newOnOff = newGroupDatapoints.find { it.key == "onoff" }
                val newLevel = newGroupDatapoints.find { it.key == "level" }
                val newMired = newGroupDatapoints.find { it.key == "mired" }
                var change = false
                val groupMemberIds = groupMembersList.toList().filter { it.parentGateway == gateway && it.groupId == groupId && !it.isVirtualMember }.map { it.deviceId }
                val devices = deviceList.toList().filter { it.id in groupMemberIds }
                if (devices.any { !it.online }) {
                    change = true
                }

                if (existingOnOff?.value != newOnOff?.value) {
                    if (newOnOff?.value != null) {
                        existingDatapoints.replaceOrAdd(newOnOff)
                    } else {
                        existingDatapoints.remove(existingOnOff)
                    }
                    change = true
                }
                if (existingLevel?.value != newLevel?.value) {
                    if (newLevel?.value != null) {
                        existingDatapoints.replaceOrAdd(newLevel)
                    } else {
                        existingDatapoints.remove(existingLevel)
                    }
                    change = true
                }
                if (existingMired?.value != newMired?.value) {
                    if (newMired?.value != null) {
                        existingDatapoints.replaceOrAdd(newMired)
                    } else {
                        existingDatapoints.remove(existingMired)
                    }
                    change = true
                }

                if (change) {
                    notifyItemChanged(groupList.indexOfFirst { it.id == groupId })
                }
            }

            this.datapointList = existingDatapoints
        }
    }

    inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val name: TextView = itemView.tvName
        private val state: ImageView = itemView.ivState
        private val levels: TextView = itemView.tvLevels
        private val ivOffline: ImageView = itemView.ivOffline

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            state.setOnClickListener {
                onStateClickListener?.onItemClick(state, adapterPosition)
            }
            ivOffline.setOnClickListener {
                try {
                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.device_unavailable))
                        .setMessage(context.getString(R.string.space_unavailable_body))
                        .setPositiveButton(context.getString(R.string.ok), null)
                        .create()
                        .show()
                } catch (ex: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(ex)
                }
            }
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            return onItemLongClickListener?.onItemLongClick(cardView, adapterPosition) ?: false
        }

        fun setGroup(group: Group) {
            val datapoints = datapointList.toList()
            val groupDatapoints = datapoints.filter { it.parentGateway == group.parentGateway && it.id == group.id }
            val groupOnOff = groupDatapoints.find { it.key.toLowerCase() == "onoff" }?.value as? Boolean ?: false
            val groupMemberIds = groupMembersList.toList().filter { it.parentGateway == group.parentGateway && it.groupId == group.id && !it.isVirtualMember }.map { it.deviceId }
            val devices = deviceList.toList().filter { it.id in groupMemberIds }
            val devicesOnOff = SyncHandler.deviceDatapointsList.filter { deviceDatapoint ->
                deviceDatapoint.id in groupMemberIds &&
                        devices.find { deviceDatapoint.id == it.id }?.online == true
            }.mapNotNull { it.value as? Boolean }.any { it }

            val onOff = if (SyncHandler.deviceDatapointsList.isNotEmpty() && devices.any { !it.online }) {
                devicesOnOff
            } else {
                groupOnOff
            }

            val offline = devices.isNotEmpty() && devices.all { !it.online }

            name.text = group.name

            if (onOff && !offline) {
                var level = groupDatapoints.find { it.key.toLowerCase() == "level" }?.value as? Int ?: 0
                var mired = groupDatapoints.find { it.key.toLowerCase() == "mired" }?.value as? Int ?: -1
                if (mired == 0) {
                    mired = 454
                }
                val colourTemperatureString = " | ${((1000000 / mired) / 100) * 100}K"

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

                val levelString = "$level%${if (mired != -1) colourTemperatureString else ""}"
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                name.setTextColor(context.getColor(R.color.colorTileTextActive))
                levels.setTextColor(context.getColor(R.color.colorTileTextActive))
                state.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.power_button_active))
                levels.text = levelString
            } else {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                name.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                levels.setTextColor(context.getColor(R.color.colorPrimaryBackground))
                state.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.power_button_inactive))
                levels.text = context.getString(R.string.off)
            }

            if (devices.any { !it.online }) {
                ivOffline.visibility = View.VISIBLE
            } else {
                ivOffline.visibility = View.GONE
            }
        }
    }
}