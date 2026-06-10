package com.aurora.aonev3.ui.fragments.gateways.sharing.user

import androidx.lifecycle.ViewModel
import com.android.volley.VolleyError
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.Share
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray

class SharedUserViewModel : ViewModel() {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    suspend fun unshare(serial: String, email: String) = CloudHandler.unshare(serial, email)
}