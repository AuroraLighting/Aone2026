package com.aurora.aonev3.ui.activities.login

import com.aurora.aonev3.synthetic.*
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.ClientError
import com.android.volley.VolleyError
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginViewModel : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm
    var response = JSONObject()

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(username: String, password: String) {
        if (username.isBlank()) {
            _loginForm.postValue(
                LoginFormState(
                    usernameError = "Email cannot be blank"
                )
            )
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CloudHandler.verify(username, password)
                response = CloudHandler.login(username, password)
                CloudHandler.getGateways()
                _loginSuccess.postValue(true)
            } catch (err: VolleyError) {
                try {
                    val errorResponse = JSONObject(String(err.networkResponse.data))
                    val errorCode = errorResponse.optInt("error")

                    if (errorCode == 300 || errorCode == 301) {
                        _loginForm.postValue(
                            LoginFormState(usernameError = "Couldn't find user")
                        )
                    }
                    if (errorCode == 0 && errorResponse.optJSONArray("non_field_errors") != null) {
                        _loginForm.postValue(
                            LoginFormState(usernameError = "Unable to log in with provided credentials", passwordError = "Unable to log in with provided credentials")
                        )
                    }
                } catch (ex: Exception) {

                }
                _loginSuccess.postValue(false)
            } catch (ex: ClientError) {
                try {
                    val errorResponse = JSONObject(String(ex.networkResponse.data))

                    _loginForm.postValue(
                        LoginFormState(
                            usernameError = errorResponse.optJSONArray("username")?.optString(0) ?: "",
                            passwordError = errorResponse.optJSONArray("password")?.optString(0) ?: ""
                        )
                    )
                } catch (ex: Exception) {

                }
                _loginSuccess.postValue(false)
            }

            NabtoHandler.openSession(username)
            val fingerprint = NabtoHandler.getFingerprint(username) ?: ""
            try {
                CloudHandler.postFingerprint(fingerprint)
            } catch (ex: VolleyError) {

            }
            NabtoHandler.openTunnels(username)
        }
    }

    // A placeholder username validation check
    fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            false
        }
    }

    // A placeholder password validation check
    fun isPasswordValid(password: String): Boolean {
        return password.length > 8 &&
                password.toLowerCase() != password &&
                password.toUpperCase() != password &&
                password.matches(".*\\d+.*".toRegex())
    }

    /**
     * Data validation state of the login form.
     */
    data class LoginFormState(
        val usernameError: String = "",
        val passwordError: String = "",
    )
}
