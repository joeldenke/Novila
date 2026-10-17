package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Reminder
import se.novila.model.ReminderSetting
import se.novila.model.Reminders

@RunWith(RobolectricTestRunner::class)
class RemindersScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(reminders: Reminders, notificationsOff: Boolean = false, actions: RemindersActions) {
        composeRule.setContent {
            NovilaTheme {
                RemindersScreen(reminders, notificationsOff, actions, contentPadding = PaddingValues())
            }
        }
    }

    @Test fun `only reminders that are on show a time, and the switch goes to the entry`() {
        val toggled = mutableListOf<Pair<Reminder, Boolean>>()
        val sleepOn = Reminders().with(Reminder.Sleep, ReminderSetting(on = true, time = Reminder.Sleep.defaultTime))
        show(sleepOn, actions = RemindersActions(onToggle = { reminder, on -> toggled += reminder to on }))

        assertEquals(1, composeRule.onAllNodesWithText("Time").fetchSemanticsNodes().size)
        composeRule.onAllNodesWithText("Remind me")[0].performClick()
        assertEquals(listOf(Reminder.Sleep to false), toggled)
    }

    @Test fun `notifications that are off lead to the phone's settings`() {
        var opened = false
        show(
            Reminders(),
            notificationsOff = true,
            actions = RemindersActions(onOpenNotificationSettings = { opened = true }),
        )

        composeRule.onNodeWithText("Notifications are off").assertExists()
        composeRule.onNodeWithText("Open settings").performClick()
        assertEquals(true, opened)
    }
}
