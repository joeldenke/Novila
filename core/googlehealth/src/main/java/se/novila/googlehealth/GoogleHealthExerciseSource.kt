package se.novila.googlehealth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseSource
import se.novila.exercise.ExerciseState
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.LocalDate
import java.time.ZoneId

/** OAuth scope for reading workouts. Restricted: Google has to verify the app before public release. */
const val GOOGLE_HEALTH_EXERCISE_SCOPE = "https://www.googleapis.com/auth/googlehealth.activity_and_fitness.readonly"

/** An OAuth access token with [GOOGLE_HEALTH_EXERCISE_SCOPE], or null when the user hasn't signed in. */
fun interface AccessTokenProvider {
    suspend fun accessToken(): String?
}

/** Sends one authorized GET and returns the body. Replaced by a fake in tests. */
fun interface GoogleHealthTransport {
    suspend fun get(url: String, accessToken: String): String
}

/** Thrown when the user has to sign in to Google (again) before workouts can be read. */
class GoogleHealthAuthException(message: String) : IOException(message)

/**
 * Workouts from the Google Health API (REST v4), read-only: nothing is written back.
 * Pages through `users/me/dataTypes/exercise/dataPoints`, filtered on the local start time.
 *
 * Not wired into the app (AppGraph) yet: that needs Google sign-in (an [AccessTokenProvider]), the INTERNET
 * permission and an updated privacy screen, which the owner approves separately.
 */
class GoogleHealthExerciseSource(
    private val tokens: AccessTokenProvider,
    private val transport: GoogleHealthTransport = HttpTransport,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ExerciseSource {
    override suspend fun read(from: LocalDate): Result<ExerciseState> = runCatching {
        val token = tokens.accessToken() ?: throw GoogleHealthAuthException("Not signed in to Google")
        val timeZone = zone()
        val exercises = mutableListOf<Exercise>()
        var pageToken: String? = null
        var pages = 0
        do {
            val page = GoogleHealthJson.parsePage(transport.get(url(from, pageToken), token))
            exercises += GoogleHealthJson.exercises(page, timeZone)
            pageToken = page.nextPageToken?.takeIf { it.isNotEmpty() }
        } while (pageToken != null && ++pages < MAX_PAGES)
        ExerciseState.Connected(exercises.filter { it.date >= from }.sortedBy { it.date })
    }.onFailure { if (it is CancellationException) throw it }

    companion object {
        const val BASE_URL = "https://health.googleapis.com/v4/users/me/dataTypes/exercise/dataPoints"
        private const val PAGE_SIZE = 100
        private const val MAX_PAGES = 10

        /** The list URL: workouts that started on [from] or later in local (civil) time. */
        fun url(from: LocalDate, pageToken: String? = null): String {
            val filter = "exercise.interval.civil_start_time >= \"${from}T00:00:00\""
            val query = buildList {
                add("filter=${encode(filter)}")
                add("pageSize=$PAGE_SIZE")
                pageToken?.let { add("pageToken=${encode(it)}") }
            }
            return "$BASE_URL?${query.joinToString("&")}"
        }

        private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    }
}

/** Plain HttpURLConnection: no extra HTTP library for a few GET requests. */
private object HttpTransport : GoogleHealthTransport {
    private const val TIMEOUT_MS = 15_000

    override suspend fun get(url: String, accessToken: String): String = withContext(Dispatchers.IO) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_UNAUTHORIZED -> throw GoogleHealthAuthException("Access token rejected")
                else -> throw IOException("Google Health API answered $code")
            }
        } finally {
            connection.disconnect()
        }
    }
}
