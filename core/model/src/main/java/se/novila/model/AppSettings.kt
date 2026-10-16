package se.novila.model

/**
 * How the app looks, chosen under Settings, and whether the intro is done. Saved by `SettingsStore` in `:core:data`.
 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    /** Wallpaper colors (Material You, Android 12+) instead of the Skog & sol palette. */
    val dynamicColor: Boolean = false,
    /** The intro at first start is done or skipped, so the app opens on Today. */
    val onboarded: Boolean = false,
)

/** Light, dark, or whatever the phone uses. */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    /** Whether the app is dark when the phone is dark when [systemDark]. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        /** The saved name, or [System] for a missing or unknown value, so an old or broken file never crashes. */
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: System
    }
}
