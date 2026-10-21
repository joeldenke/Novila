package se.novila.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.Insight
import se.novila.model.SleepAnalysis
import se.novila.ui.common.hoursText
import se.novila.ui.common.sleepChangeText
import se.novila.ui.common.sleepLinkText
import se.novila.ui.common.stepsText
import kotlin.math.abs
import se.novila.ui.R as UiR

/**
 * What the logs say, worked out on the phone: this week's change in [sleep] and what came with it, or else the
 * strongest link between the user's days and nights, then the week's [insights]. Explains how to get insights while
 * there are none. With [onOpenAll], it ends with the way to all discoveries.
 */
@Composable
fun InsightsCard(insights: List<Insight>, sleep: SleepAnalysis, onOpenAll: (() -> Unit)? = null) {
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
        if (onOpenAll != null) {
            NovilaTextButton(text = stringResource(R.string.discoveries_open), onClick = onOpenAll)
        }
    }
}

@Composable
private fun InsightRow(insight: Insight) {
    val pillars = NovilaTheme.pillars
    val sleepIcon = painterResource(UiR.drawable.ic_sleep)
    val nutritionIcon = painterResource(UiR.drawable.ic_nutrition)
    when (insight) {
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
