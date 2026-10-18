package se.novila.feature.training

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.R as UiR

/**
 * The weekly review: how many of this week's planned sessions are done, and what next week's program holds,
 * which builds on the training logged this week.
 */
@Composable
fun WeeklyReviewCard(review: WeeklyReview, modifier: Modifier = Modifier) {
    NovilaCard(
        title = stringResource(R.string.review_title),
        icon = painterResource(UiR.drawable.ic_training),
        role = NovilaTheme.pillars.training,
        modifier = modifier,
    ) {
        NovilaInsight(
            icon = painterResource(UiR.drawable.ic_training),
            text = pluralStringResource(R.plurals.review_done, review.done, review.done, review.planned),
            role = NovilaTheme.pillars.training,
        )
        NovilaText(
            text = stringResource(
                R.string.review_next,
                review.nextWeek.sessions.size,
                review.nextWeek.sessions.sumOf { it.minutes },
            ),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}
