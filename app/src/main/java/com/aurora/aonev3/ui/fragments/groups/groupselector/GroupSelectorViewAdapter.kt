package com.aurora.aonev3.ui.fragments.groups.groupselector

import com.aurora.aonev3.synthetic.*
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.toCapitalisedLowerCase
import com.aurora.aonev3.ui.activities.login.afterTextChanged
import com.aurora.aonev3.ui.fragments.group.nestedgroups.AddNestedGroupsRecyclerViewAdapter.*
import com.google.android.material.card.MaterialCardView

class GroupSelectorViewAdapter internal constructor(val context: Context, private val createSpace: Boolean = true) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var groupList = ArrayList<NestedGroupData>()
    var selectedGroup: Group? = null
    var newGroup: Boolean = false
    var newGroupName = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            NestedGroupDataType.GROUP.ordinal -> {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_group_selector_tile, parent, false)
                GroupCardViewHolder(layoutView)
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
            NestedGroupDataType.GROUP -> {
                val group = item.group ?: return

                (holder as? GroupCardViewHolder)?.setGroup(group)
            }
            NestedGroupDataType.SECTION -> (holder as? SectionHeaderViewHolder)?.setSectionHeader(item.section.name.toCapitalisedLowerCase())
        }
    }

    override fun getItemCount() = groupList.size

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.type?.ordinal ?: 0
    }

    fun getItem(position: Int): NestedGroupData? = if (position in groupList.indices) groupList[position] else null

    internal fun setGroups(groups: List<Group>) {
        val realGroups = groups.filter { !it.metadata.optBoolean("is_virtual_group") }.sortedBy { it.name }
        val virtualGroups = groups.filter { it.metadata.optBoolean("is_virtual_group") }.sortedBy { it.name }

        this.groupList = ArrayList()

        if (realGroups.isNotEmpty()) {
            this.groupList.add(
                NestedGroupData(
                    null,
                    NestedGroupSection.SPACES,
                    NestedGroupDataType.SECTION
                )
            )
            this.groupList.addAll(realGroups.map {
                NestedGroupData(
                    it,
                    NestedGroupSection.SPACES,
                    NestedGroupDataType.GROUP
                )
            })
        }

        if (virtualGroups.isNotEmpty()) {
            this.groupList.add(
                NestedGroupData(
                    null,
                    NestedGroupSection.GROUPS,
                    NestedGroupDataType.SECTION
                )
            )
            this.groupList.addAll(virtualGroups.map {
                NestedGroupData(
                    it,
                    NestedGroupSection.GROUPS,
                    NestedGroupDataType.GROUP
                )
            })
        }
        notifyDataSetChanged()
    }

    fun setSelected(group: Group?) {
        val previousIndex = groupList.indexOfFirst { it.group == selectedGroup }
        val index = groupList.indexOfFirst { it.group == group }

        selectedGroup = if (selectedGroup != group) {
            group
        } else {
            null
        }

        notifyItemChanged(previousIndex)
        notifyItemChanged(index)
    }

    inner class GroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var cardView: MaterialCardView = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardView)
        var standardLayout: ConstraintLayout = itemView.standardLayout
        var name: TextView = standardLayout.tvName
        var icon: ImageView = standardLayout.ivIcon
        var newGroupLayout: ConstraintLayout = itemView.newGroupLayout
        var newGroupEt: EditText = newGroupLayout.etName

        init {
            itemView.setOnClickListener(this)
            newGroupEt.afterTextChanged {
                newGroupName = it
            }
        }

        override fun onClick(p0: View?) {
            val group = getItem(adapterPosition)?.group ?: return

            setSelected(group)
        }

        fun setGroup(group: Group) {
            standardLayout.visibility = View.VISIBLE
            newGroupLayout.visibility = View.GONE
            name.text = group.name
            icon.visibility = View.GONE
            if (group == selectedGroup) {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                name.setTextColor(context.getColor(R.color.colorPrimary))
            } else {
                cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                name.setTextColor(context.getColor(R.color.colorTextPrimary))
            }
        }
    }

}
