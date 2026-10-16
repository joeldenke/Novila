package se.novila.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaLevelPicker
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.Insight
import se.novila.ui.common.hoursText
import se.novila.ui.common.stepsText
import kotlin.math.abs
import se.novila.ui.R as UiR

/** How the user feels today, on a calm five-step scale from bad to great. */
@Composable
fun MoodCard(mood: Int, onChange: (Int) -> Unit) {
    NovilaCard(title = stringResource(R.string.mood_title)) {
        NovilaLevelPicker(
            levels = Mood.entries.map { stringResource(it.label) },
            selected = mood,
            onSelect = onChange,
            hint = stringResource(R.string.mood_hint),
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

/** What the week's logs say, worked out on the phone. Explains how to get insights while there are none. */
@Composable
fun InsightsCard(insights: List<Insight>) {
    NovilaCard(title = stringResource(R.string.insights_title)) {
        if (insights.isEmpty()) {
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
