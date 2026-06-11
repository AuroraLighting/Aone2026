package com.aurora.aonev3.data.logic.rules

import android.os.Parcelable
import com.aurora.aonev3.gson
import com.aurora.aonev3.logic.Action
import com.aurora.aonev3.logic.Condition
import com.aurora.aonev3.logic.RuleMetadata
import com.aurora.aonev3.logic.Trigger
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

@Parcelize
data class LogicRule(
    val parentGateway: String,
    val logicCollectionId: Int,
    val id: Int,
    var name: String,
    var metadata: RuleMetadata,
    var isEnabled: Boolean,
    var triggers: Array<Trigger>?,
    var conditions: Array<Condition>?,
    var actions: Array<Action>?
): Parcelable {
    fun toJSONObject(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("metadata", gson.toJson(metadata))
            .put("enabled", isEnabled)
            .put("triggers", triggers?.let { JSONArray(gson.toJson(triggers)) })
            .put("conditions", conditions?.let { JSONArray(gson.toJson(conditions)) })
            .put("actions", actions?.let {JSONArray(gson.toJson(actions)) })
    }

    override fun equals(other: Any?): Boolean {
        return other is LogicRule &&
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

data class NewLogicRule(
    var name: String,
    var metadata: RuleMetadata? = null,
    var isEnabled: Boolean = true,
    var triggers: Array<Trigger>? = null,
    var conditions: Array<Condition>? = null,
    var actions: Array<Action>?
) {
    fun toJSONObject(): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("metadata", metadata?.let { gson.toJson(metadata) })
            .put("enabled", isEnabled)
            .put("triggers", triggers?.let { JSONArray(gson.toJson(triggers)) })
            .put("conditions", conditions?.let { JSONArray(gson.toJson(conditions)) })
            .put("actions", actions?.let {JSONArray(gson.toJson(actions)) })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewLogicRule

        if (name != other.name) return false
        if (metadata != other.metadata) return false
        if (isEnabled != other.isEnabled) return false
        if (triggers != null) {
            if (other.triggers == null) return false
            if (!triggers.contentEquals(other.triggers)) return false
        } else if (other.triggers != null) return false
        if (conditions != null) {
            if (other.conditions == null) return false
            if (!conditions.contentEquals(other.conditions)) return false
        } else if (other.conditions != null) return false
        if (actions != null) {
            if (other.actions == null) return false
            if (!actions.contentEquals(other.actions)) return false
        } else if (other.actions != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + (triggers?.contentHashCode() ?: 0)
        result = 31 * result + (conditions?.contentHashCode() ?: 0)
        result = 31 * result + (actions?.contentHashCode() ?: 0)
        return result
    }
}