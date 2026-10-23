package se.novila.feature.today

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaMeter
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.Discovery
import se.novila.model.DiscoveryProgress
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
 * there are none, and shows what the logs will unlock ([upcoming]) as bars that fill as the user logs. With
 * [onOpenTrends] and [onOpenAll], it ends with the ways to the trends of all parts and to all discoveries.
 */
@Composable
fun InsightsCard(
    insights: List<Insight>,
    sleep: SleepAnalysis,
    upcoming: List<DiscoveryProgress> = emptyList(),
    onOpenTrends: (() -> Unit)? = null,
    onOpenAll: (() -> Unit)? = null,
    onOpenAchievements: (() -> Unit)? = null,
) {
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
        val locked = upcoming.filterNot { it.unlocked }
        if (locked.isNotEmpty()) {
            NovilaText(
                stringResource(R.string.insights_upcoming),
                style = NovilaTheme.typography.labelLarge,
                color = NovilaTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = NovilaTheme.spacing.s),
            )
            locked.forEach { UpcomingRow(it) }
        }
        if (onOpenTrends != null) {
            NovilaButton(
                text = stringResource(R.string.trends_open),
                onClick = onOpenTrends,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (onOpenAll != null) {
            NovilaTextButton(text = stringResource(R.string.discoveries_open), onClick = onOpenAll)
        }
        if (onOpenAchievements != null) {
            NovilaTextButton(text = stringResource(R.string.achievements_open), onClick = onOpenAchievements)
        }
    }
}

/** A discovery the logs will unlock, as a bar in the color of the part it is about. */
@Composable
private fun UpcomingRow(progress: DiscoveryProgress) {
    val pillars = NovilaTheme.pillars
    val (label, role) = when (progress.discovery) {
        Discovery.SleepLinks -> R.string.discovery_sleep_links to pillars.sleep
        Discovery.TrainingAfterSleep -> R.string.discovery_training_after_sleep to pillars.training
        Discovery.Weekdays -> R.string.discovery_weekdays to pillars.nutrition
    }
    NovilaMeter(
        label = stringResource(label),
        value = stringResource(
            if (progress.discovery == Discovery.SleepLinks) R.string.discovery_nights else R.string.discovery_days,
            progress.logged,
            progress.needed,
        ),
        progress = progress.progress,
        role = role,
    )
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
