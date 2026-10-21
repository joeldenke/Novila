package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import se.novila.designsystem.theme.NovilaTheme

/** Content is never wider than this, so cards stay readable on a tablet or an unfolded phone. */
private val MaxContentWidth = 720.dp

/**
 * Scrollable screen body. Insets from the scaffold go into contentPadding (edge-to-edge skill),
 * so content scrolls behind system bars without being hidden. On a wide screen the content is centered at
 * [MaxContentWidth], while the list itself still scrolls from the whole width.
 */
@Composable
fun ScreenList(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val spacing = NovilaTheme.spacing
    BoxWithConstraints(modifier.fillMaxSize().consumeWindowInsets(contentPadding)) {
        val start = contentPadding.calculateStartPadding(layoutDirection)
        val end = contentPadding.calculateEndPadding(layoutDirection)
        // What is left on each side once the content is at its widest, or nothing on a phone.
        val extra = ((maxWidth - start - end - MaxContentWidth) / 2).coerceAtLeast(0.dp)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = start + max(spacing.screenMargin, extra),
                top = contentPadding.calculateTopPadding() + spacing.s,
                end = end + max(spacing.screenMargin, extra),
                bottom = contentPadding.calculateBottomPadding() + spacing.l,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
            content = content,
        )
    }
}
