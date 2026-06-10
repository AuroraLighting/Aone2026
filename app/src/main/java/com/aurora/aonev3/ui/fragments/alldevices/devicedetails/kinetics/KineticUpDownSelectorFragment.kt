package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.kinetics

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.fragment_selector.*
import kotlinx.android.synthetic.main.layout_group_selector_tile.view.*

class KineticUpDownSelectorFragment : Fragment() {

    private val viewModel: KineticDetailViewModel by activityViewModels()
    private val navArgs: KineticUpDownSelectorFragmentArgs by navArgs()
    private val isSecondaryMode: Boolean by lazy { navArgs.secondaryMode }
    private var recallValues: Array<UpDownMode> = emptyArray()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return

        viewModel.selectedGroup?.let { group ->
            val groupMembersIds = SyncHandler
                .groupMembersList
                .filter { gm -> gm.parentGateway == group.parentGateway && gm.groupId == group.id }
                .map { it.deviceId }
            val devices = SyncHandler
                .devicesList
                .filter { it.parentGateway == group.parentGateway && it.id in groupMembersIds }

            recallValues = if (devices.any { it.deviceClass in arrayOf(Device.DeviceClass.AURORATWBULB, Device.DeviceClass.AURORARGBWBULB) }) {
                arrayOf(UpDownMode.CYCLE_SCENES, UpDownMode.STEP_COLOUR_TEMPERATURE, UpDownMode.STEP)
            } else {
                arrayOf(UpDownMode.CYCLE_SCENES, UpDownMode.STEP)
            }
        }

        val eventDayAdapter = RemoteRecallViewAdapter(activity).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val items = getItems().mapIndexed { index, item -> if (index == position) { Pair(item.first, !item.second) } else Pair(item.first, false) }
                    setItems(items)
                }
            }
        }

        tvTitle.text = getString(R.string.assign_functionality_of_the_up_down_buttons)

        with(recyclerView) {
            adapter = eventDayAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        btnSave.setOnClickListener {
            val mode = eventDayAdapter.getSelected()?.first ?: return@setOnClickListener
            if (!isSecondaryMode)
                viewModel.targetMode.postValue(mode)
            else
                viewModel.secondaryMode.postValue(mode)

            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        if (!isSecondaryMode) {
            viewModel.targetMode.observe(viewLifecycleOwner, { mode ->
                mode?.let {
                    val items = eventDayAdapter.getItems().map { item ->
                        if (item.first == mode) {
                            Pair(mode, true)
                        } else {
                            Pair(item.first, false)
                        }
                    }
                    eventDayAdapter.setItems(items)
                }
            })
        } else {
            viewModel.secondaryMode.observe(viewLifecycleOwner, { mode ->
                mode?.let {
                    val items = eventDayAdapter.getItems().map { item ->
                        if (item.first == mode) {
                            Pair(mode, true)
                        } else {
                            Pair(item.first, false)
                        }
                    }
                    eventDayAdapter.setItems(items)
                }
            })
        }
    }

    companion object {
        fun newInstance() =
            KineticUpDownSelectorFragment()
    }

    inner class RemoteRecallViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<RemoteRecallViewAdapter.RemoteRecallCardViewHolder>() {

        private var modeList = recallValues.map { Pair(it, false) }.toList()
        var onItemClickListener: ItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteRecallCardViewHolder {
            val layoutView = LayoutInflater.from(parent.context).inflate(R.layout.layout_group_selector_tile, parent, false)
            return RemoteRecallCardViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: RemoteRecallCardViewHolder, position: Int) {
            if (position < modeList.size) {
                val mode = modeList[position]

                holder.name.text = mode.first.displayName

                if (mode.second) {
                    holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    holder.name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    holder.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    holder.name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }

        override fun getItemCount() = modeList.size

        fun getItem(position: Int): Pair<UpDownMode, Boolean>? = if (position in modeList.indices) modeList[position] else null

        fun getItems() = modeList

        fun getSelected(): Pair<UpDownMode, Boolean>? = modeList.find { it.second }

        fun setItems(items: List<Pair<UpDownMode, Boolean>>) {
            modeList = items
            notifyDataSetChanged()
        }

        inner class RemoteRecallCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.cardView
            var name: TextView = itemView.tvName

            init {
                itemView.setOnClickListener(this)
                itemView.ivIcon.visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(cardView, adapterPosition)
            }
        }
    }
}
