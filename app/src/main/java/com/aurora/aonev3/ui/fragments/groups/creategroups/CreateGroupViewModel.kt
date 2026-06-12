package com.aurora.aonev3.ui.fragments.groups.creategroups

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.groups.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class CreateGroupViewModel : ViewModel() {
    private val groupRepository = GroupRepository()

    fun getGroups(gateway: NabtoHandler.NabtoGateway) = groupRepository.getAllGroups(gateway)

    fun createGroup(name: String, isVirtual: Boolean) {
        val metadata = JSONObject().put("is_virtual_group", isVirtual)
        NabtoHandler.selectedGateway?.let { gateway ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    DevelcoHandler.postGroups(
                        gateway, JSONObject()
                            .put("name", name)
                            .put("grpType", "generic")
                            .put("metadata", metadata.toString())
                    )
                } catch (err: VolleyError) {
                    App.actionFailed()
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(
                            gateway,
                            CloudHandler.getCredentials().first
                        )
                    }
                    err.printStackTrace()
                }
            }
        }
    }
}
