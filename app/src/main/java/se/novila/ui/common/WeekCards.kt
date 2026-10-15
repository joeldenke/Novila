package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.DayEntry
import se.novila.GOOD_NUTRITION_SCORE
import se.novila.Insight
import se.novila.R
import se.novila.SLEEP_GOAL_HOURS
import se.novila.STEPS_GOAL
import se.novila.averageNutrition
import se.novila.averageSleep
import se.novila.designsystem.component.NovilaBarChart
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaLineChart
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.nutritionScore
import kotlin.math.abs

/** Sleep chart axis: short nights and long ones both stay readable. */
private val SleepChartRange = 4f..10f

/** The week's nights as a curve against the sleep goal, with the average per night. */
@Composable
fun SleepWeekCard(week: List<DayEntry>) {
    val sleep = NovilaTheme.pillars.sleep
    val average = hoursText(week.averageSleep() ?: 0f)
    NovilaCard(
        title = stringResource(R.string.week_sleep_title),
        icon = painterResource(R.drawable.ic_sleep),
        role = sleep,
    ) {
        WeekSummary(average, stringResource(R.string.week_sleep_caption, hoursText(SLEEP_GOAL_HOURS)))
        NovilaLineChart(
            values = week.map { day -> day.sleepHours.takeIf { it > 0f } },
            labels = weekdayLabels(week),
            valueRange = SleepChartRange,
            role = sleep,
            contentDescription = stringResource(R.string.week_sleep_description, average),
            goal = SLEEP_GOAL_HOURS,
        )
    }
}

/** The week's nutrition scores as bars; days with a good score are solid. */
@Composable
fun NutritionWeekCard(week: List<DayEntry>) {
    val nutrition = NovilaTheme.pillars.nutrition
    val average = week.averageNutrition()?.let { stringResource(R.string.balance_value, it) }
        ?: stringResource(R.string.value_missing)
    NovilaCard(
        title = stringResource(R.string.week_nutrition_title),
        icon = painterResource(R.drawable.ic_nutrition),
        role = nutrition,
    ) {
        WeekSummary(average, stringResource(R.string.week_nutrition_caption, GOOD_NUTRITION_SCORE))
        NovilaBarChart(
            values = week.map { it.nutritionScore()?.total?.toFloat() },
            labels = weekdayLabels(week),
            maxValue = 100f,
            role = nutrition,
            contentDescription = stringResource(R.string.week_nutrition_description, average),
            goal = GOOD_NUTRITION_SCORE.toFloat(),
        )
    }
}

@Composable
private fun WeekSummary(value: String, caption: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(value, style = NovilaTheme.typography.headlineSmall)
        NovilaText(caption, style = NovilaTheme.typography.bodySmall, color = NovilaTheme.colors.onSurfaceVariant)
    }
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
    val sleepIcon = painterResource(R.drawable.ic_sleep)
    val nutritionIcon = painterResource(R.drawable.ic_nutrition)
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

        Insight.StepsGoalReached -> NovilaInsight(
            painterResource(R.drawable.ic_training),
            stringResource(R.string.insight_steps_goal, stepsText(STEPS_GOAL)),
            pillars.training,
        )
    }
}
