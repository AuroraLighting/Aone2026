package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.batterydimmer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aurora.aonev3.R

class SecondaryModeInstructionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_secondary_mode_instructions, container, false)
    }

    companion object {
        fun newInstance() =
            SecondaryModeInstructionsFragment()
    }
}