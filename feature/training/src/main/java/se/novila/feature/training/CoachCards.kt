package se.novila.feature.training

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.novila.coach.BodyArea
import se.novila.coach.CoachFeedback
import se.novila.coach.CoachGoal
import se.novila.coach.CoachNote
import se.novila.coach.CoachPreferences
import se.novila.coach.Consideration
import se.novila.coach.EffortAdvice
import se.novila.coach.EffortCheck
import se.novila.coach.RehabAdvice
import se.novila.coach.RehabArea
import se.novila.coach.RehabCheck
import se.novila.exercise.StrengthLevel
import se.novila.feature.training.R

/** One of the coach's reasons as a sentence. */
@Composable
fun coachNoteText(note: CoachNote): String = when (note) {
    is CoachNote.Goal -> stringResource(
        when (note.goal) {
            CoachGoal.Health -> R.string.coach_note_goal_health
            CoachGoal.Strength -> R.string.coach_note_goal_strength
            CoachGoal.Endurance -> R.string.coach_note_goal_endurance
            CoachGoal.Mobility -> R.string.coach_note_goal_mobility
            CoachGoal.Rehab -> R.string.coach_note_goal_rehab
        },
    )

    is CoachNote.Level -> stringResource(
        when (note.level) {
            StrengthLevel.Beginner -> R.string.coach_note_level_beginner
            StrengthLevel.Intermediate -> R.string.coach_note_level_intermediate
            StrengthLevel.Advanced -> R.string.coach_note_level_advanced
        },
    )

    CoachNote.RestBetweenStrengthDays -> stringResource(R.string.coach_note_rest)

    is CoachNote.Focus -> stringResource(
        R.string.coach_note_focus,
        muscleList(BodyArea.entries.filter { it in note.areas }.flatMap { it.muscles }.toSet()),
    )

    is CoachNote.Considered -> stringResource(
        when (note.consideration) {
            Consideration.Knees -> R.string.coach_note_knees
            Consideration.Back -> R.string.coach_note_back
            Consideration.Quiet -> R.string.coach_note_quiet
        },
    )

    is CoachNote.CardioBelowGuideline -> stringResource(R.string.coach_note_cardio_low, note.minutes)

    CoachNote.CardioProgression -> stringResource(R.string.coach_note_cardio_progression)

    is CoachNote.RehabPhase -> stringResource(
        R.string.coach_note_rehab_phase,
        stringResource(note.area.label),
        note.phase,
        CoachPreferences.MAX_REHAB_PHASE,
    )

    CoachNote.PainGuide -> stringResource(R.string.coach_note_pain)

    is CoachNote.Effort -> effortNoteText(note.check)

    is CoachNote.Rehab -> rehabNoteText(note.check)
}

@Composable
private fun effortNoteText(check: EffortCheck): String {
    val pain = check.pain
    val effort = check.effort ?: 0
    return when {
        pain != null -> stringResource(R.string.coach_note_effort_pain, pain)
        check.advice == EffortAdvice.Easier -> stringResource(R.string.coach_note_effort_easier, effort)
        check.advice == EffortAdvice.Harder -> stringResource(R.string.coach_note_effort_harder, effort)
        else -> stringResource(R.string.coach_note_effort_same, effort)
    }
}

@Composable
private fun rehabNoteText(check: RehabCheck): String {
    val pain = check.pain ?: 0
    return when (check.advice) {
        RehabAdvice.MoveOn -> stringResource(R.string.coach_note_rehab_move_on, check.calmSessions, check.phase)

        RehabAdvice.KeepGoing -> stringResource(
            R.string.coach_note_rehab_keep_going,
            check.calmSessions,
            CoachFeedback.CALM_SESSIONS,
        )

        RehabAdvice.Stay -> stringResource(R.string.coach_note_rehab_stay, pain)

        RehabAdvice.StepBack -> stringResource(R.string.coach_note_rehab_step_back, pain, check.phase)

        RehabAdvice.SeekCare -> stringResource(R.string.coach_note_rehab_seek_care, pain)

        RehabAdvice.Done -> stringResource(R.string.coach_note_rehab_done, check.calmSessions)
    }
}

@get:StringRes
val CoachGoal.label: Int
    get() = when (this) {
        CoachGoal.Health -> R.string.coach_goal_health
        CoachGoal.Strength -> R.string.coach_goal_strength
        CoachGoal.Endurance -> R.string.coach_goal_endurance
        CoachGoal.Mobility -> R.string.coach_goal_mobility
        CoachGoal.Rehab -> R.string.coach_goal_rehab
    }

@get:StringRes
val RehabArea.label: Int
    get() = when (this) {
        RehabArea.Knee -> R.string.coach_rehab_knee
        RehabArea.LowerBack -> R.string.coach_rehab_lower_back
        RehabArea.Shoulder -> R.string.coach_rehab_shoulder
        RehabArea.Neck -> R.string.coach_rehab_neck
        RehabArea.Hip -> R.string.coach_rehab_hip
    }

/** What a rehab [phase] is about, in a few words. */
@StringRes
fun rehabPhaseLabel(phase: Int): Int = when (phase) {
    1 -> R.string.coach_rehab_phase_1
    2 -> R.string.coach_rehab_phase_2
    else -> R.string.coach_rehab_phase_3
}

@get:StringRes
val BodyArea.label: Int
    get() = when (this) {
        BodyArea.Legs -> R.string.coach_area_legs
        BodyArea.Core -> R.string.coach_area_core
        BodyArea.UpperBody -> R.string.coach_area_upper_body
    }

@get:StringRes
val Consideration.label: Int
    get() = when (this) {
        Consideration.Knees -> R.string.coach_consider_knees
        Consideration.Back -> R.string.coach_consider_back
        Consideration.Quiet -> R.string.coach_consider_quiet
    }
