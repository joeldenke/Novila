package se.novila.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
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

/**
 * Card with an optional title. Content is laid out in a column with standard spacing.
 * With an [icon], the title gets a badge in the [role] color, so each part of the app is
 * recognisable at a glance (training = pine, sleep = lake blue, nutrition = falu red).
 */
@Composable
fun NovilaCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: Painter? = null,
    role: NovilaColorRole = NovilaTheme.pillars.training,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
                ) {
                    if (icon != null) {
                        IconBadge(icon, role, size = 36.dp, iconSize = 20.dp)
                    }
                    NovilaText(title, style = NovilaTheme.typography.titleMedium)
                }
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
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(NovilaTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        IconBadge(icon, role, size = 80.dp, iconSize = 40.dp)
        NovilaText(title, style = NovilaTheme.typography.titleLarge, textAlign = TextAlign.Center)
        NovilaText(
            body,
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Decorative icon on a circle in the role's container color (contrast checked by ColorContrastTest). */
@Composable
private fun IconBadge(
    icon: Painter,
    role: NovilaColorRole,
    size: Dp,
    iconSize: Dp,
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(role.container),
        contentAlignment = Alignment.Center,
    ) {
        NovilaIcon(painter = icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = role.color)
    }
}
