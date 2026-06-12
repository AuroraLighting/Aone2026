package com.aurora.aonev3.ui.fragments.help.account

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import com.android.volley.VolleyError
import com.aurora.aonev3.network.handlers.CloudHandler
import org.json.JSONObject

class AccountDeleteViewModel : ViewModel() {

    suspend fun deleteUser(username: String?, password: String?) {
        val credentials = CloudHandler.getCredentials()

        if (username == credentials.first && password == credentials.second) {
            val response = try {
                CloudHandler.verifyLogin(username, password)
            } catch (err: VolleyError) {
                throw Exception("Failed to verify User")
            }
            val body = response.optJSONObject("body") ?: JSONObject()
            val token = body.optString("token")

            if (token == CloudHandler.token) {
                try {
                    CloudHandler.deleteUser(username)
                } catch (err: VolleyError) {
                    throw Exception("Failed to delete User")
                }
            }
        } else {
            throw Exception("Failed to verify User")
        }
    }
}
