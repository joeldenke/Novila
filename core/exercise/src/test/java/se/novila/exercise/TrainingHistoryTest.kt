package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

class TrainingHistoryTest {
    private val monday = LocalDate.of(2026, 10, 12)
    private val tuesday = monday.plusDays(1)

    @Test fun `a logged walk and the watch's recording of it count once`() {
        val logged = Exercise(ExerciseType.Walking, monday, 30.minutes)
        val recorded = Exercise(ExerciseType.Walking, monday, 34.minutes)
        assertEquals(listOf(recorded), TrainingHistory.merge(listOf(logged), listOf(recorded)))
        assertEquals(34, TrainingHistory.minutes(listOf(logged), listOf(recorded), monday, ExerciseKind.Cardio))
    }

    @Test fun `the side with more time counts`() {
        val logged = listOf(Exercise(ExerciseType.Running, monday, 45.minutes))
        val recorded = listOf(Exercise(ExerciseType.Walking, monday, 20.minutes))
        assertEquals(logged, TrainingHistory.merge(logged, recorded))
    }

    @Test fun `different days and kinds are all kept, oldest first`() {
        val strength = Exercise(ExerciseType.Strength, monday, 25.minutes)
        val walk = Exercise(ExerciseType.Walking, monday, 30.minutes)
        val ride = Exercise(ExerciseType.Cycling, tuesday, 40.minutes)
        assertEquals(listOf(strength, walk, ride), TrainingHistory.merge(listOf(ride, strength), listOf(walk)))
        assertEquals(0, TrainingHistory.minutes(listOf(strength), emptyList(), monday, ExerciseKind.Cardio))
    }

    @Test fun `the watch's recording keeps the effort the user gave for the same walk`() {
        val logged = listOf(
            Exercise(ExerciseType.Walking, monday, 20.minutes, effort = 3),
            Exercise(ExerciseType.Walking, monday, 10.minutes, effort = 6),
        )
        val recorded = listOf(Exercise(ExerciseType.Walking, monday, 34.minutes))
        // Weighted by minutes: (20 × 3 + 10 × 6) / 30 = 4.
        assertEquals(listOf(recorded.single().copy(effort = 4)), TrainingHistory.merge(logged, recorded))
        // Without an effort, the recording stays as it was.
        val unrated = listOf(Exercise(ExerciseType.Walking, monday, 30.minutes))
        assertEquals(recorded, TrainingHistory.merge(unrated, recorded))
    }
}
