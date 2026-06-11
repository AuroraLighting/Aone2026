package com.aurora.aonev3.ui.fragments.pairing.kinetic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.databinding.FragmentKineticStoreChannelBinding
import com.aurora.aonev3.R

class KineticStoreChannelFragment : Fragment() {

    private var _binding: FragmentKineticStoreChannelBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentKineticStoreChannelBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDone.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance() =
            KineticStoreChannelFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
