package com.aurora.aonev3.data.datapoints

import com.aurora.aonev3.synthetic.*
data class DeviceDatapoint(
    val parentGateway: String,
    val id: Int,
    val ldev: String,
    val key: String,
    var value: Any?,
    var lastUpdated: String
) {
    override fun equals(other: Any?): Boolean {
        return other is DeviceDatapoint &&
                this.parentGateway == other.parentGateway &&
                this.id == other.id &&
                this.ldev == other.ldev &&
                this.key == other.key
    }

    override fun hashCode(): Int {
        var result = parentGateway.hashCode()
        result = 31 * result + id
        result = 31 * result + ldev.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }
}
