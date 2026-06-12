package com.aurora.aonev3.ui.fragments.groups.creategroups

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.data.groups.Group
import com.google.android.material.card.MaterialCardView

class CreateGroupsViewAdapter internal constructor(val context: Context) :
    RecyclerView.Adapter<CreateGroupsViewAdapter.GroupCardViewHolder>() {

    private var groupList = emptyList<Group>()
    var onItemClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupCardViewHolder {
        val layoutView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_create_group_tile, parent, false)
        return GroupCardViewHolder(layoutView)
    }

    override fun onBindViewHolder(holder: GroupCardViewHolder, position: Int) {
//        if (position < groupList.size) {
            val group = groupList[position]

            holder.name.text = group.name
            holder.icon.visibility = View.GONE
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
//        } else if (position == groupList.size) {
//            holder.name.text = context.getString(R.string.add_custom_space)
//            holder.icon.visibility = View.VISIBLE
//            holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
//        }
    }

    override fun getItemCount() = groupList.size

    fun getItem(position: Int): Group? =
        if (position in groupList.indices) groupList[position] else null

    internal fun setGroups(groups: List<Group>) {
        this.groupList = groups
        notifyDataSetChanged()
    }

    inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var cardView: MaterialCardView = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardView)
        var name: TextView = itemView.findViewById<android.widget.TextView>(R.id.tvName)
        var icon: ImageView = itemView.findViewById<android.widget.ImageView>(R.id.ivIcon)

        init {
            cardView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onItemClickListener?.onItemClick(cardView, adapterPosition)
        }
    }
}