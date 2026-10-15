package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import se.novila.DayEntry
import se.novila.MAX_SLEEP_HOURS
import se.novila.R
import se.novila.WATER_GOAL
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaProgressBar
import se.novila.designsystem.component.NovilaSlider
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.formatHours
import se.novila.healthconnect.MovementState
import se.novila.healthconnect.TodayMovement
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Scrollable screen body. Insets from the scaffold go into contentPadding (edge-to-edge skill),
 * so content scrolls behind system bars without being hidden.
 */
@Composable
fun ScreenList(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val spacing = NovilaTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize().consumeWindowInsets(contentPadding),
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

@Composable
fun WaterCard(today: DayEntry, onAdd: () -> Unit, onRemove: () -> Unit) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(
        title = stringResource(R.string.water_title),
        icon = painterResource(R.drawable.ic_nutrition),
        role = nutrition,
    ) {
        NovilaText(
            stringResource(R.string.water_progress, today.waterGlasses, WATER_GOAL),
            style = NovilaTheme.typography.headlineSmall,
        )
        NovilaProgressBar(progress = today.waterProgress, role = nutrition)
        Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            NovilaButton(
                text = stringResource(R.string.water_remove_label),
                onClick = onRemove,
                enabled = today.waterGlasses > 0,
                contentDescription = stringResource(R.string.water_remove_description),
                role = nutrition,
            )
            // Logging a glass is the screen's main action, so it gets the sun.
            NovilaAccentButton(text = stringResource(R.string.water_add), onClick = onAdd)
        }
    }
}

@Composable
fun SleepCard(hours: Float, onChange: (Float) -> Unit) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(R.string.sleep_title),
        icon = painterResource(R.drawable.ic_sleep),
        role = sleep,
    ) {
        NovilaText(hoursText(hours), style = NovilaTheme.typography.headlineSmall)
        NovilaSlider(
            value = hours,
            onValueChange = onChange,
            valueRange = 0f..MAX_SLEEP_HOURS,
            steps = (MAX_SLEEP_HOURS * 2).toInt() - 1, // half-hour steps
            role = sleep,
        )
    }
}

@Composable
fun MoodCard(mood: Int, onChange: (Int) -> Unit) {
    NovilaCard(title = stringResource(R.string.mood_title), icon = painterResource(R.drawable.ic_today)) {
        Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs)) {
            Mood.entries.forEach { option ->
                NovilaChoiceChip(
                    label = stringResource(option.emoji),
                    selected = mood == option.value,
                    onClick = { onChange(option.value) },
                    contentDescription = stringResource(option.label),
                )
            }
        }
    }
}

/**
 * Today's steps and active calories from Health Connect, or what the user can do to see them.
 * Shows nothing on devices without Health Connect.
 */
@Composable
fun MovementCard(
    movement: MovementState,
    onConnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onInstall: () -> Unit,
) {
    if (movement == MovementState.Unavailable) return
    val training = NovilaTheme.pillars.training
    NovilaCard(
        title = stringResource(R.string.movement_title),
        icon = painterResource(R.drawable.ic_training),
        role = training,
    ) {
        when (movement) {
            is MovementState.Connected -> MovementValues(movement.today)

            is MovementState.NeedsPermission -> {
                MovementBody(stringResource(R.string.movement_connect_body))
                if (movement.denied) {
                    MovementBody(stringResource(R.string.movement_denied_body))
                    NovilaAccentButton(text = stringResource(R.string.movement_open_settings), onClick = onOpenSettings)
                } else {
                    NovilaAccentButton(text = stringResource(R.string.movement_connect), onClick = onConnect)
                }
            }

            MovementState.NeedsInstall -> {
                MovementBody(stringResource(R.string.movement_install_body))
                NovilaAccentButton(text = stringResource(R.string.movement_install), onClick = onInstall)
            }

            MovementState.Unavailable -> Unit
        }
    }
}

@Composable
private fun MovementValues(today: TodayMovement) {
    Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xl)) {
        MovementValue(stepsText(today.steps), stringResource(R.string.movement_steps))
        MovementValue(kcalText(today.activeKilocalories), stringResource(R.string.movement_active_calories))
    }
    NovilaText(
        stringResource(R.string.movement_source),
        style = NovilaTheme.typography.bodySmall,
        color = NovilaTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun MovementValue(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(value, style = NovilaTheme.typography.headlineSmall)
        NovilaText(label, style = NovilaTheme.typography.labelMedium, color = NovilaTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun MovementBody(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

/** One row per day: date followed by the given values, aligned in columns. */
@Composable
fun HistoryRow(day: DayEntry, vararg values: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = NovilaTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l),
    ) {
        NovilaText(
            day.date.format(rememberShortDateFormatter()),
            style = NovilaTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        values.forEach { NovilaText(it, style = NovilaTheme.typography.bodyMedium) }
    }
}

@Composable
@ReadOnlyComposable
fun hoursText(hours: Float): String {
    val value = formatHours(hours, currentLocale()) ?: return stringResource(R.string.value_missing)
    return stringResource(R.string.hours_value, value)
}

@Composable
@ReadOnlyComposable
fun glassesText(count: Int): String = pluralStringResource(R.plurals.glasses_count, count, count)

@Composable
@ReadOnlyComposable
fun moodText(mood: Int): String =
    Mood.of(mood)?.let { stringResource(it.emoji) } ?: stringResource(R.string.value_missing)

/** Step count with the locale's digit grouping, e.g. "8,412" / "8 412". */
@Composable
fun stepsText(steps: Long?): String =
    steps?.let { rememberIntegerFormat().format(it) } ?: stringResource(R.string.value_missing)

@Composable
fun kcalText(kilocalories: Int?): String = kilocalories?.let {
    stringResource(R.string.kcal_value, rememberIntegerFormat().format(it))
} ?: stringResource(R.string.value_missing)

@Composable
private fun rememberIntegerFormat(): NumberFormat {
    val locale = currentLocale()
    return remember(locale) { NumberFormat.getIntegerInstance(locale) }
}

@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale =
    // Read straight from LocalConfiguration (observable) so text updates when the app language changes.
    LocalConfiguration.current.locales[0]

/** Localized weekday and date, e.g. "Tue 13 Oct" / "tis 13 okt". */
@Composable
private fun rememberShortDateFormatter(): DateTimeFormatter = rememberDateFormatter("EEEdMMM")

/** Full weekday and date with a capital first letter, e.g. "Wednesday, October 14" / "Onsdag 14 oktober". */
@Composable
fun longDateText(date: LocalDate): String {
    val locale = currentLocale()
    return date.format(rememberDateFormatter("EEEEdMMMM")).replaceFirstChar { it.titlecase(locale) }
}

@Composable
private fun rememberDateFormatter(skeleton: String): DateTimeFormatter {
    val locale = currentLocale()
    return remember(locale, skeleton) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
        DateTimeFormatter.ofPattern(pattern, locale)
    }
}

private enum class Mood(val value: Int, val emoji: Int, val label: Int) {
    Bad(1, R.string.mood_emoji_1, R.string.mood_1),
    NotGreat(2, R.string.mood_emoji_2, R.string.mood_2),
    Okay(3, R.string.mood_emoji_3, R.string.mood_3),
    Good(4, R.string.mood_emoji_4, R.string.mood_4),
    Great(5, R.string.mood_emoji_5, R.string.mood_5),
    ;

    companion object {
        fun of(value: Int): Mood? = entries.firstOrNull { it.value == value }
    }
}
