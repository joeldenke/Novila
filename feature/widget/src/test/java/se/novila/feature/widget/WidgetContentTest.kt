package se.novila.feature.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.designsystem.glance.NovilaGlanceTheme
import se.novila.model.CardioLog
import se.novila.model.CardioType
import se.novila.model.DayEntry
import se.novila.model.Goals
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class WidgetContentTest {
    private val june = LocalDate.of(2026, 6, 10)

    private fun show(day: DayEntry?, goals: Goals? = Goals(), check: Check) = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(300.dp, 220.dp))
        setContext(RuntimeEnvironment.getApplication())
        provideComposable { NovilaGlanceTheme { WidgetContent(day, goals) } }
        check()
    }

    @Test fun `shows the parts of the day and a glass of water`() = show(
        DayEntry(june, sleepHours = 7.5f, waterGlasses = 3),
        Goals(waterGlasses = 8),
    ) {
        onNode(hasText("Today's balance")).assertExists()
        onNode(hasText("7.5 h")).assertExists()
        onNode(hasText("3/8")).assertExists()
        onNode(hasText("+ Glass of water")).assertExists()
    }

    @Test fun `a summer walk is the latest log, with a word about vitamin D`() = show(
        DayEntry(june, sleepHours = 7f).withCardio(CardioLog(CardioType.Walking, 30)),
    ) {
        onNode(hasText("Latest: walk, 30 min")).assertExists()
        onNode(hasText("vitamin D", substring = true)).assertExists()
    }

    @Test fun `nothing logged asks for the night and the food`() = show(DayEntry(june)) {
        onNode(hasText("Log last night", substring = true)).assertExists()
        onNode(hasText("Latest:", substring = true)).assertDoesNotExist()
    }

    @Test fun `nothing but the background before the stores are read`() = show(day = null) {
        onNode(hasText("Today's balance")).assertDoesNotExist()
    }
}

private typealias Check = GlanceAppWidgetUnitTest.() -> Unit
