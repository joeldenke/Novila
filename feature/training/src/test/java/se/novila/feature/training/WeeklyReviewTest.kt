package se.novila.feature.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class WeeklyReviewTest {
    private val state = TrainingUiState(
        program = TrainingPreviewData.program,
        coach = TrainingPreviewData.newCoach,
        today = DayOfWeek.THURSDAY,
        strengthMinutesToday = 0,
        weekDone = mapOf(DayOfWeek.MONDAY to 28, DayOfWeek.TUESDAY to 32),
        nextProgram = TrainingPreviewData.nextProgram,
    )

    @Test fun `nothing to review before the first session or without next week`() {
        assertNull(state.copy(weekDone = emptyMap()).weeklyReview())
        assertNull(state.copy(nextProgram = null).weeklyReview())
        assertNull(state.copy(program = state.program.copy(sessions = emptyList())).weeklyReview())
    }

    @Test fun `the review counts the planned sessions that are done`() {
        val review = state.weeklyReview()!!
        assertEquals(state.program.sessions.size, review.planned)
        assertEquals(2, review.done)
        assertEquals(TrainingPreviewData.nextProgram, review.nextWeek)
    }

    @Test fun `training on a rest day is not a planned session`() {
        val rest = DayOfWeek.entries.first { day -> state.program.sessions.none { it.day == day } }
        val extra = state.copy(weekDone = state.weekDone + (rest to 20))
        assertEquals(2, extra.weeklyReview()!!.done)
    }

    @Test fun `every planned session done`() {
        val all = state.program.sessions.associate { it.day to 30 }
        val review = state.copy(weekDone = all).weeklyReview()!!
        assertEquals(review.planned, review.done)
    }
}
