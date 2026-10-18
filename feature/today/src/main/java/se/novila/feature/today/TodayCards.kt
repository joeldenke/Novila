package se.novila.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaLevelPicker
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextField
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.Insight
import se.novila.model.MAX_DIARY_LENGTH
import se.novila.model.SleepAnalysis
import se.novila.ui.common.hoursText
import se.novila.ui.common.sleepChangeText
import se.novila.ui.common.sleepLinkText
import se.novila.ui.common.stepsText
import kotlin.math.abs
import se.novila.ui.R as UiR

/**
 * How the user feels today: mood, energy and stress, each on a calm five-step scale, and a line in the diary.
 * All four are optional; [diary] is kept to one short line of at most [MAX_DIARY_LENGTH] characters.
 */
@Composable
fun MoodCard(
    mood: Int,
    energy: Int,
    stress: Int,
    diary: String,
    onMood: (Int) -> Unit,
    onEnergy: (Int) -> Unit,
    onStress: (Int) -> Unit,
    onDiary: (String) -> Unit,
) {
    NovilaCard(title = stringResource(R.string.mood_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l)) {
            LevelRow(
                label = stringResource(R.string.mood_label),
                levels = Mood.entries.map { stringResource(it.label) },
                selected = mood,
                onSelect = onMood,
                hint = stringResource(R.string.mood_hint),
            )
            LevelRow(
                label = stringResource(R.string.energy_label),
                levels = EnergyLevels.map { stringResource(it) },
                selected = energy,
                onSelect = onEnergy,
                hint = stringResource(R.string.energy_hint),
            )
            LevelRow(
                label = stringResource(R.string.stress_label),
                levels = StressLevels.map { stringResource(it) },
                selected = stress,
                onSelect = onStress,
                hint = stringResource(R.string.stress_hint),
            )
            Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs)) {
                NovilaText(
                    stringResource(R.string.diary_label),
                    style = NovilaTheme.typography.labelLarge,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
                NovilaTextField(
                    value = diary,
                    onValueChange = { onDiary(it.take(MAX_DIARY_LENGTH)) },
                    hint = stringResource(R.string.diary_hint),
                )
            }
        }
    }
}

/** A labelled five-step scale: the label, then the picker in the mood card's pine color. */
@Composable
private fun LevelRow(label: String, levels: List<String>, selected: Int, onSelect: (Int) -> Unit, hint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs)) {
        NovilaText(label, style = NovilaTheme.typography.labelLarge, color = NovilaTheme.colors.onSurfaceVariant)
        NovilaLevelPicker(
            levels = levels,
            selected = selected,
            onSelect = onSelect,
            hint = hint,
            role = NovilaTheme.pillars.training,
        )
    }
}

/** Mood levels 1–5, in order from bad to great. */
private enum class Mood(val label: Int) {
    Bad(R.string.mood_1),
    NotGreat(R.string.mood_2),
    Okay(R.string.mood_3),
    Good(R.string.mood_4),
    Great(R.string.mood_5),
}

/** Energy levels 1–5, in order from exhausted to full of energy. */
private val EnergyLevels = listOf(
    R.string.energy_1,
    R.string.energy_2,
    R.string.energy_3,
    R.string.energy_4,
    R.string.energy_5,
)

/** Stress levels 1–5, in order from calm to very stressed. */
private val StressLevels = listOf(
    R.string.stress_1,
    R.string.stress_2,
    R.string.stress_3,
    R.string.stress_4,
    R.string.stress_5,
)

/**
 * What the logs say, worked out on the phone: this week's change in [sleep] and what came with it, or else the
 * strongest link between the user's days and nights, then the week's [insights]. Explains how to get insights while
 * there are none.
 */
@Composable
fun InsightsCard(insights: List<Insight>, sleep: SleepAnalysis) {
    NovilaCard(title = stringResource(R.string.insights_title)) {
        val change = sleep.change
        val link = sleep.links.firstOrNull()
        val sleepText = when {
            change != null -> sleepChangeText(change)
            link != null -> sleepLinkText(link)
            else -> null
        }
        sleepText?.let { NovilaInsight(painterResource(UiR.drawable.ic_sleep), it, NovilaTheme.pillars.sleep) }
        if (insights.isEmpty() && sleepText == null) {
            NovilaText(
                stringResource(R.string.insights_empty),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        insights.forEach { InsightRow(it) }
    }
}

@Composable
private fun InsightRow(insight: Insight) {
    val pillars = NovilaTheme.pillars
    val sleepIcon = painterResource(UiR.drawable.ic_sleep)
    val nutritionIcon = painterResource(UiR.drawable.ic_nutrition)
    when (insight) {
        is Insight.MoodFollowsSleep -> NovilaInsight(
            sleepIcon,
            stringResource(R.string.insight_mood_sleep, hoursText(insight.hours)),
            pillars.sleep,
        )

        is Insight.SleepVsAverage -> NovilaInsight(
            sleepIcon,
            stringResource(
                if (insight.hours > 0f) R.string.insight_sleep_more else R.string.insight_sleep_less,
                hoursText(abs(insight.hours)),
            ),
            pillars.sleep,
        )

        Insight.SteadySleep -> NovilaInsight(sleepIcon, stringResource(R.string.insight_steady_sleep), pillars.sleep)

        is Insight.NutritionStreak -> NovilaInsight(
            nutritionIcon,
            stringResource(R.string.insight_nutrition_streak, insight.days),
            pillars.nutrition,
        )

        is Insight.NutritionTrend -> NovilaInsight(
            nutritionIcon,
            stringResource(
                if (insight.points > 0) R.string.insight_nutrition_up else R.string.insight_nutrition_down,
                abs(insight.points),
            ),
            pillars.nutrition,
        )

        is Insight.StepsGoalReached -> NovilaInsight(
            painterResource(UiR.drawable.ic_training),
            stringResource(R.string.insight_steps_goal, stepsText(insight.steps.toLong())),
            pillars.training,
        )
    }
}
