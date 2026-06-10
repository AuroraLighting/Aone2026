package com.aurora.aonev3.ui.fragments.gateways

import androidx.lifecycle.ViewModel
import com.android.volley.VolleyError
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics

class GatewaySwitchViewModel: ViewModel() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    suspend fun releaseGateway(gateway: NabtoHandler.NabtoGateway) {
        try {
            CloudHandler.releaseGateway(gateway.serial)
            CloudHandler.getGateways()
        } catch(err: VolleyError) {
            crashlytics.log("E/$TAG: Releasing gateway")
            crashlytics.recordException(err)
        }
    }

    companion object {
        private val TAG = "GatewaySwitchViewModel"
    }
}