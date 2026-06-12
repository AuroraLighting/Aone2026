package com.aurora.aonev3.ui.fragments.gateways.acquiregateway

import com.aurora.aonev3.synthetic.*
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.aurora.aonev3.network.handlers.CloudHandler
import org.json.JSONObject


class AcquireGatewayViewModel : ViewModel() {
    var gatewayEuid: String = ""
    var error: String = ""
    var errorCode: Int = 0

    suspend fun acquireGateway(): Boolean {
        val response: JSONObject =
            try {
                CloudHandler.acquireGateway(gatewayEuid)
            } catch (error: VolleyError) {
                val errorMessage = error.networkResponse?.data?.toString(Charsets.UTF_8)
                Log.e(
                    AcquiringFragment.TAG, """AcquireGateway exception:
                            ${error.networkResponse?.statusCode}
                            $errorMessage"""
                )
                error.printStackTrace()

                try {
                    val response = JSONObject(errorMessage!!)

                    if (response.optBoolean("fail")) {
                        errorCode = response.optInt("err_code")
                        return false
                    }
                } catch (ex: Exception) {
                    if (error !is TimeoutError) {
                        this.error = errorMessage ?: "Failed to acquire"
                    } else {
                        this.error = errorMessage ?: "Acquired Hub but failed to connect"
                    }
                }

                try {
                    CloudHandler.getGateways()
                } catch (ex: NoConnectionError) {
                } catch (ex: TimeoutError) {
                }

                return false
            }

        val responseBody = response.optJSONObject("body") ?: JSONObject()

        CloudHandler.getGateway(responseBody.optString("uuid"))

        return true
    }
}
