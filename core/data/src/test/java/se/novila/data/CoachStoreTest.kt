package se.novila.data

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPreferences
import se.novila.coach.CoachPreferencesCodec
import se.novila.coach.RehabArea
import se.novila.exercise.ExerciseLog
import se.novila.exercise.ExerciseResult
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CoachStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val rehab = CoachPreferences(goal = CoachGoal.Rehab, rehabArea = RehabArea.LowerBack, rehabPhase = 2)

    @After fun close() = stores.close()

    @Test fun `answers saved in SharedPreferences are moved over`() {
        context.getSharedPreferences("coach", Context.MODE_PRIVATE).edit()
            .putString("preferences", CoachPreferencesCodec.encode(rehab))
            .commit()

        assertEquals(rehab, CoachStore(context, stores.scope, stores.cipher).answers.await().preferences)
        assertTrue(context.getSharedPreferences("coach", Context.MODE_PRIVATE).all.isEmpty())
    }

    @Test fun `no answers until the coach is set up`() {
        assertNull(CoachStore(context, stores.scope, stores.cipher).answers.await().preferences)
    }

    @Test fun `saved answers survive a restart, and a reset forgets them`() {
        val store = CoachStore(context, stores.scope, stores.cipher)
        store.save(CoachPreferences(daysPerWeek = 2))
        store.save(rehab)
        store.answers.await { it.preferences == rehab }

        stores.restart()
        val restarted = CoachStore(context, stores.scope, stores.cipher)
        assertEquals(rehab, restarted.answers.await().preferences)

        restarted.clear()
        restarted.answers.await { it.preferences == null }
    }

    @Test fun `exercise results survive a restart and are forgotten when cleared`() {
        val store = CoachStore(context, stores.scope, stores.cipher)
        val date = LocalDate.of(2026, 10, 22)
        val results = listOf(
            ExerciseResult("squat", date, 10, listOf(10, 9)),
            ExerciseResult("plank", date, 30, listOf(30)),
        )
        store.recordSession(results)
        store.progress.await { it == ExerciseLog(results) }

        stores.restart()
        val reopened = CoachStore(context, stores.scope, stores.cipher)
        assertEquals(ExerciseLog(results), reopened.progress.await())

        reopened.clear()
        reopened.progress.await { it == ExerciseLog() }
    }
}
