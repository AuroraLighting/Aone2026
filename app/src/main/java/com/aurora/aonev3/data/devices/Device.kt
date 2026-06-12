package com.aurora.aonev3.data.devices

import com.aurora.aonev3.synthetic.*
import org.json.JSONObject

data class Device(
    val parentGateway: String,
    val eui: String,
    val id: Int,
    var name: String,
    var defaultName: String,
    var metadata: JSONObject,
    var online: Boolean
) {
    var ldevs: Array<String> = arrayOf()
    var otaStatus = "idle"
    var firmwareVersion = ""
    var deviceClass: DeviceClass = DeviceClass.UNKNOWN
    fun getDeviceType() = DeviceType.fromDeviceClass(deviceClass)
    fun getDeviceCategory() = DeviceCategory.fromDeviceClass(deviceClass)

    init {
        if (defaultName.equals("squidzigbee", ignoreCase = true)) {
            deviceClass = DeviceClass.GATEWAY
            ldevs = arrayOf("network")
        }
    }

    enum class DeviceClass(val ldevs: Array<String>) {
        AURORABULB(arrayOf( "bulb")),
        AURORADUALSOCKET(arrayOf( "socket1", "socket2", "lights", "lock")),
        AURORAGEYSER(arrayOf("circuit")),
        AURORASMARTPLUG(arrayOf("smartplug")),
        AURORARGBWBULB(arrayOf( "bulb")),
        AURORATWBULB(arrayOf( "bulb")),
        AURORAWALLDIMMER(arrayOf( "dimmer")),
        AURORAWALLDIMMER2(arrayOf( "dimmer")),
        BATTERYDIMMER(arrayOf("remote")),
        BATTERYDIMMERDUAL(arrayOf( "remote1", "remote2")),
        DOORWINDOW(arrayOf("alarm", "battery")),
        MOTION(arrayOf("occupancy", "light", "battery", "alarm", "identify", "diagnostic")),
        PTM215ZE(arrayOf("switchA", "switchB", "switchAB", "energybar")),
        REMOTE(arrayOf("remote", "identify")),
        SMARTPLUG(arrayOf("smartplug", "identify", "diagnostic")),
        WINDOW(arrayOf("alarm", "battery", "identify", "diagnostic")),
        GATEWAY(arrayOf("network")),
        UNKNOWN(arrayOf())
    }

    enum class DeviceType {
        BATTERY_DIMMER,
        BATTERY_DIMMER_DUAL,
        DOOR_SENSOR,
        DUAL_SOCKET,
        KINETIC,
        GATEWAY,
        GERYSER,
        LIGHT,
        MOTION_SENSOR,
        REMOTE,
        RGBW_LIGHT,
        SMARTPLUG,
        TW_LIGHT,
        WALL_DIMMER_CONTROL,
        WALL_DIMMER_INLINE;

        companion object {
            fun fromDeviceClass(deviceClass: DeviceClass): DeviceType? {
                return when (deviceClass) {
                    DeviceClass.AURORABULB -> LIGHT
                    DeviceClass.AURORADUALSOCKET -> DUAL_SOCKET
                    DeviceClass.AURORAGEYSER -> GERYSER
                    DeviceClass.AURORASMARTPLUG,
                    DeviceClass.SMARTPLUG -> SMARTPLUG
                    DeviceClass.AURORARGBWBULB -> RGBW_LIGHT
                    DeviceClass.AURORATWBULB -> TW_LIGHT
                    DeviceClass.AURORAWALLDIMMER -> WALL_DIMMER_INLINE
                    DeviceClass.AURORAWALLDIMMER2 -> WALL_DIMMER_CONTROL
                    DeviceClass.BATTERYDIMMER -> BATTERY_DIMMER
                    DeviceClass.BATTERYDIMMERDUAL -> BATTERY_DIMMER_DUAL
                    DeviceClass.DOORWINDOW,
                    DeviceClass.WINDOW -> DOOR_SENSOR
                    DeviceClass.MOTION -> MOTION_SENSOR
                    DeviceClass.PTM215ZE -> KINETIC
                    DeviceClass.REMOTE -> REMOTE
                    DeviceClass.GATEWAY -> GATEWAY
                    else -> null
                }
            }
        }
    }

    enum class DeviceCategory {
        HUB,
        LIGHTS,
        POWER,
        SOCKETS,
        SENSORS,
        SWITCHES;

        companion object {
            fun fromDeviceClass(deviceClass: DeviceClass): DeviceCategory? {
                return when (deviceClass) {
                    DeviceClass.AURORABULB,
                    DeviceClass.AURORARGBWBULB,
                    DeviceClass.AURORATWBULB -> LIGHTS
                    DeviceClass.AURORADUALSOCKET -> SOCKETS
                    DeviceClass.AURORASMARTPLUG,
                    DeviceClass.SMARTPLUG -> POWER
                    DeviceClass.AURORAWALLDIMMER,
                    DeviceClass.AURORAWALLDIMMER2,
                    DeviceClass.BATTERYDIMMER,
                    DeviceClass.BATTERYDIMMERDUAL,
                    DeviceClass.PTM215ZE,
                    DeviceClass.REMOTE -> SWITCHES
                    DeviceClass.DOORWINDOW,
                    DeviceClass.WINDOW,
                    DeviceClass.MOTION -> SENSORS
                    DeviceClass.GATEWAY -> HUB
                    else -> null
                }
            }
        }
    }

    fun toJSONObject(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("metadata", metadata.toString())
            .put("eui", eui)
    }

    override fun equals(other: Any?) = other is Device && this.eui == other.eui

    override fun hashCode() = eui.hashCode()
}
