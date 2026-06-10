package com.aurora.aonev3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.android.synthetic.main.fragment_double_socket_power.*
import java.text.DecimalFormat
import kotlin.math.ln

class DoubleSocketPowerFragment : Fragment() {

    private val args: DoubleSocketPowerFragmentArgs by navArgs()
    private val deviceId: Int by lazy { args.deviceId }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_double_socket_power, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val powers = SyncHandler.deviceDatapointsList.filter { datapoint -> datapoint.id == deviceId && datapoint.key == "power" }
        val power1 = powers.find { it.ldev == "socket1" }?.value as? Number
        val power2 = powers.find { it.ldev == "socket2" }?.value as? Number

        if (power1 == null) {
            context?.let {
                Toast.makeText(context, "Failed to get power", Toast.LENGTH_SHORT).show()
            }
            findNavController().popBackStack()
            return
        }

        val df = DecimalFormat("0.##")
        val power1Log = if (power1 != 0) {
            (ln(power1.toFloat()) * 10).toInt()
        } else {
            0
        }

        power1_value.text = df.format(power1)

        context?.let { context ->
            when {
                power1Log < 50 -> power_progress_bar1.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_low)
                power1Log < 70 -> power_progress_bar1.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_med)
                else -> power_progress_bar1.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_high)
            }
        }

        power_progress_bar1.progress = power1Log

        if (power2 == null) {
            context?.let {
                Toast.makeText(context, "Failed to get power", Toast.LENGTH_SHORT).show()
            }
            findNavController().popBackStack()
            return
        }

        val power2Log = if (power2 != 0) {
            (ln(power2.toFloat()) * 10).toInt()
        } else {
            0
        }

        power2_value.text = df.format(power2)

        context?.let { context ->
            when {
                power2Log < 50 -> power_progress_bar2.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_low)
                power2Log < 70 -> power_progress_bar2.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_med)
                else -> power_progress_bar2.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_high)
            }
        }

        power_progress_bar2.progress = power2Log
    }

    companion object {
        fun newInstance() =
            DoubleSocketPowerFragment()
    }
}