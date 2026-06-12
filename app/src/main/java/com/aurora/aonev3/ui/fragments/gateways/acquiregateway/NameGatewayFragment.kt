package com.aurora.aonev3.ui.fragments.gateways.acquiregateway

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentNameGatewayBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.activities.login.toJSONObject
import com.aurora.aonev3.ui.fragments.groups.GroupsFragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class NameGatewayFragment : Fragment() {

    private var _binding: FragmentNameGatewayBinding? = null
    private val binding get() = _binding!!

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentNameGatewayBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    rename()
                }
            }
            false
        }

        binding.btnDone.setOnClickListener {
            rename()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun rename() {
        val name = etName.text?.toString()?.trim()

        if (name.isNullOrBlank()) {
            Toast.makeText(context, "Hub name cannot be blank", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            NabtoHandler.selectedGateway?.let { gateway ->
                try {
                    CloudHandler.putGateway(gateway.id, JSONObject().put("name", name))

                    gateway.name = name

                    val action = NameGatewayFragmentDirections.actionNameGatewayFragmentToNoGroupsFragment()
//                    findNavController().popBackStack(R.id.groupsFragment, false)

                    activity?.runOnUiThread {
                        try {
                            findNavController().navigate(action)
                        } catch (ex: IllegalArgumentException) {
                            ex.printStackTrace()
                            Log.e(
                                GroupsFragment.TAG,
                                "Tried to navigate from incorrect destination"
                            )
                        }
                    }
                } catch (err: VolleyError) {
                    val error =
                        err.networkResponse?.data?.toString(Charsets.UTF_8)?.toJSONObject()
                            ?: return@let

                    var errorMessage = ""

                    error.keys().forEach {
                        errorMessage += "${error.optString(it)}\n"
                    }

                    crashlytics.recordException(err)

                    activity?.runOnUiThread {
                        AlertDialog.Builder(context)
                            .setTitle(getString(R.string.failed_to_rename_hub))
                            .setMessage(errorMessage)
                            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                findNavController().popBackStack(R.id.groupsFragment, false)
                            }
                            .create()
                            .show()
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "NameGatewayFragment"
        @JvmStatic
        fun newInstance() =
            NameGatewayFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
