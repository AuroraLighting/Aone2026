package com.aurora.aonev3.data.logic

import android.os.Parcelable
import com.aurora.aonev3.gson
import com.aurora.aonev3.logic.CollectionMetadata
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONObject

@Parcelize
data class LogicCollection(
    val parentGateway: String,
    val id: Int,
    override var name: String,
    override var metadata: CollectionMetadata,
    override var isEnabled: Boolean
): NewLogicCollection(name, metadata, isEnabled), Parcelable {

    override fun toJSONObject(): JSONObject {
        return super.toJSONObject()
            .put("id", id)
    }

    override fun equals(other: Any?): Boolean {
        return other is LogicCollection &&
                other.parentGateway == this.parentGateway &&
                other.id == this.id
    }

    override fun hashCode(): Int {
        var result = parentGateway.hashCode()
        result = 31 * result + id
        return result
    }
}

open class NewLogicCollection(
    open var name: String,
    open var metadata: CollectionMetadata,
    open var isEnabled: Boolean = true
) {
    open fun toJSONObject(): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("metadata", gson.toJson(metadata))
            .put("enabled", isEnabled)
    }
}