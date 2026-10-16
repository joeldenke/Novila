package se.novila.model

/**
 * Who uses the app, entered under Settings > Profile. Every field is optional, and it is saved by `ProfileStore` in
 * `:core:data` on the phone only: there is no account, so the profile needs no sign-in and never leaves the device.
 */
data class Profile(
    /** What the greeting on Today calls the user. Empty when not given. */
    val name: String = "",
    val birthYear: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
) {
    /** Nothing is filled in. */
    val isEmpty: Boolean get() = this == Profile()

    /** The age the user turns in [year], or null without a birth year. */
    fun ageIn(year: Int): Int? = birthYear?.let { year - it }

    companion object {
        const val MAX_NAME_LENGTH = 40

        /** Heights and weights a person can have; anything else is a typo. */
        val HEIGHT_CM = 100..250
        val WEIGHT_KG = 30..300

        /** The birth years that make sense in [currentYear]: nobody older than 120 or born in the future. */
        fun birthYears(currentYear: Int): IntRange = (currentYear - 120)..currentYear

        /** [text] as a name: trimmed, and cut at [MAX_NAME_LENGTH] characters. */
        fun cleanName(text: String): String = text.trim().take(MAX_NAME_LENGTH)
    }
}

/** A number the user typed: nothing, a value in range, or something that is not one. */
sealed interface NumberInput {
    data object Empty : NumberInput

    data class Valid(val value: Int) : NumberInput

    data object Invalid : NumberInput

    /** The value to save: null when empty, and also when invalid, which [Profile] then keeps as it was. */
    val valueOrNull: Int? get() = (this as? Valid)?.value

    companion object {
        /** Reads [text] as a whole number in [range]. Surrounding spaces are fine, anything else is [Invalid]. */
        fun parse(text: String, range: IntRange): NumberInput {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return Empty
            val value = trimmed.toIntOrNull() ?: return Invalid
            return if (value in range) Valid(value) else Invalid
        }
    }
}

/**
 * The profile as the user is typing it: text, so a half-typed or wrong number stays on screen until it is fixed.
 * Each field is checked against [Profile]'s ranges.
 */
data class ProfileForm(
    val name: String = "",
    val birthYear: String = "",
    val height: String = "",
    val weight: String = "",
) {
    fun birthYearInput(currentYear: Int) = NumberInput.parse(birthYear, Profile.birthYears(currentYear))

    val heightInput: NumberInput get() = NumberInput.parse(height, Profile.HEIGHT_CM)

    val weightInput: NumberInput get() = NumberInput.parse(weight, Profile.WEIGHT_KG)

    /**
     * What to save: valid fields as typed and empty fields cleared. A wrong number keeps what [saved] had, so a typo
     * on the way to the right value never erases it.
     */
    fun toProfile(saved: Profile, currentYear: Int) = Profile(
        name = Profile.cleanName(name),
        birthYear = birthYearInput(currentYear).orKeep(saved.birthYear),
        heightCm = heightInput.orKeep(saved.heightCm),
        weightKg = weightInput.orKeep(saved.weightKg),
    )

    private fun NumberInput.orKeep(saved: Int?): Int? = if (this == NumberInput.Invalid) saved else valueOrNull

    companion object {
        /** The form filled in with what is saved. */
        fun of(profile: Profile) = ProfileForm(
            name = profile.name,
            birthYear = profile.birthYear?.toString().orEmpty(),
            height = profile.heightCm?.toString().orEmpty(),
            weight = profile.weightKg?.toString().orEmpty(),
        )
    }
}
