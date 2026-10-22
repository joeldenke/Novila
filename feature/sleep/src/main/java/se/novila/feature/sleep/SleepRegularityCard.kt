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
import se.novila.model.MIN_REGULARITY_PAIRS
import se.novila.model.RegularityLevel
import se.novila.model.SleepRegularity
import se.novila.ui.common.durationText
import kotlin.math.abs
import se.novila.ui.R as UiR

/**
 * How regular the user's sleep is: the Sleep Regularity Index in words, with the nights behind it, and how much the
 * weekend moves the sleep (social jetlag). Until there are enough nights in a row with times, how many there are.
 */
@Composable
fun SleepRegularityCard(regularity: SleepRegularity) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(R.string.sleep_regularity_title),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        val index = regularity.index
        val level = regularity.level
        if (index == null || level == null) {
            Body(stringResource(R.string.sleep_regularity_empty, regularity.pairs, MIN_REGULARITY_PAIRS))
        } else {
            NovilaInsight(
                painterResource(UiR.drawable.ic_sleep),
                stringResource(level.text, index, regularity.pairs),
                sleep,
            )
        }
        regularity.socialJetlagMinutes?.takeIf { regularity.notableJetlag }?.let { minutes ->
            NovilaInsight(
                painterResource(UiR.drawable.ic_sleep),
                stringResource(
                    if (minutes > 0) {
                        R.string.sleep_regularity_weekend_later
                    } else {
                        R.string.sleep_regularity_weekend_earlier
                    },
                    durationText(abs(minutes)),
                ),
                sleep,
            )
        }
        NovilaText(
            stringResource(R.string.sleep_regularity_footer),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

private val RegularityLevel.text: Int
    get() = when (this) {
        RegularityLevel.Steady -> R.string.sleep_regularity_steady
        RegularityLevel.FairlySteady -> R.string.sleep_regularity_fairly_steady
        RegularityLevel.Irregular -> R.string.sleep_regularity_irregular
    }

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

/** Fairly steady on weekdays, with the weekend an hour and a half later. */
@Preview(showBackground = true)
@Composable
private fun SleepRegularityCardPreview() {
    NovilaTheme {
        SleepRegularityCard(SleepRegularity(index = 72, pairs = 24, socialJetlagMinutes = 90, 20, 8))
    }
}

/** Not enough nights in a row with times yet. */
@Preview(showBackground = true)
@Composable
private fun SleepRegularityCardEmptyPreview() {
    NovilaTheme { SleepRegularityCard(SleepRegularity(null, pairs = 2, null, 3, 1)) }
}
