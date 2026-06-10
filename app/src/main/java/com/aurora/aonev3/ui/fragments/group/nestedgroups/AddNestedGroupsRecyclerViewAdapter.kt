package com.aurora.aonev3.ui.fragments.group.nestedgroups

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.toCapitalisedLowerCase
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.layout_add_nested_group_tile.view.*

class AddNestedGroupsRecyclerViewAdapter internal constructor(val context: Context, val group: Group?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mGroups = ArrayList<NestedGroupData>()
    var selectedGroups = ArrayList<Group>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == NestedGroupDataType.GROUP.ordinal) {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_add_nested_group_tile, parent, false)
            GroupCardViewHolder(layoutView)
        } else {
            val layoutView =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_section_header, parent, false)
            SectionHeaderViewHolder(layoutView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (item.type) {
            NestedGroupDataType.GROUP -> item.group?.let {
                (holder as? GroupCardViewHolder)?.setGroup(it)
            }
            NestedGroupDataType.SECTION -> (holder as? SectionHeaderViewHolder)?.setSectionHeader(item.section.name.toCapitalisedLowerCase())
        }
    }

    override fun getItemCount() = mGroups.size

    fun getItem(position: Int): NestedGroupData? =
        if (position in mGroups.indices) mGroups[position] else null

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.type?.ordinal ?: 0
    }

    internal fun setGroups(groups: List<Group>) {
        val spaces = groups.filter { !it.metadata.optBoolean("is_virtual_group") && it.id != group?.id && group?.metadata?.optBoolean("is_virtual_group") != true }
        val virtualGroups = groups.filter { it.metadata.optBoolean("is_virtual_group") && it.id != group?.id }

        this.mGroups = ArrayList()
        if (spaces.isNotEmpty()) {
            this.mGroups.add(
                NestedGroupData(
                    null,
                    NestedGroupSection.SPACES,
                    NestedGroupDataType.SECTION
                )
            )
            this.mGroups.addAll(spaces.map {
                NestedGroupData(
                    it,
                    NestedGroupSection.SPACES,
                    NestedGroupDataType.GROUP
                )
            })
        }
        if (virtualGroups.isNotEmpty()) {
            this.mGroups.add(
                NestedGroupData(
                    null,
                    NestedGroupSection.GROUPS,
                    NestedGroupDataType.SECTION
                )
            )
            this.mGroups.addAll(virtualGroups.map {
                NestedGroupData(
                    it,
                    NestedGroupSection.GROUPS,
                    NestedGroupDataType.GROUP
                )
            })
        }
        notifyDataSetChanged()
    }

    internal fun setSelectedGroups(groups: List<Group>) {
        this.selectedGroups = groups.toMutableList() as ArrayList<Group>
        notifyDataSetChanged()
    }

    internal fun selectGroup(group: Group) {
        if (group !in selectedGroups) {
            selectedGroups.add(group)
        } else {
            selectedGroups.remove(group)
        }
        notifyItemChanged(mGroups.indexOfFirst { it.group ==group })
    }

    inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val cardView: MaterialCardView = itemView.cardView
        private val name: TextView = itemView.tvName

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            if (adapterPosition in mGroups.indices) {
                val group = mGroups[adapterPosition].group ?: return
                selectGroup(group)
            }
        }

        fun setGroup(group: Group) {
            name.text = group.name

            if (group in selectedGroups) {
                name.setTextColor(context.getColor(R.color.colorPrimary))
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
            } else {
                name.setTextColor(context.getColor(R.color.colorTextPrimary))
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
            }
        }
    }

    data class NestedGroupData(val group: Group?, val section: NestedGroupSection, val type: NestedGroupDataType)

    enum class NestedGroupSection {
        SPACES,
        GROUPS
    }

    enum class NestedGroupDataType {
        GROUP,
        SECTION
    }
}
