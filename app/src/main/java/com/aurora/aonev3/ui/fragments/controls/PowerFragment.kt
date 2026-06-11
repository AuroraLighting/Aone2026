package com.aurora.aonev3.ui.fragments.controls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.databinding.FragmentPowerBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.SyncHandler
import java.text.DecimalFormat
import kotlin.math.ln

class PowerFragment : Fragment() {

    private var _binding: FragmentPowerBinding? = null
    private val binding get() = _binding!!


    private val args: PowerFragmentArgs by navArgs()
    private val deviceId: Int by lazy { args.deviceId }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentPowerBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val power = SyncHandler.deviceDatapointsList.find { datapoint -> datapoint.id == deviceId && datapoint.key == "power" }?.value as? Number

        if (power == null) {
            context?.let {
                Toast.makeText(context, "Failed to get power", Toast.LENGTH_SHORT).show()
            }
            findNavController().popBackStack()
            return
        }

        val df = DecimalFormat("0.##")
        val powerLog = if (power != 0) {
                (ln(power.toFloat()) * 10).toInt()
        } else {
            0
        }

        power_value.text = df.format(power)

        context?.let { context ->
            when {
                powerLog < 50 -> power_progress_bar.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_low)
                powerLog < 70 -> power_progress_bar.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_med)
                else -> power_progress_bar.progressDrawable =
                    ContextCompat.getDrawable(context, R.drawable.circular_progress_bar_high)
            }
        }

        power_progress_bar.progress = powerLog
    }

    companion object {
        fun newInstance() =
            PowerFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
