package com.aurora.aonev3.data.logic.timers

import com.aurora.aonev3.gson
import com.aurora.aonev3.logic.Action
import org.json.JSONArray
import org.json.JSONObject

data class LogicTimer(
    val parentGateway: String,
    val logicCollectionId: Int,
    val id: Int,
    override var name: String,
    override var metadata: JSONObject?,
    override var timeout: Int,
    override var actions: Array<Action>?,
    var remaining: Int,
    var status: String
): NewLogicTimer(name, metadata, timeout, actions) {
    override fun toJSONObject(): JSONObject {
        return super.toJSONObject()
            .put("id", id)
            .put("status", status)
    }

    override fun equals(other: Any?): Boolean {
        return other is LogicTimer &&
                other.parentGateway == this.parentGateway &&
                other.logicCollectionId == this.logicCollectionId &&
                other.id == this.id
    }

    override fun hashCode(): Int {
        var result = parentGateway.hashCode()
        result = 31 * result + logicCollectionId
        result = 31 * result + id
        return result
    }
}

open class NewLogicTimer(
    open var name: String,
    open var metadata: JSONObject? = null,
    open var timeout: Int,
    open var actions: Array<Action>?
) {
    open fun toJSONObject(): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("metadata", metadata?.toString())
            .put("timeout", timeout)
            .put("actions", actions?.let { JSONArray(gson.toJson(actions)) })
    }
}