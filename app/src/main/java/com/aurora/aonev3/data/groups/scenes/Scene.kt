package com.aurora.aonev3.data.groups.scenes

import com.aurora.aonev3.synthetic.*
import org.json.JSONObject

data class Scene(
    val parentGateway: String,
    val groupId: Int,
    val id: Int,
    var name: String,
    var metadata: JSONObject,
    val transitionTime: Int
) {
    override fun equals(other: Any?): Boolean {
        return other is Scene &&
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
