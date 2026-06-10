package com.aurora.aonev3

import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.data.groups.Group
import org.json.JSONArray

class NestedGroupTree(groups: List<Group>) {
     private val nestedGroupTreeNodes = groups.map { NestedGroupTreeNode(it) }

     init {
        nestedGroupTreeNodes.forEach { nestedGroupTreeNode ->
            val nestedGroupsIds = (nestedGroupTreeNode.group.metadata.optJSONArray("nested_groups") ?: JSONArray()).toIntArray()
            val nestedGroups = nestedGroupsIds.map { id ->
                SyncHandler
                    .groupsList
                    .find {
                        it.parentGateway == nestedGroupTreeNode.group.parentGateway
                                && it.id == id
                    }
            }.filterNotNull()
            nestedGroupTreeNodes.filter { it.group in nestedGroups }
            .forEach {
                nestedGroupTreeNode.isChild = false
                it.nestedIn.add(nestedGroupTreeNode)
            }
        }
    }

    fun getChild() = nestedGroupTreeNodes.firstOrNull { it.isChild }
 }

data class NestedGroupTreeNode(val group: Group) {
    var nestedIn = ArrayList<NestedGroupTreeNode>()
    var isChild: Boolean = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NestedGroupTreeNode

        if (group != other.group) return false

        return true
    }

    override fun hashCode(): Int {
        return group.hashCode()
    }
}