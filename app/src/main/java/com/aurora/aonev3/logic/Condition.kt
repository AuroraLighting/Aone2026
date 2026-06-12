package com.aurora.aonev3.logic

import com.aurora.aonev3.synthetic.*
import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.aurora.aonev3.gson
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.google.gson.*
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type

@Parcelize
open class Condition(val type: String): Parcelable

@Parcelize
class OrCondition(val conditions: Array<Condition>): Condition("Or") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrCondition

        if (!conditions.contentEquals(other.conditions)) return false

        return true
    }

    override fun hashCode(): Int {
        return conditions.contentHashCode()
    }
}

@Parcelize
class AndCondition(val conditions: Array<Condition>): Condition("And") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndCondition

        if (!conditions.contentEquals(other.conditions)) return false

        return true
    }

    override fun hashCode(): Int {
        return conditions.contentHashCode()
    }
}

@Parcelize
class ResourceValueCondition(val path: String, val rule: String): Condition("ResourceValue") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceValueCondition

        if (path != other.path) return false
        if (rule != other.rule) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + rule.hashCode()
        return result
    }
}

@Parcelize
class TimeIntervalCondition(val startHour: Any, val startMin: Any, val endHour: Any, val endMin: Any): Condition("TimeInterval") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimeIntervalCondition

        if (startHour is Number && other.startHour is Number) {
            if (startHour.toInt() != other.startHour.toInt()) return false
        } else {
            if (startHour != other.startHour) return false
        }
        if (startMin is Number && other.startMin is Number) {
            if (startMin.toInt() != other.startMin.toInt()) return false
        } else {
            if (startMin != other.startMin) return false
        }
        if (endHour is Number && other.endHour is Number) {
            if (endHour.toInt() != other.endHour.toInt()) return false
        } else {
            if (endHour != other.endHour) return false
        }
        if (endMin is Number && other.endMin is Number) {
            if (endMin.toInt() != other.endMin.toInt()) return false
        } else {
            if (endMin != other.endMin) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = startHour.hashCode()
        result = 31 * result + startMin.hashCode()
        result = 31 * result + endHour.hashCode()
        result = 31 * result + endMin.hashCode()
        return result
    }

    companion object: Parceler<TimeIntervalCondition> {
        @SuppressLint("ParcelClassLoader")
        override fun create(parcel: Parcel): TimeIntervalCondition {
            val startHour = parcel.readValue(null) ?: 0
            val startMin = parcel.readValue(null) ?: 0
            val endHour = parcel.readValue(null) ?: 0
            val endMin = parcel.readValue(null) ?: 0

            return TimeIntervalCondition(startHour, startMin, endHour, endMin)
        }

        override fun TimeIntervalCondition.write(parcel: Parcel, flags: Int) {
            parcel.writeValue(startHour)
            parcel.writeValue(startMin)
            parcel.writeValue(endHour)
            parcel.writeValue(endMin)
        }
    }
}

@Parcelize
class DayOfWeekCondition(val days: Array<DayOfWeek>): Condition("DayOfWeek") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DayOfWeekCondition

        if (!days.contentEquals(other.days)) return false

        return true
    }

    override fun hashCode(): Int {
        return days.contentHashCode()
    }
}

enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

class ConditionTypeAdapter: JsonSerializer<Condition>, JsonDeserializer<Condition?> {
    override fun serialize(
        src: Condition?,
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
    ): Condition? {
        try {
            if (json.isJsonObject) {
                val jsonObject = json.asJsonObject

                if (!jsonObject.has("type")) return null

                return when (val type = jsonObject.get("type").asString) {
                    "ResourceValue" -> gson.fromJson(json, ResourceValueCondition::class.java)
                    "TimeInterval" -> gson.fromJson(json, TimeIntervalCondition::class.java)
                    "DayOfWeek" -> gson.fromJson(json, DayOfWeekCondition::class.java)
                    "Or" -> gson.fromJson(json, OrCondition::class.java)
                    "And" -> gson.fromJson(json, AndCondition::class.java)
                    else -> Condition(type)
                }
            }

            return null
        } catch(ex: Exception) {
            return null
        }
    }
}
