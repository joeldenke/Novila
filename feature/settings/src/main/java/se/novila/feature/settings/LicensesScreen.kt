package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.style.LibraryActionBadges
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme

/**
 * The open-source licenses of everything Novila ships, from the app's generated library data. The rows expand in
 * place, and a license opens in a dialog; the footer keeps the bundled typeface's notice next to the code licenses.
 *
 * @param libraries the generated data, or null until it has been read
 */
@Composable
fun LicensesScreen(libraries: Libs?, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    LibrariesContainer(
        libraries = libraries,
        modifier = modifier,
        contentPadding = contentPadding,
        actionLabels = LibraryActionBadges(
            source = stringResource(R.string.licenses_source),
            website = stringResource(R.string.licenses_website),
            sponsor = stringResource(R.string.licenses_sponsor),
            viewLicense = stringResource(R.string.licenses_view_license),
        ),
        licenseDialogConfirmText = stringResource(R.string.licenses_close),
        footer = {
            item(key = "typefaces") {
                NovilaText(
                    text = stringResource(R.string.licenses_typefaces),
                    style = NovilaTheme.typography.bodySmall,
                    color = NovilaTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(NovilaTheme.spacing.l),
                )
            }
        },
    )
}

@Composable
private fun LicensesContent(libraries: Libs? = PreviewLibraries) {
    NovilaTheme {
        NovilaScaffold { padding -> LicensesScreen(libraries = libraries, contentPadding = padding) }
    }
}

@Preview(heightDp = 900)
@Composable
private fun LicensesPreview() = LicensesContent()

@Preview(heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun LicensesDarkPreview() = LicensesContent()

private val PreviewLicenses = setOf(
    License(name = "Apache License 2.0", url = null, spdxId = "Apache-2.0", hash = "apache-2.0"),
    License(name = "MIT License", url = null, spdxId = "MIT", hash = "mit"),
)

private val PreviewLibraries = Libs(
    libraries = listOf(
        Library(
            uniqueId = "androidx.compose.material3:material3",
            artifactVersion = "1.5.0",
            name = "Material 3",
            description = "Material Design components for Compose.",
            website = "https://developer.android.com/jetpack/androidx/releases/compose-material3",
            developers = emptyList(),
            organization = null,
            scm = null,
            licenses = setOf(PreviewLicenses.first()),
        ),
        Library(
            uniqueId = "org.jetbrains.kotlinx:kotlinx-coroutines-core",
            artifactVersion = "1.11.0",
            name = "Kotlin Coroutines",
            description = "Library support for Kotlin coroutines.",
            website = "https://github.com/Kotlin/kotlinx.coroutines",
            developers = emptyList(),
            organization = null,
            scm = null,
            licenses = setOf(PreviewLicenses.first()),
        ),
        Library(
            uniqueId = "com.mikepenz:aboutlibraries-compose-m3",
            artifactVersion = "15.3.0",
            name = "AboutLibraries",
            description = "Collects and shows the licenses of the libraries an app uses.",
            website = "https://github.com/mikepenz/AboutLibraries",
            developers = emptyList(),
            organization = null,
            scm = null,
            licenses = setOf(PreviewLicenses.last()),
        ),
    ),
    licenses = PreviewLicenses,
)
