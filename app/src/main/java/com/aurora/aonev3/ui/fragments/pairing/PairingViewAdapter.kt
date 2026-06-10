package com.aurora.aonev3.ui.fragments.pairing

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.data.devices.Device
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.layout_pairing_device_tile.view.*

class PairingViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<PairingViewAdapter.DeviceCardViewHolder>() {

    private var deviceList = emptyList<Device>()
    var onItemClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceCardViewHolder {
        val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_pairing_device_tile, parent, false)
        return DeviceCardViewHolder(layoutView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DeviceCardViewHolder, position: Int) {
        if (position < deviceList.size) {
            val device = deviceList[position]

            if (device.defaultName != "Unknown device") {
                holder.name.text = device.name
                holder.progress.visibility = View.GONE
                holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                holder.name.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            } else {
                holder.name.text = context.getString(R.string.identifying_device)
                holder.progress.visibility = View.VISIBLE
                holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                holder.name.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            }
        } else if (position == deviceList.size) {
            holder.name.text = context.getString(R.string.searching_for_new_devices)
            holder.progress.visibility = View.VISIBLE
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.searchingNewDevicesBackground))
            holder.name.setTextColor(ContextCompat.getColor(context, R.color.searchingNewDevicesText))
        }
    }

    override fun getItemCount() = deviceList.size + 1

    fun getItem(position: Int): Device? = if (position in deviceList.indices) deviceList[position] else null

    internal fun setDevices(devices: List<Device>) {
        this.deviceList = devices
        notifyDataSetChanged()
    }

    inner class DeviceCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var cardView: MaterialCardView = itemView.cardView
        var name: TextView = itemView.tvName
        var progress: ProgressBar = itemView.progress_circular
        var icon: ImageView = itemView.ivIcon

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }
    }
}