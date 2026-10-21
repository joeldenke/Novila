package se.novila.feature.breathing

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.novila.breathing.BreathPhase
import se.novila.breathing.BreathingSession

/** The session's name, from its id, so the catalog stays free of Android strings. */
internal fun sessionName(session: BreathingSession): Int = when (session.id) {
    "calm" -> R.string.breathing_session_calm
    "box" -> R.string.breathing_session_box
    "four-seven-eight" -> R.string.breathing_session_four_seven_eight
    else -> R.string.breathing_session_calm
}

/** What the session does, in one calm sentence. */
internal fun sessionBody(session: BreathingSession): Int = when (session.id) {
    "calm" -> R.string.breathing_session_calm_body
    "box" -> R.string.breathing_session_box_body
    "four-seven-eight" -> R.string.breathing_session_four_seven_eight_body
    else -> R.string.breathing_session_calm_body
}

/** The name of the phase shown while breathing. Both holds read "Hold". */
internal val BreathPhase.label: Int
    get() = when (this) {
        BreathPhase.Inhale -> R.string.breathing_phase_inhale
        BreathPhase.HoldIn -> R.string.breathing_phase_hold_in
        BreathPhase.Exhale -> R.string.breathing_phase_exhale
        BreathPhase.HoldOut -> R.string.breathing_phase_hold_out
    }

/** The session's length as "About 1 min 20 s". */
@Composable
internal fun sessionDuration(session: BreathingSession): String {
    val minutes = session.totalSeconds / 60
    val seconds = session.totalSeconds % 60
    val value = when {
        minutes > 0 && seconds > 0 -> stringResource(R.string.breathing_duration_minutes_seconds, minutes, seconds)
        minutes > 0 -> stringResource(R.string.breathing_duration_minutes, minutes)
        else -> stringResource(R.string.breathing_duration_seconds, seconds)
    }
    return stringResource(R.string.breathing_duration_about, value)
}

/** A count of seconds as "1:16", for the time left of a session. */
@Composable
internal fun clockCountText(seconds: Int): String = stringResource(R.string.breathing_time, seconds / 60, seconds % 60)
