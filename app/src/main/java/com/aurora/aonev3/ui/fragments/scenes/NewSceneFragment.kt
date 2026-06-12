package com.aurora.aonev3.ui.fragments.scenes

import com.aurora.aonev3.synthetic.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentNewSceneBinding
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.ui.IconsScenes
import com.aurora.aonev3.ui.activities.MainActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NewSceneFragment : Fragment() {

    protected var _binding: FragmentNewSceneBinding? = null
    protected val binding get() = _binding!!


    companion object {
        const val TAG = "NewSceneFragment"
    }

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private var originalMode: Int? = null
    private val viewModel: NewSceneViewModel by viewModels()
    private lateinit var mGroup: Group
    private var mColour = ""
    private var mIcon = ""
    private val args: NewSceneFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentNewSceneBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        if (!this::mGroup.isInitialized) {
            findNavController().popBackStack(R.id.groupsFragment, false)
            return
        }
        originalMode = activity?.window?.attributes?.softInputMode

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("colour")
            ?.observe(viewLifecycleOwner) {
                viewModel.selectedColour = it
            }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<IconsScenes>("icon")
            ?.observe(viewLifecycleOwner) {
                viewModel.selectedIcon = it
            }

        val gridAdapter = SceneDevicesRecyclerViewAdapter(requireActivity()).apply {
            onItemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val device = getItem(position)?.device
                    val group = getItem(position)?.group

                    device?.let {
                        viewModel.toggleDeviceOnOff(device)
                    }

                    group?.let {
                        viewModel.toggleGroupOnOff(group)
                    }
                }
            }

            onItemLongClickListener = object : ItemLongClickListener {
                override fun onItemLongClick(view: View, position: Int): Boolean {
                    val device = getItem(position)?.device
                    val group = getItem(position)?.group

                    device?.let {
                        val action =
                            NewSceneFragmentDirections.actionGlobalControlsFragment(device.id)
                        findNavController().navigate(action)
                        return true
                    }

                    group?.let {
                        val action = NewSceneFragmentDirections.actionNewSceneFragmentToGroupFragment(group.id, false)
                        findNavController().navigate(action)
                        return true
                    }

                    return false
                }
            }

            onLeftSocketClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val device = getItem(position)?.device ?: return

                    viewModel.toggleDeviceOnOff(device, "socket1")
                }
            }

            onRightSocketClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val device = getItem(position)?.device ?: return

                    viewModel.toggleDeviceOnOff(device, "socket2")
                }
            }
        }

        with(binding.rvLights) {
            adapter = gridAdapter
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false).apply {
                spanSizeLookup = SceneSpanSizeLookup(gridAdapter, spanCount)
            }

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return@let
            SyncHandler.syncHandlerCoroutineScope.launch {

                val nestedGroupIds =
                    mGroup.metadata.optJSONArray("nested_groups")?.toIntArray() ?: intArrayOf()
                val nestedGroups = SyncHandler
                    .groupsList
                    .filter { group -> group.parentGateway == gateway.serial && group.id in nestedGroupIds }
                val nestedGroupsDatapointsLiveData = viewModel.getGroupDatapoints(gateway)

                val memberIds = viewModel.getMembersEntities(gateway, mGroup).map { it.deviceId }
                val members = SyncHandler.devicesList.filter { it.parentGateway == mGroup.parentGateway && it.id in memberIds }
                val nestedDevices = findNestedDevices(nestedGroups)

                val allGroupMembers = SyncHandler
                    .groupMembersList
                    .filter { it.parentGateway == gateway.serial && it.groupId == mGroup.id }
                val nonVirtualMembers = members.filter { member ->
                    allGroupMembers.find {
                        it.deviceId == member.id
                    }?.isVirtualMember == false && member !in nestedDevices
                }
                val lights = nonVirtualMembers.filter { device ->
                    device.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                }
                val power = nonVirtualMembers.filter { device ->
                    device.getDeviceCategory() == Device.DeviceCategory.POWER
                }
                val sockets = nonVirtualMembers.filter { device ->
                    device.getDeviceCategory() == Device.DeviceCategory.SOCKETS
                }
                val switches = nonVirtualMembers.filter { device ->
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
                            sockets.distinctBy { it.id })
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

                activity?.runOnUiThread {
                    gridAdapter.setDevices(devicesList)

                    if (nestedGroups.isNotEmpty()) {
                        gridAdapter.setGroups(nestedGroups)
                    }

                    nestedGroupsDatapointsLiveData.observe(viewLifecycleOwner) {
                        activity?.runOnUiThread {
                            gridAdapter.setGroupDatapoints(it)
                        }
                    }
                }

                val ids = nonVirtualMembers.map { it.id }.toIntArray()
                val datapointsLiveData = viewModel.getAllDeviceDatapoints(gateway)
//                    ids,
                    arrayOf(
                        "onoff",
                        "level",
                        "mired",
                        "hue",
                        "colourtempmin",
                        "colourtempmax"
                    )

                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    datapointsLiveData.observe(viewLifecycleOwner, { dp ->
                        val datapoints = dp.toList().filter {
                            it.parentGateway == gateway.serial
                                    && it.id in ids
                                    && it.key in arrayOf("onoff", "level", "mired", "hue", "colourtempmin", "colourtempmax")
                        }
                        activity?.runOnUiThread {
                            gridAdapter.setDatapoints(datapoints)
                        }
                    })
                }

                viewModel.reportGroupDatapoints(gateway)
            }
        }

        binding.iconLayout.setOnClickListener {
            val action =
                NewSceneFragmentDirections
                    .actionNewSceneFragmentToSceneIconSelectorFragment(
                        viewModel.selectedColour,
                        viewModel.selectedIcon
                    )
            findNavController().navigate(action)
        }

        viewModel.scene?.let { scene ->
            val metadata = scene.metadata

            if (binding.etName.text.isNullOrBlank()) {
                binding.etName.setText(scene.name)
            }
            if (metadata.has("icon_colour") && viewModel.selectedColour == null) {
                viewModel.selectedColour = metadata.optString("icon_colour")
                setColour()
            }
            if (metadata.has("icon") && viewModel.selectedIcon == IconsScenes.NULL) {
                val icon = try {
                    IconsScenes.fromString(metadata.optString("icon"))
                } catch (ex: IllegalArgumentException) {
                    ex.printStackTrace()
                    IconsScenes.NULL
                }

                viewModel.selectedIcon = icon
                setIcon()
            }

            binding.fabDelete.visibility = View.VISIBLE
        }

        binding.fabDone.setOnClickListener {
            val name = binding.etName.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(context, "Please give your Scene a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.fabDone.isEnabled = false

            viewModel.saveScene(name, mColour, mIcon) {
                val activity = activity ?: return@saveScene
                App.requestReviewIfAppropriate(activity)
                activity.runOnUiThread {
                    findNavController().popBackStack()
                }
            }
        }

        binding.fabControls.setOnClickListener {
            viewModel.turnGroupOn()

            val action = NewSceneFragmentDirections.actionGlobalControlsFragment(
                groupId = mGroup.id,
                isRgb = viewModel.isGroupRgb,
                isCt = viewModel.isGroupCt,
                ctMax = viewModel.groupColourTemperatureMax,
                ctMin = viewModel.groupColourTemperatureMin
            )
            findNavController().navigate(action)
        }

        binding.fabDelete.setOnClickListener {
            if (!requireActivity().isFinishing) {
                viewModel.scene?.let { scene ->
                    AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.delete_confirmation, scene.name))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.deleteScene(scene) {
                                activity?.runOnUiThread {
                                    findNavController().popBackStack()
                                }
                            }
                        }
                        .setNegativeButton(R.string.no) { _, _ ->

                        }
                        .create()
                        .show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        )

        NabtoHandler.selectedGateway?.let { gateway ->
            if (!gateway.isConnected) return
            if (viewModel.selectedGroup == null) {
                val groupId = args.groupId
                viewModel.selectedGroup = SyncHandler
                    .groupsList
                    .find { group -> group.parentGateway == gateway.serial && group.id == groupId }
                viewModel.scene = SyncHandler
                    .scenesList
                    .find { scene -> scene.parentGateway == gateway.serial && scene.id == args.sceneId && scene.groupId == groupId }
                viewModel.isGroupRgb = args.isRgb
                viewModel.isGroupCt = args.isCt
                viewModel.groupColourTemperatureMax = args.ctMax
                viewModel.groupColourTemperatureMin = args.ctMin
            }

            val group: Group = viewModel.selectedGroup ?: return
            mGroup = group
        }
    }

    private fun setColour() {
        var colour = viewModel.selectedColour
        if (!colour.isNullOrBlank()) {
            try {
                if (!colour.startsWith("#")) {
                    colour = "#$colour"
                }
                binding.iconLayout.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colour))
                mColour = colour
            } catch (ex: IllegalArgumentException) {
                crashlytics.log("E/$TAG:$colour")
                crashlytics.recordException(ex)
            }
        }
    }

    private fun setIcon() {
        val icon = viewModel.selectedIcon
        if (icon != IconsScenes.NULL) {
            binding.sceneIconIv.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    icon.resourceValue
                )
            )
            mIcon = icon.stringValue
        }
    }

    private fun findNestedDevices(groups: List<Group>): List<Device> {
        val gateway = NabtoHandler.selectedGateway ?: return emptyList()
        val groupIds = groups.toList().map { it.id }
        val groupMembers = SyncHandler
            .groupMembersList
            .filter {
                it.parentGateway == gateway.serial && !it.isVirtualMember && it.groupId in groupIds
            }
        val deviceIds = groupMembers.map { it.deviceId }
        return SyncHandler
            .devicesList
            .filter { it.parentGateway == gateway.serial && it.id in deviceIds }
    }

    override fun onDestroy() {
        super.onDestroy()
        originalMode?.let { activity?.window?.setSoftInputMode(it) }
    }

    override fun onResume() {
        super.onResume()

        setColour()
        setIcon()
    }

    private class SceneSpanSizeLookup(
        private val adapter: SceneDevicesRecyclerViewAdapter,
        private val spanCount: Int
    ) : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (adapter.getItemViewType(position) == SceneDeviceDataType.SECTION.ordinal ||
                adapter.getItemViewType(position) == SceneDeviceDataType.SOCKET.ordinal ||
                adapter.getItemViewType(position) == SceneDeviceDataType.GROUP.ordinal
            ) {
                spanCount / 1
            } else {
                spanCount / 2
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
