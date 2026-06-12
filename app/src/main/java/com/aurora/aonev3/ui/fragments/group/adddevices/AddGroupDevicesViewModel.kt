package com.aurora.aonev3.ui.fragments.group.adddevices

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class AddGroupDevicesViewModel: ViewModel() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    var group: Group? = null
    set(value) {
        field = value
        groupsNestedIn = value?.findGroupsNestedIn(ArrayList()) ?: emptyList()
    }
    var groupsNestedIn = emptyList<Group>()

    suspend fun addGroupMember(device: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return
        val group = group ?: return

        try {
            DevelcoHandler.postGroupMember(
                gateway,
                group.id,
                device.id,
                device.ldevs.firstOrNull() ?: ""
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

            try {
                val error = JSONArray(err.networkResponse?.data?.toString(Charsets.UTF_8))
                val errorMessage = error.optJSONObject(0)?.optString("message") ?: ""

                when {
                    errorMessage.toLowerCase() == "request timed out." -> {
                        throw TimeoutException(
                            JSONObject()
                                .put("deviceId", device.id)
                                .put("groupId", group.id)
                                .toString()
                        )
                    }
                    errorMessage.toLowerCase().contains("insufficientspace") -> {
                        throw InsufficientSpaceException(
                            JSONObject()
                                .put("deviceId", device.id)
                                .put("groupId", group.id)
                                .toString()
                        )
                    }
                    else -> {
                        crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                        crashlytics.recordException(
                            UnknownApiException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", group.id)
                                    .put("message", errorMessage)
                                    .toString()
                            )
                        )
                        if (errorMessage.isNotBlank()) {
                            throw UnknownApiException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", group.id)
                                    .put("message", errorMessage)
                                    .toString()
                            )
                        }
                    }
                }
            } catch (ex: JSONException) {
                ex.printStackTrace()
                crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                crashlytics.recordException(ex)
            } catch (ex: NullPointerException) {
                ex.printStackTrace()
                crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                crashlytics.recordException(ex)
            }
        }

        groupsNestedIn.forEach {
            try {
                DevelcoHandler.postGroupMember(
                    gateway,
                    it.id,
                    device.id,
                    device.ldevs.firstOrNull() ?: ""
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

                try {
                    val error = JSONArray(err.networkResponse?.data?.toString(Charsets.UTF_8))
                    val errorMessage = error.optJSONObject(0)?.optString("message")

                    when {
                        errorMessage?.toLowerCase() == "request timed out." -> {
                            throw TimeoutException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", group.id)
                                    .toString()
                            )
                        }
                        errorMessage?.toLowerCase()?.contains("insufficientspace") == true -> {
                            throw InsufficientSpaceException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", group.id)
                                    .toString()
                            )
                        }
                        else -> {
                            crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                            crashlytics.recordException(
                                UnknownApiException(
                                    JSONObject()
                                        .put("deviceId", device.id)
                                        .put("groupId", group.id)
                                        .put("message", errorMessage)
                                        .toString()
                                )
                            )
                            throw UnknownApiException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", group.id)
                                    .put("message", errorMessage)
                                    .toString()
                            )
                        }
                    }
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                    crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                    crashlytics.recordException(ex)
                } catch (ex: NullPointerException) {
                    ex.printStackTrace()
                    crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                    crashlytics.recordException(ex)
                }
            }
        }

        try {
            if (device.deviceClass == Device.DeviceClass.AURORADUALSOCKET) {
                DevelcoHandler.postGroupMember(
                    gateway,
                    group.id,
                    device.id,
                    "socket2"
                )

                groupsNestedIn.forEach {
                    DevelcoHandler.postGroupMember(
                        gateway,
                        it.id,
                        device.id,
                        "socket2"
                    )
                }
            }
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

            try {
                val error = JSONArray(err.networkResponse?.data?.toString(Charsets.UTF_8))
                val errorMessage = error.optJSONObject(0)?.optString("message")

                when {
                    errorMessage?.toLowerCase() == "request timed out." -> {
                        throw TimeoutException(
                            JSONObject()
                                .put("deviceId", device.id)
                                .put("groupId", group.id)
                                .toString()
                        )
                    }
                    errorMessage?.toLowerCase()?.contains("insufficientspace") == true -> {
                        throw InsufficientSpaceException(
                            JSONObject()
                                .put("deviceId", device.id)
                                .put("groupId", group.id)
                                .toString()
                        )
                    }
                    else -> {
                        crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                        crashlytics.recordException(
                            UnknownApiException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", group.id)
                                    .put("message", errorMessage)
                                    .toString()
                            )
                        )
//                        throw UnknownApiException(
//                            JSONObject()
//                                .put("deviceId", device.id)
//                                .put("groupId", group.id)
//                                .put("message", errorMessage)
//                                .toString()
//                        )
                    }
                }
            } catch (ex: JSONException) {
                ex.printStackTrace()
                crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                crashlytics.recordException(ex)
            } catch (ex: NullPointerException) {
                ex.printStackTrace()
                crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                crashlytics.recordException(ex)
            }
        }
    }

    suspend fun addVirtualGroupMember(device: Device) {
        val gateway = NabtoHandler.selectedGateway ?: return
        val group = group ?: return

        val metadata = group.metadata
        val virtualMembers =
            metadata.optJSONArray("virtual_members")
                ?: JSONArray()

        virtualMembers.put(
            JSONObject()
                .put("id", device.id)
                .put("ldev", device.ldevs.firstOrNull())
        )

        metadata.put("virtual_members", virtualMembers)
        try {
            DevelcoHandler.putGroup(
                gateway,
                group.id,
                JSONObject().put(
                    "metadata",
                    metadata.toString()
                )
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

    suspend fun deleteGroupMembers(
        groupMembers: List<GroupMember>
    ) {
        val gateway = NabtoHandler.selectedGateway ?: return

        groupMembers.forEach { groupMember ->
            if (!groupMember.isVirtualMember) {
                try {
                    DevelcoHandler.deleteGroupMember(
                        gateway,
                        groupMember.groupId,
                        groupMember.id
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

                    try {
                        val error = JSONArray(err.networkResponse?.data?.toString(Charsets.UTF_8))
                        val errorMessage = error.optJSONObject(0)?.optString("message")

                        if (errorMessage?.toLowerCase() == "request timed out.") {
                            throw TimeoutException(
                                JSONObject()
                                    .put("deviceId", groupMember.deviceId)
                                    .put("groupId", groupMember.groupId)
                                    .toString()
                            )
                        } else {
                            crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                            crashlytics.recordException(
                                UnknownApiException(
                                    JSONObject()
                                        .put("deviceId", groupMember.deviceId)
                                        .put("groupId", groupMember.groupId)
                                        .put("message", errorMessage)
                                        .toString()
                                )
                            )
                            throw UnknownApiException(
                                JSONObject()
                                    .put("deviceId", groupMember.deviceId)
                                    .put("groupId", groupMember.groupId)
                                    .put("message", errorMessage)
                                    .toString()
                            )
                        }
                    } catch (ex: JSONException) {
                        ex.printStackTrace()
                        crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                        crashlytics.recordException(ex)
                    } catch (ex: NullPointerException) {
                        ex.printStackTrace()
                        crashlytics.log(err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null")
                        crashlytics.recordException(ex)
                    }
                }
            } else {
                val group = SyncHandler
                    .groupsList
                    .find { it.parentGateway == gateway.serial && it.id == groupMember.groupId }

                if (group != null) {
                    val virtualMembers =
                        group.metadata.optJSONArray("virtual_members")
                            ?: JSONArray()
                    val newVirtualMembers = JSONArray()

                    for (i in virtualMembers.indices()) {
                        val virtualMember =
                            virtualMembers.optJSONObject(i) ?: JSONObject()

                        if (virtualMember.optInt("id") != groupMember.deviceId) {
                            newVirtualMembers.put(virtualMember)
                        }
                    }

                    val metadata =
                        group.metadata.put("virtual_members", newVirtualMembers)

                    try {
                        DevelcoHandler.putGroup(
                            gateway,
                            group.id,
                            JSONObject().put("metadata", metadata.toString())
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
}
