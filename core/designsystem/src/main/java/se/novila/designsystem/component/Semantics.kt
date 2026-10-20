package se.novila.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics

/**
 * Says how far a drawn ring or bar has come, so screen readers announce it as progress. The children stay in the
 * node, so the number, unit and label around the drawing are read with it.
 */
internal fun Modifier.novilaProgressSemantics(progress: Float): Modifier = semantics(mergeDescendants = true) {
    progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
}
