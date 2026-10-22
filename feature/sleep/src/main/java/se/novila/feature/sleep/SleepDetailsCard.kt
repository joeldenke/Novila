package se.novila.feature.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaLevelPicker
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.sleep.R
import se.novila.model.DayEntry
import se.novila.model.SleepFactor

/**
 * How the night felt (1–5) and what may have affected it. Both are optional; together with the day before they are
 * what [se.novila.model.analyzeSleep] learns from.
 */
@Composable
fun SleepDetailsCard(day: DayEntry, actions: SleepActions) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(title = stringResource(R.string.sleep_quality_title)) {
        NovilaLevelPicker(
            levels = QualityLevels.map { stringResource(it) },
            selected = day.sleepQuality,
            onSelect = actions.onQuality,
            hint = stringResource(R.string.sleep_quality_hint),
            role = sleep,
        )
        NovilaText(
            stringResource(R.string.sleep_factors_title),
            style = NovilaTheme.typography.labelLarge,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
        ) {
            SleepFactor.entries.forEach { factor ->
                val on = factor in day.sleepFactors
                NovilaChoiceChip(
                    label = stringResource(factor.label),
                    selected = on,
                    onClick = { actions.onFactor(factor, !on) },
                    role = sleep,
                )
            }
        }
    }
}

/** Sleep quality 1–5, from poor to rested. */
internal val QualityLevels = listOf(
    R.string.sleep_quality_1,
    R.string.sleep_quality_2,
    R.string.sleep_quality_3,
    R.string.sleep_quality_4,
    R.string.sleep_quality_5,
)

private val SleepFactor.label: Int
    get() = when (this) {
        SleepFactor.LateCaffeine -> R.string.sleep_factor_late_caffeine
        SleepFactor.Screens -> R.string.sleep_factor_screens
        SleepFactor.Alcohol -> R.string.sleep_factor_alcohol
        SleepFactor.Stress -> R.string.sleep_factor_stress
        SleepFactor.LateMeal -> R.string.sleep_factor_late_meal
        SleepFactor.LateExercise -> R.string.sleep_factor_late_exercise
        SleepFactor.Period -> R.string.sleep_factor_period
    }
