package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doublesocket

import com.aurora.aonev3.synthetic.*
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.aurora.aonev3.allowEditing
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.DeviceDetailFragment

class DoubleSocketDetailFragment : DeviceDetailFragment() {

    private val doubleSocketViewModel: DoubleSocketViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        val gateway = NabtoHandler.selectedGateway ?: return
        val device = viewModel.selectedDevice ?: return

        btnIdentify.visibility = View.GONE
        doubleSocketLevelOuterLayout.visibility = View.VISIBLE

        doubleSocketViewModel.getAllDeviceDatapoints(gateway).observe(viewLifecycleOwner) {
            val datapoints = it.toList().find { datapoint ->  datapoint.parentGateway == gateway.serial && datapoint.id == device.id && datapoint.key == "level" }
            val level = (datapoints?.value as? Int) ?: return@observe

            when {
                level >= 75 -> {
                    doubleSocketLevelButtons.check(btnHigh.id)
                }
                level in 40..74 -> {
                    doubleSocketLevelButtons.check(btnMedium.id)
                }
                else -> {
                    doubleSocketLevelButtons.check(btnLow.id)
                }
            }
        }

        doubleSocketLevelButtons.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    btnHigh.id -> doubleSocketViewModel.setLevel(device, 100)
                    btnMedium.id -> doubleSocketViewModel.setLevel(device, 50)
                    btnLow.id -> doubleSocketViewModel.setLevel(device, 25)
                }
            }
        }

        doubleSocketLevelButtons.isClickable = allowEditing()
    }

    companion object {
        private const val TAG = "DoubleSocketDetailFragment"
        fun newInstance() =
            DoubleSocketDetailFragment()
    }
}
