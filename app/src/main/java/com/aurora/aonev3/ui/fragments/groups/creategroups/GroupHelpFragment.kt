package com.aurora.aonev3.ui.fragments.groups.creategroups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.R
import kotlinx.android.synthetic.main.fragment_group_help.*

class GroupHelpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group_help, container, false)
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
}