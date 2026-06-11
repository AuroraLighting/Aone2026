package com.aurora.aonev3.ui.fragments.pairing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.databinding.FragmentAoneInstructionsBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.ui.fragments.pairing.PairingInstructionsFragment.PairingInstruction.*

class InstructionsFragment : Fragment() {

    private var _binding: FragmentAoneInstructionsBinding? = null
    private val binding get() = _binding!!


    private val viewModel: PairingViewModel by viewModels()
    private val args: InstructionsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return when (args.instructions) {
            AONE -> inflater.inflate(R.binding.layout.fragment_aone_instructions, container, false)
            BATTERY_DIMMER -> inflater.inflate(R.binding.layout.fragment_battery_dimmer_instructions, container, false)
            REMOTE -> inflater.inflate(R.binding.layout.fragment_remote_instructions, container, false)
            PLUG -> inflater.inflate(R.binding.layout.fragment_plug_instructions, container, false)
            DOOR,
            SOCKET,
            LAMP,
            MOTION,
            RELAY,
            WALLDIMMER -> inflater.inflate(R.binding.layout.fragment_generic_instructions, container, false)
            KINETIC -> inflater.inflate(R.binding.layout.fragment_kinetic_instructions, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (args.instructions) {
            DOOR -> {
                binding.tvTitle.text = getString(R.string.pairing_your_door_window_sensor)
                imageView.setImageDrawable(ContextCompat
                    .getDrawable(requireContext(), R.drawable.door_hold_button))
                textView.text = getString(R.string.pairing_develco)
            }
            SOCKET -> {
                binding.tvTitle.text = getString(R.string.pairing_your_socket)
                imageView.setImageDrawable(ContextCompat
                    .getDrawable(requireContext(), R.drawable.socket_hold_button))
                textView.text = getString(R.string.pairing_socket)
            }
            LAMP -> {
                binding.tvTitle.text = getString(R.string.pairing_your_lamp)
                imageView.setImageDrawable(ContextCompat
                    .getDrawable(requireContext(), R.drawable.wall_switch))
                textView.text = getString(R.string.pairing_lamp)
            }
            MOTION -> {
                binding.tvTitle.text = getString(R.string.pairing_your_motion_sensor)
                imageView.setImageDrawable(ContextCompat
                    .getDrawable(requireContext(), R.drawable.motion_hold_button))
                textView.text = getString(R.string.pairing_develco)
            }
            RELAY -> {
                binding.tvTitle.text = getString(R.string.pairing_your_relay)
                imageView.setImageDrawable(ContextCompat
                    .getDrawable(requireContext(), R.drawable.relay_hold_button))
                textView.text = getString(R.string.pairing_develco)
            }
            WALLDIMMER -> {
                binding.tvTitle.text = getString(R.string.pairing_your_walldimmer)
                imageView.setImageDrawable(ContextCompat
                    .getDrawable(requireContext(), R.drawable.dimmer_hold_button))
                textView.text = getString(R.string.pairing_walldimmer)
            }
            KINETIC -> {
                val gateway = NabtoHandler.selectedGateway ?: return
                var existingDevices: List<Device>? = null

                viewModel.getAllDeviceDatapoints(gateway).observe(viewLifecycleOwner) { datapoints ->
                    if (datapoints.isNullOrEmpty()) return@observe

                    val channel = (datapoints
                        .toList()
                        .find { it.ldev == "network" && it.key == "channel" }
                        ?.value) as? Int ?: return@observe
                    textView.text = getString(R.string.kinetic_pairing, channel - 10)
                }

                viewModel.getDevices(gateway).observe(viewLifecycleOwner) {
                    val devices = it.toList().filter { d -> d.parentGateway == gateway.serial }

                    if (existingDevices == null) {
                        existingDevices = devices
                    }
                    val existingDevices1 = existingDevices ?: return@observe

                    val newDevices = devices.filter { device -> !existingDevices1.any { it == device } }

                    if (newDevices.isNotEmpty()) {
                        val action = InstructionsFragmentDirections.actionInstructionsFragmentToKineticStopFragment()

                        try {
                            findNavController().navigate(action)
                        } catch (ex: IllegalArgumentException) {
                            ex.printStackTrace()
                            Log.e(TAG, "Tried to navigate from incorrect destination")
                        }
                    }
                }
            }
            else -> {}
        }
    }

    companion object {
        private const val TAG = "InstructionsFragment"
        fun newInstance() =
            InstructionsFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
