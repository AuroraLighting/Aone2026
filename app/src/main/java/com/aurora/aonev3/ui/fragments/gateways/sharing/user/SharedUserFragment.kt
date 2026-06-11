package com.aurora.aonev3.ui.fragments.gateways.sharing.user

import android.app.AlertDialog
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentSharedUserBinding
import com.aurora.aonev3.App
import com.aurora.aonev3.R
import com.aurora.aonev3.indices
import com.aurora.aonev3.network.handlers.AccessTemplate
import com.aurora.aonev3.network.handlers.Share
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class SharedUserFragment : Fragment() {

    private var _binding: FragmentSharedUserBinding? = null
    private val binding get() = _binding!!


    private val args: SharedUserFragmentArgs by navArgs()
    private val mShare: String by lazy { args.share }
    private val mGatewaySerial: String by lazy { args.gatewaySerial }
    private val viewModel: SharedUserViewModel by viewModels()
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentSharedUserBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val share: Share
        try {
            share = Share.fromJsonString(mShare)
        } catch (ex: JSONException) {
            findNavController().popBackStack()
            return
        }
        val grants = share.grants

        tvNameValue.text = share.name
        tvEmailValue.text = share.email

        for (i in grants.indices()) {
            val grant = grants.optJSONObject(i) ?: JSONObject()

            if (grant.optString("TP") == AccessTemplate.EDIT_ROOT.displayName) {
                tvPermissionsValue.text = getString(R.string.full_control_edit_permissions)
            } else {
                tvPermissionsValue.text = getString(R.string.control_only_permissions)
            }
        }

        btnRemove.setOnClickListener {
            it.isEnabled = false

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    viewModel.unshare(mGatewaySerial, share.email)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        it?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        it?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }

                    activity?.runOnUiThread {
                        findNavController().popBackStack()
                    }
                } catch (err: VolleyError) {
                    App.actionFailed()
                    crashlytics.recordException(Exception("failed to unshare"))

                    var errorHandled = false

                    val error = try {
                        JSONObject(String(err.networkResponse?.data ?: byteArrayOf()))
                    } catch (ex: JSONException) {
                        JSONObject()
                    }

                    if (error.optBoolean("fail")) {
                        val errorCode = error.optInt("err_code")

                        if (errorCode == 500) {
                            errorHandled = true
                            activity?.runOnUiThread {
                                findNavController().popBackStack()
                            }
                        }
                    }

                    if (!errorHandled) {
                        activity?.runOnUiThread {
                            if (activity?.isFinishing == false) {
                                AlertDialog.Builder(activity)
                                    .setMessage("Failed to remove User due to an unknown error, this has been logged. Sorry for the inconvenience")
                                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                        activity?.runOnUiThread {
                                            findNavController().popBackStack()
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
        fun newInstance() =
            SharedUserFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
