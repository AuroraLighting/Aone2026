package com.aurora.aonev3.ui.fragments.pairing.kinetic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.R
import kotlinx.android.synthetic.main.fragment_kinetic_store_channel.*

class KineticStoreChannelFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_kinetic_store_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnDone.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance() =
            KineticStoreChannelFragment()
    }
}