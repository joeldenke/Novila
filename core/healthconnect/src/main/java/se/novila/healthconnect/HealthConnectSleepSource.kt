package se.novila.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import se.novila.sleep.SleepSession
import se.novila.sleep.SleepSource
import se.novila.sleep.SleepState
import se.novila.sleep.nightsOf
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * [SleepSource] backed by Health Connect: nights that a watch or another app (Fitbit, Samsung Health, Sleep as
 * Android …) recorded. Only when and how long the user slept: read-only and only while the app is open, never
 * written to Health Connect, saved by Novila or sent anywhere. Permissions are checked on every read.
 */
class HealthConnectSleepSource(private val context: Context) : SleepSource {

    override suspend fun read(from: LocalDate): Result<SleepState> {
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        return healthConnectRead(
            context = context,
            sdkStatus = sdkStatus,
            unavailable = SleepState.Unavailable,
            needsPermission = SleepState.NeedsPermission(),
            required = { SLEEP_PERMISSION in it },
        ) { client, _ ->
            SleepState.Connected(nightsOf(readSessions(client, from)).filter { it.date >= from })
        }
    }

    private suspend fun readSessions(client: HealthConnectClient, from: LocalDate): List<SleepSession> {
        val sessions = mutableListOf<SleepSession>()
        var pageToken: String? = null
        var pages = 0
        do {
            // From the evening before, so the first morning's night is whole.
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(from.minusDays(1).atStartOfDay()),
                    pageToken = pageToken,
                ),
            )
            sessions += response.records.map { record ->
                SleepSession(
                    start = local(record.startTime, record.startZoneOffset),
                    end = local(record.endTime, record.endZoneOffset),
                    awakeMinutes = record.stages
                        .filter { it.stage in AWAKE_STAGES }
                        .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                        .toInt(),
                )
            }
            pageToken = response.pageToken?.takeIf { it.isNotEmpty() }
        } while (pageToken != null && ++pages < MAX_PAGES)
        return sessions
    }

    /** Local time where the user was, so the night counts on the morning they woke up. */
    private fun local(time: Instant, offset: ZoneOffset?): LocalDateTime =
        time.atZone(offset ?: ZoneId.systemDefault()).toLocalDateTime()

    private companion object {
        const val MAX_PAGES = 10
        val AWAKE_STAGES = setOf(
            SleepSessionRecord.STAGE_TYPE_AWAKE,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
        )
    }
}
