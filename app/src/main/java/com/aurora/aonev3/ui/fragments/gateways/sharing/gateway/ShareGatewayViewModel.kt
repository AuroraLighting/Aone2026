package com.aurora.aonev3.ui.fragments.gateways.sharing.gateway

import androidx.lifecycle.ViewModel
import com.android.volley.VolleyError
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.Share
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray

class ShareGatewayViewModel : ViewModel() {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    suspend fun getShares(serial: String): ArrayList<Share>? {
        return try {
            CloudHandler.getGateway(serial)
            NabtoHandler.nabtoGateways.find { it.serial == serial }?.shares
        } catch (err: VolleyError) {
            crashlytics.recordException(err)
            null
        }
    }

    suspend fun share(serial: String, email: String, permissions: JSONArray) = CloudHandler.share(serial, email, permissions)

    suspend fun unshare(serial: String, email: String) = CloudHandler.unshare(serial, email)
}