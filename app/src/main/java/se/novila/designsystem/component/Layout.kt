package se.novila.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import se.novila.designsystem.theme.NovilaTheme

/** App-level scaffold. Content receives padding that already includes system bar insets. */
@Composable
fun NovilaScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = NovilaTheme.colors.surface,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovilaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(title, style = NovilaTheme.typography.titleLarge) },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NovilaTheme.colors.surface,
            titleContentColor = NovilaTheme.colors.onSurface,
        ),
    )
}

@Composable
fun NovilaNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = NovilaTheme.colors.surfaceContainer,
        content = content,
    )
}

@Composable
fun RowScope.NovilaNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        // The label already describes the item, so the icon is decorative.
        icon = { NovilaIcon(painter = icon, contentDescription = null) },
        label = { Text(label) },
        modifier = modifier,
    )
}
