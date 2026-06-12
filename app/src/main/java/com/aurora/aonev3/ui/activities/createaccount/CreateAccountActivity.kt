package com.aurora.aonev3.ui.activities.createaccount

import com.aurora.aonev3.synthetic.*
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.ActivityCreateAccountBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.ui.activities.PrivacyPolicyActivity
import com.aurora.aonev3.ui.activities.TermsActivity
import com.aurora.aonev3.ui.activities.login.LoginActivity
import com.aurora.aonev3.ui.activities.login.afterTextChanged
import com.aurora.aonev3.ui.activities.login.toJSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAccountBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etName.afterTextChanged {
            binding.btnRegister.isEnabled = binding.etName.text.toString().isNotBlank() &&
                    binding.etEmail.text.toString().isNotBlank() &&
                    binding.etPassword.text.toString().isNotBlank() &&
                    binding.termsSwitch.isChecked &&
                    binding.ppSwitch.isChecked
        }
        binding.etEmail.afterTextChanged {
            binding.btnRegister.isEnabled = binding.etName.text.toString().isNotBlank() &&
                    binding.etEmail.text.toString().isNotBlank() &&
                    binding.etPassword.text.toString().isNotBlank() &&
                    binding.termsSwitch.isChecked &&
                    binding.ppSwitch.isChecked
        }
        binding.etPassword.afterTextChanged {
            binding.btnRegister.isEnabled = binding.etName.text.toString().isNotBlank() &&
                    binding.etEmail.text.toString().isNotBlank() &&
                    binding.etPassword.text.toString().isNotBlank() &&
                    binding.termsSwitch.isChecked &&
                    binding.ppSwitch.isChecked
        }
        binding.termsSwitch.setOnCheckedChangeListener { _, _ ->
            binding.btnRegister.isEnabled = binding.etName.text.toString().isNotBlank() &&
                    binding.etEmail.text.toString().isNotBlank() &&
                    binding.etPassword.text.toString().isNotBlank() &&
                    binding.termsSwitch.isChecked &&
                    binding.ppSwitch.isChecked
        }

        binding.termsTv.setOnClickListener {
            startActivity(Intent(this, TermsActivity::class.java))
        }

        binding.ppTv.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        binding.ppSwitch.setOnCheckedChangeListener { _, _ ->
            binding.btnRegister.isEnabled = binding.etName.text.toString().isNotBlank() &&
                    binding.etEmail.text.toString().isNotBlank() &&
                    binding.etPassword.text.toString().isNotBlank() &&
                    binding.termsSwitch.isChecked &&
                    binding.ppSwitch.isChecked
        }

        binding.btnRegister.setOnClickListener {
            val name: String = binding.etName.text?.toString() ?: ""
            val email: String = binding.etEmail.text?.toString() ?: ""
            val password: String = binding.etPassword.text?.toString() ?: ""
            val names = name.split(" ", limit = 2)

            val firstName = names.firstOrNull() ?: ""
            val lastName = if (names.size > 1) {
                names.lastOrNull() ?: ""
            } else {
                ""
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CloudHandler.register(firstName, lastName, email = email, password = password)
                    startActivity(Intent(this@CreateAccountActivity, ActivateAccountActivity::class.java))
                    finish()
                } catch (err: VolleyError) {
                    val error =
                        err.networkResponse?.data?.toString(Charsets.UTF_8)?.toJSONObject()
                            ?: return@launch

                    var errorMessage = ""

                    error.keys().forEach {
                        errorMessage += "${error.optString(it)}\n"
                    }

                    runOnUiThread {
                        if (!isFinishing) {
                            AlertDialog.Builder(this@CreateAccountActivity)
                                .setTitle("Failed to create account")
                                .setMessage(errorMessage)
                                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                    startActivity(
                                        Intent(
                                            this@CreateAccountActivity,
                                            LoginActivity::class.java
                                        )
                                    )
                                    finish()
                                }
                                .create()
                                .show()
                        }
                    }
                    return@launch
                }

                SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                    putString("abc012", CloudHandler.encodeCredentials(email, password))
                }
            }
        }
    }

    companion object {
        const val TAG = "CreateAccountActivity"
    }
}
