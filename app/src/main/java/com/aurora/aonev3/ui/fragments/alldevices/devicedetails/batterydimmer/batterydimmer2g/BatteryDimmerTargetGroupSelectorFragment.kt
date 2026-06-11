package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer2g

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.toCapitalisedLowerCase
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.BatteryDimmerSecondaryModeFragment
import com.aurora.aonev3.ui.fragments.group.nestedgroups.AddNestedGroupsRecyclerViewAdapter.*
import com.google.android.material.card.MaterialCardView

class BatteryDimmerTargetGroupSelectorFragment: Fragment() {

    private var _binding: FragmentSelectorBinding? = null
    private val binding get() = _binding!!


    private val viewModel: BatteryDimmerTargetGroupSelectorViewModel by viewModels()
    private val args: BatteryDimmerTargetGroupSelectorFragmentArgs by navArgs()
    private val returnKey: String by lazy { args.returnKey }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gateway = NabtoHandler.selectedGateway ?: return

        viewModel.selectedGroup = SyncHandler
            .groupsList
            .find {
                it.parentGateway == gateway.serial
                        && it.id == args.groupId
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gateway = NabtoHandler.selectedGateway ?: return

        tvTitle.text = getString(R.string.select_target_space)

        val adapter = GroupRecyclerViewAdapter()
        adapter.setGroups(
            SyncHandler
                .groupsList
                .filter {
                    it.parentGateway == gateway.serial
                }
        )

        with(recyclerView) {
            this.adapter = adapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(
                GridItemDecoration(
                margin,
                margin,
                margin * 2,
                margin * 2
            )
            )
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(returnKey, viewModel.selectedGroup?.id)
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance() =
            BatteryDimmerSecondaryModeFragment()
    }

    private inner class GroupRecyclerViewAdapter():
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var groupList = ArrayList<NestedGroupData>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == NestedGroupDataType.GROUP.ordinal) {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_add_nested_group_tile, parent, false)
                SecondaryModeViewHolder(layoutView)
            } else {
                val layoutView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_section_header, parent, false)
                SectionHeaderViewHolder(layoutView)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position) ?: return

            when (item.type){
                NestedGroupDataType.GROUP -> {
                    val group = item.group ?: return
                    (holder as? SecondaryModeViewHolder)?.setGroup(group)
                }
                NestedGroupDataType.SECTION -> (holder as? SectionHeaderViewHolder)?.setSectionHeader(item.section.name.toCapitalisedLowerCase())
            }
        }

        override fun getItemCount() = groupList.size

        override fun getItemViewType(position: Int): Int {
            return getItem(position)?.type?.ordinal ?: -1
        }

        fun getItem(position: Int) = if (position in groupList.indices) groupList[position] else null

        private fun selectGroup(group: Group) {
            val previousGroup = viewModel.selectedGroup
            if (group != viewModel.selectedGroup) {
                viewModel.selectedGroup = group
            } else {
                viewModel.selectedGroup = null
            }
            notifyItemChanged(groupList.indexOfFirst { it.group == previousGroup } )
            notifyItemChanged(groupList.indexOfFirst { it.group == viewModel.selectedGroup })
        }

        fun setGroups(groups: List<Group>) {
            val spaces = groups.filter { !it.metadata.optBoolean("is_virtual_group") }
            val virtualGroups = groups.filter { it.metadata.optBoolean("is_virtual_group") }

            groupList = ArrayList()
            groupList
                .add(NestedGroupData(null, NestedGroupSection.SPACES, NestedGroupDataType.SECTION))
            groupList
                .addAll(spaces
                    .map { NestedGroupData(it, NestedGroupSection.SPACES, NestedGroupDataType.GROUP) })
            groupList
                .add(NestedGroupData(null, NestedGroupSection.GROUPS, NestedGroupDataType.SECTION))
            groupList
                .addAll(virtualGroups
                    .map { NestedGroupData(it, NestedGroupSection.GROUPS, NestedGroupDataType.GROUP) })
            notifyDataSetChanged()
        }

        private inner class SecondaryModeViewHolder(itemView: View):
            RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            var name: TextView = itemView.findViewById(R.id.tvName)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                val group = getItem(adapterPosition)?.group ?: return
                selectGroup(group)
            }

            fun setGroup(group: Group) {
                name.text = group.name

                if (group == viewModel.selectedGroup) {
                    name.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                    binding.cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorTileActive))
                } else {
                    name.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
                    binding.cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorTileInactive))
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
