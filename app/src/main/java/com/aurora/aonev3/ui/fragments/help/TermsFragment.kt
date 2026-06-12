package com.aurora.aonev3.ui.fragments.help

import com.aurora.aonev3.synthetic.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aurora.aonev3.databinding.FragmentTermsBinding
import com.aurora.aonev3.R

class TermsFragment : Fragment() {

    protected var _binding: FragmentTermsBinding? = null
    protected val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentTermsBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    companion object {
        fun newInstance() =
            TermsFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
