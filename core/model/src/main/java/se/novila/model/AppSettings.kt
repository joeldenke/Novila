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
    /**
     * The intro was left without finishing it ("set up later"), so the app opened without a name, goals or coach
     * answers. Cleared when the setup is finished. A user who finished the intro before this key existed never
     * has it set, so their setup stays complete.
     */
    val setupSkipped: Boolean = false,
) {
    /** The whole setup is done: the intro was finished, not skipped. */
    val setupComplete: Boolean get() = onboarded && !setupSkipped

    /** The app is open on the tabs, but the intro was skipped, so the setup is still worth finishing. */
    val setupIncomplete: Boolean get() = onboarded && setupSkipped
}

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
