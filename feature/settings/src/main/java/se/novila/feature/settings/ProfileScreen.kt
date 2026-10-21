package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaAvatar
import se.novila.designsystem.component.NovilaBottomSheet
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextField
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.model.MeasurementForm
import se.novila.model.NumberInput
import se.novila.model.Profile
import se.novila.model.ProfileForm
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import java.time.LocalDate
import se.novila.ui.R as UiR

/** What the profile can do. The entry connects these to the ViewModel and to My goals. */
data class ProfileActions(
    val onChange: (ProfileForm) -> Unit = {},
    val onMeasurementsChange: (MeasurementForm) -> Unit = {},
    val onSelectDay: (LocalDate) -> Unit = {},
    val onSaveMeasurements: () -> Unit = {},
    val onOpenMyGoals: () -> Unit = {},
)

/**
 * The user's profile in one place: who they are (the avatar with their initials, name, birth year and height), their
 * body measurements over time (weight, waist, neck and hips, logged per day in a sheet) and the way to My goals.
 * There is no account; everything is saved on the phone as it is typed, and a wrong number says what range it should
 * be in. The profile's weight is the newest measured one, so it is logged with the measurements, not typed here.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    actions: ProfileActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    openSheet: Boolean = false,
) {
    var logging by rememberSaveable(openSheet) { mutableStateOf(openSheet) }
    val form = state.form
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "header") { Header(state.profile, state.currentYear) }
        item(key = "about-you") { AboutYouCard(form, state.currentYear, actions.onChange) }
        item(key = "measurements") {
            LatestMeasurementsCard(state.measurements.entries, onLog = { logging = true })
        }
        item(key = "goals") { GoalsCard(actions.onOpenMyGoals) }
        if (state.measurements.entries.isNotEmpty()) {
            item(key = "history") { MeasurementsHistoryCard(state.measurements.entries) }
        }
        item(key = "privacy") {
            NovilaText(
                stringResource(R.string.profile_privacy),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
    if (logging) {
        NovilaBottomSheet(title = stringResource(R.string.measure_log), onDismiss = { logging = false }) {
            MeasurementsSheet(
                state = state.measurements,
                onChange = actions.onMeasurementsChange,
                onSelectDay = actions.onSelectDay,
                onSave = {
                    actions.onSaveMeasurements()
                    logging = false
                },
            )
        }
    }
}

/** The avatar with the user's initials, their name, and their age, height and weight in short. */
@Composable
private fun Header(profile: Profile, currentYear: Int) {
    val name = profile.name.ifEmpty { stringResource(R.string.profile_title) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l),
    ) {
        NovilaAvatar(
            initials = initialsOf(profile.name),
            placeholder = painterResource(UiR.drawable.ic_profile),
            contentDescription = stringResource(R.string.profile_avatar, name),
        )
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
            NovilaText(name, style = NovilaTheme.typography.headlineSmall)
            NovilaText(
                profileSummary(profile, currentYear) ?: stringResource(R.string.settings_profile_empty),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

/** Up to two initials, the first letters of the first and last word, e.g. "AL" for "Alex Lind". */
internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull())
        .joinToString("") { it.take(1) }
        .uppercase()
}

/** "36 years · 178 cm · 74 kg", with what is filled in, or null when nothing is. */
@Composable
internal fun profileSummary(profile: Profile, currentYear: Int): String? {
    val parts = listOfNotNull(
        profile.birthYear?.let { year ->
            val age = currentYear - year
            pluralStringResource(R.plurals.profile_age, age, age)
        },
        profile.heightCm?.let { stringResource(R.string.measure_value, it, stringResource(R.string.profile_unit_cm)) },
        profile.weightKg?.let { stringResource(R.string.measure_value, it, stringResource(R.string.profile_unit_kg)) },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(stringResource(R.string.profile_separator))
}

@Composable
private fun AboutYouCard(form: ProfileForm, currentYear: Int, onChange: (ProfileForm) -> Unit) {
    val years = Profile.birthYears(currentYear)
    NovilaCard(title = stringResource(R.string.profile_about_you)) {
        NovilaTextField(
            value = form.name,
            onValueChange = { onChange(form.copy(name = it)) },
            label = stringResource(R.string.profile_name),
            supportingText = stringResource(R.string.profile_name_summary),
        )
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
            last = true,
        )
    }
}

/** Everything Novila plans by, one tap from the profile. */
@Composable
private fun GoalsCard(onOpenMyGoals: () -> Unit) {
    NovilaCard {
        NovilaListRow(
            title = stringResource(R.string.my_goals_title),
            summary = stringResource(R.string.settings_my_goals_summary),
            onClick = onOpenMyGoals,
            trailing = { Chevron() },
        )
    }
}

@Composable
private fun NumberInput.errorOrNull(message: @Composable () -> String): String? =
    if (this == NumberInput.Invalid) message() else null

@Composable
private fun ProfileContent(profile: Profile, form: ProfileForm = ProfileForm.of(profile), measured: Boolean = true) {
    NovilaTheme {
        NovilaScaffold { padding ->
            ProfileScreen(
                state = ProfileUiState(
                    form = form,
                    currentYear = 2026,
                    profile = profile,
                    measurements = MeasurementsUiState(
                        form = MeasurementForm(),
                        day = PreviewData.date,
                        today = PreviewData.date,
                        entries = if (measured) sampleMeasurements else emptyList(),
                    ),
                ),
                actions = ProfileActions(),
                contentPadding = padding,
            )
        }
    }
}

@Preview(heightDp = 1800)
@Composable
private fun ProfilePreview() = ProfileContent(PreviewData.profile)

// A height still being typed, to show how a wrong number looks.
@Preview(heightDp = 1800, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ProfileDarkPreview() = ProfileContent(
    PreviewData.profile,
    ProfileForm.of(PreviewData.profile).copy(height = "17"),
)

/** Nothing filled in yet: the avatar shows the person icon and the measurements their dashes. */
@Preview(heightDp = 1200)
@Composable
private fun ProfileEmptyPreview() = ProfileContent(Profile(), measured = false)
