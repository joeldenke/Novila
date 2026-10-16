package se.novila.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.novila.coach.AssistantAvailability
import se.novila.coach.CoachLanguageModel
import se.novila.coach.CoachPreferences
import se.novila.coach.CoachPrompt
import se.novila.coach.CoachPromptInput
import se.novila.data.CoachStore
import java.time.LocalDate

/**
 * The coach screen: the answers, the plan they give and Gemini Nano's comment on it. Scoped to the coach entry, so
 * answers tried out before the coach is set up are forgotten when the user leaves it.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class CoachViewModel(
    training: TrainingRepository,
    private val coachStore: CoachStore,
    /** Gemini Nano on the device. The coach's plan never depends on it; it only explains the plan in its own words. */
    private val languageModel: CoachLanguageModel,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** Answers the user is trying out before the coach is set up. Not saved until they create the program. */
    private val draft = MutableStateFlow<CoachPreferences?>(null)

    val state: StateFlow<TrainingUiState> = combine(date, draft, training.changes) { date, draft, _ ->
        training.read(date, draft)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, training.read(date.value))

    private val _assistant = MutableStateFlow(AssistantUiState())
    val assistant: StateFlow<AssistantUiState> = _assistant.asStateFlow()

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }

    /** Asks the system whether Gemini Nano can run here. Called when the coach is opened, never in the background. */
    fun checkAssistant() {
        viewModelScope.launch {
            val availability = languageModel.availability()
            _assistant.update { it.copy(availability = availability) }
        }
    }

    /** The user asked for Gemini Nano: AICore downloads it and keeps it up to date. */
    fun downloadAssistant() {
        _assistant.update { it.copy(availability = AssistantAvailability.Downloading, failed = false) }
        viewModelScope.launch {
            val done = languageModel.download()
            val availability = if (done) AssistantAvailability.Available else languageModel.availability()
            _assistant.update { it.copy(availability = availability, failed = !done) }
        }
    }

    /** Lets Gemini Nano explain the coach's current plan, from [input] in the user's language. */
    fun writeAssistantMessage(input: CoachPromptInput) {
        val coach = state.value.coach
        _assistant.update { it.copy(writing = true, message = null, failed = false) }
        viewModelScope.launch {
            val answer = languageModel.write(CoachPrompt.build(coach.plan, coach.preferences, input))
                .map(CoachPrompt::clean)
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            // A message for a plan that changed meanwhile would explain the wrong week.
            val current = state.value.coach.plan == coach.plan
            _assistant.update { it.copy(writing = false, message = answer.takeIf { current }, failed = answer == null) }
        }
    }

    /** A changed answer. Once the coach is set up it is saved and changes the week's program right away. */
    fun change(preferences: CoachPreferences) {
        if (state.value.coach.active) coachStore.save(preferences) else draft.value = preferences
        forgetAssistantMessage()
    }

    /** Saves the answers, which makes the coach's plan the week's program. */
    fun createProgram() {
        coachStore.save(state.value.coach.preferences)
    }

    /** A new suggestion from the same answers, used right away. */
    fun nextSuggestion() {
        coachStore.save(state.value.coach.preferences.nextVariation())
        forgetAssistantMessage()
    }

    /** Back to the standard program generated from recent workouts. */
    fun reset() {
        coachStore.clear()
        draft.value = null
        forgetAssistantMessage()
    }

    /** The plan changed, so the message no longer fits it. */
    private fun forgetAssistantMessage() {
        _assistant.update { it.copy(message = null, failed = false) }
    }
}
