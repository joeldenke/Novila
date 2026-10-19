package se.novila.model

import java.time.LocalDate

/**
 * What the body measures on one day: weight plus the three circumferences, every value optional and in whole
 * kilos and centimetres, the same precision as [Profile]'s weight. Logged over time under Settings >
 * Measurements, so the trend is visible instead of only the newest number in the profile.
 */
data class BodyMeasurements(
    val weightKg: Int? = null,
    val waistCm: Int? = null,
    val neckCm: Int? = null,
    val hipsCm: Int? = null,
) {
    /** Nothing is logged for the day, which the store then forgets. */
    val isEmpty: Boolean get() = this == BodyMeasurements()

    companion object {
        /** Waist, neck and hips a person can have; anything else is a typo. Weight follows [Profile.WEIGHT_KG]. */
        val WAIST_CM = 40..250
        val NECK_CM = 20..80
        val HIPS_CM = 40..200
    }
}

/** The measurements of one day, the unit the measurements history is kept in. */
data class MeasurementEntry(val date: LocalDate, val measurements: BodyMeasurements)

/** What is logged for [date], or nothing measured when the day is not in the history. */
fun Iterable<MeasurementEntry>.on(date: LocalDate): BodyMeasurements =
    firstOrNull { it.date == date }?.measurements ?: BodyMeasurements()

/**
 * The newest weight among the logged days, or null when no day has one: the profile's current weight follows this
 * whenever measurements are saved or forgotten, so goal suggestions and everything else that needs a weight use the
 * latest number the user stepped on the scale with. The newest day wins, not the newest log, so correcting an older
 * day leaves the profile where the newest scale reading put it.
 */
fun Iterable<MeasurementEntry>.latestWeightKg(): Int? = asSequence()
    .filter { it.measurements.weightKg != null }
    .maxByOrNull { it.date }
    ?.measurements
    ?.weightKg

/**
 * Simple versioned text format without Android dependencies, so it can be unit tested and migrated.
 * v1: "v1;weight;waist;neck;hips", each number empty when it is not logged. An unknown version or a line with the
 * wrong shape reads as nothing logged; a number that is unreadable or out of range is dropped on its own, so a
 * damaged line never shows a wrong measurement but still keeps the parts that are readable.
 */
object MeasurementsCodec {
    private const val VERSION = "v1"
    private const val FIELDS = 5

    fun encode(measurements: BodyMeasurements): String = listOf(
        VERSION,
        measurements.weightKg?.toString().orEmpty(),
        measurements.waistCm?.toString().orEmpty(),
        measurements.neckCm?.toString().orEmpty(),
        measurements.hipsCm?.toString().orEmpty(),
    ).joinToString(";")

    fun decode(raw: String?): BodyMeasurements {
        val parts = raw?.split(';') ?: return BodyMeasurements()
        if (parts[0] != VERSION || parts.size != FIELDS) return BodyMeasurements()
        return BodyMeasurements(
            weightKg = parts[1].number(Profile.WEIGHT_KG),
            waistCm = parts[2].number(BodyMeasurements.WAIST_CM),
            neckCm = parts[3].number(BodyMeasurements.NECK_CM),
            hipsCm = parts[4].number(BodyMeasurements.HIPS_CM),
        )
    }

    private fun String.number(range: IntRange): Int? = toIntOrNull()?.takeIf { it in range }
}

/**
 * The measurements as the user is typing them: text, so a half-typed or wrong number stays on screen until it is
 * fixed. Each field is checked against [BodyMeasurements]'s ranges and [Profile]'s weight.
 */
data class MeasurementForm(
    val weight: String = "",
    val waist: String = "",
    val neck: String = "",
    val hips: String = "",
) {
    val weightInput: NumberInput get() = NumberInput.parse(weight, Profile.WEIGHT_KG)

    val waistInput: NumberInput get() = NumberInput.parse(waist, BodyMeasurements.WAIST_CM)

    val neckInput: NumberInput get() = NumberInput.parse(neck, BodyMeasurements.NECK_CM)

    val hipsInput: NumberInput get() = NumberInput.parse(hips, BodyMeasurements.HIPS_CM)

    /**
     * What to save: valid fields as typed and empty fields cleared. A wrong number keeps what [saved] had, so a
     * typo on the way to the right value never erases a measurement.
     */
    fun toMeasurements(saved: BodyMeasurements) = BodyMeasurements(
        weightKg = weightInput.orKeep(saved.weightKg),
        waistCm = waistInput.orKeep(saved.waistCm),
        neckCm = neckInput.orKeep(saved.neckCm),
        hipsCm = hipsInput.orKeep(saved.hipsCm),
    )

    private fun NumberInput.orKeep(saved: Int?): Int? = if (this == NumberInput.Invalid) saved else valueOrNull

    companion object {
        /** The form filled in with what is saved. */
        fun of(measurements: BodyMeasurements) = MeasurementForm(
            weight = measurements.weightKg?.toString().orEmpty(),
            waist = measurements.waistCm?.toString().orEmpty(),
            neck = measurements.neckCm?.toString().orEmpty(),
            hips = measurements.hipsCm?.toString().orEmpty(),
        )
    }
}
