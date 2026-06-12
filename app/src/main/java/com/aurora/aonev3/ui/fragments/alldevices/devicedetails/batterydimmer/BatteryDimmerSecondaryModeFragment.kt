package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer.batterydimmer1g.BatteryDimmerMode
import com.google.android.material.card.MaterialCardView

class BatteryDimmerSecondaryModeFragment : Fragment() {

    private var _binding: FragmentSelectorBinding? = null
    private val binding get() = _binding!!


    private val viewModel: BatteryDimmerSecondaryModeViewModel by viewModels()
    private val args: BatteryDimmerSecondaryModeFragmentArgs by navArgs()
    private val returnKey: String by lazy { args.returnKey }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.mode = args.mode
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

        binding.tvTitle.text = getString(R.string.secondary_mode)

        val adapter = SecondaryModeRecyclerViewAdapter()

        with(binding.recyclerView) {
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

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(returnKey, viewModel.mode)
            findNavController().popBackStack()
        }

        binding.ivHelp.visibility = View.VISIBLE

        binding.ivHelp.setOnClickListener(
            Navigation.
            createNavigateOnClickListener(BatteryDimmerSecondaryModeFragmentDirections
                .actionBatteryDimmerSecondaryModeFragmentToSecondaryModeInstructionsFragment())
        )
    }

    companion object {
        fun newInstance() =
            BatteryDimmerSecondaryModeFragment()
    }

    private inner class SecondaryModeRecyclerViewAdapter:
        RecyclerView.Adapter<SecondaryModeRecyclerViewAdapter.SecondaryModeViewHolder>() {

        private var mModes: List<BatteryDimmerMode> = BatteryDimmerMode.values().toList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SecondaryModeViewHolder {
            val layoutView = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_add_nested_group_tile, parent, false)
            return SecondaryModeViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: SecondaryModeViewHolder, position: Int) {
            if (position !in mModes.indices) return
            val mode = mModes[position]

            holder.setMode(mode)
        }

        override fun getItemCount() = mModes.size

        private fun selectMode(mode: BatteryDimmerMode) {
            val previousMode = viewModel.mode
            if (mode != viewModel.mode) {
                viewModel.mode = mode
            } else {
                viewModel.mode = BatteryDimmerMode.NONE
            }
            notifyItemChanged(mModes.indexOf(previousMode))
            notifyItemChanged(mModes.indexOf(viewModel.mode))
        }

        private inner class SecondaryModeViewHolder(itemView: View):
            RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            var name: TextView = itemView.findViewById(R.id.tvName)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                if (adapterPosition in mModes.indices) {
                    val device = mModes[adapterPosition]
                    selectMode(device)
                }
            }

            fun setMode(mode: BatteryDimmerMode) {
                name.text = mode.displayName

                if (mode == viewModel.mode) {
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
