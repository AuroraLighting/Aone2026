package com.aurora.aonev3.ui.activities.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.VolleyError
import com.aurora.aonev3.R
import com.aurora.aonev3.network.handlers.CloudHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_forgot_password.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        etEmail.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    resetPassword()
                }
            }
            false
        }

        btnReset.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        val email = etEmail?.text?.trim()?.toString() ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                CloudHandler.resetPassword(email)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, getString(R.string.email_sent), Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                runOnUiThread {
                    if (!isFinishing) {
                        AlertDialog.Builder(this@ForgotPasswordActivity)
                            .setTitle(getString(R.string.failed_to_reset_password))
                            .setMessage(getString(R.string.please_try_again))
                            .setPositiveButton(R.string.ok) { _, _ -> }
                            .create()
                            .show()
                    }
                }
            }
        }
    }
}
