package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.databinding.FragmentTimeoutBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.debug
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventViewModel

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val SENDER = "param1"

/**
 * A simple [Fragment] subclass.
 * Use the [TimeoutFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TimeoutFragment : Fragment() {

    private var _binding: FragmentTimeoutBinding? = null
    private val binding get() = _binding!!


    private lateinit var viewModel: ITimeoutViewModel
    private val args: TimeoutFragmentArgs by navArgs()
    private val sender: String by lazy { args.sender }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()

        viewModel = if (sender == MotionSensorEventFragment::class.simpleName) {
            ViewModelProvider(activity).get(MotionSensorEventViewModel::class.java)
        } else {
            ViewModelProvider(activity).get(DoorSensorEventViewModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentTimeoutBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        debug(sender)

        if (sender != DoorSensorEventFragment::class.simpleName) {
            binding.infoIv.visibility = View.GONE
        } else {
            binding.infoIv.visibility = View.VISIBLE
        }

        viewModel.timeout.observe(viewLifecycleOwner, { value ->
            if (value == null) {
                val minutes = 0
                val seconds = 0

                binding.minutePicker.value = minutes
                binding.secondPicker.value = seconds
            } else {
                val minutes = value / 60
                val seconds = value % 60

                binding.minutePicker.value = minutes
                binding.secondPicker.value = seconds
            }
        })

        binding.minutePicker.maxValue = 60
        binding.secondPicker.maxValue = 59

        binding.secondPicker.setOnValueChangedListener { _, oldVal, newVal ->
            if (oldVal == 59 && newVal == 0) {
                if (binding.minutePicker.value != 60) {
                    binding.minutePicker.value++
                }
            }
            if (oldVal == 0 && newVal == 59) {
                if (binding.minutePicker.value != 0) {
                    binding.minutePicker.value--
                }
            }
        }

        binding.minutePicker.setOnValueChangedListener { _, oldVal, newVal ->
            if (newVal == 60) {
                binding.secondPicker.maxValue = 0
            } else if (oldVal == 60) {
                binding.secondPicker.maxValue = 59
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.timeout.postValue(binding.minutePicker.value * 60 + binding.secondPicker.value)

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.infoIv.setOnClickListener {
            val activity = activity ?: return@setOnClickListener

            if (!activity.isFinishing) {
                AlertDialog.Builder(activity)
                    .setMessage(getString(R.string.timeout_keep_lights_on))
                    .setPositiveButton(getString(R.string.ok), null)
                    .create()
                    .show()
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param sender Sending fragment.
         * @return A new instance of fragment TimeoutFragmentFragment.
         */
        fun newInstance(sender: String?) =
            TimeoutFragment().apply {
                arguments = Bundle().apply {
                    putString(SENDER, sender)
                }
            }
    }
}

interface ITimeoutViewModel {
    var timeout: MutableLiveData<Int?>


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
