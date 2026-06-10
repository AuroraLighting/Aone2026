package com.aurora.aonev3.ui.activities.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.aurora.aonev3.R
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.createaccount.ActivateAccountActivity
import com.aurora.aonev3.ui.activities.createaccount.CreateAccountActivity
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            if (loginState.usernameError.isNotEmpty()) {
                etEmail.error = loginState.usernameError
            }
            if (loginState.passwordError.isNotEmpty()) {
                etPassword.error = loginState.passwordError
            }
        })

        loginViewModel.loginSuccess.observe(this@LoginActivity, Observer {
            val loginSuccess = it ?: return@Observer
            loading.visibility = View.GONE

            if (loginSuccess) {
                if (loginViewModel.response.optJSONObject("body")?.optString("status") == "PE") {
                    startActivity(Intent(this, ActivateAccountActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
        })

        etEmail.afterTextChanged {
            login.isEnabled = etEmail.text.toString().isNotBlank() && etPassword.text.toString().isNotBlank()
        }

        etPassword.apply {
            afterTextChanged {
                login.isEnabled = etEmail.text.toString().isNotBlank() && etPassword.text.toString().isNotBlank()
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        val username = etEmail.text.toString()
                        val password = etPassword.text.toString()
                        if (loginViewModel.isUserNameValid(username) &&
                            loginViewModel.isPasswordValid(password)) {
                            loading.visibility = View.VISIBLE
                            loginViewModel.login(
                                username,
                                password
                            )
                        }
                    }
                }
                false
            }

            login.setOnClickListener {
                val username = etEmail.text.toString()
                val password = etPassword.text.toString()
                loading.visibility = View.VISIBLE
                loginViewModel.login(username, password)
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
        }

        tvHelp.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://auroralightinghelp.zendesk.com"))
            startActivity(browserIntent)
        }
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

fun String.toJSONObject(): JSONObject? {
    return try {
        JSONObject(this)
    } catch(ex: JSONException) {
        null
    }
}


