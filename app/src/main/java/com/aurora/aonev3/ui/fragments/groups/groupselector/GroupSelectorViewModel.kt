package com.aurora.aonev3.ui.fragments.groups.groupselector

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
import com.aurora.aonev3.data.groups.GroupRepository
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class GroupSelectorViewModel: ViewModel() {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val groupRepository = GroupRepository()

    fun getGroups(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Group> = groupRepository.getAllGroups(gateway)

    suspend fun createGroup(name: String): Int? {
        val gateway = NabtoHandler.selectedGateway ?: return null
        val response = try {
            DevelcoHandler.postGroups(
                gateway,
                JSONObject()
                    .put("name", name)
                    .put("grpType", "generic")
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
            JSONObject()
        }

        return response.optJSONObject("body")?.optInt("id")
    }

    suspend fun addGroupMember(device: Device, group: Group) {
        val gateway = NabtoHandler.selectedGateway ?: return

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
                val errorMessage = error.optJSONObject(0)?.optJSONArray("errors")?.optJSONObject(0)?.optString("message")

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
                        crashlytics.log(
                            err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null"
                        )
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

        group.findGroupsNestedIn(ArrayList()).forEach {
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
                                    .put("groupId", it.id)
                                    .toString()
                            )
                        }
                        errorMessage?.toLowerCase()?.contains("insufficientspace") == true -> {
                            throw InsufficientSpaceException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", it.id)
                                    .toString()
                            )
                        }
                        else -> {
                            crashlytics.log(
                                err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null"
                            )
                            crashlytics.recordException(
                                UnknownApiException(
                                    JSONObject()
                                        .put("deviceId", device.id)
                                        .put("groupId", it.id)
                                        .put("message", errorMessage)
                                        .toString()
                                )
                            )
                            throw UnknownApiException(
                                JSONObject()
                                    .put("deviceId", device.id)
                                    .put("groupId", it.id)
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

                group.findGroupsNestedIn(ArrayList()).forEach {
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
                        crashlytics.log(
                            err.networkResponse?.data?.toString(Charsets.UTF_8) ?: "null"
                        )
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

    suspend fun addVirtualGroupMember(device: Device, group: Group) {
        val gateway = NabtoHandler.selectedGateway ?: return

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

    suspend fun deleteGroupMembers(groupMembersForDevice: List<GroupMember>) {
        val gateway = NabtoHandler.selectedGateway ?: return

        groupMembersForDevice.forEach { groupMember ->
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
                    .find {
                        it.parentGateway == groupMember.parentGateway
                                && it.id == groupMember.groupId
                    }

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
