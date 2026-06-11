package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aurora.aonev3.databinding.FragmentSecondaryModeInstructionsBinding
import com.aurora.aonev3.R

class SecondaryModeInstructionsFragment : Fragment() {

    private var _binding: FragmentSecondaryModeInstructionsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentSecondaryModeInstructionsBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    companion object {
        fun newInstance() =
            SecondaryModeInstructionsFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
