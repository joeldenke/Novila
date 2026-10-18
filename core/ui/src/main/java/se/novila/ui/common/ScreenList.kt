package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import se.novila.designsystem.theme.NovilaTheme

/**
 * Scrollable screen body. Insets from the scaffold go into contentPadding (edge-to-edge skill),
 * so content scrolls behind system bars without being hidden. Pass [state] to scroll the list from code.
 */
@Composable
fun ScreenList(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val spacing = NovilaTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize().consumeWindowInsets(contentPadding),
        state = state,
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection) + spacing.screenMargin,
            top = contentPadding.calculateTopPadding() + spacing.s,
            end = contentPadding.calculateEndPadding(layoutDirection) + spacing.screenMargin,
            bottom = contentPadding.calculateBottomPadding() + spacing.l,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
        content = content,
    )
}
