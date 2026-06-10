package com.aurora.aonev3.ui

import com.aurora.aonev3.R

enum class IconsDevices(val defaultNames: Array<String>, val onResId: Int, val offResId: Int) {
    AONE(arrayOf("Aurora LED (Fixed White)", "Single Colour LED Strip Controller", "AOne 1-10V controller", "NXP Strip (Tunable White)", "LDS Strip (Tunable White)", "Aurora LED (Tunable White)", "CX LED Strip Controller", "RGBCX LED Strip Controller"), R.drawable.aone, R.drawable.aone_off),
    BATTERY_DIMMER_1G(arrayOf("Aurora Battery Dimmer NPD4440"), R.drawable.battery_dimmer_1g, R.drawable.battery_dimmer_1g),
    BATTERY_DIMMER_2G(arrayOf("Aurora Battery Dimmer dual knob"), R.drawable.battery_dimmer_2g, R.drawable.battery_dimmer_2g),
    CANDLE_CX(arrayOf("Aurora Smart Candle (Tunable White)"), R.drawable.candle_cx, R.drawable.candle_off),
    CANDLE_FW(arrayOf(), R.drawable.candle_fw, R.drawable.candle_off),
    CANDLE_RGBW(arrayOf("Aurora Smart Candle (RGBW)"), R.drawable.candle_rgb, R.drawable.candle_off),
    DOOR_WINDOW(arrayOf("Aurora Door Sensor", "Door Sensor", "Magnetic Sensor", "Window Sensor"), R.drawable.door_window, R.drawable.door_window),
    FILAMENT_1(arrayOf("A60 filament", "PREPRODUCTION SAMPLE FW"), R.drawable.filament_1, R.drawable.filament_1_off),
    FILAMENT_2(arrayOf("ST64 filament"), R.drawable.filament_2, R.drawable.filament_2_off),
    FILAMENT_3(arrayOf("G125 filament", "G95 filament"), R.drawable.filament_3, R.drawable.filament_3_off),
    GLS_CX(arrayOf("Aurora Smart Bulb (Tunable White)", "Aurora Smart Lamp (Tunable White)"), R.drawable.gls_cx, R.drawable.gls_off),
    GLS_FW(arrayOf("Aurora Smart Bulb (Fixed White)", "Aurora Smart Lamp (Fixed White)"), R.drawable.gls_fw, R.drawable.gls_off),
    GLS_RGBW(arrayOf("Aurora Smart Bulb (RGBW)", "Aurora Smart Lamp (RGBW)"), R.drawable.gls_rgb, R.drawable.gls_off),
    GU10_CX(arrayOf("Aurora Smart GU10 (Tunable White)"), R.drawable.gu10_cx, R.drawable.gu10_off),
    GU10_FW(arrayOf("Aurora Smart GU10 (Fixed White)"), R.drawable.gu10_fw, R.drawable.gu10_off),
    GU10_RGBW(arrayOf("Aurora Smart GU10 (RGBW)", "Aurora Smart GU10 (RGBW) v2"), R.drawable.gu10_rgb, R.drawable.gu10_off),
    KINETIC(arrayOf("AOne Kinetic Controller"), R.drawable.kinetic_walldimmer, R.drawable.kinetic_walldimmer),
    MOTION(arrayOf("Motion Sensor", "PIR Motion Sensor"), R.drawable.pir, R.drawable.pir),
    MPROZ(arrayOf("Aurora MPRO-X (Fixed White)", "Aurora MPRO-X (Tunable White)"), R.drawable.mproz, R.drawable.mproz_off),
    PLUG(arrayOf("Aurora Smart Plug in Adaptor", "Smart Plug"), R.drawable.plug, R.drawable.plug_off),
    RELAY(arrayOf("Smart Cable", "Smart Relay"), R.drawable.relay, R.drawable.relay_off),
    REMOTE(arrayOf("Remote"), R.drawable.remote, R.drawable.remote),
    ROTARY_WALLDIMMER(arrayOf("Aurora Wall Dimmer (control)", "Aurora Wall Dimmer", "Aurora Wall Dimmer (inline)"), R.drawable.rotary_walldimmer_1g, R.drawable.rotary_walldimmer_off),
    UNKNOWN(emptyArray(), R.drawable.gls_fw, R.drawable.gls_off);

    companion object {
        fun fromDefaultName(defaultName: String): IconsDevices {
            return values().find { value -> value.defaultNames.any { it.toLowerCase() == defaultName.toLowerCase() } }
                ?: UNKNOWN
        }
    }
}