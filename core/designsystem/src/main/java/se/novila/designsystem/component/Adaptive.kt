package se.novila.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A vertical fold or hinge across the window, from the window's left edge to its right edge. On an unfolded phone
 * two panes split along it, so no card lies across the fold (adaptive skill, hinge-aware panes).
 */
@Immutable
data class NovilaFold(val left: Dp, val right: Dp)

/** The window's vertical fold, or null: a phone, a tablet, a folded phone or a fold that runs across. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberNovilaFold(): NovilaFold? {
    val hinge = currentWindowAdaptiveInfo().windowPosture.hingeList.firstOrNull { it.isVertical } ?: return null
    return with(LocalDensity.current) { NovilaFold(hinge.bounds.left.toDp(), hinge.bounds.right.toDp()) }
}

/**
 * The fold the app's window has, provided once at the top of the app from [rememberNovilaFold]. Previews provide
 * one of their own to show an unfolded phone; without one there is no fold.
 */
val LocalNovilaFold = staticCompositionLocalOf<NovilaFold?> { null }

/** For previews of an unfolded phone: a flat fold down the middle of the preview. */
@Composable
fun NovilaUnfolded(content: @Composable () -> Unit) {
    val middle = LocalConfiguration.current.screenWidthDp.dp / 2
    CompositionLocalProvider(LocalNovilaFold provides NovilaFold(middle, middle), content = content)
}

/** A button is never wider than this, so it doesn't stretch across a tablet. */
val NovilaMaxButtonWidth: Dp = 360.dp

/**
 * A card's or panel's main button: the whole width on a phone, and on a wider card [NovilaMaxButtonWidth] at the
 * end, where the eye ends up after reading.
 */
fun Modifier.novilaActionWidth(): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.End)
    .widthIn(max = NovilaMaxButtonWidth)
    .fillMaxWidth()
