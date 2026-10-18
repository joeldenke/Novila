package se.novila.feature.training

import se.novila.exercise.TrainingProgram

/**
 * The week's sessions against the plan, for the weekly review: [done] of the [planned] sessions have training
 * logged on their day, and [nextWeek]'s program says what the training leads to.
 */
data class WeeklyReview(val planned: Int, val done: Int, val nextWeek: TrainingProgram)

/**
 * The weekly review for the Training tab: how many of this week's planned sessions are done, and what next week
 * holds. Null until the first planned session is done (nothing to review yet) or when the program is empty.
 * Training on unplanned days doesn't count here: the review is about keeping the plan.
 */
fun TrainingUiState.weeklyReview(): WeeklyReview? {
    val nextWeek = nextProgram ?: return null
    val planned = program.sessions.size
    if (planned == 0) return null
    val done = program.sessions.count { (weekDone[it.day] ?: 0) > 0 }
    return if (done == 0) null else WeeklyReview(planned, done, nextWeek)
}
