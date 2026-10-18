package se.novila.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseSource
import se.novila.exercise.ExerciseState
import se.novila.exercise.ExerciseType
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.toKotlinDuration

/**
 * [ExerciseSource] backed by Health Connect: workouts that a watch or another app (Google Fit, Fitbit, Samsung
 * Health, Strava …) recorded. Read-only and only while the app is open: nothing is written to Health Connect,
 * saved by Novila or sent anywhere. Health Connect itself only gives data from 30 days before access was first
 * granted, which is fine: the program looks at the last four weeks. Permissions are checked on every read.
 */
class HealthConnectExerciseSource(private val context: Context) : ExerciseSource {

    override suspend fun read(from: LocalDate): Result<ExerciseState> {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        return healthConnectRead(
            context = context,
            sdkStatus = sdkStatus,
            unavailable = ExerciseState.Unavailable,
            needsPermission = ExerciseState.NeedsPermission(),
            required = { EXERCISE_PERMISSION in it },
        ) { client, _ ->
            ExerciseState.Connected(readSessions(client, from))
        }
    }

    private suspend fun readSessions(client: HealthConnectClient, from: LocalDate): List<Exercise> {
        val exercises = mutableListOf<Exercise>()
        var pageToken: String? = null
        var pages = 0
        do {
            // Local time, so a workout counts on the day it started for the user.
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(from.atStartOfDay()),
                    pageToken = pageToken,
                ),
            )
            exercises += response.records.map { record ->
                val zone = record.startZoneOffset ?: ZoneId.systemDefault()
                Exercise(
                    type = exerciseType(record.exerciseType),
                    date = record.startTime.atZone(zone).toLocalDate(),
                    duration = Duration.between(record.startTime, record.endTime).toKotlinDuration(),
                )
            }
            pageToken = response.pageToken?.takeIf { it.isNotEmpty() }
        } while (pageToken != null && ++pages < MAX_PAGES)
        return exercises.filter { it.date >= from }.sortedBy { it.date }
    }

    private companion object {
        const val MAX_PAGES = 10
    }
}

/** Health Connect's exercise types as Novila's. Anything that isn't strength or mobility counts as cardio. */
fun exerciseType(healthConnectType: Int): ExerciseType = when (healthConnectType) {
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    -> ExerciseType.Walking

    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    -> ExerciseType.Running

    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    -> ExerciseType.Cycling

    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    -> ExerciseType.Swimming

    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    -> ExerciseType.Strength

    ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
    ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
    -> ExerciseType.Mobility

    else -> ExerciseType.Other
}
