package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.remotes

import com.aurora.aonev3.synthetic.*
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentDeviceSelectorBinding
import com.aurora.aonev3.GridItemDecoration
import com.aurora.aonev3.R
import com.aurora.aonev3.SectionHeaderViewHolder
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.fragments.group.GroupRecyclerViewAdapter
import com.google.android.material.card.MaterialCardView

class RemoteDeviceSelectorFragment : Fragment() {

    protected var _binding: FragmentDeviceSelectorBinding? = null
    protected val binding get() = _binding!!


    private val viewModel: RemoteDetailViewModel by activityViewModels()
    private var mGroup: Group? = null
    private lateinit var mGroupMembers: List<GroupMember>
    private val isGroupSelected: MutableLiveData<Boolean> = MutableLiveData(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGroup = viewModel.selectedGroup
        mGroupMembers = SyncHandler
            .groupMembersList
            .filter { it.parentGateway == mGroup?.parentGateway ?: "" && it.groupId == mGroup?.id }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentDeviceSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return
        val lightsAdapter = RemoteDeviceViewAdapter(activity)

        binding.tvTitle.text = getString(R.string.select_device)

        val devices = SyncHandler
            .devicesList
            .filter { device ->
                device.parentGateway == NabtoHandler.selectedGateway?.serial ?: ""
                        && (device.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                        || device.getDeviceCategory() == Device.DeviceCategory.POWER
                        || device.getDeviceType() == Device.DeviceType.WALL_DIMMER_INLINE)
                        && mGroupMembers.any { it.deviceId == device.id }
            }

        lightsAdapter.setItems(devices)

        with(binding.rvLights) {
            adapter = lightsAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(GridItemDecoration(margin, margin, margin, margin))
        }

        binding.btnSave.setOnClickListener {
            val target = lightsAdapter.getSelected() ?: return@setOnClickListener

            if (target.type != DeviceTargetRecyclerViewType.GROUP) {
                val device = target.device ?: return@setOnClickListener

                val ldev = device.ldevs.first()
                viewModel.targetDevice.postValue(Pair(device, ldev))
                viewModel.targetGroup.postValue(null)
            } else {
                val group = mGroup ?: return@setOnClickListener
                val ldev = group.ldevs.first()
                viewModel.targetDevice.postValue(null)
                viewModel.targetGroup.postValue(Pair(group, ldev))
            }

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.targetDevice.observe(viewLifecycleOwner) { devicePair ->
            devicePair?.let {
                val deviceData = lightsAdapter.getItems().find { it.device == devicePair.first } ?: return@observe
                lightsAdapter.setSelected(deviceData)
            }
        }

        viewModel.targetGroup.observe(viewLifecycleOwner) { targetGroup ->
            targetGroup?.let {
                val deviceData = lightsAdapter.getItems().find { it.type == DeviceTargetRecyclerViewType.GROUP } ?: return@observe
                lightsAdapter.setSelected(deviceData)
            }
        }
    }

    companion object {
        fun newInstance() =
            RemoteDeviceSelectorFragment()
    }

    class RemoteDeviceViewAdapter internal constructor(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var deviceList: ArrayList<DeviceTargetData> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                DeviceTargetRecyclerViewType.SECTION.ordinal -> {
                    val layoutView =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.layout_section_header, parent, false)
                    SectionHeaderViewHolder(layoutView)
                }
                DeviceTargetRecyclerViewType.GROUP.ordinal -> {
                    val layoutView =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.layout_group_selector_tile, parent, false)
                    RemoteGroupCardViewHolder(layoutView)
                }
                else -> {
                    val layoutView =
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.layout_group_selector_tile, parent, false)
                    RemoteDeviceCardViewHolder(layoutView)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position) ?: return

            when {
                item.type == DeviceTargetRecyclerViewType.SECTION -> {
                    val section = item.section ?: return
                    (holder as? SectionHeaderViewHolder)?.setSectionHeader(section.displayName)
                }
                item.type != DeviceTargetRecyclerViewType.GROUP -> {
                    (holder as? RemoteDeviceCardViewHolder)?.setDevice(item)
                }
                else -> {
                    (holder as? RemoteGroupCardViewHolder)?.setData(item)
                }
            }
        }

        override fun getItemCount() = deviceList.size

        fun getItem(position: Int): DeviceTargetData? = if (position in deviceList.indices) deviceList[position] else null

        fun getItems() = deviceList

        fun getSelected(): DeviceTargetData? = deviceList.find { it.isSelected }

        override fun getItemViewType(position: Int): Int {
            return getItem(position)?.type?.ordinal ?: -1
        }

        @SuppressLint("NotifyDataSetChanged")
        fun setItems(items: List<Device>) {
            deviceList.clear()

            val lights = items
                .filter { device ->
                    device.getDeviceCategory() == Device.DeviceCategory.LIGHTS
                }
            val power = items
                .filter { device ->
                    device.getDeviceCategory() == Device.DeviceCategory.POWER
                }
            val switches = items
                .filter { device ->
                    device.getDeviceCategory() == Device.DeviceCategory.SWITCHES
                }

            deviceList.add(DeviceTargetData(type = DeviceTargetRecyclerViewType.GROUP))
            if (lights.isNotEmpty()) {
                deviceList.add(DeviceTargetData(section = DeviceTargetDataType.LIGHTS, type = DeviceTargetRecyclerViewType.SECTION))
                deviceList.addAll(lights.map { DeviceTargetData(device = it, type = DeviceTargetRecyclerViewType.LIGHTS) })
            }
            if (power.isNotEmpty()) {
                deviceList.add(DeviceTargetData(section = DeviceTargetDataType.POWER, type = DeviceTargetRecyclerViewType.SECTION))
                deviceList.addAll(power.map { DeviceTargetData(device = it, type = DeviceTargetRecyclerViewType.POWER) })
            }
            if (switches.isNotEmpty()) {
                deviceList.add(DeviceTargetData(section = DeviceTargetDataType.SWITCHES, type = DeviceTargetRecyclerViewType.SECTION))
                deviceList.addAll(switches.map { DeviceTargetData(device = it, type = DeviceTargetRecyclerViewType.SWITCHES) })
            }
            notifyDataSetChanged()
        }

        fun setSelected(deviceData: DeviceTargetData) {
            val existingSelectedDeviceDataIndex = deviceList.indexOfFirst { it.isSelected }
            val existingSelectedDeviceData = deviceList.getOrNull(existingSelectedDeviceDataIndex)
            deviceList.getOrNull(existingSelectedDeviceDataIndex)?.isSelected = false

            if (existingSelectedDeviceData != deviceData) {
                val selectedDeviceIndex = deviceList.indexOf(deviceData)
                deviceList.getOrNull(selectedDeviceIndex)?.isSelected = true

                notifyItemChanged(selectedDeviceIndex)
            }
            if (existingSelectedDeviceDataIndex != -1) {
                notifyItemChanged(existingSelectedDeviceDataIndex)
            }
        }

        inner class RemoteDeviceCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardView)
            var name: TextView = itemView.findViewById<android.widget.TextView>(R.id.tvName)
            lateinit var deviceData: DeviceTargetData

            init {
                itemView.setOnClickListener(this)
                itemView.findViewById<android.widget.ImageView>(R.id.ivIcon).visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                if (this::deviceData.isInitialized) {
                    setSelected(deviceData)
                }
            }

            fun setDevice(deviceData: DeviceTargetData) {
                val device = deviceData.device ?: return
                this.deviceData = deviceData

                name.text = device.name

                if (deviceData.isSelected) {
                    binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }

        inner class RemoteGroupCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardView)
            var name: TextView = itemView.findViewById<android.widget.TextView>(R.id.tvName)
            lateinit var deviceData: DeviceTargetData

            init {
                itemView.setOnClickListener(this)
                itemView.findViewById<android.widget.ImageView>(R.id.ivIcon).visibility = View.GONE
                name.text = context.getString(R.string.control_entire_space)
            }

            override fun onClick(p0: View?) {
                if (this::deviceData.isInitialized) {
                    setSelected(deviceData)
                }
            }

            fun setData(deviceData: DeviceTargetData) {
                this.deviceData = deviceData

                if (deviceData.isSelected) {
                    binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    binding.cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }
    }

    data class DeviceTargetData(val device: Device? = null, val section: DeviceTargetDataType? = null, val type: DeviceTargetRecyclerViewType ) {
        var isSelected: Boolean = false

        override fun equals(other: Any?): Boolean {
            return other is DeviceTargetData
                    && device == other.device
                    && section == other.section
                    && type == other.type
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    enum class DeviceTargetRecyclerViewType {
        GROUP,
        SECTION,
        LIGHTS,
        POWER,
        SWITCHES,
    }

    enum class DeviceTargetDataType(val displayName: String) {
        LIGHTS("Lights"),
        POWER("Power"),
        SWITCHES("Switches")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
