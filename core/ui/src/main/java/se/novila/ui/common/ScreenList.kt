package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import se.novila.designsystem.theme.NovilaTheme

/** Content is never wider than this, so cards stay readable on a tablet or an unfolded phone. */
private val MaxContentWidth = 640.dp

/** From this much room a screen with an [aside] shows it as a second pane, so each pane is about a phone wide. */
private val TwoPaneMinWidth = 700.dp

/** Two panes are never wider than this together, so the cards in them keep a phone's proportions. */
private val MaxTwoPaneWidth = 1040.dp

/**
 * Scrollable screen body. Insets from the scaffold go into contentPadding (edge-to-edge skill),
 * so content scrolls behind system bars without being hidden. On a wide screen the content is centered at
 * [MaxContentWidth], while the list itself still scrolls from the whole width.
 *
 * A tab's start page passes what supports its overview (the ways in, insights) as [aside]. On a phone it follows the
 * content in the same list; from [TwoPaneMinWidth] (an unfolded phone, a tablet, landscape) it becomes a pane of its
 * own next to the content, so no card or button is stretched across the screen (adaptive skill, supporting pane).
 */
@Composable
fun ScreenList(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    aside: (LazyListScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val spacing = NovilaTheme.spacing
    BoxWithConstraints(modifier.fillMaxSize().consumeWindowInsets(contentPadding)) {
        val start = contentPadding.calculateStartPadding(layoutDirection)
        val end = contentPadding.calculateEndPadding(layoutDirection)
        val top = contentPadding.calculateTopPadding() + spacing.s
        val bottom = contentPadding.calculateBottomPadding() + spacing.l
        val available = maxWidth - start - end
        if (aside == null || available < TwoPaneMinWidth) {
            // What is left on each side once the content is at its widest, or nothing on a phone.
            val side = max(spacing.screenMargin, ((available - MaxContentWidth) / 2).coerceAtLeast(0.dp))
            Pane(PaddingValues(start + side, top, end + side, bottom), Modifier.fillMaxSize()) {
                content()
                aside?.invoke(this)
            }
        } else {
            // Two panes of the same width, centered together, with the usual gap between them.
            val gap = spacing.m
            val panes = minOf(available - spacing.screenMargin * 2, MaxTwoPaneWidth)
            val pane = (panes - gap) / 2
            val side = (available - panes) / 2
            Row(Modifier.fillMaxSize()) {
                Pane(
                    PaddingValues(start + side, top, gap / 2, bottom),
                    Modifier.width(start + side + pane + gap / 2).fillMaxHeight(),
                    content,
                )
                Pane(PaddingValues(gap / 2, top, end + side, bottom), Modifier.weight(1f).fillMaxHeight(), aside)
            }
        }
    }
}

@Composable
private fun Pane(contentPadding: PaddingValues, modifier: Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
        content = content,
    )
}
