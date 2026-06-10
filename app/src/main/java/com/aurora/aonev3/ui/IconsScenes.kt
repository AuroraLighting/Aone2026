package com.aurora.aonev3.ui

import com.aurora.aonev3.R

enum class IconsScenes constructor(val resourceValue: Int, val stringValue: String, val backgroundColour: Int) {
    MORNING(R.drawable.ic_morning, "morning", R.color.morningBackground),
    DAY(R.drawable.ic_day, "day", R.color.dayBackground),
    EVENING(R.drawable.ic_evening, "evening", R.color.eveningBackground),
    NIGHTLIGHT(R.drawable.ic_nightlight, "nightlight", R.color.nightlightBackground),
    DINNER(R.drawable.ic_dinner, "dinner", R.color.dinnerBackground),
    MOVIE(R.drawable.ic_movie, "movie", R.color.movieBackground),
    PARTY(R.drawable.ic_party, "party", R.color.partyBackground),
    NULL(R.drawable.scenes, "", R.color.sceneDefault);

    companion object {

        fun fromString(string: String): IconsScenes {
            for (i in values()) {
                if (i.stringValue == string)
                    return i
            }

            return NULL
        }
    }
}

enum class ColourScenes constructor(val resourceValue: Int, val stringValue: String) {
    DEFAULT(R.color.sceneDefault, "#00437B"),
    WHITE(R.color.sceneWhite, "#FFFFFF"),
    YELLOW(R.color.sceneYellow, "#FFD300"),
    ORANGE(R.color.sceneOrange, "#FF9C00"),
    RED(R.color.sceneRed, "#D0021B"),
    PINK(R.color.scenePink, "#D901CC"),
    PURPLE(R.color.scenePurple, "#8800FF"),
    BLUE(R.color.sceneBlue, "#0037FF"),
    CYAN(R.color.sceneCyan, "#00BFFF"),
    GREEN(R.color.sceneGreen, "#00DB25"),
    TURQUOISE(R.color.sceneTurquoise, "#50E3C2"),
    PALEBLUE(R.color.scenePaleBlue, "#B8D5E8");

    companion object {
        fun fromString(string: String): ColourScenes? {
            val s = string.replace("#", "")
            for (i in values()) {
                if (i.stringValue == s)
                    return i
            }

            return null
        }
    }
}