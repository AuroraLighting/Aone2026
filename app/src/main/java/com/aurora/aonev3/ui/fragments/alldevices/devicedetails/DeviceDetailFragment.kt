package com.aurora.aonev3.ui.fragments.alldevices.devicedetails

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentDeviceDetailBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.NestedGroupTree
import com.aurora.aonev3.R
import com.aurora.aonev3.allowEditing
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.fragments.alldevices.AllDevicesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

open class DeviceDetailFragment : Fragment() {

    private var _binding: FragmentDeviceDetailBinding? = null
    private val binding get() = _binding!!


    protected val viewModel: AllDevicesViewModel by activityViewModels()
    private var mGroups: List<Group> = emptyList()
    private var mGroupMembers: List<GroupMember> = emptyList()
    protected var mGroup: Group? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentDeviceDetailBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)
        val device = viewModel.selectedDevice ?: return

        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.getGroups(gateway).observe(viewLifecycleOwner, {
                mGroups = it.toList()

                val groupMembers =
                    mGroupMembers.filter { member -> member.parentGateway == device.parentGateway && member.deviceId == device.id }
                val groups = mGroups.filter { group -> groupMembers.any { member -> member.groupId == group.id } }

                mGroup = NestedGroupTree(groups).getChild()?.group
                viewModel.group.postValue(mGroup)
            })

            viewModel.getGroupMembers(gateway).observe(viewLifecycleOwner, Observer { members ->
                mGroupMembers = members.toList()
                if (members.isNullOrEmpty()) {
                    tvSpace.text = getString(R.string.unassigned)

                    return@Observer
                }
                val groupMembers =
                    members.filter { it.parentGateway == device.parentGateway && it.deviceId == device.id }
                val groups = mGroups.filter { group -> groupMembers.any { member -> member.groupId == group.id } }

                mGroup = NestedGroupTree(groups).getChild()?.group
                viewModel.group.postValue(mGroup)
            })
        }

        viewModel.group.observe(viewLifecycleOwner, { group ->
            tvSpace.text = group?.name ?: getString(R.string.unassigned)
            if (group != null) {
                groupLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileActive)
                tvSpace.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            } else {
                groupLayout.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorTileInactive)
                tvSpace.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
                )
            }
            viewModel.selectedGroup = group
        })

        etName.setText(device.name)

        tvDefaultNameValue.text = device.defaultName
        tvOtaStatusValue.text = device.otaStatus
        if (device.firmwareVersion == "") {
            tvFirmwareVersionValue.visibility = View.GONE
            tvFirmwareVersionLabel.visibility = View.GONE
        } else {
            tvFirmwareVersionValue.visibility = View.VISIBLE
            tvFirmwareVersionLabel.visibility = View.VISIBLE
            tvFirmwareVersionValue.text = device.firmwareVersion
        }
        tvEuiValue.text = device.eui

        etName.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    etName.clearFocus()
                }
            }
            false
        }
        etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.deviceName = etName.text.toString()
            }
        }

        groupLayout.setOnClickListener(
            Navigation
                .createNavigateOnClickListener(
                    DeviceDetailFragmentDirections
                        .actionGlobalGroupSelectorFragment()
                )
        )

        binding.btnIdentify.setOnClickListener {
            NabtoHandler.selectedGateway?.let { gateway ->
                var ldev = device.ldevs.firstOrNull() ?: ""

                device.ldevs.forEach {
                    if (it == "identify") {
                        ldev = it
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        DevelcoHandler.putDeviceDatapoint(
                            gateway,
                            device.id,
                            ldev,
                            "identify",
                            6,
                            first = true
                        )
                    } catch (err: VolleyError) {
                        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                            gateway.isConnected = false
                            val credentials = CloudHandler.getCredentials()
                            if (credentials.first.isEmpty()) {
                                activity?.finishAffinity()
                                startActivity(Intent(context, MainActivity::class.java))
                            }
                            NabtoHandler.openTunnel(gateway, credentials.first)
                        }
                        err.printStackTrace()
                    }
                }
            }
        }

        binding.btnSave.setOnClickListener(btnSaveClickListener())

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDelete.setOnClickListener {
            activity?.let {
                if (!it.isFinishing) {
                    AlertDialog.Builder(it)
                        .setMessage(getString(R.string.delete_confirmation, device.name))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            btnDeleteClickListener()
                        }
                        .setNegativeButton(R.string.no) { _, _ ->

                        }
                        .create()
                        .show()
                }
            }
        }

        if (!allowEditing()) {
            etName.isClickable = false
            etName.isEnabled = false
            groupLayout.isClickable = false
            binding.btnDelete.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
            binding.btnCancel.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "DeviceDetailFragment"

        fun newInstance() =
            DeviceDetailFragment()
    }

    open fun btnSaveClickListener(): View.OnClickListener {
        return View.OnClickListener {
            val device = viewModel.selectedDevice ?: return@OnClickListener
            if (viewModel.deviceName.isNotBlank() && viewModel.deviceName != device.name) {
                NabtoHandler.selectedGateway?.let { gateway ->
                    if (gateway.isConnected) {
                        binding.binding.btnSave.isEnabled = false
                        activity?.layoutGreyOut?.visibility = View.VISIBLE
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            try {
                                DevelcoHandler.putDevice(
                                    gateway,
                                    device.id,
                                    JSONObject().put("name", viewModel.deviceName)
                                )
                            } catch (err: VolleyError) {
                                App.actionFailed()
                                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                    gateway.isConnected = false
                                    val credentials = CloudHandler.getCredentials()
                                    if (credentials.first.isEmpty()) {
                                        activity?.finishAffinity()
                                        startActivity(Intent(context, MainActivity::class.java))
                                    }
                                    NabtoHandler.openTunnel(gateway, credentials.first)
                                }
                                err.printStackTrace()
                            }

                            activity?.runOnUiThread {
                                binding.binding.btnSave.isEnabled = true
                                activity?.layoutGreyOut?.visibility = View.GONE
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            } else {
                findNavController().popBackStack()
            }
        }
    }

    open fun btnDeleteClickListener() {
        val device = viewModel.selectedDevice ?: return

        NabtoHandler.selectedGateway?.let { gateway ->
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                if (gateway.isConnected) {
                    try {
                        DevelcoHandler.deleteDevice(gateway, device.id)
                    } catch (err: VolleyError) {
                        App.actionFailed()
                        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                            gateway.isConnected = false
                            val credentials = CloudHandler.getCredentials()
                            if (credentials.first.isEmpty()) {
                                activity?.finishAffinity()
                                startActivity(Intent(context, MainActivity::class.java))
                            }
                            NabtoHandler.openTunnel(gateway, credentials.first)
                        }

                    }

                    activity?.runOnUiThread {
                        findNavController().popBackStack()
                    }
                }

                viewModel.deviceName = ""
            }
        }
    }

    override fun onDetach() {
        viewModel.clearViewModel()
        super.onDetach()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
