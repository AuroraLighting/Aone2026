package com.aurora.aonev3.ui.fragments.help

import com.aurora.aonev3.synthetic.*
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentHelpBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.ui.fragments.gateways.GatewaySwitchFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HelpFragment : Fragment() {

    protected var _binding: FragmentHelpBinding? = null
    protected val binding get() = _binding!!

    val viewModel: HelpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return run {
            _binding = FragmentHelpBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAccount.setOnClickListener {
            binding.btnAccount.isEnabled = false
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    viewModel.getUser()
                } catch (err: VolleyError) {
                    val activity = activity ?: return@launch

                    activity.runOnUiThread {
                        if (!activity.isFinishing) {
                            val dialog = AlertDialog.Builder(activity)
                            dialog
                                .setMessage(getString(R.string.get_user_failed))
                                .setPositiveButton(R.string.ok, null)
                                .create()
                                .show()
                        }
                    }
                }

                activity?.runOnUiThread {
                    binding.btnAccount.isEnabled = true
                    val action = HelpFragmentDirections.actionHelpFragmentToAccountFragment()
                    try {
                        findNavController().navigate(action)
                    } catch (ex: IllegalArgumentException) {
                        ex.printStackTrace()
                        Log.e(
                            TAG,
                            "Tried to navigate from incorrect destination"
                        )
                    }
                }
            }
        }

        binding.btnHelp.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroralightinghelp.zendesk.com"))
            startActivity(browserIntent)
        }

        binding.btnResetIntro.setOnClickListener {
            SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                putBoolean("introDone", false)
            }

            Toast.makeText(
                context,
                getString(R.string.intro_reset),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnResetTours.setOnClickListener {
            SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                putBoolean("homeTourDone", false)
                putBoolean("allDevicesTourDone", false)
                putBoolean("groupTourDone", false)
            }
        }

        binding.btnAbout.setOnClickListener(
            Navigation
                .createNavigateOnClickListener(
                    HelpFragmentDirections
                        .actionHelpFragmentToAboutFragment()
                )
        )

        if (!SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("office_mode", false)) {
            binding.btnOfficeMode.text = getString(R.string.enable_office_mode)
        } else {
            binding.btnOfficeMode.text = getString(R.string.disable_office_mode)
        }

        binding.btnOfficeMode.setOnClickListener {
            if (!SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("office_mode", false)) {
                if (activity?.isFinishing == false) {
                    AlertDialog.Builder(activity)
                        .setMessage(getString(R.string.office_mode_desc))
                        .setPositiveButton(R.string.ok) { _, _ ->
                            SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                                putBoolean("office_mode", true)
                            }
                            binding.btnOfficeMode.text = getString(R.string.disable_office_mode)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show()
                }
            } else {
                if (activity?.isFinishing == false) {
                    val dialog = AlertDialog.Builder(activity)
                    val dialogView = layoutInflater.inflate(R.layout.disable_office_mode_dialog_layout, null)
                    dialog
                        .setView(dialogView)
                        .setMessage(getString(R.string.disable_office_mode_dialog))
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val credentials = CloudHandler.getCredentials()

                            if (dialogView.etPassword.text?.toString() == credentials.second) {
                                SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                                    putBoolean("office_mode", false)
                                }
                                binding.btnOfficeMode.text = getString(R.string.enable_office_mode)
                            } else {
                                Toast.makeText(activity, "Password was incorrect", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "HelpFragment"
        fun newInstance() = HelpFragment()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
