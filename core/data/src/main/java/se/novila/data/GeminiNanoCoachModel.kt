package se.novila.data

import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import se.novila.coach.AssistantAvailability
import se.novila.coach.CoachLanguageModel
import kotlin.coroutines.cancellation.CancellationException

/**
 * Gemini Nano through ML Kit's Prompt API. The model runs in Android's AICore on supported phones, so the
 * prompt (the week's plan) never leaves the device. AICore downloads and updates the model itself; Novila
 * only asks it to, when the user taps the button. Bound as the app's [CoachLanguageModel].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class GeminiNanoCoachModel : CoachLanguageModel {
    private val model by lazy { Generation.getClient() }

    override suspend fun availability(): AssistantAvailability = catching(AssistantAvailability.Unavailable) {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> AssistantAvailability.Available
            FeatureStatus.DOWNLOADABLE -> AssistantAvailability.Downloadable
            FeatureStatus.DOWNLOADING -> AssistantAvailability.Downloading
            else -> AssistantAvailability.Unavailable
        }
    }

    override suspend fun download(): Boolean = catching(false) {
        model.download()
            .first { it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed }
            .let { it is DownloadStatus.DownloadCompleted }
    }

    override suspend fun write(prompt: String): Result<String> = try {
        val response = model.generateContent(
            generateContentRequest(TextPart(prompt)) {
                temperature = TEMPERATURE
                topK = TOP_K
                maxOutputTokens = MAX_OUTPUT_TOKENS
            },
        )
        Result.success(response.candidates.first().text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Logged so "the AI comment never works" is diagnosable; the screen only shows that it failed.
        Log.w(TAG, "Gemini Nano failed to explain the plan", e)
        Result.failure(e)
    }

    /** Runs [block], turning any failure but cancellation into [fallback], logged so the model's state is visible. */
    private inline fun <T> catching(fallback: T, block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Gemini Nano unavailable", e)
        fallback
    }

    private companion object {
        const val TAG = "GeminiNano"
        const val TEMPERATURE = 0.3f
        const val TOP_K = 16
        const val MAX_OUTPUT_TOKENS = 256
    }
}
