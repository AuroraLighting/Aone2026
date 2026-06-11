package com.aurora.aonev3.ui.activities.binding.login

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
import com.aurora.aonev3.databinding.ActivityLoginBinding
import com.aurora.aonev3.R
import com.aurora.aonev3.ui.activities.MainActivity
import com.aurora.aonev3.ui.activities.createaccount.ActivateAccountActivity
import com.aurora.aonev3.ui.activities.createaccount.CreateAccountActivity
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding


    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            if (loginState.usernameError.isNotEmpty()) {
                binding.etEmail.error = loginState.usernameError
            }
            if (loginState.passwordError.isNotEmpty()) {
                binding.etPassword.error = loginState.passwordError
            }
        })

        loginViewModel.loginSuccess.observe(this@LoginActivity, Observer {
            val loginSuccess = it ?: return@Observer
            binding.loading.visibility = View.GONE

            if (loginSuccess) {
                if (loginViewModel.response.optJSONObject("body")?.optString("status") == "PE") {
                    startActivity(Intent(this, ActivateAccountActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
        })

        binding.etEmail.afterTextChanged {
            binding.login.isEnabled = binding.etEmail.text.toString().isNotBlank() && binding.etPassword.text.toString().isNotBlank()
        }

        binding.etPassword.apply {
            afterTextChanged {
                binding.login.isEnabled = binding.etEmail.text.toString().isNotBlank() && binding.etPassword.text.toString().isNotBlank()
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        val username = binding.etEmail.text.toString()
                        val password = binding.etPassword.text.toString()
                        if (loginViewModel.isUserNameValid(username) &&
                            loginViewModel.isPasswordValid(password)) {
                            binding.loading.visibility = View.VISIBLE
                            loginViewModel.binding.login(
                                username,
                                password
                            )
                        }
                    }
                }
                false
            }

            binding.login.setOnClickListener {
                val username = binding.etEmail.text.toString()
                val password = binding.etPassword.text.toString()
                binding.loading.visibility = View.VISIBLE
                loginViewModel.binding.login(username, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
        }

        binding.tvHelp.setOnClickListener {
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

