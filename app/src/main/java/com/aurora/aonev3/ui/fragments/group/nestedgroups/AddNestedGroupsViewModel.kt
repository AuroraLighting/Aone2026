package com.aurora.aonev3.ui.fragments.group.nestedgroups

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.debug
import com.aurora.aonev3.findGroupsNestedIn
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.GroupRepository
import com.aurora.aonev3.data.groups.groupmembers.GroupMember
import com.aurora.aonev3.data.groups.groupmembers.GroupMemberRepository
import com.aurora.aonev3.toIntArray
import org.json.JSONArray
import org.json.JSONObject

class AddNestedGroupsViewModel : ViewModel() {
    private val groupRepository = GroupRepository()
    private val groupMemberRepository = GroupMemberRepository()
    var group: Group? = null
        set(value) {
            field = value
            nestedGroupIds =
                value?.metadata?.optJSONArray("nested_groups")?.toIntArray() ?: IntArray(0)
        }
    var nestedGroupIds = IntArray(0)

    fun getGroups(gateway: NabtoHandler.NabtoGateway) = groupRepository.getAllGroups(gateway)

    suspend fun getGroupMemberEntities(gateway: NabtoHandler.NabtoGateway, groupId: Int) =
        groupMemberRepository.getGroupMemberEntities(gateway, groupId)

    suspend fun saveNestedGroups(gateway: NabtoHandler.NabtoGateway, ids: List<Int>) {
        val group = group ?: return
        val metadata = group.metadata
        val existingNestedGroups =
            metadata.optJSONArray("nested_groups")?.toIntArray() ?: intArrayOf()
        val groupsNestedIn = group.findGroupsNestedIn(ArrayList())
        debug("Nested groups = [${groupsNestedIn.joinToString { it.id.toString() }}]")

        val groupIdsToAdd = ids.filter { it !in existingNestedGroups }
        val groupMembersToAdd = findGroupMembersToAdd(gateway, groupIdsToAdd)
        val groupIdsToRemove = existingNestedGroups.filter { it !in ids }
        val groupMembersToRemove = findGroupMembersToRemove(gateway, groupIdsToRemove)

        removeGroupMembers(gateway, groupMembersToRemove, group.id)
        groupsNestedIn.forEach {
            removeGroupMembers(gateway, groupMembersToRemove, it.id)
        }
        addGroupMembers(gateway, groupMembersToAdd, group.id)
        groupsNestedIn.forEach {
            addGroupMembers(gateway, groupMembersToAdd, it.id)
        }
        metadata.put("nested_groups", JSONArray(ids))

        try {
            DevelcoHandler.putGroup(gateway, group.id, JSONObject().put("metadata", metadata.toString()))
        } catch (err: VolleyError) {
            App.actionFailed()
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
            }
            err.printStackTrace()
        }
    }

    private suspend fun findGroupMembersToRemove(
        gateway: NabtoHandler.NabtoGateway,
        groupIds: List<Int>
    ): List<GroupMember> {
        val groupMembersToRemove = ArrayList<GroupMember>()
        groupIds.forEach { groupId ->
            groupMembersToRemove.addAll(
                getGroupMemberEntities(
                    gateway,
                    groupId
                ).filter { !it.isVirtualMember })
        }

        return groupMembersToRemove
    }

    private suspend fun findGroupMembersToAdd(
        gateway: NabtoHandler.NabtoGateway,
        groupIds: List<Int>
    ): List<GroupMember> {
        val groupMembersToAdd = ArrayList<GroupMember>()
        groupIds.forEach { groupId ->
            groupMembersToAdd.addAll(
                getGroupMemberEntities(
                    gateway,
                    groupId
                ).filter { !it.isVirtualMember })
        }

        return groupMembersToAdd
    }

    private suspend fun removeGroupMembers(
        gateway: NabtoHandler.NabtoGateway,
        groupMembersToRemove: List<GroupMember>,
        groupId: Int
    ) {
        val groupMembers = SyncHandler
            .groupMembersList
            .filter { it.parentGateway == gateway.serial && it.groupId == groupId }
        val membersToRemove = groupMembers.filter { member ->
            groupMembersToRemove.any { it.deviceId == member.deviceId && it.deviceLdev == member.deviceLdev }
        }

        membersToRemove.forEach {
            try {
                DevelcoHandler.deleteGroupMember(gateway, groupId, it.id)
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

    private suspend fun addGroupMembers(
        gateway: NabtoHandler.NabtoGateway,
        groupMembersToAdd: List<GroupMember>,
        groupId: Int
    ) {
        try {
            groupMembersToAdd.forEach {
                DevelcoHandler.postGroupMember(
                    gateway,
                    groupId,
                    it.deviceId,
                    it.deviceLdev
                )
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
        }
    }
}
