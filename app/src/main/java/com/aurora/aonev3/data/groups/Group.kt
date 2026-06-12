package com.aurora.aonev3.data.groups

import com.aurora.aonev3.synthetic.*
import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class Group (
    val parentGateway: String,
    val id: Int,
    var name: String,
    var metadata: JSONObject,
    val grpType: String
): Parcelable {
    var ldevs: Array<String> = arrayOf("generic")

    override fun equals(other: Any?): Boolean {
        return other is Group &&
                other.parentGateway == this.parentGateway &&
                other.id == this.id
    }

    override fun hashCode(): Int {
        var result = parentGateway.hashCode()
        result = 31 * result + id
        return result
    }

    companion object: Parceler<Group> {
        override fun create(parcel: Parcel): Group {
            val parentGateway = parcel.readString() ?: ""
            val id = parcel.readInt()
            val name = parcel.readString() ?: ""
            val metadata = try {
                JSONObject(parcel.readString() ?: "")
            } catch (ex: Exception) {
                JSONObject()
            }
            val grpType = parcel.readString() ?: ""
            val ldevs: Array<String> = emptyArray()
            parcel.readStringArray(ldevs)
            val group = Group(
                parentGateway = parentGateway,
                id = id,
                name = name,
                metadata = metadata,
                grpType = grpType
            )
            group.ldevs = ldevs

            return group
        }

        override fun Group.write(parcel: Parcel, flags: Int) {
            parcel.writeString(parentGateway)
            parcel.writeInt(id)
            parcel.writeString(name)
            parcel.writeString(metadata.toString())
            parcel.writeString(grpType)
            parcel.writeStringArray(ldevs)
        }

    }
}
