package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.gateway

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentGatewayDetailBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.allowEditing
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.activities.MainActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

open class GatewayDetailFragment : Fragment() {

    private var _binding: FragmentGatewayDetailBinding? = null
    private val binding get() = _binding!!


    private val viewModel: GatewayDetailViewModel by viewModels()
    private var metadataTemplateVersion: Int? = null
    private var metadataFirmwareVersion: Int? = null
    private var firmwareVersion: Int? = null

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentGatewayDetailBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setupUI(view)

        NabtoHandler.selectedGateway?.let { gateway ->
            etName.setText(gateway.name)
            tvSerialNumberValue.text = gateway.serial
            tvSystemStatusValue.text = gateway.tunnelState?.name ?: "Closed"
            if (gateway.lastError == null || gateway.lastError == 1) {
                tvSystemErrorLabel.visibility = View.GONE
                tvSystemErrorValue.visibility = View.GONE
            } else {
                tvSystemErrorLabel.visibility = View.VISIBLE
                tvSystemErrorValue.visibility = View.VISIBLE
                tvSystemErrorValue.text = gateway.lastError?.toString()
            }

            gateway.gwFirmware.observe(viewLifecycleOwner, {
                firmwareVersion = try {
                    it.split("-")[0].split(".")[2].toInt()
                } catch (ex: Exception) {
                    null
                }
                val string = if (firmwareVersion != null && metadataFirmwareVersion != null) {
                    "$firmwareVersion ($metadataFirmwareVersion)"
                } else if (firmwareVersion == null) {
                    "($metadataFirmwareVersion)"
                } else {
                    "$firmwareVersion (n/a)"
                }

                tvFirmwareVersionValue.text = string
            })

            viewModel.getDevices(gateway).observe(viewLifecycleOwner) {
                val device = it.toList().find { device -> device.parentGateway == gateway.serial && device.id == 0 }
                device?.let {
                    val tV = device.metadata.optInt("template_version", -1)
                    val fV = device.metadata.optInt("firmware_version", -1)
                    if (tV != -1) {
                        metadataTemplateVersion = tV
                        tvTemplateVersionValue.text = metadataTemplateVersion.toString()
                    }

                    if (fV != -1) {
                        metadataFirmwareVersion = fV
                        val string = if (firmwareVersion != null) {
                            "$firmwareVersion ($metadataFirmwareVersion)"
                        } else {
                            "($metadataFirmwareVersion)"
                        }

                        tvFirmwareVersionValue.text = string
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                NabtoHandler.selectedGateway?.let { gateway ->
                    val name = etName.text?.trim()?.toString() ?: gateway.name
                    if (name.isNotEmpty() && gateway.name != name) {
                        try {
                            CloudHandler.putGateway(gateway.id, JSONObject().put("name", name))
                        } catch (err: VolleyError) {
                            err.printStackTrace()
                        }
                        gateway.name = name
                    }
                }

                activity?.runOnUiThread {
                    findNavController().popBackStack()
                }
            }
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        btnRelease.setOnClickListener {
            if (activity?.isFinishing == false) {
                AlertDialog.Builder(activity)
                    .setMessage("Are you sure you want to remove this Hub?")
                    .setPositiveButton(R.string.yes) { _, _ ->
                        NabtoHandler.selectedGateway?.let { gateway ->
                            viewModel.viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    CloudHandler.putGateway(gateway.id, JSONObject().put("name", gateway.serial))
                                    CloudHandler.releaseGateway(gateway.serial)
                                    CloudHandler.getGateways()
                                } catch(err: VolleyError) {
                                    crashlytics.log("E/${TAG}: Releasing gateway")
                                    crashlytics.recordException(err)
                                }

                                activity?.startActivity(Intent(context, MainActivity::class.java))
                                activity?.finishActivity(0)
                                activity?.runOnUiThread {
                                    findNavController().popBackStack()
                                }
                            }
                        }
                    }
                    .setNegativeButton(R.string.no, null)
                    .create()
                    .show()
            }
        }

        btnShare.setOnClickListener {
            val gateway = NabtoHandler.selectedGateway ?: return@setOnClickListener
            val action = GatewayDetailFragmentDirections.actionGatewayDetailFragmentToShareGatewayFragment(gateway.serial)
            findNavController().navigate(action)

        }

        if (!allowEditing()) {
            btnSave.visibility = View.GONE
            btnCancel.visibility = View.GONE
            btnRelease.visibility = View.GONE

            etName.isEnabled = false
        }

        if (NabtoHandler.selectedGateway?.accessLevel != NabtoHandler.GatewayAccessLevel.OWNER) {
            btnShare.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "GatewayDetailFragment"

        fun newInstance() =
            GatewayDetailFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
