package se.novila.feature.training

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.anatomy.ExerciseLibrary
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.training.R
import se.novila.ui.common.ScreenList
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * One day's strength or rehab session, opened from the week: what it trains, every exercise with its dose (each
 * opening its instructions) and the button that starts it.
 */
@Composable
fun SessionScreen(
    state: SessionUiState,
    onStart: () -> Unit,
    onOpenExercise: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        if (session == null) {
            item(key = "none") {
                NovilaCard {
                    NovilaText(
                        stringResource(R.string.session_none),
                        style = NovilaTheme.typography.bodyMedium,
                        color = NovilaTheme.colors.onSurfaceVariant,
                    )
                }
            }
            return@ScreenList
        }
        item(key = "session") {
            StrengthSessionCard(
                session = session,
                onOpenExercise = onOpenExercise,
                onStart = onStart,
                next = true,
                minutesDone = state.minutesDone,
            )
        }
        item(key = "muscles") {
            val motions = session.exercises.mapNotNull { ExerciseLibrary.byId(it.exerciseId) }
            NovilaCard(title = stringResource(R.string.session_muscles_title)) {
                NovilaText(
                    muscleList(motions.flatMap { it.primary }.toSet()),
                    style = NovilaTheme.typography.bodyMedium,
                )
                NovilaText(
                    stringResource(R.string.session_muscles_body),
                    style = NovilaTheme.typography.bodySmall,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Session on Thursday", for the top bar. */
@Composable
fun sessionTitle(day: DayOfWeek): String {
    val locale = LocalConfiguration.current.locales[0]
    return stringResource(R.string.session_title, day.getDisplayName(TextStyle.FULL, locale))
}

@Composable
private fun SessionScreenContent(state: SessionUiState) {
    NovilaTheme {
        NovilaScaffold { padding ->
            SessionScreen(state, onStart = {}, onOpenExercise = {}, contentPadding = padding)
        }
    }
}

@Preview(heightDp = 1400)
@Composable
private fun SessionScreenPreview() = SessionScreenContent(
    SessionUiState(TrainingPreviewData.program.sessions.first { it.exercises.isNotEmpty() }, minutesDone = null),
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun SessionScreenNoneDarkPreview() = SessionScreenContent(SessionUiState(session = null, minutesDone = null))
