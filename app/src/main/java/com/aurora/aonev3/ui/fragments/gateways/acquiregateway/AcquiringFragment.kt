package com.aurora.aonev3.ui.fragments.gateways.acquiregateway


import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.NoConnectionError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.aurora.aonev3.R
import com.aurora.aonev3.indices
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.ui.activities.MainActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.fragment_acquiring.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 */
class AcquiringFragment : Fragment() {

    private val viewModel: AcquireGatewayViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_acquiring, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val success = viewModel.acquireGateway()

            if (!success) {
                if (viewModel.error == "") {
                    viewModel.error = when (viewModel.errorCode) {
                        ErrorCodes.ALREADY_OWN.value -> "You already own this Hub"
                        ErrorCodes.NOT_OWNER.value -> "Another User already owns this Hub"
                        ErrorCodes.NOT_GATEWAY.value -> "Couldn't find Hub"
                        else -> ""
                    }
                }

                activity?.runOnUiThread {
                    val action =
                        AcquiringFragmentDirections.actionAcquiringFragmentToAcquireGatewayFragment()

                    try {
                        findNavController().navigate(action)
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                        Log.e(TAG, "Tried to navigate from incorrect destination")
                    }
                }

                return@launch
            }

            viewModel.error = ""

            val email = CloudHandler.getCredentials().first
            val gateway = NabtoHandler.selectedGateway ?: return@launch

            NabtoHandler.openTunnel(gateway, email)

            viewModel.viewModelScope.launch(Dispatchers.Main) {
                try {
                    gateway.isMigrating.observe(viewLifecycleOwner) { isMigrating ->
                        val activity = activity ?: return@observe
                        activity.runOnUiThread {
                            if (isMigrating == true && !activity.isFinishing) {
                                AlertDialog.Builder(activity)
                                    .setMessage(getString(R.string.gateway_migrating))
                                    .setPositiveButton(R.string.ok) { _, _ ->
                                        activity.finishAffinity()
                                    }
                                    .create()
                                    .show()
                            }
                        }
                    }
                } catch (ex: IllegalStateException) {
                    ex.printStackTrace()
                    FirebaseCrashlytics.getInstance().recordException(ex)
                }
            }

            NabtoHandler.connectingCallback = object : NabtoHandler.NabtoConnecting {
                override fun finish(success: Boolean) {
                    if (success) {
                        activity?.runOnUiThread {
                            val animationView = animation_view ?: return@runOnUiThread
                            animationView.setAnimation(R.raw.success)
                            animationView.repeatCount = 0
                            animationView.playAnimation()
                        }

                        NabtoHandler.selectedGateway?.let { gateway ->
                            CoroutineScope(Dispatchers.IO).launch {
                                var newFw = false
                                val firmwareResponse = try {
                                    DevelcoHandler.getFirmwareStatus(gateway)
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
                                    JSONObject()
                                }
                                val firmwareBody =
                                    firmwareResponse.optJSONArray("body") ?: JSONArray()
                                var firmware = JSONObject()

                                for (i in firmwareBody.indices()) {
                                    val firmwareJson = firmwareBody.optJSONObject(i) ?: JSONObject()

                                    if (firmwareJson.optString("id") == "dp-release") {
                                        firmware = firmwareJson
                                    }
                                }

                                if (firmware.optString("version") != OtaHandler.gatewayFirmware.value?.optString("version")) {
                                    newFw = true
                                    if (activity?.isFinishing == false) {
                                        activity?.runOnUiThread {
                                            val fw = OtaHandler.gatewayFirmware.value ?: return@runOnUiThread
                                            val dialog = AlertDialog.Builder(activity)
                                                .setTitle("Firmware upgrade")
                                                .setMessage("First time firmware update required. The firmware on your Hub has never been updated. You must update now to ensure all devices work as expected.")
                                                .setPositiveButton("Upgrade") { _, _ ->
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        try {
                                                            DevelcoHandler.postFirmwareDownload(
                                                                gateway,
                                                                JSONObject()
                                                                    .put("uri", fw.optString("uri"))
                                                                    .put(
                                                                        "hash",
                                                                        fw.optString("hash")
                                                                    )
                                                                    .put(
                                                                        "technology",
                                                                        fw.optString("technology")
                                                                    )
                                                                    .put(
                                                                        "algorithm",
                                                                        fw.optString("algorithm")
                                                                    )
                                                                    .put("storage", "persisted")
                                                            )
                                                            activity?.runOnUiThread {
                                                                AlertDialog.Builder(activity)
                                                                    .setTitle("Firmware upgrade")
                                                                    .setMessage(getString(R.string.gateway_migrating))
                                                                    .setPositiveButton(R.string.ok) { _, _ ->
                                                                        activity?.finishAffinity()
                                                                    }
                                                                    .create()
                                                                    .show()
                                                            }
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
                                                .create()

                                            dialog.setCancelable(false)
                                            dialog.show()
                                        }
                                    }
                                }

                                if (!newFw) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val action = AcquiringFragmentDirections.actionAcquiringFragmentToNameGatewayFragment()

                                        activity?.runOnUiThread {
                                            try {
                                                findNavController().navigate(action)
                                            } catch (ex: IllegalArgumentException) {
                                                ex.printStackTrace()
                                                Log.e(
                                                    TAG,
                                                    "Tried to navigate from incorrect destination"
                                                )
                                            } catch (ex: IllegalStateException) {
                                                ex.printStackTrace()
                                                Log.e(
                                                    TAG,
                                                    "Tried to navigate from incorrect destination"
                                                )
                                            }
                                        }
                                    }, 2000)
                                }
                            }
                        }

                    } else {
                        activity?.runOnUiThread {
                            if (activity?.isFinishing == false) {
                                AlertDialog.Builder(activity)
                                    .setTitle(getString(R.string.failed_to_open_tunnel))
                                    .setMessage("${NabtoHandler.selectedGateway?.lastError}")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                        val action = AcquiringFragmentDirections.actionAcquiringFragmentToNameGatewayFragment()

                                        try {
                                            findNavController().navigate(action)
                                        } catch (ex: IllegalArgumentException) {
                                            ex.printStackTrace()
                                            Log.e(TAG, "Tried to navigate from incorrect destination")
                                        }
                                    }
                                    .create()
                                    .show()
                            }
                        }
                    }
                }
            }

        }
    }

    companion object {
        const val TAG = "AcquiringFragment"

        @JvmStatic
        fun newInstance() = AcquiringFragment()
    }

}
