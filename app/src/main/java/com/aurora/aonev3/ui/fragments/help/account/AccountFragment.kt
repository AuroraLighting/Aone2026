package com.aurora.aonev3.ui.fragments.help.account

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.FragmentAccountBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.OtaHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!


    companion object {
        private const val TAG = "AccountFragment"
        fun newInstance() = AccountFragment()
    }

    private val viewModel: AccountViewModel by viewModels()
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return run {
            _binding = FragmentAccountBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = CloudHandler.user.optString("email")

        tvEmailValue.text = email
        tvNameValue.text = arrayOf(CloudHandler.user.optString("first_name"), CloudHandler.user.optString("last_name")).joinToString(" ")

        binding.btnDeleteAccount.visibility = if (OtaHandler.isAccountDeleteAvailable) View.VISIBLE else View.GONE

        binding.btnChangePassword.setOnClickListener {
            binding.btnChangePassword.isEnabled = false

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    viewModel.resetPassword(email)
                    val activity = activity ?: return@launch
                    activity.runOnUiThread {
                        if (!activity.isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(activity)
                                .setTitle(getString(R.string.email_sent))
                                .setMessage(getString(R.string.change_password_email, email))
                                .setPositiveButton(R.string.ok) { _, _ -> }
                                .create()
                                .show()
                        }
                    }
                } catch (err: VolleyError) {
                    crashlytics.recordException(err)
                    val activity = activity ?: return@launch
                    activity.runOnUiThread {
                        if (!activity.isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(activity)
                                .setTitle(getString(R.string.failed_to_reset_password))
                                .setMessage(getString(R.string.please_try_again))
                                .setPositiveButton(R.string.ok) { _, _ -> }
                                .create()
                                .show()
                        }
                    }
                }

                activity?.runOnUiThread {
                    binding.btnChangePassword.isEnabled = true
                }
            }
        }

        binding.btnDeleteAccount.setOnClickListener {
            val activity = activity ?: return@setOnClickListener

            activity.runOnUiThread {
//                val outValue = TypedValue()
//                val test = requireContext().theme.resolveAttribute(androidx.appcompat.R.attr.alertDialogTheme, outValue, true)
//                print(test)
                if (!activity.isFinishing) {
                    AlertDialog
                        .Builder(activity, R.style.DeleteAlertDialogCustom)
                        .setTitle("Delete Account")
                        .setMessage("Warning, this will permanently delete your account and all associated data from our servers")
                        .setPositiveButton(R.string.understand) { _, _ ->
                            val action = AccountFragmentDirections.actionAccountFragmentToAccountDeleteFragment()
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
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show()
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
