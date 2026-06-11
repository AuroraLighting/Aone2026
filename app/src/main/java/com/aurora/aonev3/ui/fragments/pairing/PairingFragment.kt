package com.aurora.aonev3.ui.fragments.pairing

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentPairingBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.ItemClickListener
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.ui.activities.SplashscreenActivity
import com.aurora.aonev3.ui.fragments.alldevices.AllDevicesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PairingFragment : Fragment() {

    private var _binding: FragmentPairingBinding? = null
    private val binding get() = _binding!!


    companion object {
        private const val TAG = "PairingFragment"
        fun newInstance() = PairingFragment()
    }

    private val viewModel: PairingViewModel by viewModels()
    private val allDevicesViewModel: AllDevicesViewModel by activityViewModels()
    private var existingDevices = emptyList<Device>()
    private var newDevices = emptyList<Device>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentPairingBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.deviceFound.postValue(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")

        val listAdapter = PairingViewAdapter(activity).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val device = getItem(position) ?: return

                    NabtoHandler.selectedGateway?.let { gateway ->
                        var ldev = device.ldevs.firstOrNull() ?: ""

                        device.ldevs.forEach {
                            if (it == "identify") {
                                ldev = it
                            }
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                DevelcoHandler.putDeviceDatapoint(
                                    gateway,
                                    device.id,
                                    ldev,
                                    "identify",
                                    6,
                                    first = true
                                )
                            } catch (err: VolleyError) {
                                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                    gateway.isConnected = false
                                    val credentials = CloudHandler.getCredentials()
                                    if (credentials.first.isEmpty()) {
                                        activity?.finishAffinity()
                                        startActivity(Intent(context, SplashscreenActivity::class.java))
                                    }
                                    NabtoHandler.openTunnel(gateway, credentials.first)
                                }
                                err.printStackTrace()
                            }
                        }

                        allDevicesViewModel.selectedDevice = device

                        val action = when (device.deviceClass) {
                            Device.DeviceClass.AURORABULB,
                            Device.DeviceClass.AURORARGBWBULB,
                            Device.DeviceClass.AURORATWBULB,
                            Device.DeviceClass.AURORASMARTPLUG,
                            Device.DeviceClass.SMARTPLUG -> PairingFragmentDirections.actionPairingFragmentToDeviceDetailFragment2()
                            Device.DeviceClass.AURORADUALSOCKET -> PairingFragmentDirections.actionPairingFragmentToDoubleSocketDetailFragment()
                            Device.DeviceClass.AURORAGEYSER -> throw Exception("Device.DeviceClass.AURORAGEYSER not implemented")
                            Device.DeviceClass.AURORAWALLDIMMER -> PairingFragmentDirections.actionPairingFragmentToWallDimmerInlineFragment()
                            Device.DeviceClass.AURORAWALLDIMMER2 -> PairingFragmentDirections.actionPairingFragmentToWallDimmerControlFragment()
                            Device.DeviceClass.BATTERYDIMMER -> PairingFragmentDirections.actionPairingFragmentToBatteryDimmer1GFragment()
                            Device.DeviceClass.BATTERYDIMMERDUAL -> PairingFragmentDirections.actionPairingFragmentToBatteryDimmer2GFragment()
                            Device.DeviceClass.DOORWINDOW,
                            Device.DeviceClass.WINDOW -> PairingFragmentDirections.actionPairingFragmentToDoorSensorDetails()
                            Device.DeviceClass.MOTION -> PairingFragmentDirections.actionPairingFragmentToMotionSensorDetails()
                            Device.DeviceClass.PTM215ZE -> PairingFragmentDirections.actionPairingFragmentToKineticDetails()
                            Device.DeviceClass.REMOTE -> PairingFragmentDirections.actionPairingFragmentToRemoteDetails()
                            else -> null
                        }
                        action?.let { findNavController().navigate(it) }
                    }
                }

            }
        }

        with(rvDiscoveredDevices) {
            adapter = listAdapter
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(
                context,
                1,
                androidx.recyclerview.widget.RecyclerView.VERTICAL,
                false
            )

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(
                com.aurora.aonev3.GridItemDecoration(margin, margin, margin * 2, margin * 2)
            )
        }

        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.getDevices(gateway).observe(viewLifecycleOwner, {
                val devices = it.toList().filter { d -> d.parentGateway == gateway.serial }

                if (!devices.isNullOrEmpty()) {
                    if (existingDevices.isEmpty()) {
                        existingDevices = devices
                    }

                    newDevices = devices.filter { device -> !existingDevices.any { it == device } }

                    if (newDevices.isNotEmpty()) {
                        if (viewModel.deviceFound.value != true) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                viewModel.deviceFound.postValue(true)
                            }, 2000)
                        }

                        activity.runOnUiThread {
                            val string = if (newDevices.size == 1) {
                                "${newDevices.size} Device found"
                            } else {
                                "${newDevices.size} Devices found"
                            }
                            tvSearching.text = string

                            listAdapter.setDevices(newDevices)
                        }
                    }
                }
            })

            viewModel.deviceFound.observe(viewLifecycleOwner, {
                if (it == true) {
                    tvTitle.text = getString(R.string.discovered_devices)
                    tvSubtitle.text = getString(R.string.discovered_devices_subtitle)
                    layoutSearching.visibility = View.GONE
                    layoutDiscovered.visibility = View.VISIBLE
                }
            })

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    DevelcoHandler.putZigbee(
                        gateway,
                        autoAdd = true,
                        enableScan = true,
                        rejectUnknown = true
                    )
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        val credentials = CloudHandler.getCredentials()
                        if (credentials.first.isEmpty()) {
                            activity?.finishAffinity()
                            startActivity(Intent(context, SplashscreenActivity::class.java))
                        }
                        NabtoHandler.openTunnel(gateway, credentials.first)
                    }

                }
            }
        }

        btnInstructions.setOnClickListener(Navigation.createNavigateOnClickListener(PairingFragmentDirections.actionPairingFragmentToPairingInstructions()))

        btnFinish.setOnClickListener {
            NabtoHandler.selectedGateway?.let { gateway ->
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    try {
                        DevelcoHandler.putZigbee(
                            gateway,
                            autoAdd = true,
                            enableScan = false,
                            rejectUnknown = true
                        )
                    } catch (err: VolleyError) {
                        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                            gateway.isConnected = false
                            val credentials = CloudHandler.getCredentials()
                            if (credentials.first.isEmpty()) {
                                activity?.finishAffinity()
                                startActivity(Intent(context, SplashscreenActivity::class.java))
                            }
                            NabtoHandler.openTunnel(gateway, credentials.first)
                        }
                        err.printStackTrace()
                    }
                }
            }

            findNavController().popBackStack()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
