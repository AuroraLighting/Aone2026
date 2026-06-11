package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.databinding.FragmentDaylightSensitivityBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.ui.fragments.alldevices.AllDevicesViewModel
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class DaylightSensitivityFragment : Fragment() {

    private var _binding: FragmentDaylightSensitivityBinding? = null
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = DaylightSensitivityFragment()
    }

    private val viewModel: DaylightSensitivityViewModel by viewModels()
    private val allDevicesViewModel: AllDevicesViewModel by activityViewModels()
    private val motionSensorEventViewModel: MotionSensorEventViewModel by activityViewModels()
    private lateinit var mDevice: Device
    private var mLux = 0
    private var mConvertedLux = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDevice = allDevicesViewModel.selectedDevice ?: throw Exception("Invalid device")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentDaylightSensitivityBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: throw Exception("Invalid activity")
        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.getAllDeviceDatapoints(gateway)
                .observe(viewLifecycleOwner, {
                    mLux = it.toList()
                        .find { deviceDatapoint ->
                            deviceDatapoint.parentGateway == mDevice.parentGateway
                                    && deviceDatapoint.id == mDevice.id
                                    && deviceDatapoint.key == "illuminance"
                        }
                        ?.value as? Int ?: 0
                    mConvertedLux = luxToLog(mLux)

                    when {
                        lux_seekbar.progress == lux_seekbar?.max -> {
                            lux_seekbar.progressDrawable =
                                ContextCompat.getDrawable(activity, R.drawable.lux_seekbar_accent)
                            tvSensitivity.setTextColor(activity.getColor(R.color.colorAccent))
                            tvSensitivity.text = getText(R.string.daylight_always)
                        }
                        lux_seekbar.progress > mLux -> {
                            lux_seekbar.progressDrawable =
                                ContextCompat.getDrawable(activity, R.drawable.lux_seekbar_green)
                            tvSensitivity.setTextColor(activity.getColor(R.color.daylightGreen))
                            tvSensitivity.text = getText(R.string.motion_will_activate)
                        }
                        else -> {
                            lux_seekbar.progressDrawable =
                                ContextCompat.getDrawable(activity, R.drawable.lux_seekbar_red)
                            tvSensitivity.setTextColor(activity.getColor(R.color.daylightRed))
                            tvSensitivity.text = getText(R.string.motion_wont_activate)
                        }
                    }
                })
        }

        lux_seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val lux = logToLux(progress)

                when {
                    progress == seekBar?.max -> {
                        lux_seekbar.progressDrawable =
                            ContextCompat.getDrawable(activity, R.drawable.lux_seekbar_accent)
                        tvSensitivity.setTextColor(activity.getColor(R.color.colorAccent))
                        tvSensitivity.text = getText(R.string.daylight_always)
                    }
                    lux > mLux -> {
                        lux_seekbar.progressDrawable =
                            ContextCompat.getDrawable(activity, R.drawable.lux_seekbar_green)
                        tvSensitivity.setTextColor(activity.getColor(R.color.daylightGreen))
                        tvSensitivity.text = getText(R.string.motion_will_activate)
                    }
                    else -> {
                        lux_seekbar.progressDrawable =
                            ContextCompat.getDrawable(activity, R.drawable.lux_seekbar_red)
                        tvSensitivity.setTextColor(activity.getColor(R.color.daylightRed))
                        tvSensitivity.text = getText(R.string.motion_wont_activate)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar ?: return
                if (seekBar.progress != seekBar.max) {
                    viewModel.lux.postValue(logToLux(seekBar.progress))
                } else {
                    viewModel.lux.postValue(null)
                }
            }
        })

        viewModel.lux.observe(viewLifecycleOwner, {
            if (it == null) {
                lux_seekbar.progress = lux_seekbar.max
            } else {
                lux_seekbar.progress = luxToLog(it)
            }
        })

        motionSensorEventViewModel.targetLux.observe(viewLifecycleOwner, {
            viewModel.lux.postValue(it)
        })

        btnSave.setOnClickListener {
            motionSensorEventViewModel.targetLux.postValue(viewModel.lux.value)

            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun luxToLog(lux: Int): Int {
        val log = log10(lux.toFloat())
        val multiplied = 3.52 * log
        val square = multiplied.pow(2)
        return square.toInt()
    }

    private fun logToLux(log: Int): Int {
        val sqrt = sqrt(log.toFloat())
        val division = sqrt / 3.52
        val pow10 = 10.0.pow(division)
        return pow10.toInt()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
