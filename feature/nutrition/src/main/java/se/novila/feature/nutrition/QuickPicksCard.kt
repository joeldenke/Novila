package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaIconButton
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.MealPick

/**
 * The meals the user eats often, to log again with one tap ([onLogMeal]): the starred ones first, then the ones
 * logged most often lately. The star next to each ([onToggleFavorite]) keeps a meal first, or lets it go. Nothing is
 * shown until a meal has been logged in the user's own words.
 */
@Composable
fun QuickPicksCard(
    picks: List<MealPick>,
    onLogMeal: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (picks.isEmpty()) return
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(modifier = modifier, title = stringResource(R.string.quick_picks_title), role = nutrition) {
        picks.forEach { pick ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            ) {
                NovilaButton(
                    text = pick.text,
                    onClick = { onLogMeal(pick.text) },
                    role = nutrition,
                    contentDescription = stringResource(R.string.quick_picks_log, pick.text),
                    modifier = Modifier.weight(1f),
                )
                NovilaIconButton(
                    painter = painterResource(if (pick.favorite) R.drawable.ic_star else R.drawable.ic_star_border),
                    contentDescription = stringResource(
                        if (pick.favorite) R.string.quick_picks_unfavorite else R.string.quick_picks_favorite,
                        pick.text,
                    ),
                    onClick = { onToggleFavorite(pick.text) },
                )
            }
        }
        NovilaText(
            stringResource(R.string.quick_picks_hint),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

/** A starred breakfast first, then the lunch and dinner logged most often lately. */
@Preview(showBackground = true)
@Composable
private fun QuickPicksCardPreview() {
    NovilaTheme {
        QuickPicksCard(
            picks = listOf(
                MealPick("Frukost: gröt, bär", favorite = true),
                MealPick("Lunch: lax, potatis, sallad", favorite = false),
                MealPick("Middag: pasta, broccoli, kyckling", favorite = false),
            ),
            onLogMeal = {},
            onToggleFavorite = {},
        )
    }
}
