package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.DayEntry
import se.novila.MAX_SLEEP_HOURS
import se.novila.R
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaLevelPicker
import se.novila.designsystem.component.NovilaSlider
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.formatHours
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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

/** How the user feels today, on a calm five-step scale from bad to great. */
@Composable
fun MoodCard(mood: Int, onChange: (Int) -> Unit) {
    NovilaCard(title = stringResource(R.string.mood_title)) {
        NovilaLevelPicker(
            levels = Mood.entries.map { stringResource(it.label) },
            selected = mood,
            onSelect = onChange,
            hint = stringResource(R.string.mood_hint),
            role = NovilaTheme.pillars.training,
        )
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

@Composable
@ReadOnlyComposable
fun hoursText(hours: Float): String {
    val value = formatHours(hours, currentLocale()) ?: return stringResource(R.string.value_missing)
    return stringResource(R.string.hours_value, value)
}

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

/** Short weekday per day for chart axes, e.g. "Mon" / "Mån". */
@Composable
fun weekdayLabels(days: List<DayEntry>): List<String> {
    val locale = currentLocale()
    return remember(days, locale) {
        days.map { day ->
            day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                .trimEnd('.')
                .replaceFirstChar { it.titlecase(locale) }
        }
    }
}

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

/** Mood levels 1–5, in order from bad to great. */
private enum class Mood(val label: Int) {
    Bad(R.string.mood_1),
    NotGreat(R.string.mood_2),
    Okay(R.string.mood_3),
    Good(R.string.mood_4),
    Great(R.string.mood_5),
}
