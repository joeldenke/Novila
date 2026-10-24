package se.novila.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import se.novila.designsystem.component.LocalNovilaFold
import se.novila.designsystem.theme.NovilaTheme

/** Content in one column is never wider than this, so cards stay readable on a tablet or an unfolded phone. */
private val MaxContentWidth = 640.dp

/**
 * From this much room (an expanded window, 840 dp, less the navigation rail) a screen with an aside shows it as a
 * pane of its own: Material 3's supporting pane.
 */
private val TwoPaneMinWidth = 760.dp

/** The supporting pane has a fixed width, about a phone's, and the content pane takes the rest. */
private val SupportingPaneWidth = 360.dp

/** The content pane never grows wider than this, so its cards keep their proportions on a large tablet. */
private val MaxContentPaneWidth = 840.dp

/** A pane along a fold is never narrower than this; a fold too far to one side is ignored. */
private val MinFoldPaneWidth = 280.dp

/** The margins and the space between panes on a tablet or an unfolded phone (Material 3, medium and wider). */
private val WideMargin = 24.dp

/** What builds a [ScreenList]: the list itself, and whether the content and its aside lie in [twoPanes]. */
@OptIn(ExperimentalFoundationApi::class)
class ScreenListScope internal constructor(list: LazyListScope, val twoPanes: Boolean) : LazyListScope by list

/**
 * Scrollable screen body. Insets from the scaffold go into contentPadding (edge-to-edge skill),
 * so content scrolls behind system bars without being hidden. On a wide screen the content is centered at
 * [MaxContentWidth], while the list itself still scrolls from the whole width.
 *
 * A tab's start page passes what supports its overview (the ways in, insights) as [aside]. On a phone and a
 * portrait tablet it follows the content in the same list. From [TwoPaneMinWidth] (a landscape tablet, an unfolded
 * phone) it is a supporting pane of its own, [SupportingPaneWidth] wide next to the content (adaptive skill,
 * supporting pane). On an unfolded phone the panes split along the fold ([LocalNovilaFold]), so no card lies across
 * it. Both builders know through [ScreenListScope.twoPanes] where they are, so a card can move to the aside, or a
 * card that only fits a wide pane can be added.
 */
@Composable
fun ScreenList(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    aside: (ScreenListScope.() -> Unit)? = null,
    content: ScreenListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val spacing = NovilaTheme.spacing
    val fold = LocalNovilaFold.current
    // Where this list starts in the window, to find the fold in it.
    var left by remember { mutableStateOf(0.dp) }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .consumeWindowInsets(contentPadding)
            .onPlaced { left = with(density) { it.positionInWindow().x.toDp() } },
    ) {
        val start = contentPadding.calculateStartPadding(layoutDirection)
        val end = contentPadding.calculateEndPadding(layoutDirection)
        val top = contentPadding.calculateTopPadding() + spacing.s
        val bottom = contentPadding.calculateBottomPadding() + spacing.l
        val available = maxWidth - start - end
        if (aside == null || available < TwoPaneMinWidth) {
            // What is left on each side once the content is at its widest, or nothing on a phone.
            val side = max(spacing.screenMargin, ((available - MaxContentWidth) / 2).coerceAtLeast(0.dp))
            Pane(PaddingValues(start + side, top, end + side, bottom), Modifier.fillMaxSize()) {
                ScreenListScope(this, twoPanes = false).run {
                    content()
                    aside?.invoke(this)
                }
            }
        } else {
            // The fold from this list's start edge, when it leaves room for a pane on each side.
            val foldStart = fold?.let {
                if (layoutDirection == LayoutDirection.Ltr) it.left - left else left + maxWidth - it.right
            }
            val foldWidth = fold?.let { it.right - it.left } ?: 0.dp
            val split = foldStart?.takeIf {
                it - start - WideMargin >= MinFoldPaneWidth &&
                    maxWidth - it - foldWidth - end - WideMargin >= MinFoldPaneWidth
            }
            val panes = if (split != null) {
                PaneWidths(
                    side = WideMargin,
                    content = split - start - WideMargin - WideMargin / 2,
                    gap = foldWidth + WideMargin,
                )
            } else {
                val contentWidth = minOf(available - WideMargin * 3 - SupportingPaneWidth, MaxContentPaneWidth)
                PaneWidths(
                    side = (available - contentWidth - SupportingPaneWidth - WideMargin) / 2,
                    content = contentWidth,
                    gap = WideMargin,
                )
            }
            Row(Modifier.fillMaxSize()) {
                Pane(
                    PaddingValues(start + panes.side, top, panes.gap / 2, bottom),
                    Modifier.width(start + panes.side + panes.content + panes.gap / 2).fillMaxHeight(),
                ) { ScreenListScope(this, twoPanes = true).content() }
                Pane(
                    PaddingValues(panes.gap / 2, top, end + panes.side.coerceAtLeast(WideMargin), bottom),
                    Modifier.weight(1f).fillMaxHeight(),
                ) { ScreenListScope(this, twoPanes = true).aside() }
            }
        }
    }
}

/** The space before the content pane, its width, and the gap between it and the supporting pane. */
private class PaneWidths(val side: Dp, val content: Dp, val gap: Dp)

@Composable
private fun Pane(contentPadding: PaddingValues, modifier: Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
        content = content,
    )
}
