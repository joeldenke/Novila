package se.novila.feature.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.sleep.R
import se.novila.model.SleepAnalysis
import se.novila.model.SleepDriver
import se.novila.ui.common.bedtimeSpreadText
import se.novila.ui.common.sleepChangeText
import se.novila.ui.common.sleepLinkNightsText
import se.novila.ui.common.sleepLinkText
import se.novila.ui.R as UiR

/** Nights logged before the card stops asking for more and says what it found, or that nothing stands out. */
const val NIGHTS_FOR_FINDINGS = 14

/**
 * What the user's own logs say about their sleep: this week's change and what came with it, the strongest links
 * between their days and nights, each with the nights behind it, and how regular their bedtime is. Until there is
 * enough to go on, how many nights are logged.
 */
@Composable
fun SleepFindingsCard(analysis: SleepAnalysis) {
    NovilaCard(
        title = stringResource(R.string.sleep_findings_title),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = NovilaTheme.pillars.sleep,
    ) {
        when {
            analysis.isEmpty && analysis.nightsLogged < NIGHTS_FOR_FINDINGS -> Body(
                stringResource(R.string.sleep_findings_empty, analysis.nightsLogged, NIGHTS_FOR_FINDINGS),
            )

            analysis.isEmpty -> Body(stringResource(R.string.sleep_findings_none))

            else -> {
                analysis.change?.let { change ->
                    val (icon, role) = change.drivers.first().driver.look()
                    NovilaInsight(icon, sleepChangeText(change), role)
                }
                analysis.links.forEach { link ->
                    val (icon, role) = link.driver.look()
                    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
                        NovilaInsight(icon, sleepLinkText(link), role)
                        Small(sleepLinkNightsText(link))
                    }
                }
                analysis.bedtimes?.let {
                    NovilaInsight(
                        painterResource(UiR.drawable.ic_sleep),
                        bedtimeSpreadText(it),
                        NovilaTheme.pillars.sleep,
                    )
                }
                Small(stringResource(R.string.sleep_findings_footer))
            }
        }
    }
}

/** The part of the app a driver belongs to: food and drink, training, or the evening itself. */
@Composable
private fun SleepDriver.look(): Pair<Painter, NovilaColorRole> {
    val pillars = NovilaTheme.pillars
    return when (this) {
        SleepDriver.AteWell, SleepDriver.DrankEnough, SleepDriver.Treats, SleepDriver.LateMeal ->
            painterResource(UiR.drawable.ic_nutrition) to pillars.nutrition

        SleepDriver.Trained, SleepDriver.LateExercise -> painterResource(UiR.drawable.ic_training) to pillars.training

        SleepDriver.LateCaffeine, SleepDriver.Screens, SleepDriver.Alcohol, SleepDriver.Stress, SleepDriver.Period ->
            painterResource(UiR.drawable.ic_sleep) to pillars.sleep
    }
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

@Composable
private fun Small(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodySmall, color = NovilaTheme.colors.onSurfaceVariant)
}
