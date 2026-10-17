package se.novila.feature.training

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.anatomy.ExerciseLibrary
import se.novila.coach.BodyArea
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaSearchField
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.training.R
import se.novila.ui.common.ScreenList

/**
 * Every exercise as large tiles, to find one by name or muscle ("knee", "glutes") and narrow down by body area. Each
 * tile opens its instructions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    onOpenExercise: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var area by rememberSaveable { mutableStateOf<BodyArea?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    // What a search can find: each exercise's name and the muscles it trains, in the user's language.
    val texts = ExerciseLibrary.all.associate { motion ->
        motion.id to listOf(moveName(motion.id), muscleList(motion.primary + motion.secondary))
    }
    val found = ExerciseLibrary.all.filter { motion ->
        trains(motion, area) && matchesSearch(query, texts.getValue(motion.id), locale)
    }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "search") {
            NovilaSearchField(
                query = query,
                onQueryChange = { query = it },
                label = stringResource(R.string.library_search),
                clearDescription = stringResource(R.string.library_clear),
            )
        }
        item(key = "areas") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
                verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            ) {
                NovilaChoiceChip(stringResource(R.string.library_all), selected = area == null, onClick = {
                    area = null
                })
                BodyArea.entries.forEach { option ->
                    NovilaChoiceChip(
                        stringResource(option.label),
                        selected = area == option,
                        onClick = { area = if (area == option) null else option },
                    )
                }
            }
        }
        if (found.isEmpty()) {
            item(key = "empty") {
                NovilaText(
                    stringResource(R.string.library_empty, query),
                    style = NovilaTheme.typography.bodyMedium,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        } else {
            item(key = "count") {
                NovilaText(
                    stringResource(R.string.library_count, found.size),
                    style = NovilaTheme.typography.labelLarge,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
            found.chunked(2).forEachIndexed { row, pair ->
                item(key = "row-$row-${pair.first().id}") { ExerciseGrid(pair, onOpenExercise) }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryContent(query: String = "") {
    NovilaTheme {
        NovilaScaffold { padding ->
            ExerciseLibraryScreen(onOpenExercise = {}, contentPadding = padding, initialQuery = query)
        }
    }
}

@Preview(heightDp = 2400)
@Composable
private fun ExerciseLibraryPreview() = ExerciseLibraryContent()

/** Searching for a muscle. */
@Preview(heightDp = 1200, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ExerciseLibrarySearchDarkPreview() = ExerciseLibraryContent(query = "glutes")
