package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.AppSettings
import se.novila.model.EntryLayout
import se.novila.model.Profile
import se.novila.model.ThemeMode
import se.novila.movement.MovementState

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        actions: SettingsActions,
        movement: MovementState = MovementState.NeedsPermission(),
        settings: AppSettings = AppSettings(),
        profile: Profile = Profile(),
    ) {
        composeRule.setContent {
            NovilaTheme {
                SettingsScreen(
                    settings = settings,
                    profile = profile,
                    movement = movement,
                    version = AppVersion(name = "0.1.7-debug", code = 7),
                    canUseDynamicColor = true,
                    canChooseLanguage = true,
                    actions = actions,
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    /** Scrolls the list to [text], so the lazy list composes it, and returns its node. */
    private fun scrollTo(text: String) = composeRule.run {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        onNodeWithText(text)
    }

    @Test fun `choosing a theme saves it`() {
        val chosen = mutableListOf<ThemeMode>()
        show(SettingsActions(onTheme = { chosen += it }))

        composeRule.onNodeWithText("Dark").performClick()

        assertEquals(listOf(ThemeMode.Dark), chosen)
    }

    @Test fun `choosing rows for the ways into the tabs saves it`() {
        val chosen = mutableListOf<EntryLayout>()
        show(SettingsActions(onEntryLayout = { chosen += it }))

        scrollTo("Rows").performClick()

        assertEquals(listOf(EntryLayout.Rows), chosen)
    }

    @Test fun `an empty profile says what to add and opens the profile`() {
        var opens = 0
        show(SettingsActions(onOpenProfile = { opens++ }))

        composeRule.onNodeWithText("Add your name and body measurements").performClick()

        assertEquals(1, opens)
    }

    @Test fun `the profile row shows the name and leads to measurements and goals`() {
        var opens = 0
        show(SettingsActions(onOpenProfile = { opens++ }), profile = Profile(name = "Alex Lind"))

        composeRule.onNodeWithText("Alex Lind").assertExists()
        composeRule.onNodeWithContentDescription("Profile picture for Alex Lind").assertExists()
        composeRule.onNodeWithText("Profile, measurements and goals").performClick()

        assertEquals(1, opens)
    }

    @Test fun `data is deleted only after confirming`() {
        var deletes = 0
        show(SettingsActions(onDeleteData = { deletes++ }))

        scrollTo("Delete all data").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(0, deletes)

        scrollTo("Delete all data").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        assertEquals(1, deletes)
        composeRule.onNodeWithText("All data is deleted.").assertExists()
    }

    @Test fun `without access, the Health Connect card asks to connect`() {
        var connects = 0
        show(SettingsActions(onConnectHealth = { connects++ }))

        scrollTo("Connect Health Connect").performClick()

        assertEquals(1, connects)
    }

    @Test fun `the version shows its build number`() {
        show(SettingsActions())

        scrollTo("0.1.7-debug (7)").assertExists()
    }

    @Test fun `the intro can be shown again`() {
        var shows = 0
        show(SettingsActions(onShowIntro = { shows++ }))

        scrollTo("Show the intro again").performClick()

        assertEquals(1, shows)
    }

    @Test fun `the privacy policy, the terms and the licenses open from About`() {
        var privacy = 0
        var terms = 0
        var licenses = 0
        show(SettingsActions(onOpenPrivacy = { privacy++ }, onOpenTerms = { terms++ }, onOpenLicenses = { licenses++ }))

        scrollTo("Privacy policy").performClick()
        scrollTo("Terms of use").performClick()
        scrollTo("Open source licenses").performClick()

        assertEquals(1, privacy)
        assertEquals(1, terms)
        assertEquals(1, licenses)
    }

    @Test fun `a skipped setup shows the row that finishes it`() {
        var shows = 0
        show(
            SettingsActions(onShowIntro = { shows++ }),
            settings = AppSettings(onboarded = true, setupSkipped = true),
        )

        scrollTo("Finish setup").performClick()

        assertEquals(1, shows)
    }

    @Test fun `a finished setup says nothing about it`() {
        show(SettingsActions(), settings = AppSettings(onboarded = true))

        composeRule.onNodeWithText("Finish setup").assertDoesNotExist()
    }
}
