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
import com.aurora.aonev3.databinding.FragmentRenameGroupBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RenameGroupFragment : Fragment() {

    private var _binding: FragmentRenameGroupBinding? = null
    private val binding get() = _binding!!


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
        return run {
            _binding = FragmentRenameGroupBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = if (!viewModel.group.metadata.optBoolean("is_virtual_group")) {
            getString(R.string.rename_space)
        } else {
            getString(R.string.rename_group)
        }

        binding.etName.setText(viewModel.group.name)

        binding.btnSave.setOnClickListener {
            val name = etName.text?.toString() ?: return@setOnClickListener

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                viewModel.rename(name)

                activity?.runOnUiThread {
                    findNavController().popBackStack()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance() = RenameGroupFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
