package se.novila.feature.sleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.sleep.R
import se.novila.model.BodyClock
import se.novila.model.MIN_DEBT_NIGHTS
import se.novila.model.SleepDebt
import se.novila.ui.common.clockText
import se.novila.ui.common.durationText
import se.novila.ui.common.hoursText
import java.time.LocalTime
import se.novila.ui.R as UiR

/** Less than this behind is no debt worth a number. */
private const val NO_DEBT_MINUTES = 30

/**
 * What the user's nights say about their sleep need: the two weeks' [debt] against the goal of [goalHours], and their
 * [bodyClock] from the free days, with the bedtime that puts the goal around it. Until there are enough nights, how
 * many there are.
 */
@Composable
fun SleepNeedCard(debt: SleepDebt, bodyClock: BodyClock?, goalHours: Float) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(R.string.sleep_need_title),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        val minutes = debt.minutes
        val debtText = when {
            minutes == null -> stringResource(R.string.sleep_need_debt_empty, debt.nights, MIN_DEBT_NIGHTS)
            minutes < NO_DEBT_MINUTES -> stringResource(R.string.sleep_need_no_debt, hoursText(goalHours))
            debt.notable -> stringResource(R.string.sleep_need_debt_notable, durationText(minutes))
            else -> stringResource(R.string.sleep_need_debt, durationText(minutes))
        }
        NovilaInsight(painterResource(UiR.drawable.ic_sleep), debtText, sleep)
        if (bodyClock == null) {
            NovilaText(
                stringResource(R.string.sleep_need_clock_empty),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        } else {
            NovilaInsight(
                painterResource(UiR.drawable.ic_sleep),
                stringResource(
                    R.string.sleep_need_clock,
                    clockText(bodyClock.midSleep),
                    hoursText(goalHours),
                    clockText(bodyClock.bedtimeFor(goalHours)),
                ),
                sleep,
            )
        }
        NovilaText(
            stringResource(R.string.sleep_need_footer),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

/** Five hours behind over two weeks, and a body clock a little later than the weekdays allow. */
@Preview(showBackground = true)
@Composable
private fun SleepNeedCardPreview() {
    NovilaTheme {
        SleepNeedCard(
            debt = SleepDebt(minutes = 300, nights = 13),
            bodyClock = BodyClock(LocalTime.of(3, 45), freeNights = 8, workNights = 19),
            goalHours = 8f,
        )
    }
}

/** Too few nights for either yet. */
@Preview(showBackground = true)
@Composable
private fun SleepNeedCardEmptyPreview() {
    NovilaTheme { SleepNeedCard(SleepDebt(minutes = null, nights = 2), bodyClock = null, goalHours = 8f) }
}
