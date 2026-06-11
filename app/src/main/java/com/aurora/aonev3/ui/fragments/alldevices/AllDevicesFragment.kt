package com.aurora.aonev3.ui.fragments.alldevices

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.*
import com.aurora.aonev3.logic.CollectionType
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.ui.activities.TourActivity
import kotlinx.android.synthetic.main.fragment_all_devices.*

class AllDevicesFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    companion object {
        private const val TAG = "AllDevicesFragment"
        fun newInstance() = AllDevicesFragment()
    }

    private val viewModel: AllDevicesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return

        if (!SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("allDevicesTourDone", false)) {
            val intent = Intent(activity, TourActivity::class.java)
            intent.putExtra("tour", "all_devices")
            startActivity(intent)
        }

        val devicesAdapter = getListAdapter(activity)

        if (!allowEditing()) {
            menu.visibility = View.GONE
        }

        menu.setOnClickListener {
            val popup = PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.all_devices_menu, popup.menu)
            popup.setOnMenuItemClickListener(this@AllDevicesFragment)
            popup.show()
        }

        setUpListRecyclerView(recyclerView, devicesAdapter)

        with(fabHome) {
            setOnClickListener {
                findNavController().popBackStack(R.id.groupsFragment, false)
            }
        }

        NabtoHandler.selectedGateway?.let { gateway ->
//            if (!gateway.isConnected) return@let
            val devicesLiveData = viewModel.getDevices(gateway)
            val groupsLiveData = viewModel.getGroups(gateway)
            val groupMembersLiveData = viewModel.getGroupMembers(gateway)
            val collectionLiveData = viewModel.getCollections(gateway)
            devicesAdapter.setGateway(gateway)

            devicesLiveData.observe(viewLifecycleOwner) {
                val devices = it
                    ?.toList()
                    ?.filter { d -> d.parentGateway == gateway.serial }
                    ?.sortedBy { device -> device.name }

                devices?.let {
                    val lights = devices.filter { device ->
                        device.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                    }
                    val sensors = devices.filter { device ->
                        device.getDeviceCategory() == Device.DeviceCategory.SENSORS
                    }
                    val power = devices.filter { device ->
                        device.getDeviceCategory() == Device.DeviceCategory.POWER
                    }
                    val sockets = devices.filter { device ->
                        device.getDeviceCategory() == Device.DeviceCategory.SOCKETS
                    }
                    val switches = devices.filter { device ->
                        device.getDeviceCategory() == Device.DeviceCategory.SWITCHES
                    }
                    val devicesList = ArrayList<Pair<String, List<Device>>>()

                    if (lights.isNotEmpty()) {
                        devicesList.add(
                            Pair(
                                Device.DeviceCategory.LIGHTS.name.toCapitalisedLowerCase(),
                                lights
                            )
                        )
                    }
                    if (power.isNotEmpty()) {
                        devicesList.add(
                            Pair(
                                Device.DeviceCategory.POWER.name.toCapitalisedLowerCase(),
                                power
                            )
                        )
                    }
                    if (sockets.isNotEmpty()) {
                        devicesList.add(
                            Pair(
                                Device.DeviceCategory.SOCKETS.name.toCapitalisedLowerCase(),
                                sockets
                            )
                        )
                    }
                    if (sensors.isNotEmpty()) {
                        devicesList.add(
                            Pair(
                                Device.DeviceCategory.SENSORS.name.toCapitalisedLowerCase(),
                                sensors
                            )
                        )
                    }
                    if (switches.isNotEmpty()) {
                        devicesList.add(
                            Pair(
                                Device.DeviceCategory.SWITCHES.name.toCapitalisedLowerCase(),
                                switches
                            )
                        )
                    }

                    devicesAdapter.setDevices(devicesList)
                }
            }

            groupsLiveData.observe(viewLifecycleOwner) { groups ->
                groups?.let {
                    devicesAdapter.setGroups(
                        it.toList().filter { group -> group.parentGateway == gateway.serial })
                }
            }

            groupMembersLiveData.observe(viewLifecycleOwner) { groupMembers ->
                groupMembers?.let {
                    devicesAdapter.setGroupMembers(
                        it.toList()
                            .filter { groupMember -> groupMember.parentGateway == gateway.serial })
                }
            }

            collectionLiveData.observe(viewLifecycleOwner) { collections ->
                collections?.let {
                    val legacyCollections = collections.toList().filter { collection ->
                        collection.parentGateway == gateway.serial &&
                                collection.metadata.collectionType == CollectionType.SCHEDULE &&
                                collection.metadata.parentSpace == null
                    }
                    devicesAdapter.setSchedules(legacyCollections)
                }
            }
        }
    }

    private fun setUpListRecyclerView(
        rv: RecyclerView,
        listAdapter: RecyclerView.Adapter<*>
    ) {
        with(rv) {
            adapter = listAdapter
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(
                GridItemDecoration(margin, margin, margin * 2, margin * 2)
            )
        }
    }

    private fun getListAdapter(activity: FragmentActivity): AllDevicesRecyclerViewAdapter {
        return AllDevicesRecyclerViewAdapter(activity).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val item = getItem(position) ?: return

                    item.gateway?.let {
                        val action = AllDevicesFragmentDirections.actionAllDevicesFragmentToGatewayDetailFragment()
                        findNavController().navigate(action)
                    }
                    item.device?.let { device ->
                        viewModel.selectedDevice = device

                        val action = when (device.deviceClass) {
                            Device.DeviceClass.AURORABULB,
                            Device.DeviceClass.AURORARGBWBULB,
                            Device.DeviceClass.AURORATWBULB,
                            Device.DeviceClass.AURORASMARTPLUG,
                            Device.DeviceClass.SMARTPLUG -> AllDevicesFragmentDirections.actionAllDevicesFragmentToDeviceDetailFragment()
                            Device.DeviceClass.AURORADUALSOCKET -> AllDevicesFragmentDirections.actionAllDevicesFragmentToDoubleSocketDetailFragment()
                            Device.DeviceClass.AURORAGEYSER -> throw Exception("Device.DeviceClass.AURORAGEYSER not implemented")
                            Device.DeviceClass.AURORAWALLDIMMER -> AllDevicesFragmentDirections.actionAllDevicesFragmentToWallDimmerInlineFragment()
                            Device.DeviceClass.AURORAWALLDIMMER2 -> AllDevicesFragmentDirections.actionAllDevicesFragmentToWallDimmerControlFragment()
                            Device.DeviceClass.BATTERYDIMMER -> AllDevicesFragmentDirections.actionAllDevicesFragmentToBatteryDimmer1GFragment()
                            Device.DeviceClass.BATTERYDIMMERDUAL -> AllDevicesFragmentDirections.actionAllDevicesFragmentToBatteryDimmer2GFragment()
                            Device.DeviceClass.DOORWINDOW,
                            Device.DeviceClass.WINDOW -> AllDevicesFragmentDirections.actionAllDevicesFragmentToDoorSensorDetails()
                            Device.DeviceClass.MOTION -> AllDevicesFragmentDirections.actionAllDevicesFragmentToMotionSensorDetails()
                            Device.DeviceClass.PTM215ZE -> AllDevicesFragmentDirections.actionAllDevicesFragmentToKineticDetails()
                            Device.DeviceClass.REMOTE -> AllDevicesFragmentDirections.actionAllDevicesFragmentToRemoteDetails()
                            else -> null
                        }
                        
                        action?.let {
                            try {
                                findNavController().navigate(it)
                            } catch (ex: IllegalArgumentException) {
                                ex.printStackTrace()
                                Log.e(TAG, "Tried to navigate from incorrect destination")
                            }
                        }
                    }
                    item.collection?.let { collection ->
                        val action = AllDevicesFragmentDirections.actionAllDevicesFragmentToLegacyScheduleFragment(collection.id)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.pairing) {
            val action = AllDevicesFragmentDirections.actionAllDevicesFragmentToPairingFragment()
            findNavController().navigate(action)

            true
        } else {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class AllDevicesData(val device: Device? = null, val gateway: NabtoHandler.NabtoGateway? = null, val collection: LogicCollection? = null, val section: String? = null, val type: AllDevicesDataType)

enum class AllDevicesDataType {
    GATEWAY,
    DEVICE,
    LEGACY_SCHEDULE,
    SECTION
}
