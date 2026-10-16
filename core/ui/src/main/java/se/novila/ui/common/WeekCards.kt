package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaBarChart
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaLineChart
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.GOOD_NUTRITION_SCORE
import se.novila.model.Insight
import se.novila.model.SLEEP_GOAL_HOURS
import se.novila.model.STEPS_GOAL
import se.novila.model.averageNutrition
import se.novila.model.averageSleep
import se.novila.model.nutritionScore
import se.novila.ui.R
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
