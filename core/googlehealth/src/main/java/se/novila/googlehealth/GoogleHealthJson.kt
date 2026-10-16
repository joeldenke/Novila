package se.novila.googlehealth

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinInstant

/**
 * The parts of the Google Health API's exercise data points that Novila uses
 * (`GET /v4/users/me/dataTypes/exercise/dataPoints`). Unknown fields are ignored, so the API can add
 * fields without breaking the app.
 */
@Serializable
internal data class DataPointsPage(
    val dataPoints: List<DataPoint> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
internal data class DataPoint(val exercise: ExerciseDto? = null)

@Serializable
internal data class ExerciseDto(
    val interval: IntervalDto? = null,
    val exerciseType: String? = null,
    /** Protobuf Duration in JSON, e.g. "1800s" or "1800.5s". */
    val activeDuration: String? = null,
)

@Serializable
internal data class IntervalDto(
    val startTime: String? = null,
    val endTime: String? = null,
)

/** Turns API responses into [Exercise]s. Pure Kotlin, so it is unit tested with sample responses. */
internal object GoogleHealthJson {
    private val json = Json { ignoreUnknownKeys = true }

    /** Parses one page. A malformed body is a [SerializationException], wrapped by the caller in a Result. */
    fun parsePage(body: String): DataPointsPage = json.decodeFromString(DataPointsPage.serializer(), body)

    /** Workouts in [page], with their start day in [zone]. Points without a start time are skipped. */
    fun exercises(page: DataPointsPage, zone: ZoneId): List<Exercise> = page.dataPoints.mapNotNull { point ->
        val exercise = point.exercise ?: return@mapNotNull null
        val start = exercise.interval?.startTime?.let(::instant) ?: return@mapNotNull null
        val end = exercise.interval?.endTime?.let(::instant)
        val duration = exercise.activeDuration?.let(::duration)
            ?: end?.let { it.toKotlinInstant() - start.toKotlinInstant() }
            ?: Duration.ZERO
        Exercise(
            type = exerciseType(exercise.exerciseType),
            date = start.atZone(zone).toLocalDate(),
            duration = duration.coerceAtLeast(Duration.ZERO),
        )
    }

    /**
     * Maps the API's exercise type names (e.g. "RUNNING", "WALKING") to Novila's types. Matches on parts
     * of the name, so variants such as "TREADMILL_RUNNING" or "WEIGHTLIFTING" land in the right place.
     */
    fun exerciseType(name: String?): ExerciseType {
        val type = name?.uppercase() ?: return ExerciseType.Other
        return when {
            "RUN" in type || "JOG" in type -> ExerciseType.Running
            "WALK" in type || "HIK" in type -> ExerciseType.Walking
            "BIK" in type || "CYCL" in type -> ExerciseType.Cycling
            "SWIM" in type -> ExerciseType.Swimming
            "STRENGTH" in type || "WEIGHT" in type || "CALISTHENICS" in type -> ExerciseType.Strength
            "YOGA" in type || "PILATES" in type || "STRETCH" in type -> ExerciseType.Mobility
            else -> ExerciseType.Other
        }
    }

    private fun instant(text: String): Instant? = try {
        Instant.parse(text)
    } catch (_: DateTimeException) {
        null
    }

    private fun duration(text: String): Duration? {
        val seconds = text.removeSuffix("s").toDoubleOrNull() ?: return null
        return (seconds * 1000).toLong().milliseconds
    }
}
