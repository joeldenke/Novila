package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextField
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.model.NumberInput
import se.novila.model.Profile
import se.novila.model.ProfileForm
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData

/**
 * The user's profile: name, birth year, height and weight, all optional. There is no account; the profile is saved
 * on the phone as it is typed, and a wrong number says what range it should be in.
 */
@Composable
fun ProfileScreen(
    form: ProfileForm,
    currentYear: Int,
    onChange: (ProfileForm) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "about-you") { AboutYouCard(form, onChange) }
        item(key = "body") { BodyCard(form, currentYear, onChange) }
        item(key = "privacy") {
            NovilaText(
                stringResource(R.string.profile_privacy),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutYouCard(form: ProfileForm, onChange: (ProfileForm) -> Unit) {
    NovilaCard(title = stringResource(R.string.profile_about_you)) {
        NovilaTextField(
            value = form.name,
            onValueChange = { onChange(form.copy(name = it)) },
            label = stringResource(R.string.profile_name),
            supportingText = stringResource(R.string.profile_name_summary),
        )
    }
}

@Composable
private fun BodyCard(form: ProfileForm, currentYear: Int, onChange: (ProfileForm) -> Unit) {
    val years = Profile.birthYears(currentYear)
    NovilaCard(title = stringResource(R.string.profile_body)) {
        NovilaTextField(
            value = form.birthYear,
            onValueChange = { onChange(form.copy(birthYear = it)) },
            label = stringResource(R.string.profile_birth_year),
            error = form.birthYearInput(currentYear).errorOrNull {
                stringResource(R.string.profile_birth_year_error, years.first, years.last)
            },
            numeric = true,
        )
        NovilaTextField(
            value = form.height,
            onValueChange = { onChange(form.copy(height = it)) },
            label = stringResource(R.string.profile_height),
            suffix = stringResource(R.string.profile_unit_cm),
            error = form.heightInput.errorOrNull {
                stringResource(R.string.profile_height_error, Profile.HEIGHT_CM.first, Profile.HEIGHT_CM.last)
            },
            numeric = true,
        )
        NovilaTextField(
            value = form.weight,
            onValueChange = { onChange(form.copy(weight = it)) },
            label = stringResource(R.string.profile_weight),
            suffix = stringResource(R.string.profile_unit_kg),
            error = form.weightInput.errorOrNull {
                stringResource(R.string.profile_weight_error, Profile.WEIGHT_KG.first, Profile.WEIGHT_KG.last)
            },
            numeric = true,
            last = true,
        )
    }
}

@Composable
private fun NumberInput.errorOrNull(message: @Composable () -> String): String? =
    if (this == NumberInput.Invalid) message() else null

@Composable
private fun ProfileContent(form: ProfileForm) {
    NovilaTheme {
        NovilaScaffold { padding ->
            ProfileScreen(form = form, currentYear = 2026, onChange = {}, contentPadding = padding)
        }
    }
}

@Preview(heightDp = 1000)
@Composable
private fun ProfilePreview() = ProfileContent(ProfileForm.of(PreviewData.profile))

// A weight still being typed, to show how a wrong number looks.
@Preview(heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ProfileDarkPreview() = ProfileContent(ProfileForm.of(PreviewData.profile).copy(weight = "7"))
