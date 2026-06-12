package com.aurora.aonev3.ui.fragments.pairing

import com.aurora.aonev3.synthetic.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentPairingInstructionsBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R

class PairingInstructionsFragment : Fragment() {

    protected var _binding: FragmentPairingInstructionsBinding? = null
    protected val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentPairingInstructionsBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.recyclerView) {
            adapter = PairingInstructionRecyclerViewAdapter()
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 3, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }
    }

    companion object {
        fun newInstance() =
            PairingInstructionsFragment()
    }

    private inner class PairingInstructionRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val instructionsList = PairingInstruction.values()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layoutView = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.layout_pairing_instruction_tile, parent, false)
            return PairingInstructionsViewHolder(layoutView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position !in instructionsList.indices) return
            val item = instructionsList[position]
            (holder as? PairingInstructionsViewHolder)?.setItem(item)
        }

        override fun getItemCount() = instructionsList.size

        fun getItem(position: Int) = if (position in instructionsList.indices) instructionsList[position] else null

        private inner class PairingInstructionsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            private val nameTv: TextView = itemView.findViewById(R.id.tvName)
            private val iconIv: ImageView = itemView.findViewById(R.id.ivIcon)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(p0: View?) {
                val item = getItem(adapterPosition) ?: return
                val action = PairingInstructionsFragmentDirections.actionPairingInstructionsFragmentToInstructionsFragment(item)
                findNavController().navigate(action)
            }

            fun setItem(item: PairingInstruction) {
                nameTv.text = item.displayName
                iconIv.setImageDrawable(ContextCompat.getDrawable(requireContext(), item.resourceId))
            }
        }
    }

    enum class PairingInstruction(val displayName: String, val resourceId: Int) {
        AONE("AOne Controller", R.drawable.aone_pairing),
        BATTERY_DIMMER("Battery Dimmer", R.drawable.dimmer_pairing),
        REMOTE("Battery Remote", R.drawable.remote_pairing),
        DOOR("Door / Window", R.drawable.door_window_pairing),
        SOCKET("Double Socket", R.drawable.double_socket_pairing),
        LAMP("Lamp", R.drawable.fw_pairing),
        KINETIC("Kinetic", R.drawable.kinetic_pairing),
        MOTION("Motion Sensor", R.drawable.motion_pairing),
        PLUG("Plug in Adapter", R.drawable.plug_pairing),
        RELAY("Smart Relay", R.drawable.relay_pairing),
        WALLDIMMER("Rotary Dimmer", R.drawable.dimmer_pairing)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
