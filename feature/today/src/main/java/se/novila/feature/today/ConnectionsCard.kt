package se.novila.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.model.Connection
import se.novila.model.TrainingAfterSleep
import se.novila.model.WeekdayMetric
import se.novila.model.WeekdayPattern
import se.novila.model.WellbeingConnection
import se.novila.model.WellbeingDriver
import se.novila.ui.common.connectionText
import se.novila.ui.R as UiR

/**
 * What the parts say about each other, from the user's own logs: how the night before shows in training, how food or
 * training shows in how they felt, and which weekday stands out. Shown only when there is enough data behind it
 * ([connections] is non-empty), so it never guesses. A connection isn't proof of cause; the footer says so.
 */
@Composable
fun ConnectionsCard(connections: List<Connection>) {
    NovilaCard(title = stringResource(R.string.connection_title)) {
        connections.forEach { connection ->
            val (icon, role) = connection.look()
            NovilaInsight(icon, connectionText(connection), role)
        }
        NovilaText(
            stringResource(R.string.connection_footer),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

/** The part a connection is mostly about, so its row carries that part's icon and colour. */
@Composable
private fun Connection.look(): Pair<Painter, NovilaColorRole> {
    val pillars = NovilaTheme.pillars
    return when (this) {
        is TrainingAfterSleep -> painterResource(UiR.drawable.ic_training) to pillars.training

        is WellbeingConnection -> when (driver) {
            WellbeingDriver.Nutrition -> painterResource(UiR.drawable.ic_nutrition) to pillars.nutrition
            WellbeingDriver.Training -> painterResource(UiR.drawable.ic_training) to pillars.training
        }

        is WeekdayPattern -> when (metric) {
            WeekdayMetric.TrainingMinutes -> painterResource(UiR.drawable.ic_training) to pillars.training
            WeekdayMetric.SleepHours -> painterResource(UiR.drawable.ic_sleep) to pillars.sleep
            WeekdayMetric.NutritionScore -> painterResource(UiR.drawable.ic_nutrition) to pillars.nutrition
        }
    }
}
