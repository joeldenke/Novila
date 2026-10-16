package se.novila.feature.sleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaSlider
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.sleep.R
import se.novila.model.MAX_SLEEP_HOURS
import se.novila.ui.common.hoursText
import se.novila.ui.R as UiR

@Composable
fun SleepCard(hours: Float, onChange: (Float) -> Unit) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(R.string.sleep_title),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        NovilaText(hoursText(hours), style = NovilaTheme.typography.headlineSmall)
        NovilaSlider(
            value = hours,
            onValueChange = onChange,
            valueRange = 0f..MAX_SLEEP_HOURS,
            steps = (MAX_SLEEP_HOURS * 2).toInt() - 1, // half-hour steps
            role = sleep,
        )
    }
}
