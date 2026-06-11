package com.aurora.aonev3.ui.fragments.groups.groupselector

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentGroupSelectorBinding
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.ui.fragments.alldevices.AllDevicesViewModel
import com.aurora.aonev3.ui.fragments.groups.GroupsViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class GroupSelectorFragment : Fragment() {

    private var _binding: FragmentGroupSelectorBinding? = null
    private val binding get() = _binding!!


    companion object {
        private const val TAG = "GroupSelectorFragment"
        fun newInstance() = GroupSelectorFragment()
    }

    private val viewModel by viewModels<GroupSelectorViewModel>()
    private val groupsViewModel by activityViewModels<GroupsViewModel>()
    private val allDevicesViewModel by activityViewModels<AllDevicesViewModel>()

    private var groupMembersForDevice: List<GroupMember> = emptyList()
    private var device: Device? = null
    private var group: Group? = null
    private var mGroups: List<Group> = emptyList()

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentGroupSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        device = allDevicesViewModel.selectedDevice
        group = allDevicesViewModel.selectedGroup
        NabtoHandler.selectedGateway?.let { gateway ->
            allDevicesViewModel.getGroupMembers(gateway)
                .observe(viewLifecycleOwner, { members ->
                    members?.let {
                        groupMembersForDevice =
                            members.toList().filter { groupMember -> groupMember.deviceId == device?.id }
                    }
                })
        }

        val listAdapter = GroupSelectorViewAdapter(requireActivity()).apply {
            setSelected(group)
        }

        with(rvGroups) {
            adapter = listAdapter
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widgbinding.et.GridLayoutManager(
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

        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.getGroups(gateway).observe(viewLifecycleOwner, Observer { groups ->
                if (groups == null) return@Observer
                mGroups = groups.toList().sortedBy { it.name }
                listAdapter.setGroups(mGroups)
            })
        }

        btnCancel.setOnClickListener {
            try {
                val v = activity?.currentFocus
                v?.let {
                    val imm =
                        activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)
                }
                v?.clearFocus()
            } catch (ex: Exception) {
                crashlytics.recordException(ex)
            }
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            listAdapter.newGroup = false
            try {
                val v = activity?.currentFocus
                v?.let {
                    val imm =
                        activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)
                }
                v?.clearFocus()
            } catch (ex: Exception) {
                crashlytics.recordException(ex)
            }

            val groupLiveData = MutableLiveData<Group>(null)
            NabtoHandler.selectedGateway?.let { gateway ->
                val device = device ?: return@let

                if (listAdapter.selectedGroup == null && listAdapter.newGroupName.isBlank()) {
                    Toast.makeText(
                        activity,
                        getString(R.string.please_select_a_space),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@let
                }

                btnSave.isEnabled = false
                activity?.layoutGreyOut?.visibility = View.VISIBLE

                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    try {
                        viewModel.deleteGroupMembers(groupMembersForDevice)
                    } catch (err: TimeoutException) {
                        val activity = activity ?: return@launch
                        val json = try {
                            JSONObject(err.message ?: "")
                        } catch (jsonErr: JSONException) {
                            JSONObject()
                        }

                        val group = SyncHandler
                            .groupsList
                            .find { it.parentGateway == device.parentGateway && it.id == json.optInt("groupId") }

                        activity.runOnUiThread {
                            if (!activity.isFinishing) {
                                AlertDialog.Builder(activity)
                                    .setMessage("Failed to remove ${device.name} from ${group?.name}, check it's still plugged in / has power")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                        btnSave.isEnabled = true
                                        activity.layoutGreyOut.visibility = View.GONE
                                    }
                                    .create()
                                    .show()
                            }
                        }

                        return@launch
                    } catch (err: UnknownApiException) {
                        val activity = activity ?: return@launch
                        val json = try {
                            JSONObject(err.message ?: "")
                        } catch (jsonErr: JSONException) {
                            JSONObject()
                        }

                        val group = SyncHandler
                            .groupsList
                            .find { it.parentGateway == device.parentGateway && it.id == json.optInt("groupId") }

                        activity.runOnUiThread {
                            if (!activity.isFinishing) {
                                AlertDialog.Builder(activity)
                                    .setMessage("Failed to remove ${device.name} from ${group?.name}. ${json.optString("message")}")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                        btnSave.isEnabled = true
                                        activity.layoutGreyOut.visibility = View.GONE
                                    }
                                    .create()
                                    .show()
                            }
                        }
                        return@launch
                    }

                    if (listAdapter.selectedGroup == null) {

                        val groupId = viewModel.createGroup(listAdapter.newGroupName)
                            ?: return@launch

                        groupsViewModel.viewModelScope.launch(Dispatchers.Main) {
                            val groups = groupsViewModel.getGroups(gateway)//(gateway, groupId)
                            groups.observe(viewLifecycleOwner, {
                                val group = it?.toList()?.find { group -> group.parentGateway == gateway.serial && group.id == groupId } ?: return@observe

                                groups.removeObservers(viewLifecycleOwner)
                                groupLiveData.postValue(group)
                            })
                        }
                    } else {
                        groupLiveData.postValue(listAdapter.selectedGroup)
                    }
                }

                groupLiveData.observe(viewLifecycleOwner, Observer { group ->
                    group ?: return@Observer

                    if (device.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                        || device.deviceClass == Device.DeviceClass.AURORAWALLDIMMER
                    ) {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            if (!addGroupMember(device, group)) {

                                activity?.runOnUiThread {
                                    activity?.layoutGreyOut?.visibility = View.GONE
                                    findNavController().popBackStack()
                                }
                            }
                        }
                    } else if (device.getDeviceCategory() == Device.DeviceCategory.POWER ||
                            device.getDeviceCategory() == Device.DeviceCategory.SOCKETS) {
                        activity?.runOnUiThread {
                            if (activity?.isFinishing != true) {
                                AlertDialog.Builder(activity)
                                    .setMessage(getString(R.string.control_device_with_space))
                                    .setPositiveButton(R.string.yes) { _, _ ->
                                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                                            if (!addGroupMember(device, group)) {

                                                activity?.runOnUiThread {
                                                    activity?.layoutGreyOut?.visibility = View.GONE
                                                    findNavController().popBackStack()
                                                }
                                            }
                                        }
                                    }
                                    .setNegativeButton(R.string.no) { _, _ ->
                                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                                            viewModel.addVirtualGroupMember(device, group)

                                            activity?.runOnUiThread {
                                                activity?.layoutGreyOut?.visibility = View.GONE
                                                findNavController().popBackStack()
                                            }
                                        }
                                    }
                                    .setOnCancelListener {
                                        btnSave.isEnabled = true
                                        activity?.layoutGreyOut?.visibility = View.GONE
                                    }
                                    .create()
                                    .show()
                            }
                        }
                    } else {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            viewModel.addVirtualGroupMember(device, group)

                            activity?.runOnUiThread {
                                activity?.layoutGreyOut?.visibility = View.GONE
                                findNavController().popBackStack()
                            }
                        }
                    }
                })
            }
        }
    }

    private suspend fun addGroupMember(device: Device, group: Group): Boolean {
        var error = false
        try {
            viewModel.addGroupMember(device, group)
        } catch (err: TimeoutException) {
            val activity = activity ?: return true

            activity.runOnUiThread {
                if (!activity.isFinishing) {
                    AlertDialog.Builder(activity)
                        .setMessage("Failed to add ${device.name} to ${group.name}, check it's still plugged in / has power")
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            btnSave?.isEnabled = true
                            activity.layoutGreyOut.visibility = View.GONE
                        }
                        .create()
                        .show()
                }
            }
            error = true
        } catch (err: InsufficientSpaceException) {
            val activity = activity ?: return true

            activity.runOnUiThread {
                if (!activity.isFinishing) {
                    AlertDialog.Builder(activity)
                        .setMessage("Failed to add ${device.name} to ${group.name} as it's in too many Spaces / Groups")
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            btnSave.isEnabled = true
                            activity.layoutGreyOut.visibility = View.GONE
                        }
                        .create()
                        .show()
                }
            }
            error = true
        } catch (err: UnknownApiException) {
            val activity = activity ?: return true
            val json = try {
                JSONObject(err.message ?: "")
            } catch (jsonErr: JSONException) {
                JSONObject()
            }

            activity.runOnUiThread {
                if (!activity.isFinishing) {
                    AlertDialog.Builder(activity)
                        .setMessage("Failed to add ${device.name} to ${group.name}. ${json.optString("message")}")
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            btnSave.isEnabled = true
                            activity.layoutGreyOut.visibility = View.GONE
                        }
                        .create()
                        .show()
                }
            }
            error = true
        }

        return error
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
