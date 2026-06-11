package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.walldimmer

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
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.google.android.material.card.MaterialCardView

class WallDimmerControlTargetFragment : Fragment() {

    private var _binding: FragmentSelectorBinding? = null
    private val binding get() = _binding!!


    private val viewModel: WallDimmerControlTargetViewModel by viewModels()
    private val args: WallDimmerControlTargetFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gateway = NabtoHandler.selectedGateway ?: return

        viewModel.target = SyncHandler
            .devicesList
            .find { it.parentGateway == gateway.serial && it.id == args.targetId }
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

        tvTitle.text = getString(R.string.select_inline_dimmer)

        val inlineDimmers = SyncHandler
            .devicesList
            .filter { it.parentGateway == gateway.serial && it.deviceClass == Device.DeviceClass.AURORAWALLDIMMER }

        val adapter = WallDimmerControlTargetRecyclerViewAdapter()
        adapter.setDevices(inlineDimmers)

        with(recyclerView) {
            this.adapter = adapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(
                margin,
                margin,
                margin * 2,
                margin * 2
            ))
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set("targetId", viewModel.target?.id ?: -1)
            findNavController().popBackStack() }
    }

    companion object {
        fun newInstance() =
            WallDimmerControlTargetFragment()
    }

    inner class WallDimmerControlTargetRecyclerViewAdapter():
        RecyclerView.Adapter<WallDimmerControlTargetRecyclerViewAdapter.WallDimmerControlTargetViewHolder>() {

        private var mDevices: List<Device> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallDimmerControlTargetViewHolder {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_add_nested_group_tile, parent, false)
            return WallDimmerControlTargetViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: WallDimmerControlTargetViewHolder, position: Int) {
            if (position !in mDevices.indices) return
            val device = mDevices[position]

            holder.setDevice(device)
        }

        override fun getItemCount() = mDevices.size

        fun getItem(position: Int) = if (position in mDevices.indices) mDevices[position] else null

        fun setDevices(devices: List<Device>) {
            mDevices = devices
            notifyDataSetChanged()
        }

        private fun selectDevice(device: Device) {
            val previousDevice = viewModel.target
            if (device != viewModel.target) {
                viewModel.target = device
            } else {
                viewModel.target = null
            }
            notifyItemChanged(mDevices.indexOf(previousDevice))
            notifyItemChanged(mDevices.indexOf(device))
        }

        inner class WallDimmerControlTargetViewHolder(itemView: View):
            RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var binding.cardView: MaterialCardView = itemView.binding.cardView
            var name: TextView = itemView.binding.tvName

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                if (adapterPosition in mDevices.indices) {
                    val device = mDevices[adapterPosition]
                    selectDevice(device)
                }
            }

            fun setDevice(device: Device) {
                name.text = device.name

                if (device == viewModel.target) {
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
