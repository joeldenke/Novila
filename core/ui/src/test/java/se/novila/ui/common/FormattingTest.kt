package se.novila.ui.common

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The shared formatters, which every screen's numbers and durations go through. */
@RunWith(RobolectricTestRunner::class)
class FormattingTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `duration is rounded to the nearest five minutes`() {
        compose.setContent {
            // The duration strings are translatable=false, so the digits are the same in every locale.
            assertEquals("0 min", durationText(0))
            assertEquals("5 min", durationText(3))
            assertEquals("35 min", durationText(37))
            assertEquals("1 h", durationText(62))
            assertEquals("1 h 15 min", durationText(76))
            assertEquals("2 h", durationText(118))
        }
    }
}
