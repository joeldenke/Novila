package se.novila.ui.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaSectionHeader
import se.novila.ui.R

/**
 * The parts every tab is built from, always in this order, so that once the user knows one tab they know them all:
 * how the day looks with its main action ([Overview]), what can be logged ([Log]), the coach's plan or suggestions
 * ([Plan]) and history and what the logs say over time ([Trends]).
 */
enum class ScreenSection { Overview, Log, Plan, Trends }

/** The [section]'s header in a [ScreenList]. */
fun LazyListScope.section(section: ScreenSection) {
    item(key = "section-${section.name}") {
        NovilaSectionHeader(
            stringResource(
                when (section) {
                    ScreenSection.Overview -> R.string.section_overview
                    ScreenSection.Log -> R.string.section_log
                    ScreenSection.Plan -> R.string.section_plan
                    ScreenSection.Trends -> R.string.section_trends
                },
            ),
        )
    }
}
