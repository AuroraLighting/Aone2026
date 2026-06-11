package com.aurora.aonev3.ui.fragments.help.account

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.aurora.aonev3.R
import com.aurora.aonev3.signOut
import com.aurora.aonev3.ui.activities.login.LoginActivity
import kotlinx.android.synthetic.main.fragment_account_delete.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccountDeleteFragment : Fragment() {

    companion object {
        fun newInstance() = AccountDeleteFragment()
    }

    private val viewModel: AccountDeleteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_delete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        btnDeleteAccount.setOnClickListener {
            btnDeleteAccount.isEnabled = false
            val username = etEmail.text?.toString()
            val password = etPassword.text?.toString()
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    viewModel.deleteUser(username, password)

                    val activity = activity ?: return@launch
                    activity.runOnUiThread {
                        if (!activity.isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(activity)
                                .setMessage("Account deleted")
                                .setPositiveButton(R.string.ok) { _, _ ->
                                    signOut()

                                    activity.startActivity(Intent(activity, LoginActivity::class.java))
                                    activity.finishAffinity()
                                }
                                .create()
                                .show()
                        }
                    }
                } catch (ex: Exception) {
                    val activity = activity ?: return@launch
                    activity.runOnUiThread {
                        if (!activity.isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(activity)
                                .setTitle("Failed to delete account")
                                .setMessage(ex.message)
                                .setPositiveButton(R.string.ok) { _, _ -> }
                                .create()
                                .show()
                        }
                        btnDeleteAccount.isEnabled = true
                    }
                }
            }
        }
    }

}