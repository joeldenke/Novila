package se.novila.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.CoachUiState
import se.novila.R
import se.novila.coach.BodyArea
import se.novila.coach.CoachGoal
import se.novila.coach.CoachNote
import se.novila.coach.CoachPreferences
import se.novila.coach.Consideration
import se.novila.coach.RehabArea
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.StrengthLevel

/** The way into the coach from the Training tab, with what it has set up so far. */
@Composable
fun CoachCard(coach: CoachUiState, onOpen: () -> Unit) {
    val preferences = coach.preferences
    NovilaCard(
        title = stringResource(R.string.coach_title),
        icon = painterResource(R.drawable.ic_training),
        role = NovilaTheme.pillars.training,
    ) {
        NovilaText(
            if (coach.active) {
                stringResource(
                    R.string.coach_card_active,
                    stringResource(preferences.goal.label),
                    preferences.daysPerWeek,
                    preferences.sessionMinutes,
                )
            } else {
                stringResource(R.string.coach_card_inactive)
            },
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaButton(
            stringResource(if (coach.active) R.string.coach_adjust else R.string.coach_open),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

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
