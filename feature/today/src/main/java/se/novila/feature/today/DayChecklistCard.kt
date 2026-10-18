package se.novila.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaProgressBar
import se.novila.designsystem.component.NovilaTaskRow
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.DayChecklist
import se.novila.model.DayTask
import se.novila.ui.common.hoursText
import se.novila.ui.common.nextPortionText

/** What each task on Today's checklist leads to. The entry connects them to the other tabs and the ViewModel. */
data class ChecklistActions(
    val onOpenTraining: () -> Unit = {},
    val onOpenSleep: () -> Unit = {},
    val onOpenNutrition: () -> Unit = {},
    val onAddWater: () -> Unit = {},
)

/**
 * The day as a short list of things to do, in the order of the day: sleep, training, food and water. Each open task has one button that does it or leads where it is done; the first open one is the screen's
 * main action, in the sun's color. Done tasks keep their place with a check, so the day reads as progress.
 */
@Composable
fun DayChecklistCard(checklist: DayChecklist, actions: ChecklistActions) {
    NovilaCard(title = stringResource(R.string.checklist_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs)) {
            NovilaText(
                if (checklist.allDone) {
                    stringResource(R.string.checklist_all_done)
                } else {
                    stringResource(R.string.checklist_progress, checklist.doneCount, checklist.tasks.size)
                },
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
            NovilaProgressBar(progress = checklist.doneCount.toFloat() / checklist.tasks.size)
        }
        val next = checklist.next
        checklist.tasks.forEach { task -> TaskRow(task, isNext = task == next, actions) }
    }
}

@Composable
private fun TaskRow(task: DayTask, isNext: Boolean, actions: ChecklistActions) {
    val pillars = NovilaTheme.pillars
    when (task) {
        is DayTask.Sleep -> Task(
            title = stringResource(if (task.done) R.string.checklist_sleep_done else R.string.checklist_sleep),
            summary = if (task.done) hoursText(task.hours) else stringResource(R.string.checklist_sleep_summary),
            task = task,
            role = pillars.sleep,
            action = stringResource(R.string.checklist_sleep_action) to actions.onOpenSleep,
            isNext = isNext,
        )

        is DayTask.Training -> {
            val today = task.today
            Task(
                title = stringResource(if (today.isRest) R.string.checklist_rest else R.string.checklist_training),
                summary = when {
                    today.isRest -> stringResource(R.string.checklist_rest_summary)

                    task.done -> stringResource(R.string.checklist_training_done, today.doneMinutes)

                    today.doneMinutes > 0 -> stringResource(
                        R.string.checklist_training_partly,
                        today.doneMinutes,
                        today.plannedMinutes,
                    )

                    else -> stringResource(R.string.checklist_training_summary, today.plannedMinutes)
                },
                task = task,
                role = pillars.training,
                action = stringResource(R.string.checklist_training_action) to actions.onOpenTraining,
                isNext = isNext,
            )
        }

        is DayTask.Nutrition -> Task(
            title = stringResource(R.string.checklist_nutrition),
            summary = task.next.let { next ->
                when {
                    task.done -> stringResource(R.string.checklist_nutrition_done, task.score ?: 0)
                    next != null -> nextPortionText(next)
                    else -> stringResource(R.string.checklist_nutrition_summary)
                }
            },
            task = task,
            role = pillars.nutrition,
            action = stringResource(R.string.checklist_nutrition_action) to actions.onOpenNutrition,
            isNext = isNext,
        )

        is DayTask.Water -> Task(
            title = stringResource(R.string.checklist_water),
            summary = stringResource(R.string.checklist_water_summary, task.glasses, task.goal),
            task = task,
            role = pillars.nutrition,
            action = stringResource(R.string.checklist_water_action) to actions.onAddWater,
            isNext = isNext,
        )
    }
}

/** A task with its [action]: the screen's main button when it is the [isNext] one, otherwise a quieter one. */
@Composable
private fun Task(
    title: String,
    summary: String,
    task: DayTask,
    role: NovilaColorRole,
    action: Pair<String, () -> Unit>,
    isNext: Boolean,
) {
    NovilaTaskRow(
        title = title,
        done = task.done,
        summary = summary,
        role = role,
        action = if (task.done) {
            null
        } else {
            {
                val (text, onClick) = action
                if (isNext) NovilaAccentButton(text, onClick) else NovilaButton(text, onClick, role = role)
            }
        },
    )
}
