package com.aurora.aonev3.ui.fragments.controls

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.aurora.aonev3.databinding.ControlsFragmentRgbwBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.GroupDatapoint
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.ui.views.SegmentedColourPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val ARG_DEVICE_ID = "deviceId"
private const val ARG_GROUP_ID = "groupId"
private const val ARG_IS_RGB = "isRgb"
private const val ARG_IS_CT = "isCt"
private const val ARG_CT_MAX = "ctMax"
private const val ARG_CT_MIN = "ctMin"

@SuppressLint("SetTextI18n")
class ControlsFragment : Fragment() {

    private var _binding: ControlsFragmentRgbwBinding? = null
    private val binding get() = _binding!!


    companion object {
        const val TAG = "ControlsFragment"
        fun newInstance(deviceId: Int? = null,
                        groupId: Int? = null,
                        isRgb: Boolean? = null,
                        isCt: Boolean? = null,
                        ctMax: Int? = null,
                        ctMin: Int? = null) =
            ControlsFragment().apply {
                arguments = Bundle().apply {
                    deviceId?.let { putInt(ARG_DEVICE_ID, it) }
                    groupId?.let { putInt(ARG_GROUP_ID, it) }
                    isRgb?.let { putBoolean(ARG_IS_RGB, it) }
                    isCt?.let { putBoolean(ARG_IS_CT, it) }
                    ctMax?.let { putInt(ARG_CT_MAX, it) }
                    ctMin?.let { putInt(ARG_CT_MIN, it) }
                }
            }
    }

    private val viewModel: ControlsViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            NabtoHandler.selectedGateway?.let { gateway ->
                if (!gateway.isConnected) return
                if (viewModel.selectedDevice == null && viewModel.selectedGroup == null) {
                    viewModel.selectedDevice = SyncHandler
                        .devicesList
                        .find { device -> device.parentGateway == gateway.serial && device.id == it.getInt(ARG_DEVICE_ID, -1) }
                    viewModel.selectedGroup = SyncHandler
                        .groupsList
                        .find { group ->
                            group.parentGateway == gateway.serial
                                    && group.id == it.getInt(ARG_GROUP_ID, -1)
                        }
                    viewModel.isGroupRgb = it.getBoolean(ARG_IS_RGB)
                    viewModel.isGroupCt = it.getBoolean(ARG_IS_CT)
                    viewModel.groupColourTemperatureMax = it.getInt(ARG_CT_MAX, 0)
                    viewModel.groupColourTemperatureMin = it.getInt(ARG_CT_MIN, 999)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val device: Device? = viewModel.selectedDevice
        val isGroupRgb = viewModel.isGroupRgb
        val isGroupCt = viewModel.isGroupCt

        return if (device == null) {
            when {
                isGroupRgb -> {
                    inflater.inflate(R.layout.controls_fragment_rgbw, container, false)
                }
                isGroupCt -> {
                    inflater.inflate(R.layout.controls_fragment_tw, container, false)
                }
                else -> {
                    inflater.inflate(R.layout.controls_fragment_fw, container, false)
                }
            }
        } else {
            if (device.deviceClass == Device.DeviceClass.AURORABULB || device.deviceClass == Device.DeviceClass.AURORAWALLDIMMER) {
                inflater.inflate(R.layout.controls_fragment_fw, container, false)
            } else if (device.deviceClass == Device.DeviceClass.AURORATWBULB) {
                inflater.inflate(R.layout.controls_fragment_tw, container, false)
            } else {
                inflater.inflate(R.layout.controls_fragment_rgbw, container, false)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gateway = NabtoHandler.selectedGateway ?: return
        if (!gateway.isConnected) return

        val device: Device? = viewModel.selectedDevice
        val group: Group? = viewModel.selectedGroup
        var colourTemperatureMin = 154
        var colourTemperatureMax = 370
        val hsv = FloatArray(3) { index -> if (index != 2) 361f else 1f }

        val deviceDatapoints: MutableLiveDataArrayList<DeviceDatapoint>? = viewModel.getDeviceDatapoints(gateway)
        val groupDatapoints: MutableLiveDataArrayList<GroupDatapoint>? = viewModel.getGroupDatapoints(gateway)

        if (device == null) {
            colourTemperatureMin = viewModel.groupColourTemperatureMin
            colourTemperatureMax = viewModel.groupColourTemperatureMax

            if (colourTemperatureMax > 454) {
                colourTemperatureMax = 454
            }
        }

        deviceDatapoints?.observe(viewLifecycleOwner, {
            val datapoints = it.toList().filter { dp ->
                dp.parentGateway == gateway.serial
                        && dp.id == viewModel.selectedDevice?.id
                        && dp.key in arrayOf("level", "mired", "hue", "sat", "colourtempmin", "colourtempmax")
            }
            datapoints.find { dp -> dp.key == "level" }?.also { dp ->
                var level = dp.value as? Int ?: 100

                when {
                    level % 10 == 1 && level != 1 -> {
                        level -= 1
                    }
                    level % 10 == 9 -> {
                        level += 1
                    }
                    level % 10 == 4 -> {
                        level += 1
                    }
                    level % 10 == 6 -> {
                        level += 1
                    }
                }

                activity?.runOnUiThread {
                    binding.tvLevel?.text = "$level%"
                    binding.level_seekbar?.progress = 100 - level
                }
            }
            datapoints.find { dp -> dp.key == "colourtempmin" }?.also { miredMin ->
                val value = miredMin.value as? Int ?: 154
                colourTemperatureMin = value
                activity?.runOnUiThread {
                    binding.colour_temperature_seekbar?.max =
                        colourTemperatureMax - colourTemperatureMin + (0.05 * (colourTemperatureMax - colourTemperatureMin)).toInt()
                }
            }
            datapoints.find { dp -> dp.key == "colourtempmax" }?.also { miredMax ->
                var value = miredMax.value as? Int ?: 454

                if (value > 454) {
                    value = 454
                }

                colourTemperatureMax = value
                activity?.runOnUiThread {
                    binding.colour_temperature_seekbar?.max =
                        colourTemperatureMax - colourTemperatureMin + (0.05 * (colourTemperatureMax - colourTemperatureMin)).toInt()
                }
            }
            datapoints.find { dp -> dp.key == "mired" }?.also { mired ->
                var value = mired.value as? Int ?: 454

                if (value == 0) {
                    value = 454
                }

                activity?.runOnUiThread {
                    binding.tvColourTemperature?.text = "${((1000000 / value) / 100) * 100}K"
                    binding.colour_temperature_seekbar?.progress =
                        (value as? Int ?: 220) - colourTemperatureMin
                }
            }
            datapoints.find { dp -> dp.key == "sat" }?.also { sat ->
                if (hsv[1] != 361f) return@also

                val value = sat.value as? Int ?: 0
                hsv[1] = value.toFloat() / 254f

//                binding.colourPicker?.color = Color.HSVToColor(hsv)
            }
            datapoints.find { dp -> dp.key == "hue" }?.also { hue ->
                if (hsv[0] != 361f) return@also

                val value = hue.value as? Int ?: 0
                hsv[0] = value.toFloat()

//                binding.colourPicker?.color = Color.HSVToColor(hsv)
            }
        })

        if (group != null) {
            binding.colour_temperature_seekbar?.max =
                colourTemperatureMax - colourTemperatureMin + (0.05 * (colourTemperatureMax - colourTemperatureMin)).toInt()
        }

        groupDatapoints?.observe(viewLifecycleOwner, {
            val datapoints = it.toList().filter { datapoint -> datapoint.parentGateway == gateway.serial && datapoint.id == viewModel.selectedGroup?.id }
            datapoints.find { dp -> dp.key == "level" }?.also { dp ->
                var level = dp.value as? Int ?: 100

                when {
                    level % 10 == 1 && level != 1 -> {
                        level -= 1
                    }
                    level % 10 == 9 -> {
                        level += 1
                    }
                    level % 10 == 4 -> {
                        level += 1
                    }
                    level % 10 == 6 -> {
                        level += 1
                    }
                }

                activity?.runOnUiThread {
                    binding.tvLevel?.text = "$level%"
                    binding.level_seekbar?.progress = 100 - level
                }
            }
            datapoints.find { dp -> dp.key == "mired" }?.also { mired ->
                var value = mired.value as? Int ?: 454
                if (value == 0) {
                    value = 454
                }
                activity?.runOnUiThread {
                    binding.tvColourTemperature?.text = "${((1000000 / value) / 100) * 100}K"
                    binding.colour_temperature_seekbar?.progress =
                        (value as? Int ?: 220) - colourTemperatureMin
                }
            }
            datapoints.find { dp -> dp.key == "sat" }?.also { sat ->
                if (hsv[1] != 361f) return@also

                val value = sat.value as? Int ?: 0
                hsv[1] = value.toFloat() / 254f

//                binding.colourPicker?.color = Color.HSVToColor(hsv)
            }
            datapoints.find { dp -> dp.key == "hue" }?.also { hue ->
                if (hsv[0] != 361f) return@also

                val value = hue.value as? Int ?: 0
                hsv[0] = value.toFloat()

//                binding.colourPicker?.color = Color.HSVToColor(hsv)
            }
        })

        binding.level_seekbar?.max = 105
        binding.level_seekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                p0 ?: return
                if (p1 > 100) {
                    p0.progress = 100
                }
                activity?.runOnUiThread {
                    binding.tvLevel?.text = "${100 - p0.progress}%"
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                p0?.progress?.let { level ->
                    viewModel.setLevel(level)
                }
            }
        })

        binding.colour_temperature_seekbar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            var tracking = false

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (!tracking) return
                p0 ?: return
                if (p1 > colourTemperatureMax - colourTemperatureMin) {
                    p0.progress = colourTemperatureMax - colourTemperatureMin
                }
                activity?.runOnUiThread {
                    view.binding.tvColourTemperature?.text =
                        "${((1000000 / (p0.progress + colourTemperatureMin)) / 100) * 100}K"
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                tracking = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                tracking = false
                p0?.progress?.let { mired ->
                    viewModel.setMired(mired + colourTemperatureMin)
                }
            }
        })

        binding.colourPicker?.setOnColourPickerChangeListener(object :
            SegmentedColourPicker.OnColourPickerChangeListener {
            override fun onStopTrackingTouch(picker: SegmentedColourPicker?) {
                val hueSat = binding.colourPicker?.selectedSegment?.hueSat ?: return

                viewModel.setColour(hueSat)
            }
        })

        binding.btnStop?.setOnClickListener {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.setHueMove(false)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
            }
        }

        binding.btnSlow?.setOnClickListener {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.setHueMoveRate(1)
                viewModel.setHueMove(true)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
            }
        }

        binding.btnFast?.setOnClickListener {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.setHueMoveRate(16)
                viewModel.setHueMove(true)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
            }
        }
    }

    override fun onDetach() {
        App.actionSuccessful()
        viewModel.reportStates()
        super.onDetach()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
