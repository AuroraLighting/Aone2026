package com.aurora.aonev3.ui.fragments.groups.eventgroupselector

import com.aurora.aonev3.synthetic.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentEventGroupSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.debug
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventViewModel
import com.aurora.aonev3.ui.fragments.groups.groupselector.GroupSelectorViewAdapter

class EventGroupSelectorFragment : Fragment() {

    protected var _binding: FragmentEventGroupSelectorBinding? = null
    protected val binding get() = _binding!!


    private val viewModel: EventGroupSelectorViewModel by viewModels()
    private lateinit var senderViewModel: IEventGroupSelectorViewModel
    private val args: EventGroupSelectorFragmentArgs by navArgs()
    private val sender: String by lazy { args.sender }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()

        if (sender == DoorSensorEventFragment::class.simpleName) {
            senderViewModel = ViewModelProvider(activity).get(DoorSensorEventViewModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentEventGroupSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        debug(sender)

        val listAdapter = GroupSelectorViewAdapter(requireActivity())

        with(binding.rvGroups) {
            adapter = listAdapter
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(
                context,
                1,
                RecyclerView.VERTICAL,
                false
            )

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(
                GridItemDecoration(margin, margin, margin * 2, margin * 2)
            )
        }

        listAdapter.setSelected(senderViewModel.targetGroup.value)

        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.getGroups(gateway).observe(viewLifecycleOwner, Observer { groups ->
                if (groups == null) return@Observer
                listAdapter.setGroups(groups.toList().sortedBy { it.name })
            })
        }

        binding.btnSave.setOnClickListener {
            val selectedGroup = listAdapter.selectedGroup ?: return@setOnClickListener
            senderViewModel.targetGroup.postValue(selectedGroup)
            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}

interface IEventGroupSelectorViewModel {
    var targetGroup: MutableLiveData<Group?>

}
