package com.aurora.aonev3.logic

import android.os.Parcelable
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.lang.reflect.Type
import java.util.*


//"{\"collection_type\":\"sensor\",\"trigger_id\":7,\"event\":{\"time\":{\"start\":\"sunset\",\"end_hour\":1,\"end_min\":0},\"days\":\"everyday\",\"group\":{\"id\":3}}}"
@Parcelize
class CollectionMetadata(
    @SerializedName("collection_type") var collectionType: CollectionType? = null,
    @SerializedName("parent_space") var parentSpace: Int? = null,
    @SerializedName("trigger_id") var triggerId: Int? = null,
    @SerializedName("sub_type") var subType: SubType? = null,
    @SerializedName("sub_type_2") var subType2: SubType? = null,
    @SerializedName("sub_type_right") var subTypeRight: SubType? = null,
    @SerializedName("sub_type_left") var subTypeLeft: SubType? = null,
    @SerializedName("target_space") var targetSpace: Int? = null,
    @SerializedName("target_space_right") var targetSpaceRight: Int? = null,
    @SerializedName("target_space_left") var targetSpaceLeft: Int? = null,
    var event: EventMetadata? = null,
    @SerializedName("action_groups") var actionGroups: Array<GroupEventMetadata>? = null,
    @SerializedName("action_devices") var actionDevices: Array<DeviceEventMetadata>? = null
): Parcelable

@Parcelize
class RuleMetadata(
    var action: RuleMetadataType? = null,
    var type: RuleMetadataType? = null,
    var event: EventMetadata? = null,
): Parcelable

@Parcelize
class EventMetadata(
    var days: String? = null,
    var time: TimeEventMetadata? = null,
    var group: GroupEventMetadata? = null,
    var device: DeviceEventMetadata? = null,
    var scene: SceneEventMetadata? = null,
    var trigger: TriggerEnum? = null,
    @SerializedName("trigger_offset") var triggerOffset: Int? = null,
    var action: RuleMetadataType? = null
): Parcelable

@Parcelize
class GroupEventMetadata(val id: Int): Parcelable

@Parcelize
class DeviceEventMetadata(val id: Int, val ldev: String?, val group: Int? = null): Parcelable

@Parcelize
class SceneEventMetadata(val id: Int, val group: Int?): Parcelable

@Parcelize
class TimeEventMetadata(
    val start: String? = null,
    @SerializedName("start_offset") val startOffset: Int? = null,
    val end: String? = null,
    @SerializedName("end_offset")val endOffset: Int? = null,
    @SerializedName("start_hour") val startHour: Int? = null,
    @SerializedName("start_minute") val startMinute: Int? = null,
    @SerializedName("end_hour") val endHour: Int? = null,
    @SerializedName("end_minute") val endMinute: Int? = null,
): Parcelable {
    val usesSunriseSunset: Boolean
    get() {
        return SunriseSunsetType.fromMetadata(start) == SunriseSunsetType.SUNRISE
                || SunriseSunsetType.fromMetadata(start) == SunriseSunsetType.SUNSET
                || SunriseSunsetType.fromMetadata(end) == SunriseSunsetType.SUNRISE
                || SunriseSunsetType.fromMetadata(end) == SunriseSunsetType.SUNSET
    }
}

enum class SubType(val displayName: String) {
    COLOUR_TEMPERATURE("tunable"),
    COLOUR("rgb"),
    SCENES("scene"),
    CYCLE_SCENES("cycle_scenes"),
    STEP("step"),
    STEP_COLOUR_TEMPERATURE("step_colour_temperature"),
    SET_TO_50("set_to_50");

    companion object {
        fun fromDisplayName(displayName: String): SubType? {
            values().forEach {
                if (it.displayName.equals(displayName, ignoreCase = true)) {
                    return it
                }
            }

            return null
        }
    }
}

enum class CollectionType(val displayName: String) {
    REMOTE("remote"),
    BATTERY_DIMMER("battery_dimmer"),
    BATTERY_DIMMER_2("battery_dimmer_2"),
    SCHEDULE("schedule"),
    DYNAMIC_EVENT("dynamic_event"),
    KINETIC("kinetic"),
    SENSOR("sensor"),
    WALLDIMMER("walldimmer"),
    SUNRISE_SUNSET("sunrise_sunset");

    companion object {
        fun fromDisplayName(displayName: String): CollectionType? {
            values().forEach {
                if (it.displayName.equals(displayName, ignoreCase = true)) {
                    return it
                }
            }

            return null
        }
    }
}

enum class RuleMetadataType {
    ON,
    OFF,
    START_TIMER,
    STOP_TIMER,
    LOCK,
    UNLOCK,
    UP,
    DOWN,
    COLOUR_TEMPERATURE_UP,
    COLOUR_TEMPERATURE_DOWN,
    CYCLE_UP,
    CYCLE_DOWN;

    companion object {
        fun fromMetadata(value: String): RuleMetadataType? {
            values().forEach {
                if (it.name.equals(value, ignoreCase = true)) {
                    return it
                }
            }

            return null
        }
    }
}

class CollectionTypeAdapter: JsonSerializer<CollectionType>, JsonDeserializer<CollectionType?> {
    override fun serialize(
        src: CollectionType?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        return src?.let { JsonPrimitive(src.displayName) }
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CollectionType? {
        return CollectionType.fromDisplayName(json.asString)
    }
}

class SubTypeAdapter: JsonSerializer<SubType>, JsonDeserializer<SubType?> {
    override fun serialize(
        src: SubType?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        return src?.let { JsonPrimitive(src.displayName) }
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): SubType? {
        return SubType.fromDisplayName(json.asString)
    }
}

class SunriseSunsetAdapter: JsonSerializer<SunriseSunsetType>, JsonDeserializer<SunriseSunsetType?> {
    override fun serialize(
        src: SunriseSunsetType?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        return src?.let { JsonPrimitive(src.name.toLowerCase()) }
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): SunriseSunsetType? {
        return SunriseSunsetType.fromMetadata(json.asString)
    }
}

class RuleMetadataTypeAdapter: JsonSerializer<RuleMetadataType>, JsonDeserializer<RuleMetadataType?> {
    override fun serialize(
        src: RuleMetadataType?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        return src?.let { JsonPrimitive(src.name.toLowerCase()) }
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): RuleMetadataType? {
        return RuleMetadataType.fromMetadata(json.asString)
    }
}

class TriggerEnumTypeAdapter: JsonSerializer<TriggerEnum>, JsonDeserializer<TriggerEnum?> {
    override fun serialize(
        src: TriggerEnum?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement? {
        return src?.let { JsonPrimitive(src.name.lowercase(Locale.UK)) }
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): TriggerEnum? {
        return try { TriggerEnum.valueOf(json.asString.uppercase()) } catch (ex: IllegalArgumentException) { null }
    }
}