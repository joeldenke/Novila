package se.novila.feature.settings

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaSwitchRow
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTimeDialog
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.model.Reminder
import se.novila.model.ReminderSetting
import se.novila.model.Reminders
import se.novila.ui.common.ScreenList
import se.novila.ui.common.timeText
import java.time.LocalTime
import se.novila.ui.R as UiR

/** What the Reminders screen can do. The entry connects these to the ViewModel and to the system. */
data class RemindersActions(
    val onToggle: (Reminder, Boolean) -> Unit = { _, _ -> },
    val onTime: (Reminder, LocalTime) -> Unit = { _, _ -> },
    val onOpenNotificationSettings: () -> Unit = {},
)

/**
 * The reminders, each in its part's color: on or off, and at what time. When the user has turned a reminder on but
 * Novila may not notify ([notificationsOff]), a card says so and leads to the phone's settings.
 */
@Composable
fun RemindersScreen(
    reminders: Reminders,
    notificationsOff: Boolean,
    actions: RemindersActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") { Body(stringResource(R.string.reminders_intro)) }
        if (notificationsOff) {
            item(key = "off") { NotificationsOffCard(actions.onOpenNotificationSettings) }
        }
        Reminder.entries.forEach { reminder ->
            item(key = reminder.name) { ReminderCard(reminder, reminders[reminder], actions) }
        }
    }
}

@Composable
private fun NotificationsOffCard(onOpenNotificationSettings: () -> Unit) {
    NovilaCard(title = stringResource(R.string.reminders_off_title)) {
        Body(stringResource(R.string.reminders_off_body))
        NovilaButton(
            text = stringResource(R.string.reminders_open_settings),
            onClick = onOpenNotificationSettings,
        )
    }
}

@Composable
private fun ReminderCard(reminder: Reminder, setting: ReminderSetting, actions: RemindersActions) {
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    val role = reminder.role()
    NovilaCard(
        title = stringResource(reminder.title),
        icon = painterResource(reminder.icon),
        role = role,
    ) {
        Body(stringResource(reminder.summary))
        NovilaSwitchRow(
            title = stringResource(R.string.reminders_on),
            checked = setting.on,
            onCheckedChange = { actions.onToggle(reminder, it) },
            role = role,
        )
        if (setting.on) {
            NovilaListRow(
                title = stringResource(R.string.reminders_time),
                onClick = { choosingTime = true },
                trailing = { NovilaText(timeText(setting.time), style = NovilaTheme.typography.titleMedium) },
            )
        }
    }
    if (choosingTime) {
        NovilaTimeDialog(
            title = stringResource(R.string.reminders_time_title),
            hour = setting.time.hour,
            minute = setting.time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
            confirmLabel = stringResource(R.string.reminders_save),
            dismissLabel = stringResource(R.string.settings_cancel),
            onConfirm = { hour, minute ->
                choosingTime = false
                actions.onTime(reminder, LocalTime.of(hour, minute))
            },
            onDismiss = { choosingTime = false },
            role = role,
        )
    }
}

@Composable
private fun Reminder.role(): NovilaColorRole = when (this) {
    Reminder.Sleep -> NovilaTheme.pillars.sleep
    Reminder.Training -> NovilaTheme.pillars.training
    Reminder.Nutrition -> NovilaTheme.pillars.nutrition
}

private val Reminder.title: Int
    get() = when (this) {
        Reminder.Sleep -> R.string.reminder_sleep
        Reminder.Training -> R.string.reminder_training
        Reminder.Nutrition -> R.string.reminder_nutrition
    }

private val Reminder.summary: Int
    get() = when (this) {
        Reminder.Sleep -> R.string.reminder_sleep_summary
        Reminder.Training -> R.string.reminder_training_summary
        Reminder.Nutrition -> R.string.reminder_nutrition_summary
    }

private val Reminder.icon: Int
    get() = when (this) {
        Reminder.Sleep -> UiR.drawable.ic_sleep
        Reminder.Training -> UiR.drawable.ic_training
        Reminder.Nutrition -> UiR.drawable.ic_nutrition
    }

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

@Composable
private fun RemindersContent(reminders: Reminders, notificationsOff: Boolean = false) {
    NovilaTheme {
        NovilaScaffold { padding ->
            RemindersScreen(
                reminders = reminders,
                notificationsOff = notificationsOff,
                actions = RemindersActions(),
                contentPadding = padding,
            )
        }
    }
}

@Preview(heightDp = 1200)
@Composable
private fun RemindersPreview() = RemindersContent(
    Reminders()
        .with(Reminder.Sleep, ReminderSetting(on = true, time = LocalTime.of(7, 30)))
        .with(Reminder.Nutrition, ReminderSetting(on = true, time = Reminder.Nutrition.defaultTime)),
)

/** Turned on, but notifications are off for Novila. */
@Preview(heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun RemindersNotificationsOffDarkPreview() = RemindersContent(
    reminders = Reminders().with(Reminder.Training, ReminderSetting(on = true, time = Reminder.Training.defaultTime)),
    notificationsOff = true,
)
