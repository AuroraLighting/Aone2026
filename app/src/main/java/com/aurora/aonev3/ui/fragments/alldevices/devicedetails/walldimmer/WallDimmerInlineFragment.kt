package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.walldimmer

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.DeviceDetailFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_device_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class WallDimmerInlineFragment : DeviceDetailFragment() {

    private val wallDimmerInlineViewModel: WallDimmerInlineViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        val device = viewModel.selectedDevice ?: return

        wallDimmerBacklightOuterLayout.visibility = View.VISIBLE

        NabtoHandler.selectedGateway?.let { gateway ->
            val datapoints = wallDimmerInlineViewModel
                .getAllDeviceDatapoints(gateway)
//            , intArrayOf(device.id), arrayOf("ringstate"))

            datapoints.observe(viewLifecycleOwner) {
                val context = context ?: return@observe
                val backlight = it.toList().find { deviceDatapoint ->
                    deviceDatapoint.parentGateway == gateway.serial
                            && deviceDatapoint.id == device.id
                            && deviceDatapoint.key == "ringstate"
                }?.value as? Boolean ?: return@observe
                datapoints.removeObservers(viewLifecycleOwner)

                backlightSwitch.isChecked = backlight

                if (backlight) {
                    backlightLayout.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.colorTileActive)
                    tvBacklight.setTextColor(
                        ContextCompat.getColor(context, R.color.colorPrimary)
                    )
                    tvBacklight.text = getString(R.string.backlight_on)
                } else {
                    backlightLayout.backgroundTintList =
                        ContextCompat.getColorStateList(context, R.color.colorTileInactive)
                    tvBacklight.setTextColor(
                        ContextCompat.getColor(context, R.color.colorPrimaryBackground)
                    )
                    tvBacklight.text = getString(R.string.backlight_off)
                }
            }
        }

        backlightSwitch.setOnClickListener {
            wallDimmerInlineViewModel.viewModelScope.launch(Dispatchers.IO) {
                wallDimmerInlineViewModel.setBacklight(device, backlightSwitch.isChecked)
            }
        }

        backlightSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                backlightLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                tvBacklight.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
                tvBacklight.text = getString(R.string.backlight_on)
            } else {
                backlightLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvBacklight.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryBackground)
                )
                tvBacklight.text = getString(R.string.backlight_off)
            }
        }
    }

    override fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            btnSave.isEnabled = false
            activity?.layoutGreyOut?.visibility = View.VISIBLE
            val device = viewModel.selectedDevice ?: return@OnClickListener

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.updateDeviceName(device)

                activity?.runOnUiThread {
                    btnSave.isEnabled = true
                    activity?.layoutGreyOut?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WallDimmerInlineFragment"
        fun newInstance() =
            WallDimmerInlineFragment()
    }
}