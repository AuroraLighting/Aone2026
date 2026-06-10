package com.aurora.aonev3.logic

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.gson
import com.google.gson.*
import com.google.gson.internal.LinkedTreeMap
import kotlinx.android.parcel.Parceler
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type
import kotlin.reflect.full.companionObject

@Parcelize
open class Action(val type: String): Parcelable

@Parcelize
class UpdateResourceAction(val path: String, val data: LogicData): Action("UpdateResource"), Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateResourceAction

        if (path != other.path) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}

@Parcelize
class CreateResourceAction(val path: String, val data: LogicData): Action("CreateResource"), Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreateResourceAction

        if (path != other.path) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}

@Parcelize
class GetResourceAction(val path: String, val mappings: Array<Mapping>): Action("GetResource"), Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GetResourceAction

        if (path != other.path) return false
        if (!mappings.contentEquals(other.mappings)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + mappings.contentHashCode()
        return result
    }
}

@Parcelize
class ExternalApiRequestAction(val uri: String, val headers: Array<Header>? = null, val method: String, val data: LinkedTreeMap<String, Any>? = null, val mappings: Array<Mapping>? = null): Action("ExternalApiRequest"), Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExternalApiRequestAction

        if (uri != other.uri) return false
        if (headers != null) {
            if (other.headers == null) return false
            if (!headers.contentEquals(other.headers)) return false
        } else if (other.headers != null) return false
        if (method != other.method) return false
        if (data != other.data) return false
        if (mappings != null) {
            if (other.mappings == null) return false
            if (!mappings.contentEquals(other.mappings)) return false
        } else if (other.mappings != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + (headers?.contentHashCode() ?: 0)
        result = 31 * result + method.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        result = 31 * result + (mappings?.contentHashCode() ?: 0)
        return result
    }
}

@Parcelize
class CalculateValueAction(val expressions: Array<Expression>): Action("CalculateValue"), Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CalculateValueAction

        if (!expressions.contentEquals(other.expressions)) return false

        return true
    }

    override fun hashCode(): Int {
        return expressions.contentHashCode()
    }
}

@Parcelize
class Mapping(val xpath: String, val variable: String): Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mapping

        if (xpath != other.xpath) return false
        if (variable != other.variable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xpath.hashCode()
        result = 31 * result + variable.hashCode()
        return result
    }
}

@Parcelize
class Header(val header: String, val value: String): Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Header

        if (header != other.header) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}

@Parcelize
class Expression(val expression: String? = null, val rule: String? = null): Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Expression

        if (expression != other.expression) return false
        if (rule != other.rule) return false

        return true
    }

    override fun hashCode(): Int {
        var result = expression.hashCode()
        result = 31 * result + rule.hashCode()
        return result
    }
}

@Parcelize
class LogicData(
    private val value: Any? = null,
    var id: Int? = null,
    var cycle: String? = null,
    var command: String? = null,
    var triggers: Array<Trigger>? = null,
    var conditions: Array<Condition>? = null,
    var actions: Array<Action>? = null
): Parcelable {
    private var intValue: Int?
    private var stringValue: String?
    private var booleanValue: Boolean?

    init {
        when (value) {
            is Int -> {
                intValue = value
                stringValue = null
                booleanValue = null
            }
            is String -> {
                intValue = null
                stringValue = value
                booleanValue = null
            }
            is Boolean -> {
                intValue = null
                stringValue = null
                booleanValue = value
            }
            else -> {
                intValue = null
                stringValue = null
                booleanValue = null
            }
        }
    }

    fun getValue(): Any? {
        return intValue ?: stringValue ?: booleanValue
    }

    fun setValue(value: Any?) {
        when (value) {
            is Int -> {
                intValue = value
                stringValue = null
                booleanValue = null
            }
            is String -> {
                intValue = null
                stringValue = value
                booleanValue = null
            }
            is Boolean -> {
                intValue = null
                stringValue = null
                booleanValue = value
            }
            else -> {
                intValue = null
                stringValue = null
                booleanValue = null
            }
        }
    }

    companion object: Parceler<LogicData> {
        @SuppressLint("ParcelClassLoader")
        override fun create(parcel: Parcel): LogicData {
            val intValue = parcel.readValue(null)
            val stringValue = parcel.readValue(null)
            val booleanValue = parcel.readValue(null)
            val id = parcel.readValue(null) as? Int?
            val cycle = parcel.readValue(null) as? String?
            val command = parcel.readValue(null) as? String?
            val triggersParcelable = parcel.readParcelableArray(Trigger::class.java.classLoader)
            val triggers = ArrayList<Trigger>()
            triggersParcelable?.forEach {
                if (it is Trigger) {
                    triggers.add(it)
                }
            }
            val conditionsParcelable = parcel.readParcelableArray(Condition::class.java.classLoader)
            val conditions = ArrayList<Condition>()
            conditionsParcelable?.forEach {
                if (it is Condition) {
                    conditions.add(it)
                }
            }
            val actionsParcelable = parcel.readParcelableArray(Action::class.java.classLoader)
            val actions = ArrayList<Action>()
            actionsParcelable?.forEach {
                if (it is Action) {
                    actions.add(it)
                }
            }

            val value = intValue ?: stringValue ?: booleanValue

            return LogicData(value, id, cycle, command, triggers.toTypedArray(), conditions.toTypedArray(), actions.toTypedArray())
        }

        override fun LogicData.write(parcel: Parcel, flags: Int) {
            parcel.writeValue(intValue)
            parcel.writeValue(stringValue)
            parcel.writeValue(booleanValue)
            parcel.writeValue(id)
            parcel.writeValue(cycle)
            parcel.writeValue(command)
            parcel.writeParcelableArray(triggers, flags)
            parcel.writeParcelableArray(conditions, flags)
            parcel.writeParcelableArray(actions, flags)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LogicData

        if (intValue != other.intValue) return false
        if (stringValue != other.stringValue) return false
        if (booleanValue != other.booleanValue) return false
        if (id != other.id) return false
        if (cycle != other.cycle) return false
        if (command != other.command) return false
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
        var result = value?.hashCode() ?: 0
        result = 31 * result + (id ?: 0)
        result = 31 * result + (cycle?.hashCode() ?: 0)
        result = 31 * result + (command?.hashCode() ?: 0)
        result = 31 * result + (triggers?.contentHashCode() ?: 0)
        result = 31 * result + (conditions?.contentHashCode() ?: 0)
        result = 31 * result + (actions?.contentHashCode() ?: 0)
        return result
    }
}

class ActionTypeAdapter: JsonSerializer<Action>, JsonDeserializer<Action?> {
    override fun serialize(
        src: Action?,
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
    ): Action? {
        try {
            if (json.isJsonObject) {
                val jsonObject = json.asJsonObject

                if (!jsonObject.has("type")) return null

                return when (val type = jsonObject.get("type").asString) {
                    "UpdateResource" -> gson.fromJson(json, UpdateResourceAction::class.java)
                    "CreateResource" -> gson.fromJson(json, CreateResourceAction::class.java)
                    "GetResource" -> gson.fromJson(json, GetResourceAction::class.java)
                    "ExternalApiRequest" -> gson.fromJson(json, ExternalApiRequestAction::class.java)
                    "CalculateValue" -> gson.fromJson(json, CalculateValueAction::class.java)
                    else -> Action(type)
                }
            }

            return null
        } catch(ex: Exception) {
            return null
        }
    }
}