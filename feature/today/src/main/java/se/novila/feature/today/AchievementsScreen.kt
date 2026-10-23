package se.novila.feature.today

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaMeter
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.Achievements
import se.novila.model.Habit
import se.novila.model.HabitWeeks
import se.novila.model.MilestoneKind
import se.novila.model.MilestoneTrack
import se.novila.model.achievements
import se.novila.ui.common.ScreenList
import se.novila.ui.common.shortDateLabels
import se.novila.ui.preview.PreviewData
import se.novila.ui.R as UiR

/**
 * The weeks in a row and the milestones, opened from Today's insights. Restrained on purpose: a week counts when it
 * reaches its mark, a missed day breaks nothing, rest is part of a training week, and nothing is ever taken away.
 * Every number comes from the user's own logs ([achievements]).
 */
@Composable
fun AchievementsScreen(achievements: Achievements, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "weeks") { NovilaReveal(order = 0) { WeeksCard(achievements.habits) } }
        item(key = "milestones") { NovilaReveal(order = 1) { MilestonesCard(achievements.milestones) } }
    }
}

@Composable
private fun WeeksCard(habits: List<HabitWeeks>) {
    NovilaCard(title = stringResource(R.string.achievements_weeks_title)) {
        NovilaText(
            stringResource(R.string.achievements_weeks_body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        habits.forEach { HabitRow(it) }
    }
}

@Composable
private fun HabitRow(habit: HabitWeeks) {
    val (name, rule, role) = when (habit.habit) {
        Habit.Sleep -> Triple(UiR.string.nav_sleep, R.string.achievements_rule_sleep, NovilaTheme.pillars.sleep)

        Habit.Training -> Triple(
            UiR.string.nav_training,
            R.string.achievements_rule_training,
            NovilaTheme.pillars.training,
        )

        Habit.Nutrition -> Triple(
            UiR.string.nav_nutrition,
            R.string.achievements_rule_nutrition,
            NovilaTheme.pillars.nutrition,
        )
    }
    NovilaInsight(
        icon = painterResource(habitIcon(habit.habit)),
        text = stringResource(
            R.string.achievements_habit,
            stringResource(name),
            if (habit.weeks > 0) {
                pluralStringResource(R.plurals.achievements_run, habit.weeks, habit.weeks)
            } else {
                stringResource(R.string.achievements_not_yet)
            },
        ),
        role = role,
    )
    NovilaMeter(
        label = stringResource(rule),
        value = if (habit.habit == Habit.Training) {
            stringResource(R.string.achievements_this_week_rest, habit.thisWeek, habit.habit.days, habit.restDays)
        } else {
            stringResource(R.string.achievements_this_week, habit.thisWeek, habit.habit.days)
        },
        progress = habit.progress,
        role = role,
    )
    if (habit.best > habit.weeks) {
        NovilaText(
            pluralStringResource(R.plurals.achievements_best, habit.best, habit.best),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@DrawableRes
private fun habitIcon(habit: Habit): Int = when (habit) {
    Habit.Sleep -> UiR.drawable.ic_sleep
    Habit.Training -> UiR.drawable.ic_training
    Habit.Nutrition -> UiR.drawable.ic_nutrition
}

@Composable
private fun MilestonesCard(milestones: List<MilestoneTrack>) {
    NovilaCard(title = stringResource(R.string.achievements_milestones_title)) {
        milestones.forEach { MilestoneRow(it) }
    }
}

@Composable
private fun MilestoneRow(track: MilestoneTrack) {
    val pillars = NovilaTheme.pillars
    val (label, role) = when (track.kind) {
        MilestoneKind.Nights -> R.string.achievements_nights to pillars.sleep
        MilestoneKind.TrainingDays -> R.string.achievements_training_days to pillars.training
        MilestoneKind.FoodDays -> R.string.achievements_food_days to pillars.nutrition
        MilestoneKind.WaterDays -> R.string.achievements_water_days to pillars.nutrition
    }
    val next = track.next
    NovilaMeter(
        label = stringResource(label),
        value = if (next != null) {
            stringResource(R.string.achievements_count_of, track.total, next)
        } else {
            stringResource(R.string.achievements_count, track.total)
        },
        progress = track.progress,
        role = role,
    )
    LastReached(track, role)
}

/** The last milestone reached and the day it was, quietly under its bar. */
@Composable
private fun LastReached(track: MilestoneTrack, role: NovilaColorRole) {
    val last = track.reached.lastOrNull() ?: return
    val date = shortDateLabels(listOf(last.on)).first()
    NovilaText(
        stringResource(R.string.achievements_reached, last.step, date),
        style = NovilaTheme.typography.bodySmall,
        color = if (track.next == null) role.color else NovilaTheme.colors.onSurfaceVariant,
    )
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun AchievementsScreenPreview() {
    NovilaTheme {
        AchievementsScreen(
            achievements = achievements(PreviewData.days(count = 60), PreviewData.date),
            contentPadding = PaddingValues(),
        )
    }
}

/** Before anything is logged: nothing counted yet, and nothing to feel bad about. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun AchievementsScreenEmptyPreview() {
    NovilaTheme {
        AchievementsScreen(achievements = achievements(emptyList(), PreviewData.date), contentPadding = PaddingValues())
    }
}
