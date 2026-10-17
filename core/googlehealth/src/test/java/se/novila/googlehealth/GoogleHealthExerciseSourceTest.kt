package se.novila.googlehealth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseState
import se.novila.exercise.ExerciseType
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class GoogleHealthExerciseSourceTest {
    private val stockholm = ZoneId.of("Europe/Stockholm")
    private val from = LocalDate.of(2026, 9, 14)

    /** Shaped like the API's list response; the unknown fields must be ignored. */
    private val firstPage = """
        {
          "dataPoints": [
            {
              "name": "users/123/dataTypes/exercise/dataPoints/a",
              "dataSource": { "recordingMethod": "ACTIVELY_RECORDED", "platform": "FITBIT" },
              "exercise": {
                "interval": {
                  "startTime": "2026-10-13T22:30:00Z",
                  "startUtcOffset": "7200s",
                  "endTime": "2026-10-13T23:10:00Z",
                  "endUtcOffset": "7200s"
                },
                "exerciseType": "RUNNING",
                "displayName": "Kvällslöpning",
                "activeDuration": "1800s",
                "metricsSummary": { "caloriesKcal": 320 }
              }
            },
            {
              "exercise": {
                "interval": { "startTime": "2026-10-14T06:00:00Z", "endTime": "2026-10-14T06:45:00Z" },
                "exerciseType": "STRENGTH_TRAINING"
              }
            },
            { "exercise": { "exerciseType": "YOGA" } }
          ],
          "nextPageToken": "page 2"
        }
    """.trimIndent()

    private val secondPage = """
        { "dataPoints": [ { "exercise": {
            "interval": { "startTime": "2026-10-15T16:00:00Z", "endTime": "2026-10-15T16:20:00Z" },
            "exerciseType": "WALKING", "activeDuration": "1200.5s"
        } } ] }
    """.trimIndent()

    private class FakeTransport(private val pages: Map<String?, String>) : GoogleHealthTransport {
        val urls = mutableListOf<String>()
        override suspend fun get(url: String, accessToken: String): String {
            assertEquals("token", accessToken)
            urls += url
            val pageToken = url.substringAfter("pageToken=", "").takeIf { it.isNotEmpty() }
            return pages.getValue(pageToken?.let { URLDecoder.decode(it, "UTF-8") })
        }
    }

    @Test fun `reads all pages into workouts on their local day`() = runBlocking {
        val transport = FakeTransport(mapOf(null to firstPage, "page 2" to secondPage))
        val source = GoogleHealthExerciseSource({ "token" }, transport, { stockholm })
        val exercises = (source.read(from).getOrThrow() as ExerciseState.Connected).exercises
        assertEquals(
            listOf(
                // 22:30 UTC is past midnight in Stockholm, so the run counts on the 14th.
                Exercise(ExerciseType.Running, LocalDate.of(2026, 10, 14), 30.minutes),
                // Without an active duration the interval is used.
                Exercise(ExerciseType.Strength, LocalDate.of(2026, 10, 14), 45.minutes),
                Exercise(ExerciseType.Walking, LocalDate.of(2026, 10, 15), 1_200_500.milliseconds),
            ),
            exercises,
        )
        assertEquals(2, transport.urls.size)
    }

    @Test fun `filters on the local start day`() {
        val url = URLDecoder.decode(GoogleHealthExerciseSource.url(from), "UTF-8")
        assertTrue(url, url.startsWith(GoogleHealthExerciseSource.BASE_URL))
        assertTrue(url, "filter=exercise.interval.civil_start_time >= \"2026-09-14T00:00:00\"" in url)
    }

    @Test fun `without sign-in nothing is requested`() = runBlocking {
        val transport = FakeTransport(emptyMap())
        val result = GoogleHealthExerciseSource({ null }, transport).read(from)
        assertTrue(result.exceptionOrNull() is GoogleHealthAuthException)
        assertTrue(transport.urls.isEmpty())
    }

    @Test fun `a malformed response is a failure, not a crash`() = runBlocking {
        val result = GoogleHealthExerciseSource({ "token" }, FakeTransport(mapOf(null to "<html>"))).read(from)
        assertTrue(result.isFailure)
    }

    @Test fun `exercise type names map to Novila's types`() {
        mapOf(
            "RUNNING" to ExerciseType.Running,
            "TREADMILL_RUNNING" to ExerciseType.Running,
            "WALKING" to ExerciseType.Walking,
            "HIKING" to ExerciseType.Walking,
            "BIKING" to ExerciseType.Cycling,
            "SWIMMING_POOL" to ExerciseType.Swimming,
            "WEIGHTLIFTING" to ExerciseType.Strength,
            "PILATES" to ExerciseType.Mobility,
            "TENNIS" to ExerciseType.Other,
            null to ExerciseType.Other,
        ).forEach { (name, type) -> assertEquals(name, type, GoogleHealthJson.exerciseType(name)) }
    }
}
