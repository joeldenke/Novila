package se.novila.feature.training

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.LoadComparison
import se.novila.exercise.TrainingLoad
import se.novila.feature.training.R
import kotlin.math.abs
import kotlin.math.roundToInt
import se.novila.ui.R as UiR

/**
 * The last seven days' training load (effort times minutes) in words: against the usual week, never as a warning,
 * whether the days were too alike to recover between, and how many sessions had the user's own effort.
 */
@Composable
fun TrainingLoadCard(load: TrainingLoad) {
    val training = NovilaTheme.pillars.training
    NovilaCard(
        title = stringResource(R.string.load_title),
        icon = painterResource(UiR.drawable.ic_training),
        role = training,
    ) {
        val percent = load.ratio?.let { (abs(it - 1) * 100).roundToInt() } ?: 0
        val comparison = when (load.comparison) {
            LoadComparison.Heavier -> stringResource(R.string.load_heavier, percent)
            LoadComparison.Lighter -> stringResource(R.string.load_lighter, percent)
            LoadComparison.Usual -> stringResource(R.string.load_usual)
            LoadComparison.Unknown -> stringResource(R.string.load_unknown)
        }
        NovilaInsight(painterResource(UiR.drawable.ic_training), comparison, training)
        if (load.uniform) {
            NovilaInsight(painterResource(UiR.drawable.ic_training), stringResource(R.string.load_uniform), training)
        }
        NovilaText(
            stringResource(R.string.load_footer, load.rated, load.sessions),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

/** A heavier week than the three before. */
@Preview(showBackground = true)
@Composable
private fun TrainingLoadCardPreview() {
    NovilaTheme {
        TrainingLoadCard(TrainingLoad(week = 1240, usualWeek = 880, monotony = 1.1, sessions = 3, rated = 1))
    }
}

/** The same session every day, before the usual week is known. */
@Preview(showBackground = true)
@Composable
private fun TrainingLoadCardUniformPreview() {
    NovilaTheme {
        TrainingLoadCard(TrainingLoad(week = 1050, usualWeek = null, monotony = 10.0, sessions = 7, rated = 7))
    }
}
