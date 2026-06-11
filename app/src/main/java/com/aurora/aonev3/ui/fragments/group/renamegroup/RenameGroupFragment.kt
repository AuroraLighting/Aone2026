package com.aurora.aonev3.ui.fragments.group.renamegroup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.android.synthetic.main.fragment_rename_group.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RenameGroupFragment : Fragment() {

    private val viewModel by viewModels<RenameGroupViewModel>()
    private val args: RenameGroupFragmentArgs by navArgs()
    private val groupId: Int by lazy { args.groupId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gateway = NabtoHandler.selectedGateway ?: return

        viewModel.group = SyncHandler
            .groupsList
            .find { it.parentGateway == gateway.serial && it.id == groupId } ?: return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rename_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle.text = if (!viewModel.group.metadata.optBoolean("is_virtual_group")) {
            getString(R.string.rename_space)
        } else {
            getString(R.string.rename_group)
        }

        etName.setText(viewModel.group.name)

        btnSave.setOnClickListener {
            val name = etName.text?.toString() ?: return@setOnClickListener

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.rename(name)

                activity?.runOnUiThread {
                    findNavController().popBackStack()
                }
            }
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance() = RenameGroupFragment()
    }
}