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
import se.novila.model.glassesOfWater
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class WidgetContentTest {
    private val june = LocalDate.of(2026, 6, 10)

    private fun show(
        day: DayEntry?,
        goals: Goals? = Goals(),
        size: DpSize = DpSize(300.dp, 220.dp),
        check: Check,
    ) = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(size)
        setContext(RuntimeEnvironment.getApplication())
        provideComposable { NovilaGlanceTheme { WidgetContent(day, goals, hour = 12) } }
        check()
    }

    @Test fun `shows the balance and the parts of the day`() = show(
        DayEntry(june, sleepHours = 7.5f, drinks = glassesOfWater(3)),
        Goals(waterGlasses = 8),
    ) {
        onNode(hasText("Today's balance")).assertExists()
        onNode(hasText("7.5 h")).assertExists()
        onNode(hasText("3/8")).assertExists()
    }

    @Test fun `has no way to log water`() = show(
        DayEntry(june, sleepHours = 7.5f, drinks = glassesOfWater(3)),
        Goals(waterGlasses = 8),
    ) {
        onNode(hasText("+ Glass of water")).assertDoesNotExist()
    }

    @Test fun `a summer walk cheers with a word about vitamin D`() = show(
        DayEntry(june, sleepHours = 7f).withCardio(CardioLog(CardioType.Walking, 30)),
    ) {
        onNode(hasText("vitamin D")).assertExists()
    }

    @Test fun `nothing logged asks for the night and the food`() = show(DayEntry(june)) {
        onNode(hasText("Log last night")).assertExists()
    }

    @Test fun `a small widget keeps the rings without the legend and the encouragement`() = show(
        DayEntry(june),
        size = DpSize(180.dp, 140.dp),
    ) {
        onNode(hasText("Today's balance")).assertExists()
        onNode(hasText("Sleep")).assertDoesNotExist()
        onNode(hasText("Log last night")).assertDoesNotExist()
    }

    @Test fun `a low widget keeps the legend but leaves out the encouragement`() = show(
        DayEntry(june),
        size = DpSize(320.dp, 160.dp),
    ) {
        onNode(hasText("Sleep")).assertExists()
        onNode(hasText("Log last night")).assertDoesNotExist()
    }

    @Test fun `nothing but the background before the stores are read`() = show(day = null) {
        onNode(hasText("Today's balance")).assertDoesNotExist()
    }
}

private typealias Check = GlanceAppWidgetUnitTest.() -> Unit
