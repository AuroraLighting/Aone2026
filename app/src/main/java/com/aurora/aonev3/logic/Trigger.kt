package com.aurora.aonev3.logic

import com.aurora.aonev3.synthetic.*
import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.aurora.aonev3.gson
import com.google.gson.*
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.*

@Parcelize
open class Trigger(val type: String): Parcelable

@Parcelize
data class ResourceUpdateTrigger(val path: String): Trigger("ResourceUpdate") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceUpdateTrigger

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

@Parcelize
data class TimeOfDayTrigger(val hour: Any, val min: Any): Trigger("TimeOfDay") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimeOfDayTrigger

        if (hour != other.hour) return false
        if (min != other.min) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hour.hashCode()
        result = 31 * result + min.hashCode()
        return result
    }

    companion object: Parceler<TimeOfDayTrigger> {
        @SuppressLint("ParcelClassLoader")
        override fun create(parcel: Parcel): TimeOfDayTrigger {
            val hour = parcel.readValue(null) ?: 0
            val min = parcel.readValue(null) ?: 0

            return TimeOfDayTrigger(hour, min)
        }

        override fun TimeOfDayTrigger.write(parcel: Parcel, flags: Int) {
            parcel.writeValue(hour)
            parcel.writeValue(min)
        }
    }
}

enum class TriggerEnum {
    OPEN,
    CLOSE,
    TIME,
    SUNRISE,
    SUNSET
}

class TriggerTypeAdapter: JsonSerializer<Trigger>, JsonDeserializer<Trigger?> {
    override fun serialize(
        src: Trigger?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        if (src == null) return null

        return context.serialize(src)
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Trigger? {
        try {
            if (json.isJsonObject) {
                val jsonObject = json.asJsonObject

                if (!jsonObject.has("type")) return null

                return when (val type = jsonObject.get("type").asString) {
                    "TimeOfDay" -> gson.fromJson(json, TimeOfDayTrigger::class.java)
                    "ResourceUpdate" -> gson.fromJson(json, ResourceUpdateTrigger::class.java)
                    else -> Trigger(type)
                }
            }

            return null
        } catch(ex: Exception) {
            return null
        }
    }
}
