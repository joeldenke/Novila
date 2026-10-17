package se.novila.feature.training

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.PlannedSession
import se.novila.feature.training.R
import se.novila.model.CardioLog
import se.novila.model.CardioType
import se.novila.ui.R as UiR

private const val MINUTES_STEP = 5
private const val MAX_MINUTES = 180
private const val DEFAULT_MINUTES = 30

/**
 * Today's cardio: "I did it" logs a walk, run, ride or swim, so the program builds on it. Starts from today's planned
 * cardio, when there is one. What a watch recorded through Health Connect is shown too, so the same walk isn't logged
 * twice (and if it is, the program counts it once).
 */
@Composable
fun CardioCard(
    today: CardioToday,
    planned: PlannedSession?,
    onLog: (CardioLog) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val training = NovilaTheme.pillars.training
    var type by rememberSaveable(planned) { mutableStateOf(planned?.type?.cardioType ?: CardioType.Walking) }
    var minutes by rememberSaveable(planned) { mutableIntStateOf(planned?.minutes ?: DEFAULT_MINUTES) }
    NovilaCard(
        title = stringResource(R.string.cardio_title),
        icon = painterResource(UiR.drawable.ic_training),
        role = training,
        modifier = modifier,
    ) {
        Body(
            when {
                today.logged.isNotEmpty() -> stringResource(R.string.cardio_logged, today.loggedMinutes)

                planned != null -> stringResource(
                    R.string.cardio_planned,
                    stringResource(planned.type.label),
                    planned.minutes,
                )

                else -> stringResource(R.string.cardio_unplanned)
            },
        )
        if (today.recordedMinutes > 0) Body(stringResource(R.string.cardio_recorded, today.recordedMinutes))
        CardioTypeChips(selected = type, onSelect = { type = it })
        NovilaStepper(
            title = stringResource(R.string.cardio_minutes),
            value = stringResource(R.string.minutes_value, minutes),
            onDecrement = { minutes = (minutes - MINUTES_STEP).coerceAtLeast(MINUTES_STEP) },
            onIncrement = { minutes = (minutes + MINUTES_STEP).coerceAtMost(MAX_MINUTES) },
            decrementDescription = stringResource(R.string.cardio_fewer_minutes),
            incrementDescription = stringResource(R.string.cardio_more_minutes),
            role = training,
            decrementEnabled = minutes > MINUTES_STEP,
            incrementEnabled = minutes < MAX_MINUTES,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            if (today.logged.isNotEmpty()) {
                NovilaButton(
                    text = stringResource(R.string.cardio_undo),
                    onClick = onUndo,
                    role = training,
                    modifier = Modifier.weight(1f),
                )
            }
            NovilaButton(
                text = stringResource(if (today.logged.isEmpty()) R.string.cardio_log else R.string.cardio_log_more),
                onClick = { onLog(CardioLog(type, minutes)) },
                role = training,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CardioTypeChips(selected: CardioType, onSelect: (CardioType) -> Unit) {
    CardioType.entries.chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            row.forEach { type ->
                NovilaChoiceChip(
                    label = stringResource(type.exerciseType.label),
                    selected = type == selected,
                    onClick = { onSelect(type) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

@Composable
private fun CardioCardContent(today: CardioToday) {
    NovilaTheme {
        CardioCard(
            today = today,
            planned = TrainingPreviewData.program.sessions.firstOrNull { it.type.cardioType != null },
            onLog = {},
            onUndo = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CardioCardPreview() = CardioCardContent(CardioToday())

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun CardioCardLoggedDarkPreview() = CardioCardContent(
    CardioToday(logged = listOf(CardioLog(CardioType.Running, 35)), recordedMinutes = 32),
)
