package com.aurora.aonev3.data.groups.groupmembers

data class GroupMember(
    val parentGateway: String,
    val groupId: Int,
    val deviceId: Int,
    val deviceLdev: String,
    val id: Int,
    val isVirtualMember: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        return other is GroupMember &&
                this.parentGateway == other.parentGateway &&
                this.groupId == other.groupId &&
                this.id == other.id
    }

    override fun hashCode(): Int {
        var result = parentGateway.hashCode()
        result = 31 * result + groupId
        result = 31 * result + id
        return result
    }
}