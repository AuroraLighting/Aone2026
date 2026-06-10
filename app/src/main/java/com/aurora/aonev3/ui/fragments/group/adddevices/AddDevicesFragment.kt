package com.aurora.aonev3.ui.fragments.group.adddevices

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.devices.Device.DeviceCategory.*
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.ui.fragments.alldevices.AllDevicesViewModel
import kotlinx.android.synthetic.main.fragment_add_devices.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class AddDevicesFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    companion object {
        private const val TAG = "AddDevicesFragment"
        fun newInstance() = AddDevicesFragment()
    }

    private val allDevicesViewModel: AllDevicesViewModel by activityViewModels()
    private val viewModel: AddGroupDevicesViewModel by viewModels()
    private val args: AddDevicesFragmentArgs by navArgs()
    private val mGroupId: Int by lazy { args.groupId }

    private var mGroup: Group? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return
        val group = SyncHandler.groupsList.find { it.id == mGroupId } ?: return

        val lightsAdapter = getListAdapter(activity)

        mGroup = group
        viewModel.group = group
        lightsAdapter.setGroup(group)

        setUpListRecyclerView(rvLights, lightsAdapter)

        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let
            val devicesLiveData = allDevicesViewModel.getDevices(gateway)
            val groupsLiveData = allDevicesViewModel.getGroups(gateway)
            val groupMembersLiveData = allDevicesViewModel.getGroupMembers(gateway)

            devicesLiveData.observe(viewLifecycleOwner) {
                val devices = it
                    ?.filter { d -> d.parentGateway == gateway.serial }
                    ?.sortedBy { device -> device.name }

                devices?.let {
                    val lights = devices.filter { device ->
                        device.getDeviceCategory() == LIGHTS
                    }
                    val sensors = devices.filter { device ->
                        device.getDeviceCategory() == SENSORS
                    }
                    val power = devices.filter { device ->
                        device.getDeviceCategory() == POWER
                    }
                    val sockets = devices.filter { device ->
                        device.getDeviceCategory() == SOCKETS
                    }
                    val switches = devices.filter { device ->
                        device.getDeviceCategory() == SWITCHES
                    }
                    val devicesList = ArrayList<Pair<String, List<Device>>>()

                    if (lights.isNotEmpty()) {
                        devicesList.add(Pair(LIGHTS.name.toCapitalisedLowerCase(), lights))
                    }
                    if (power.isNotEmpty()) {
                        devicesList.add(Pair(POWER.name.toCapitalisedLowerCase(), power))
                    }
                    if (sockets.isNotEmpty()) {
                        devicesList.add(Pair(SOCKETS.name.toCapitalisedLowerCase(), sockets))
                    }
                    if (sensors.isNotEmpty()) {
                        devicesList.add(Pair(SENSORS.name.toCapitalisedLowerCase(), sensors))
                    }
                    if (switches.isNotEmpty()) {
                        devicesList.add(Pair(SWITCHES.name.toCapitalisedLowerCase(), switches))
                    }

                    lightsAdapter.setDevices(devicesList)

                    if (devicesList.isNotEmpty()) {
                        rvLights.visibility = View.VISIBLE
                    } else {
                        rvLights.visibility = View.GONE
                    }
                }
            }

            groupsLiveData.observe(viewLifecycleOwner) { groups ->
                groups?.let {
                    lightsAdapter.setGroups(it)
                }
            }

            groupMembersLiveData.observe(viewLifecycleOwner) { groupMembers ->
                groupMembers?.let {
                    lightsAdapter.setGroupMembers(it)
                }
            }
        }

        btnSave.setOnClickListener {
            findNavController().popBackStack()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        menu.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.all_devices_menu, popup.menu)
            popup.setOnMenuItemClickListener(this@AddDevicesFragment)
            popup.show()
        }
    }

    private fun setUpListRecyclerView(
        rv: RecyclerView,
        listAdapter: AddDevicesRecyclerViewAdapter
    ) {
        with(rv) {
            adapter = listAdapter
            setHasFixedSize(false)
            layoutManager =
                GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(
                GridItemDecoration(margin, margin, margin * 2, margin * 2)
            )
        }
    }

    private fun getListAdapter(activity: FragmentActivity): AddDevicesRecyclerViewAdapter {
        return AddDevicesRecyclerViewAdapter(activity).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val device = getItem(position) ?: return
                    val inGroup = try {
                        inGroup[position]
                    } catch (ex: IndexOutOfBoundsException) {
                        false
                    }
                    val inSelectedGroup = try {
                        inSelectedGroup[position]
                    } catch (ex: IndexOutOfBoundsException) {
                        false
                    }

                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        if (!inGroup) {
                            addGroupMember(device)
                        } else if (inSelectedGroup) {
                            val groupMembers = SyncHandler
                                .groupMembersList
                                .filter { it.parentGateway == device.parentGateway && it.deviceId == device.id }

                            deleteGroupMembers(groupMembers, device)
                        } else {
                            val groupMembers = SyncHandler
                                .groupMembersList
                                .filter { it.parentGateway == device.parentGateway && it.deviceId == device.id }
                            if (SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean(
                                    "warnUniqueGroup",
                                    true
                                )
                            ) {
                                if (!activity.isFinishing) {
                                    activity.runOnUiThread {
                                        AlertDialog.Builder(activity)
                                            .setMessage(getString(R.string.uniqueGroupWarning))
                                            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                                    if (!deleteGroupMembers(groupMembers, device)) {
                                                        addGroupMember(device)
                                                    }
                                                }
                                            }
                                            .setNeutralButton(getString(R.string.yes_dont_warn)) { _, _ ->
                                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                                    if (!deleteGroupMembers(groupMembers, device)) {
                                                        addGroupMember(device)
                                                    }
                                                }

                                                SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                                                    putBoolean("warnUniqueGroup", false)
                                                }
                                            }
                                            .setNegativeButton(getString(R.string.no), null)
                                            .create()
                                            .show()
                                    }
                                }
                            } else {
                                if (!deleteGroupMembers(groupMembers, device)) {
                                    addGroupMember(device)
                                }
                            }
                        }
                    }
                }

                private suspend fun addGroupMember(device: Device) {
                    try {
                        when (device.getDeviceCategory()) {
                            LIGHTS -> {
                                viewModel.addGroupMember(device)
                            }
                            SOCKETS,
                            POWER -> {
                                if (!activity.isFinishing) {
                                    activity.runOnUiThread {
                                        AlertDialog.Builder(activity)
                                            .setMessage(getString(R.string.control_device_with_space))
                                            .setPositiveButton(R.string.yes) { _, _ ->
                                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                                    viewModel.addGroupMember(device)
                                                }
                                            }
                                            .setNegativeButton(R.string.no) { _, _ ->
                                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                                    viewModel.addVirtualGroupMember(device)
                                                }
                                            }
                                            .create()
                                            .show()
                                    }
                                }
                            }
                            SENSORS -> viewModel.addVirtualGroupMember(device)
                            SWITCHES -> {
                                if (device.deviceClass == Device.DeviceClass.AURORAWALLDIMMER) {
                                    viewModel.addGroupMember(device)
                                } else {
                                    viewModel.addVirtualGroupMember(device)
                                }
                            }
                            else -> {
                            }
                        }
                    } catch (err: TimeoutException) {
                        val activity = getActivity() ?: return
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
                                    .setMessage("Failed to add ${device.name} to ${group?.name}, check it's still plugged in / has power")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ -> }
                                    .create()
                                    .show()
                            }
                        }
                    } catch (err: InsufficientSpaceException) {
                        val activity = getActivity() ?: return
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
                                    .setMessage("Failed to add ${device.name} to ${group?.name} as it's in too many Spaces / Groups")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ -> }
                                    .create()
                                    .show()
                            }
                        }
                    } catch (err: UnknownApiException) {
                        val activity = getActivity() ?: return
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
                                    .setMessage("Failed to add ${device.name} to ${group?.name}. ${json.optString("message")}")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ -> }
                                    .create()
                                    .show()
                            }
                        }
                    }
                }

                private suspend fun deleteGroupMembers(groupMembers: List<GroupMember>, device: Device): Boolean {
                    try {
                        viewModel.deleteGroupMembers(groupMembers)
                    } catch (err: TimeoutException) {
                        val activity = getActivity() ?: return true
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
                                    .setPositiveButton(getString(R.string.ok)) { _, _ -> }
                                    .create()
                                    .show()
                            }
                        }
                        return true
                    } catch (err: UnknownApiException) {
                        val activity = getActivity() ?: return true
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
                                    .setPositiveButton(getString(R.string.ok)) { _, _ -> }
                                    .create()
                                    .show()
                            }
                        }
                        return true
                    }

                    return false
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.pairing) {
            val action = AddDevicesFragmentDirections.actionAddDevicesFragmentToPairingFragment()
            findNavController().navigate(action)

            true
        } else {
            false
        }
    }
}
