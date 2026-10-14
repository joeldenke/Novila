package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.R
import se.novila.designsystem.component.NovilaEmptyState
import se.novila.designsystem.theme.NovilaTheme
import se.novila.ui.common.ScreenList

@Composable
fun TrainingScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "empty") {
            NovilaEmptyState(
                icon = painterResource(R.drawable.ic_training),
                title = stringResource(R.string.training_empty_title),
                body = stringResource(R.string.training_empty_body),
                role = NovilaTheme.pillars.training,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainingScreenPreview() {
    NovilaTheme { TrainingScreen(contentPadding = PaddingValues()) }
}
