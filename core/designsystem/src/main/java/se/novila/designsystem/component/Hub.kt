package se.novila.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/**
 * How a tab shows the ways into its parts ([NovilaEntries]): as tiles in a grid, or as rows in one card, like
 * Settings. The user picks it under Settings; the theme carries it, so every tab follows at once.
 */
enum class NovilaEntryLayout { Grid, Rows }

internal val LocalNovilaEntryLayout = staticCompositionLocalOf { NovilaEntryLayout.Grid }

/**
 * One way into a part of a tab: a [title], what it stands at in short ([summary]) and where a tap goes ([onClick]),
 * with an [icon] in the [role] color of the part it belongs to.
 */
@Immutable
data class NovilaEntry(
    val title: String,
    val icon: Painter,
    val role: NovilaColorRole,
    val onClick: () -> Unit,
    val summary: String? = null,
)

/** A tile is at least this wide, so a phone fits two and a tablet four. */
private val MinTileWidth = 160.dp

/**
 * The ways into a tab's parts, below its overview, so the tab stays short and every part is one tap away. Laid out
 * as the user chose ([NovilaTheme.entryLayout]): tiles fill as many columns as the width allows (two on a phone, up
 * to four on a tablet or unfolded phone, a last odd tile takes the whole row), rows stack in one card.
 */
@Composable
fun NovilaEntries(entries: List<NovilaEntry>, modifier: Modifier = Modifier) {
    when (NovilaTheme.entryLayout) {
        NovilaEntryLayout.Grid -> EntryGrid(entries, modifier)
        NovilaEntryLayout.Rows -> EntryRows(entries, modifier)
    }
}

@Composable
private fun EntryGrid(entries: List<NovilaEntry>, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val spacing = NovilaTheme.spacing.m
        val columns = ((maxWidth + spacing) / (MinTileWidth + spacing)).toInt().coerceIn(2, 4)
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            entries.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { entry -> EntryTile(entry, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun EntryTile(entry: NovilaEntry, modifier: Modifier) {
    Surface(
        onClick = entry.onClick,
        modifier = modifier,
        shape = NovilaTheme.shapes.medium,
        color = NovilaTheme.surfaces.card,
        contentColor = NovilaTheme.colors.onSurface,
        border = BorderStroke(1.dp, NovilaTheme.surfaces.cardOutline),
    ) {
        Column(
            modifier = Modifier.padding(NovilaTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(entry.icon, entry.role, size = 36.dp, iconSize = 20.dp)
                Spacer(Modifier.weight(1f))
                Chevron()
            }
            NovilaText(entry.title, style = NovilaTheme.typography.titleSmall)
            if (entry.summary != null) {
                NovilaText(
                    entry.summary,
                    style = NovilaTheme.typography.bodySmall,
                    color = NovilaTheme.colors.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun EntryRows(entries: List<NovilaEntry>, modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = NovilaTheme.shapes.medium,
        color = NovilaTheme.surfaces.card,
        contentColor = NovilaTheme.colors.onSurface,
        border = BorderStroke(1.dp, NovilaTheme.surfaces.cardOutline),
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = NovilaTheme.spacing.l + 36.dp + NovilaTheme.spacing.m),
                        color = NovilaTheme.surfaces.cardOutline,
                    )
                }
                EntryRow(entry)
            }
        }
    }
}

@Composable
private fun EntryRow(entry: NovilaEntry) {
    Surface(
        onClick = entry.onClick,
        modifier = Modifier.fillMaxWidth(),
        color = NovilaTheme.surfaces.card,
        contentColor = NovilaTheme.colors.onSurface,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = NovilaTheme.spacing.l, vertical = NovilaTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
        ) {
            IconBadge(entry.icon, entry.role, size = 36.dp, iconSize = 20.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs),
            ) {
                NovilaText(entry.title, style = NovilaTheme.typography.bodyLarge)
                if (entry.summary != null) {
                    NovilaText(
                        entry.summary,
                        style = NovilaTheme.typography.bodyMedium,
                        color = NovilaTheme.colors.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            Chevron()
        }
    }
}

/** A drawn chevron pointing to where a tap goes, mirrored for right-to-left languages. */
@Composable
private fun Chevron() {
    val color = NovilaTheme.colors.onSurfaceVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(16.dp)) {
        val stroke = 2.dp.toPx()
        val left = size.width * 0.35f
        val right = size.width * 0.65f
        val (from, tip) = if (rtl) right to left else left to right
        drawLine(color, Offset(from, size.height * 0.2f), Offset(tip, size.height / 2), stroke, StrokeCap.Round)
        drawLine(color, Offset(tip, size.height / 2), Offset(from, size.height * 0.8f), stroke, StrokeCap.Round)
    }
}

/**
 * Where something is logged or changed without leaving the tab: a sheet that slides up over it with a [title] and
 * the [content], scrolling when it is taller than the screen. [onDismiss] runs on a swipe down, back or a tap
 * outside. It opens fully, so a form is never half hidden, and is no wider than a phone on a tablet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovilaBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = NovilaTheme.shapes.extraLarge,
        containerColor = NovilaTheme.colors.surface,
        contentColor = NovilaTheme.colors.onSurface,
    ) {
        NovilaSheetContent(title, content)
    }
}

/**
 * What a [NovilaBottomSheet] shows, on its own: the [title] and the [content] in a scrolling column. Previews and
 * screenshots draw this, since a sheet opens in its own window.
 */
@Composable
fun NovilaSheetContent(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = NovilaTheme.spacing.screenMargin,
                end = NovilaTheme.spacing.screenMargin,
                bottom = NovilaTheme.spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        NovilaText(title, style = NovilaTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        content()
    }
}

/**
 * The tab's quick actions below its overview, side by side in a card under a [title], so no button stands loose on
 * the tab: give each [NovilaButton] `Modifier.weight(1f)` and `fillMaxHeight()` so they share the width equally and
 * stay as tall as the tallest, when a label wraps. With an [icon], the title gets a badge in the [role] color.
 */
@Composable
fun NovilaQuickActions(
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    role: NovilaColorRole = NovilaTheme.pillars.training,
    content: @Composable RowScope.() -> Unit,
) {
    NovilaCard(modifier = modifier, title = title, icon = icon, role = role) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            content = content,
        )
    }
}
