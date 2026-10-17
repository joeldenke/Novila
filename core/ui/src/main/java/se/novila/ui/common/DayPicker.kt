package se.novila.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaDaySwitcher
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.model.EditableDays
import se.novila.ui.R
import java.time.LocalDate

/**
 * Goes back to an earlier day to fill in what was forgotten, and forward again to [today]. Steps a day at a time
 * within [EditableDays], in the [role] color of the part of the app it changes.
 */
@Composable
fun DayPicker(
    date: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    role: NovilaColorRole,
    modifier: Modifier = Modifier,
) {
    NovilaDaySwitcher(
        label = dayText(date, today),
        onPrevious = { onSelect(date.minusDays(1)) },
        onNext = { onSelect(date.plusDays(1)) },
        previousDescription = stringResource(R.string.day_previous),
        nextDescription = stringResource(R.string.day_next),
        role = role,
        modifier = modifier,
        previousEnabled = EditableDays.canGoBack(date, today),
        nextEnabled = EditableDays.canGoForward(date, today),
    )
}

/** "Today", "Yesterday", or the weekday and date for earlier days. */
@Composable
fun dayText(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.day_today)
    today.minusDays(1) -> stringResource(R.string.day_yesterday)
    else -> longDateText(date)
}
