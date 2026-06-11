package com.aurora.aonev3.ui.fragments.groups.creategroups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.databinding.FragmentGroupHelpBinding
import com.aurora.aonev3.R

class GroupHelpFragment : Fragment() {

    private var _binding: FragmentGroupHelpBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentGroupHelpBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnOk.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance() =
            GroupHelpFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
