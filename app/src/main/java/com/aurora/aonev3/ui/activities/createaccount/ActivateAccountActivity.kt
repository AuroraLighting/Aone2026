package com.aurora.aonev3.ui.activities.createaccount

import com.aurora.aonev3.synthetic.*
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.android.volley.VolleyError
import com.aurora.aonev3.databinding.ActivityActivateAccountBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.login.LoginActivity
import com.aurora.aonev3.ui.activities.login.afterTextChanged
import com.aurora.aonev3.ui.activities.login.toJSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivateAccountBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etCode.afterTextChanged {
            binding.btnActivate.isEnabled = binding.etCode.text.toString().isNotBlank()
        }

        binding.btnActivate.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val credentials = CloudHandler.getCredentials()
                    CloudHandler.login(credentials.first, credentials.second)
                    CloudHandler.activate(binding.etCode.text?.toString() ?: "")

                    NabtoHandler.openSession(credentials.first)
                    val fingerprint = NabtoHandler.getFingerprint(credentials.first) ?: ""
                    try {
                        CloudHandler.postFingerprint(fingerprint)
                    } catch (ex: VolleyError) {

                    }

                    CloudHandler.getGateways()

                    runOnUiThread {
                        startActivity(
                            Intent(
                                this@ActivateAccountActivity,
                                MainActivity::class.java
                            )
                        )
                        finish()
                    }
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
                            AlertDialog.Builder(this@ActivateAccountActivity)
                                .setTitle(getString(R.string.failed_to_activate_account))
                                .setMessage(errorMessage)
                                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                                    startActivity(
                                        Intent(
                                            this@ActivateAccountActivity,
                                            LoginActivity::class.java
                                        )
                                    )
                                    finish()
                                }
                                .create()
                                .show()
                        }
                    }
                }
            }
        }

        binding.btnSignOut.setOnClickListener {
            SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                remove("abc012")
            }

            startActivity(Intent(this@ActivateAccountActivity, LoginActivity::class.java))
            finish()
        }
    }
}
