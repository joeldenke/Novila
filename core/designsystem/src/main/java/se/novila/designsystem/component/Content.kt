package se.novila.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaTheme

/** Text with a required style from [NovilaTheme.typography], so text never falls back to defaults. */
@Composable
fun NovilaText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(text = text, style = style, modifier = modifier, color = color, textAlign = textAlign)
}

@Composable
fun NovilaIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(painter = painter, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

/** Card with an optional title. Content is laid out in a column with standard spacing. */
@Composable
fun NovilaCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = NovilaTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = NovilaTheme.colors.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(NovilaTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            if (title != null) {
                NovilaText(title, style = NovilaTheme.typography.titleMedium)
            }
            content()
        }
    }
}

/** Centered placeholder for screens without content yet. */
@Composable
fun NovilaEmptyState(
    icon: Painter,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(NovilaTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        NovilaIcon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = NovilaTheme.colors.primary,
        )
        NovilaText(title, style = NovilaTheme.typography.titleLarge, textAlign = TextAlign.Center)
        NovilaText(
            body,
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
