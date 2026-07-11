package com.avtar.cabbilling.data.model

/**
 * The set of visual themes the user can pick in Settings.
 *
 * [AUTO] follows the device's system-wide dark/light preference; every other
 * value forces a specific palette regardless of the system setting.
 */
enum class AppTheme(val id: String, val label: String) {
    AUTO("AUTO", "Auto (System)"),
    CLASSIC_DARK("CLASSIC_DARK", "Classic Dark"),
    CLEAN_LIGHT("CLEAN_LIGHT", "Clean Light"),
    VIBRANT_GOLD("VIBRANT_GOLD", "Vibrant Gold"),
    CLASSIC_BLUE("CLASSIC_BLUE", "Classic Blue");

    companion object {
        fun fromId(id: String?): AppTheme = entries.firstOrNull { it.id == id } ?: AUTO
    }
}
