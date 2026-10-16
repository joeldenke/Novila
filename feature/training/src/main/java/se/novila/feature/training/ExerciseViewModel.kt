package se.novila.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.novila.exercise.PlannedExercise
import java.time.LocalDate

/** One exercise from the library: how this week's program doses it, if it is in the program. */
@AssistedInject
class ExerciseViewModel(
    @Assisted private val exerciseId: String,
    private val training: TrainingRepository,
) : ViewModel() {
    /** The exercise as planned this week, or null when the program does not have it. */
    val planned: StateFlow<PlannedExercise?> = training.changes.map { find() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, find())

    private fun find(): PlannedExercise? = training.read(LocalDate.now()).program.sessions
        .flatMap { it.exercises }
        .firstOrNull { it.exerciseId == exerciseId }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(@Assisted exerciseId: String): ExerciseViewModel
    }
}
