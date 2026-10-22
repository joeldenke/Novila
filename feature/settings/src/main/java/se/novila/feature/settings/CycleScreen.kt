package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.data.SavedCycle
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaConfirmDialog
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaSwitchRow
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.model.CycleLog
import se.novila.model.periodStarts
import se.novila.ui.common.ScreenList
import se.novila.ui.common.longDateText
import java.time.LocalDate
import se.novila.ui.R as UiR

/**
 * *Follow your cycle*, off by default: what it does (a lighter session offered on a day with symptoms, and recovery
 * that compares a day only with the same half of the cycle), what it never does (predict ovulation or fertile days,
 * or plan training by phase), and that everything stays on the phone. Turning it off deletes what was logged, so a
 * log that isn't empty asks first. The cycle itself is logged on Training.
 */
@Composable
fun CycleScreen(
    cycle: SavedCycle,
    onEnabled: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var confirmingOff by rememberSaveable { mutableStateOf(false) }
    val training = NovilaTheme.pillars.training
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") { Body(stringResource(R.string.cycle_intro)) }
        item(key = "switch") {
            NovilaCard(title = stringResource(R.string.cycle_title), icon = painterResource(UiR.drawable.ic_training)) {
                NovilaSwitchRow(
                    title = stringResource(R.string.cycle_switch),
                    summary = stringResource(R.string.cycle_private),
                    checked = cycle.enabled,
                    onCheckedChange = { on ->
                        if (!on && cycle.log.days.isNotEmpty()) confirmingOff = true else onEnabled(on)
                    },
                    role = training,
                )
                if (cycle.enabled) Body(lastPeriodText(cycle.log))
            }
        }
        item(key = "does") {
            NovilaCard(title = stringResource(R.string.cycle_does_title), role = training) {
                Body(stringResource(R.string.cycle_does_lighter))
                Body(stringResource(R.string.cycle_does_recovery))
            }
        }
        item(key = "never") {
            NovilaCard(title = stringResource(R.string.cycle_never_title), role = training) {
                Body(stringResource(R.string.cycle_never_body))
            }
        }
    }
    if (confirmingOff) {
        NovilaConfirmDialog(
            title = stringResource(R.string.cycle_off_title),
            text = stringResource(R.string.cycle_off_text),
            confirmLabel = stringResource(R.string.cycle_off_confirm),
            dismissLabel = stringResource(R.string.cycle_off_dismiss),
            onConfirm = {
                confirmingOff = false
                onEnabled(false)
            },
            onDismiss = { confirmingOff = false },
        )
    }
}

/** When the last period began, or where to log one when none is logged yet. */
@Composable
private fun lastPeriodText(log: CycleLog): String {
    val start = periodStarts(log.periodDays).lastOrNull() ?: return stringResource(R.string.cycle_nothing_logged)
    return stringResource(R.string.cycle_last_period, longDateText(start))
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

@Composable
private fun CycleContent(cycle: SavedCycle) {
    NovilaTheme {
        NovilaScaffold { padding -> CycleScreen(cycle = cycle, onEnabled = {}, contentPadding = padding) }
    }
}

/** Off, as it starts. */
@Preview(heightDp = 1000)
@Composable
private fun CyclePreview() = CycleContent(SavedCycle())

/** On, with the last period logged. */
@Preview(heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun CycleOnPreview() = CycleContent(
    SavedCycle(
        enabled = true,
        log = CycleLog()
            .withPeriod(LocalDate.of(2026, 10, 9), on = true)
            .withPeriod(LocalDate.of(2026, 10, 10), on = true),
    ),
)
