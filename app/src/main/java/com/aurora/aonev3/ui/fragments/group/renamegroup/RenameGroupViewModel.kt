package com.aurora.aonev3.ui.fragments.group.renamegroup

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.groups.Group
import org.json.JSONObject

class RenameGroupViewModel: ViewModel() {

    lateinit var group: Group

    suspend fun rename(name: String) {
        val gateway = NabtoHandler.selectedGateway ?: return
        if (!this::group.isInitialized) return
        if (name == group.name || name.isBlank()) return

        try {
            DevelcoHandler.putGroup(
                gateway,
                group.id,
                JSONObject().put("name", name),
                first = true
            )
        } catch (err: VolleyError) {
            App.actionFailed()
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
            }
            err.printStackTrace()
        }
    }
}
