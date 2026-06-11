package com.aurora.aonev3.ui.fragments.group.nestedgroups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentAddNestedGroupsBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddNestedGroupsFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentAddNestedGroupsBinding? = null
    private val binding get() = _binding!!


    private val args: AddNestedGroupsFragmentArgs by navArgs()
    private val viewModel: AddNestedGroupsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val group = NabtoHandler.selectedGateway?.let { gateway ->
            SyncHandler
                .groupsList
                .find { it.parentGateway == gateway.serial && it.id == args.groupId }
        }

        viewModel.group = group
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentAddNestedGroupsBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.group == null) {
            findNavController().popBackStack()
            return
        }

        val adapter = AddNestedGroupsRecyclerViewAdapter(requireContext(), viewModel.group)

        with(binding.recyclerView) {
            this.adapter = adapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(
                margin,
                margin,
                margin * 2,
                margin * 2
            ))
        }

        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.getGroups(gateway).observe(viewLifecycleOwner) {
                val groups = it.toList()
                val nestedGroups = groups.filter { group ->
                    group.id in viewModel.nestedGroupIds
                }

                adapter.setGroups(groups)
                adapter.setSelectedGroups(nestedGroups)
            }
        }

        binding.btnSave.setOnClickListener {
            val ids = adapter.selectedGroups.map { it.id }.sorted()
            layoutGreyOut?.visibility = View.VISIBLE

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                NabtoHandler.selectedGateway?.let { gateway ->
                    viewModel.saveNestedGroups(gateway, ids)
                }

                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    layoutGreyOut?.visibility = View.GONE
                    findNavController().popBackStack()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.menu.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.binding.menu.nested_groups_menu, popup.menu)
            if (viewModel.group?.metadata?.optBoolean("is_virtual_group") == true) {
                popup.binding.menu.findItem(R.id.create).title =
                    getString(R.string.create_new_group)
            }
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.create) {
            val action = AddNestedGroupsFragmentDirections.actionAddNestedGroupsFragmentToCreateGroupFragment(viewModel.group?.metadata?.optBoolean("is_virtual_group") ?: false)
            findNavController().navigate(action)

            true
        } else {
            false
        }
    }

    companion object {
        fun newInstance() =
            AddNestedGroupsFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
