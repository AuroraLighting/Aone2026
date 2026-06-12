package com.aurora.aonev3.ui.fragments.schedules.eventselectors

import com.aurora.aonev3.synthetic.*
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurora.aonev3.databinding.FragmentSelectorBinding
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doorsensors.DoorSensorEventViewModel
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorEventFragment
import com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors.MotionSensorEventViewModel
import com.aurora.aonev3.ui.fragments.groups.eventgroupselector.IEventGroupSelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventFragment
import com.aurora.aonev3.ui.fragments.schedules.ScheduleEventViewModel
import com.google.android.material.card.MaterialCardView
import com.google.firebase.crashlytics.FirebaseCrashlytics

class EventDeviceSelectorViewModel: ViewModel() {

    var selectedDevice: Device? = null
    var selectedLdev: String? = null
}

class EventDeviceSelectorFragment : Fragment() {

    protected var _binding: FragmentSelectorBinding? = null
    protected val binding get() = _binding!!

    private val viewModel: EventDeviceSelectorViewModel by viewModels()
    private lateinit var senderViewModel: IEventDeviceSelectorViewModel
    private var mGroup: Group? = null
    private lateinit var mGroupMembers: List<GroupMember>
    private val args: EventDeviceSelectorFragmentArgs by navArgs()
    private val sender: String by lazy { args.sender }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()
        val gateway = NabtoHandler.selectedGateway ?: return

        when (sender) {
            ScheduleEventFragment::class.simpleName -> {
                senderViewModel = ViewModelProvider(activity).get(ScheduleEventViewModel::class.java)
            }
            MotionSensorEventFragment::class.simpleName -> {
                senderViewModel = ViewModelProvider(activity).get(MotionSensorEventViewModel::class.java)
            }
            DoorSensorEventFragment::class.simpleName -> {
                senderViewModel = ViewModelProvider(activity).get(DoorSensorEventViewModel::class.java)
            }
        }
        if (!this::senderViewModel.isInitialized) {
            FirebaseCrashlytics.getInstance().log(sender)
            return
        }
        mGroup = senderViewModel.targetGroup.value
        mGroupMembers = SyncHandler
            .groupMembersList
            .filter { it.parentGateway == gateway.serial && it.groupId == mGroup?.id }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentSelectorBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity ?: return
        debug(sender)
        if (!this::senderViewModel.isInitialized) {
            findNavController().popBackStack()
            return
        }

        val eventLightAdapter = EventDeviceViewAdapter(activity)
        eventLightAdapter.selectDevice(
            senderViewModel.device.value?.first,
            senderViewModel.device.value?.second
        )

        val devices = SyncHandler
            .devicesList
            .filter { device ->
                (device.getDeviceCategory() == Device.DeviceCategory.LIGHTS ||
                        device.getDeviceCategory() == Device.DeviceCategory.POWER ||
                        device.getDeviceCategory() == Device.DeviceCategory.SOCKETS ||
                        device.deviceClass == Device.DeviceClass.AURORAWALLDIMMER) &&
                        mGroupMembers.any { it.deviceId == device.id } &&
                        device.parentGateway == NabtoHandler.selectedGateway?.serial ?: ""
            }

        val lights = devices
            .filter { device ->
                device.getDeviceCategory() == Device.DeviceCategory.LIGHTS
            }
            .sortedBy { device -> device.name }
        val power = devices
            .filter { device ->
                device.getDeviceCategory() == Device.DeviceCategory.POWER
            }
            .sortedBy { device -> device.name }
        val sockets = devices
            .filter { device ->
                device.getDeviceCategory() == Device.DeviceCategory.SOCKETS
            }
            .sortedBy { device -> device.name }
        val switches = devices
            .filter { device ->
                device.getDeviceCategory() == Device.DeviceCategory.SWITCHES
            }
            .sortedBy { device -> device.name }
        val devicesList = ArrayList<Pair<String, List<Device>>>()

        if (lights.isNotEmpty()) {
            devicesList.add(Pair(Device.DeviceCategory.LIGHTS.name.toCapitalisedLowerCase(), lights))
        }
        if (power.isNotEmpty()) {
            devicesList.add(Pair(Device.DeviceCategory.POWER.name.toCapitalisedLowerCase(), power))
        }
        if (sockets.isNotEmpty() && args.sender == ScheduleEventFragment::class.simpleName) {
            devicesList.add(Pair(Device.DeviceCategory.SOCKETS.name.toCapitalisedLowerCase(), sockets))
        }
        if (switches.isNotEmpty()) {
            devicesList.add(Pair(Device.DeviceCategory.SWITCHES.name.toCapitalisedLowerCase(), switches))
        }
        eventLightAdapter.setItems(devicesList)

        binding.tvTitle.text = getString(R.string.select_device)

        with(binding.recyclerView) {
            adapter = eventLightAdapter
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 1, RecyclerView.VERTICAL, false)

            val margin = resources.getDimensionPixelSize(R.dimen.default_margin_small)
            addItemDecoration(
                GridItemDecoration(margin, margin, margin * 2, margin * 2)
            )
        }

        binding.btnSave.setOnClickListener {
            val device = viewModel.selectedDevice ?: return@setOnClickListener
            val selectedLdev = viewModel.selectedLdev ?: return@setOnClickListener
            senderViewModel.device.postValue(Pair(device, selectedLdev))

            findNavController().popBackStack()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    companion object {
        fun newInstance() =
            EventDeviceSelectorFragment()
    }

    inner class EventDeviceViewAdapter(val context: Context) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var deviceList = ArrayList<EventDevicesData>()

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            return when (viewType) {
                EventDevicesDataType.DEVICE.ordinal -> {
                    val layoutView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_group_selector_tile, parent, false)
                    EventDeviceCardViewHolder(layoutView)
                }
                EventDevicesDataType.SOCKET.ordinal -> {
                    val layoutView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_double_socket_schedule_tile, parent, false)
                    SocketCardViewHolder(layoutView)
                }
                else -> {
                    val layoutView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.layout_section_header, parent, false)
                    SectionHeaderViewHolder(layoutView)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position < deviceList.size) {
                val item = deviceList[position]

                when (item.type) {
                    EventDevicesDataType.DEVICE -> {
                        val device = item.device ?: return

                        (holder as? EventDeviceCardViewHolder)?.setDevice(device)
                    }
                    EventDevicesDataType.SOCKET -> {
                        val device = item.device ?: return

                        (holder as? SocketCardViewHolder)?.setDevice(device)
                    }
                    EventDevicesDataType.SECTION -> {
                        val section = item.section ?: return

                        (holder as? SectionHeaderViewHolder)?.setSectionHeader(section)
                    }
                }
            }
        }

        override fun getItemCount() = deviceList.size

        fun getItem(position: Int) =
            if (position in deviceList.indices) deviceList[position] else null

        override fun getItemViewType(position: Int): Int {
            return deviceList[position].type.ordinal
        }

        fun setItems(items: List<Pair<String, List<Device>>>) {
            deviceList.clear()
            items.forEach {
                this.deviceList.add(
                    EventDevicesData(
                        section = it.first,
                        type = EventDevicesDataType.SECTION
                    )
                )

                it.second.forEach { device ->
                    if (device.deviceClass != Device.DeviceClass.AURORADUALSOCKET) {
                        this.deviceList.add(
                            (EventDevicesData(
                                device = device,
                                type = EventDevicesDataType.DEVICE
                            ))
                        )
                    } else {
                        this.deviceList.add(
                            (EventDevicesData(
                                device = device,
                                type = EventDevicesDataType.SOCKET
                            ))
                        )
                    }
                }
            }
            notifyDataSetChanged()
        }

        fun selectDevice(device: Device?, ldev: String?) {
            val previousDevice = viewModel.selectedDevice
            viewModel.selectedDevice = device
            viewModel.selectedLdev = ldev
            notifyItemChanged(deviceList.indexOfFirst { it.device == previousDevice })
            notifyItemChanged(deviceList.indexOfFirst { it.device == device })
        }

        inner class EventDeviceCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
            var name: TextView = itemView.findViewById(R.id.tvName)

            init {
                itemView.setOnClickListener(this)
                itemView.findViewById<android.view.View>(R.id.ivIcon).visibility = View.GONE
            }

            override fun onClick(p0: View?) {
                val device = getItem(adapterPosition)?.device ?: return
                selectDevice(device, device.ldevs.first())
            }

            fun setDevice(device: Device) {
                name.text = device.name

                if (device == viewModel.selectedDevice) {
                    cardView.setCardBackgroundColor(context.getColor(R.color.colorTileActive))
                    name.setTextColor(context.getColor(R.color.colorPrimary))
                } else {
                    cardView.setCardBackgroundColor(context.getColor(R.color.colorTileInactive))
                    name.setTextColor(context.getColor(R.color.colorTextPrimary))
                }
            }
        }

        inner class SocketCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTv: TextView = itemView.findViewById(R.id.tvName)
            private val ibLeftSocket: ImageButton = itemView.findViewById(R.id.ibLeftSocket)
            private val ibRightSocket: ImageButton = itemView.findViewById(R.id.ibRightSocket)
            private val ibLock: ImageButton = itemView.findViewById(R.id.ibLock)

            init {
                ibLeftSocket.setOnClickListener {
                    val device = getItem(adapterPosition)?.device ?: return@setOnClickListener
                    selectDevice(device, "socket1")
                }
                ibRightSocket.setOnClickListener {
                    val device = getItem(adapterPosition)?.device ?: return@setOnClickListener
                    selectDevice(device, "socket2")
                }
                ibLock.setOnClickListener {
                    val device = getItem(adapterPosition)?.device ?: return@setOnClickListener
                    selectDevice(device, "lock")
                }
            }

            fun setDevice(device: Device) {
                nameTv.text = device.name

                if (viewModel.selectedDevice == device && viewModel.selectedLdev == "socket1") {
                    ibLeftSocket.background = ContextCompat
                        .getDrawable(requireContext(), R.drawable.left_socket_background_on)
                    ibLeftSocket.setImageDrawable(
                        ContextCompat
                            .getDrawable(requireContext(), R.drawable.socket_on_left)
                    )
                } else {
                    ibLeftSocket.background = ContextCompat
                        .getDrawable(requireContext(), R.drawable.left_socket_background_off)
                    ibLeftSocket.setImageDrawable(
                        ContextCompat
                            .getDrawable(requireContext(), R.drawable.socket_off_left)
                    )
                }

                if (viewModel.selectedDevice == device && viewModel.selectedLdev == "socket2") {
                    ibRightSocket.background = ContextCompat
                        .getDrawable(requireContext(), R.drawable.right_socket_background_on)
                    ibRightSocket.setImageDrawable(
                        ContextCompat
                            .getDrawable(requireContext(), R.drawable.socket_on_right)
                    )
                } else {
                    ibRightSocket.background = ContextCompat
                        .getDrawable(requireContext(), R.drawable.right_socket_background_off)
                    ibRightSocket.setImageDrawable(
                        ContextCompat
                            .getDrawable(requireContext(), R.drawable.socket_off_right)
                    )
                }

                if (viewModel.selectedDevice == device && viewModel.selectedLdev == "lock") {
                    ibLock.background = ContextCompat
                        .getDrawable(requireContext(), R.drawable.left_socket_background_on)
                    ibLock.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.white))
                } else {
                    ibLock.background = ContextCompat
                        .getDrawable(requireContext(), R.drawable.left_socket_background_off)
                    ibLock.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                }
            }
        }
    }
}

data class EventDevicesData(val device: Device? = null, val section: String? = null, val type: EventDevicesDataType)

enum class EventDevicesDataType {
    DEVICE,
    SOCKET,
    SECTION
}

interface IEventDeviceSelectorViewModel: IEventGroupSelectorViewModel {
    var device: MutableLiveData<Pair<Device, String>?>

}
