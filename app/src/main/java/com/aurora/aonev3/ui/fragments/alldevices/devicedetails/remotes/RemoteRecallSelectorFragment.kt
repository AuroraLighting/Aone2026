package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.remotes

import com.aurora.aonev3.synthetic.*
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.google.android.material.card.MaterialCardView

class RemoteRecallSelectorFragment : Fragment() {

    protected var _binding: FragmentSelectorBinding? = null
    protected val binding get() = _binding!!


    val viewModel: RemoteDetailViewModel by activityViewModels()
    private var recallValues: Array<RecallMode> = emptyArray()

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
        val activity = activity ?: return

        if (viewModel.targetDevice.value != null) {
            recallValues = arrayOf(RecallMode.STEP_COLOUR_TEMPERATURE, RecallMode.SET_TO_50)
        }
        if (viewModel.targetGroup.value != null) {
            viewModel.targetGroup.value?.let { group ->
                val groupMembersIds = SyncHandler
                    .groupMembersList
                    .filter { gm -> gm.parentGateway == group.first.parentGateway && gm.groupId == group.first.id }
                    .map { it.deviceId }
                val devices = SyncHandler
                    .devicesList
                    .filter { it.parentGateway == group.first.parentGateway && it.id in groupMembersIds }

                recallValues = if (devices.any { it.deviceClass in arrayOf(Device.DeviceClass.AURORATWBULB, Device.DeviceClass.AURORARGBWBULB) }) {
                    arrayOf(RecallMode.CYCLE_SCENES, RecallMode.STEP_COLOUR_TEMPERATURE, RecallMode.SET_TO_50)
                } else {
                    arrayOf(RecallMode.CYCLE_SCENES, RecallMode.SET_TO_50)
                }
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

        binding.tvTitle.text = getString(R.string.set_the_recall_button_functionality)

        with(binding.recyclerView) {
            adapter = eventDayAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        binding.btnSave.setOnClickListener {
            val mode = eventDayAdapter.getSelected()?.first ?: return@setOnClickListener
            viewModel.targetRecall.postValue(mode)

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.targetRecall.observe(viewLifecycleOwner, { mode ->
            mode?.let {
                val items = eventDayAdapter.getItems().map { item -> if (item.first == mode) { Pair(mode, true) } else { Pair(item.first, false) } }
                eventDayAdapter.setItems(items)
            }
        })
    }

    companion object {
         private const val TAG = "RemoteRecallSelectorFragment"

        fun newInstance() =
            RemoteRecallSelectorFragment()
    }

    inner class RemoteRecallViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<RemoteRecallViewAdapter.RemoteRecallCardViewHolder>() {

        private var modeList = RecallMode.values().map { Pair(it, false) }.filter { it.first in recallValues }.toList()
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

        fun getItem(position: Int): Pair<RecallMode, Boolean>? = if (position in modeList.indices) modeList[position] else null

        fun getItems() = modeList

        fun getSelected(): Pair<RecallMode, Boolean>? = modeList.find { it.second }

        fun setItems(items: List<Pair<RecallMode, Boolean>>) {
            modeList = items
            notifyDataSetChanged()
        }

        inner class RemoteRecallCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            var name: TextView = itemView.findViewById(R.id.tvName)

            init {
                itemView.setOnClickListener(this)
                itemView.findViewById<android.view.View>(R.id.ivIcon).visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                onItemClickListener?.onItemClick(cardView, adapterPosition)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
