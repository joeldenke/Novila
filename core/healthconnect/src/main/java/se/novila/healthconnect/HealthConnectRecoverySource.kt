package se.novila.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import se.novila.recovery.RecoverySource
import se.novila.recovery.RecoveryState
import se.novila.recovery.recoveryDays
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.reflect.KClass

/**
 * [RecoverySource] backed by Health Connect: the resting heart rate and heart rate variability a watch recorded.
 * Read-only and only while the app is open: nothing is written to Health Connect, saved by Novila or sent
 * anywhere. The app compares the latest reading with the user's own recent baseline, never with a fixed number.
 * Permissions are checked on every read; either of the two types is enough to read.
 */
class HealthConnectRecoverySource(private val context: Context) : RecoverySource {

    override suspend fun read(from: LocalDate): Result<RecoveryState> = readHealthConnect(
        context = context,
        unavailable = { RecoveryState.Unavailable },
        needsPermission = { RecoveryState.NeedsPermission() },
        connected = { client, granted ->
            if (granted.none { it in RECOVERY_PERMISSIONS }) {
                RecoveryState.NeedsPermission()
            } else {
                // associateBy keeps the last sample of a day, and Health Connect returns them oldest first.
                val resting = read(client, from, RestingHeartRateRecord::class)
                    .associateBy({ it.localDate() }, { it.beatsPerMinute.toInt() })
                val hrv = read(client, from, HeartRateVariabilityRmssdRecord::class)
                    .associateBy({ it.localDate() }, { it.heartRateVariabilityMillis.toLong() })
                RecoveryState.Connected(recoveryDays(resting, hrv).filter { it.date >= from })
            }
        },
    )

    /** Every [type] record from the day before [from], oldest first. */
    private suspend fun <R : Record> read(
        client: HealthConnectClient,
        from: LocalDate,
        type: KClass<R>,
    ): List<R> {
        val records = mutableListOf<R>()
        var pageToken: String? = null
        var pages = 0
        do {
            // From the evening before, so a reading just after midnight counts on the right day.
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.after(from.minusDays(1).atStartOfDay()),
                    pageToken = pageToken,
                ),
            )
            records += response.records
            pageToken = response.pageToken?.takeIf { it.isNotEmpty() }
        } while (pageToken != null && ++pages < MAX_PAGES)
        return records
    }

    private companion object {
        const val MAX_PAGES = 10
    }
}

/** The day the reading was taken where the user was, so it counts on their own calendar. */
private fun RestingHeartRateRecord.localDate(): LocalDate = day(time, zoneOffset)

private fun HeartRateVariabilityRmssdRecord.localDate(): LocalDate = day(time, zoneOffset)

private fun day(time: Instant, offset: ZoneOffset?): LocalDate =
    time.atZone(offset ?: ZoneId.systemDefault()).toLocalDate()
