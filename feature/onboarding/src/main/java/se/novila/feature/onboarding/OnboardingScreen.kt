package se.novila.feature.onboarding

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPreferences
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceCard
import se.novila.designsystem.component.NovilaHeroBanner
import se.novila.designsystem.component.NovilaIcon
import se.novila.designsystem.component.NovilaIllustration
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaPageIndicator
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaScene
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.component.NovilaTextField
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.onboarding.R
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import se.novila.ui.common.ScreenList
import se.novila.ui.common.stepsText
import se.novila.ui.preview.PreviewData
import se.novila.ui.R as UiR

/** The intro's pages, in order: a tour of the app, then the user's own answers and a summary. */
enum class OnboardingPage { Welcome, Training, Sleep, Nutrition, You, Movement, Ready }

/** What the intro can do. The entry connects these to the ViewModel and to Health Connect. */
data class OnboardingActions(
    val onName: (String) -> Unit = {},
    val onPreferences: (CoachPreferences) -> Unit = {},
    val onConnectHealth: () -> Unit = {},
    val onOpenHealthSettings: () -> Unit = {},
    val onInstallHealth: () -> Unit = {},
    val onFinish: () -> Unit = {},
)

/**
 * The intro at first start: a carousel through the app's parts, each with its own drawn scene that comes alive and
 * moves in layers as the user swipes, then the user's name, goal and time, Health Connect, and a summary of the
 * first week. Swipe or use the buttons; system back goes to the previous page. "Skip" jumps past the tour to the
 * questions, and every question has a ready answer, so the user can get started in two taps.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    initialPage: OnboardingPage = OnboardingPage.Welcome,
) {
    val pages = OnboardingPage.entries
    val pager = rememberPagerState(initialPage = initialPage.ordinal) { pages.size }
    val scope = rememberCoroutineScope()
    val current = pages[pager.currentPage]
    val goTo: (OnboardingPage) -> Unit = { page ->
        scope.launch { pager.animateScrollToPage(page.ordinal) }
    }
    BackHandler(enabled = pager.currentPage > 0) { goTo(pages[pager.currentPage - 1]) }
    NovilaScaffold(
        modifier = modifier,
        bottomBar = {
            Controls(
                page = current,
                onSkip = { goTo(OnboardingPage.You) },
                onBack = { goTo(pages[(current.ordinal - 1).coerceAtLeast(0)]) },
                onNext = {
                    if (current == OnboardingPage.Ready) actions.onFinish() else goTo(pages[current.ordinal + 1])
                },
            )
        },
    ) { padding ->
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
            // How far this page is from the middle, for the scenes' parallax. Read while drawing.
            val parallax = { index - pager.currentPage - pager.currentPageOffsetFraction }
            when (pages[index]) {
                OnboardingPage.Welcome -> WelcomePage(parallax, padding)
                OnboardingPage.Training -> TourPage(Tour.Training, parallax, padding)
                OnboardingPage.Sleep -> TourPage(Tour.Sleep, parallax, padding)
                OnboardingPage.Nutrition -> TourPage(Tour.Nutrition, parallax, padding)
                OnboardingPage.You -> YouPage(state, actions, padding)
                OnboardingPage.Movement -> MovementPage(state.movement, actions, parallax, padding)
                OnboardingPage.Ready -> ReadyPage(state, padding)
            }
        }
    }
}

/** Each page in its part's color: sleep is lake blue, food falu red, everything else pine. */
@Composable
private fun OnboardingPage.role(): NovilaColorRole = when (this) {
    OnboardingPage.Sleep -> NovilaTheme.pillars.sleep
    OnboardingPage.Nutrition -> NovilaTheme.pillars.nutrition
    else -> NovilaTheme.pillars.training
}

/** Where the user is, and the way on: skip the tour or go back on the left, the next step in sunlight on the right. */
@Composable
private fun Controls(page: OnboardingPage, onSkip: () -> Unit, onBack: () -> Unit, onNext: () -> Unit) {
    val count = OnboardingPage.entries.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovilaTheme.colors.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = NovilaTheme.spacing.screenMargin, vertical = NovilaTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        NovilaPageIndicator(
            pageCount = count,
            currentPage = page.ordinal,
            contentDescription = stringResource(R.string.onboarding_progress, page.ordinal + 1, count),
            role = page.role(),
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            when {
                page < OnboardingPage.You -> NovilaTextButton(stringResource(R.string.onboarding_skip), onSkip)
                else -> NovilaTextButton(stringResource(R.string.onboarding_back), onBack)
            }
            Spacer(Modifier.weight(1f))
            NovilaAccentButton(
                text = stringResource(
                    when (page) {
                        OnboardingPage.Welcome -> R.string.onboarding_begin
                        OnboardingPage.Ready -> R.string.onboarding_finish
                        else -> R.string.onboarding_next
                    },
                ),
                onClick = onNext,
            )
        }
    }
}

/** A page with a scene on top, its text below, and anything [more] after that. */
@Composable
private fun ScenePage(
    scene: NovilaScene,
    parallax: () -> Float,
    contentPadding: PaddingValues,
    more: LazyListScope.() -> Unit = {},
    text: @Composable () -> Unit,
) {
    ScreenList(contentPadding = contentPadding) {
        item(key = "scene") {
            NovilaIllustration(scene, Modifier.fillMaxWidth().aspectRatio(SCENE_RATIO), parallax)
        }
        item(key = "text") { NovilaReveal(order = 1) { text() } }
        more()
    }
}

/** An eyebrow in the part's color, a title and a body. */
@Composable
private fun PageText(eyebrow: String, title: String, body: String, role: NovilaColorRole, large: Boolean = false) {
    Column(
        modifier = Modifier.padding(top = NovilaTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        NovilaText(eyebrow, style = NovilaTheme.typography.labelLarge, color = role.color)
        NovilaText(
            title,
            style = if (large) NovilaTheme.typography.displaySmall else NovilaTheme.typography.headlineMedium,
        )
        NovilaText(body, style = NovilaTheme.typography.bodyLarge, color = NovilaTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun WelcomePage(parallax: () -> Float, contentPadding: PaddingValues) {
    val training = NovilaTheme.pillars.training
    ScenePage(
        scene = NovilaScene.Sunrise,
        parallax = parallax,
        contentPadding = contentPadding,
        more = {
            item(key = "pillars") {
                NovilaReveal(order = 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
                        NovilaInsight(
                            painterResource(UiR.drawable.ic_training),
                            stringResource(R.string.onboarding_training_title),
                            training,
                        )
                        NovilaInsight(
                            painterResource(UiR.drawable.ic_sleep),
                            stringResource(R.string.onboarding_sleep_title),
                            NovilaTheme.pillars.sleep,
                        )
                        NovilaInsight(
                            painterResource(UiR.drawable.ic_nutrition),
                            stringResource(R.string.onboarding_nutrition_title),
                            NovilaTheme.pillars.nutrition,
                        )
                    }
                }
            }
            item(key = "privacy") {
                NovilaReveal(order = 3) {
                    Footnote(R.drawable.ic_privacy, stringResource(R.string.onboarding_welcome_privacy))
                }
            }
        },
    ) {
        PageText(
            eyebrow = stringResource(R.string.onboarding_welcome_eyebrow),
            title = stringResource(R.string.onboarding_welcome_title),
            body = stringResource(R.string.onboarding_welcome_body),
            role = training,
            large = true,
        )
    }
}

/** One part of the app on the tour: its scene, texts and three things it does, in its color. */
private enum class Tour(
    val page: OnboardingPage,
    val scene: NovilaScene,
    @DrawableRes val icon: Int,
    @StringRes val eyebrow: Int,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val points: List<Int>,
) {
    Training(
        OnboardingPage.Training,
        NovilaScene.Trail,
        UiR.drawable.ic_training,
        R.string.onboarding_training_eyebrow,
        R.string.onboarding_training_title,
        R.string.onboarding_training_body,
        listOf(
            R.string.onboarding_training_point_1,
            R.string.onboarding_training_point_2,
            R.string.onboarding_training_point_3,
        ),
    ),
    Sleep(
        OnboardingPage.Sleep,
        NovilaScene.Night,
        UiR.drawable.ic_sleep,
        R.string.onboarding_sleep_eyebrow,
        R.string.onboarding_sleep_title,
        R.string.onboarding_sleep_body,
        listOf(R.string.onboarding_sleep_point_1, R.string.onboarding_sleep_point_2, R.string.onboarding_sleep_point_3),
    ),
    Nutrition(
        OnboardingPage.Nutrition,
        NovilaScene.Cottage,
        UiR.drawable.ic_nutrition,
        R.string.onboarding_nutrition_eyebrow,
        R.string.onboarding_nutrition_title,
        R.string.onboarding_nutrition_body,
        listOf(
            R.string.onboarding_nutrition_point_1,
            R.string.onboarding_nutrition_point_2,
            R.string.onboarding_nutrition_point_3,
        ),
    ),
}

@Composable
private fun TourPage(tour: Tour, parallax: () -> Float, contentPadding: PaddingValues) {
    val role = tour.page.role()
    val icon = painterResource(tour.icon)
    val points = tour.points.map { stringResource(it) }
    ScenePage(
        scene = tour.scene,
        parallax = parallax,
        contentPadding = contentPadding,
        more = {
            points.forEachIndexed { i, point ->
                item(key = "point-$i") { NovilaReveal(order = 2 + i) { NovilaInsight(icon, point, role) } }
            }
        },
    ) {
        PageText(stringResource(tour.eyebrow), stringResource(tour.title), stringResource(tour.body), role)
    }
}

/** The goals the intro offers. Rehab needs an area and a phase, so it is set up in the coach itself. */
private val GOALS = listOf(CoachGoal.Health, CoachGoal.Strength, CoachGoal.Endurance, CoachGoal.Mobility)

@StringRes
private fun CoachGoal.label(): Int = when (this) {
    CoachGoal.Health -> R.string.onboarding_goal_health

    CoachGoal.Strength -> R.string.onboarding_goal_strength

    CoachGoal.Endurance -> R.string.onboarding_goal_endurance

    CoachGoal.Mobility -> R.string.onboarding_goal_mobility

    // Rehab is never offered here, and the summary has its own text for it.
    CoachGoal.Rehab -> R.string.onboarding_goal_health
}

@StringRes
private fun CoachGoal.description(): Int = when (this) {
    CoachGoal.Strength -> R.string.onboarding_goal_strength_body
    CoachGoal.Endurance -> R.string.onboarding_goal_endurance_body
    CoachGoal.Mobility -> R.string.onboarding_goal_mobility_body
    CoachGoal.Health, CoachGoal.Rehab -> R.string.onboarding_goal_health_body
}

@DrawableRes
private fun CoachGoal.icon(): Int = when (this) {
    CoachGoal.Strength -> UiR.drawable.ic_training
    CoachGoal.Endurance -> R.drawable.ic_goal_endurance
    CoachGoal.Mobility -> R.drawable.ic_goal_mobility
    CoachGoal.Health, CoachGoal.Rehab -> R.drawable.ic_goal_health
}

/** The user's name, goal and time: everything the coach needs for a first week. */
@Composable
private fun YouPage(state: OnboardingUiState, actions: OnboardingActions, contentPadding: PaddingValues) {
    val training = NovilaTheme.pillars.training
    val preferences = state.preferences
    ScreenList(contentPadding = contentPadding) {
        item(key = "text") {
            NovilaReveal(order = 0) {
                PageText(
                    eyebrow = stringResource(R.string.onboarding_you_eyebrow),
                    title = stringResource(R.string.onboarding_you_title),
                    body = stringResource(R.string.onboarding_you_body),
                    role = training,
                )
            }
        }
        item(key = "name") {
            NovilaReveal(order = 1) {
                NovilaCard {
                    NovilaTextField(
                        value = state.name,
                        onValueChange = actions.onName,
                        label = stringResource(R.string.onboarding_name),
                        supportingText = stringResource(R.string.onboarding_name_summary),
                        last = true,
                    )
                }
            }
        }
        item(key = "goal-title") {
            NovilaReveal(order = 2) {
                NovilaText(
                    stringResource(R.string.onboarding_goal_title),
                    style = NovilaTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = NovilaTheme.spacing.s),
                )
            }
        }
        GOALS.forEachIndexed { i, goal ->
            item(key = "goal-${goal.name}") {
                NovilaReveal(order = 3 + i) {
                    NovilaChoiceCard(
                        title = stringResource(goal.label()),
                        body = stringResource(goal.description()),
                        icon = painterResource(goal.icon()),
                        selected = preferences.goal == goal,
                        onClick = { actions.onPreferences(preferences.copy(goal = goal)) },
                        role = training,
                    )
                }
            }
        }
        if (preferences.goal == CoachGoal.Rehab) {
            item(key = "rehab") { Body(stringResource(R.string.onboarding_goal_rehab)) }
        }
        item(key = "time") {
            NovilaReveal(order = 3 + GOALS.size) { TimeCard(preferences, actions.onPreferences) }
        }
    }
}

@Composable
private fun TimeCard(preferences: CoachPreferences, onPreferences: (CoachPreferences) -> Unit) {
    val training = NovilaTheme.pillars.training
    val minutes = preferences.sessionMinutes
    val step = CoachPreferences.MINUTES_STEP
    NovilaCard(title = stringResource(R.string.onboarding_time_title)) {
        NovilaStepper(
            title = stringResource(R.string.onboarding_days),
            value = stringResource(R.string.onboarding_days_value, preferences.daysPerWeek),
            onDecrement = { onPreferences(preferences.withDays(preferences.daysPerWeek - 1)) },
            onIncrement = { onPreferences(preferences.withDays(preferences.daysPerWeek + 1)) },
            decrementDescription = stringResource(R.string.onboarding_fewer_days),
            incrementDescription = stringResource(R.string.onboarding_more_days),
            role = training,
            decrementEnabled = preferences.daysPerWeek > CoachPreferences.MIN_DAYS,
        )
        NovilaStepper(
            title = stringResource(R.string.onboarding_minutes),
            value = stringResource(R.string.onboarding_minutes_value, minutes),
            onDecrement = { onPreferences(preferences.withMinutes(minutes - step)) },
            onIncrement = { onPreferences(preferences.withMinutes(minutes + step)) },
            decrementDescription = stringResource(R.string.onboarding_shorter),
            incrementDescription = stringResource(R.string.onboarding_longer),
            role = training,
            decrementEnabled = minutes > CoachPreferences.MIN_MINUTES,
        )
    }
}

/** Health Connect, with the one thing the user can do about it right now. */
@Composable
private fun MovementPage(
    movement: MovementState,
    actions: OnboardingActions,
    parallax: () -> Float,
    contentPadding: PaddingValues,
) {
    val training = NovilaTheme.pillars.training
    ScenePage(
        scene = NovilaScene.Steps,
        parallax = parallax,
        contentPadding = contentPadding,
        more = {
            item(key = "status") {
                NovilaReveal(order = 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
                        MovementStatus(movement, actions)
                    }
                }
            }
        },
    ) {
        PageText(
            eyebrow = stringResource(R.string.onboarding_movement_eyebrow),
            title = stringResource(R.string.onboarding_movement_title),
            body = stringResource(R.string.onboarding_movement_body),
            role = training,
        )
    }
}

@Composable
private fun MovementStatus(movement: MovementState, actions: OnboardingActions) {
    val wide = Modifier.fillMaxWidth()
    when (movement) {
        is MovementState.Connected -> NovilaInsight(
            painterResource(UiR.drawable.ic_training),
            stringResource(R.string.onboarding_movement_connected, stepsText(movement.today.steps)),
            NovilaTheme.pillars.training,
        )

        is MovementState.NeedsPermission -> if (movement.denied) {
            Body(stringResource(R.string.onboarding_movement_denied))
            NovilaButton(stringResource(UiR.string.movement_open_settings), actions.onOpenHealthSettings, wide)
        } else {
            NovilaButton(stringResource(R.string.onboarding_movement_connect), actions.onConnectHealth, wide)
        }

        MovementState.NeedsInstall ->
            NovilaButton(stringResource(R.string.onboarding_movement_install), actions.onInstallHealth, wide)

        MovementState.Unavailable -> Body(stringResource(R.string.onboarding_movement_unavailable))
    }
    if (movement is MovementState.NeedsPermission || movement == MovementState.NeedsInstall) {
        Body(stringResource(R.string.onboarding_movement_later))
    }
}

/** The first week in the hero card, what to do first in each part, and what is good to know. */
@Composable
private fun ReadyPage(state: OnboardingUiState, contentPadding: PaddingValues) {
    val preferences = state.preferences
    val name = state.name.trim()
    ScreenList(contentPadding = contentPadding) {
        item(key = "hero") {
            NovilaReveal(order = 0) {
                NovilaHeroBanner {
                    NovilaText(
                        if (name.isEmpty()) {
                            stringResource(R.string.onboarding_ready_title)
                        } else {
                            stringResource(R.string.onboarding_ready_title_named, name)
                        },
                        style = NovilaTheme.typography.headlineMedium,
                    )
                    NovilaText(
                        if (preferences.goal == CoachGoal.Rehab) {
                            stringResource(
                                R.string.onboarding_ready_body_rehab,
                                preferences.daysPerWeek,
                                preferences.sessionMinutes,
                            )
                        } else {
                            stringResource(
                                R.string.onboarding_ready_body,
                                stringResource(preferences.goal.label()),
                                preferences.daysPerWeek,
                                preferences.sessionMinutes,
                            )
                        },
                        style = NovilaTheme.typography.bodyLarge,
                    )
                    val hero = NovilaTheme.hero
                    HeroRow(UiR.drawable.ic_training, stringResource(R.string.onboarding_ready_training), hero.training)
                    HeroRow(UiR.drawable.ic_sleep, stringResource(R.string.onboarding_ready_sleep), hero.sleep)
                    HeroRow(
                        UiR.drawable.ic_nutrition,
                        stringResource(R.string.onboarding_ready_nutrition),
                        hero.nutrition,
                    )
                }
            }
        }
        item(key = "know") {
            NovilaReveal(order = 1) {
                NovilaCard(title = stringResource(R.string.onboarding_ready_know_title)) {
                    NovilaInsight(
                        painterResource(R.drawable.ic_privacy),
                        stringResource(R.string.onboarding_ready_privacy),
                        NovilaTheme.pillars.training,
                    )
                    NovilaInsight(
                        painterResource(R.drawable.ic_goal_health),
                        stringResource(R.string.onboarding_ready_disclaimer),
                        NovilaTheme.pillars.nutrition,
                    )
                }
            }
        }
    }
}

/** One first step in the hero card, with the part's icon in its light hero color. */
@Composable
private fun HeroRow(@DrawableRes icon: Int, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        NovilaIcon(painterResource(icon), contentDescription = null, tint = tint)
        NovilaText(text, style = NovilaTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Footnote(@DrawableRes icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        NovilaIcon(painterResource(icon), contentDescription = null, tint = NovilaTheme.colors.onSurfaceVariant)
        Body(text)
    }
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

/** Scenes are a little wider than tall, so the text below starts above the fold. */
private const val SCENE_RATIO = 1.1f

private val previewState = OnboardingUiState(
    name = PreviewData.profile.name,
    preferences = CoachPreferences(goal = CoachGoal.Strength, daysPerWeek = 3, sessionMinutes = 40),
    movement = MovementState.NeedsPermission(),
)

@Composable
private fun OnboardingContent(page: OnboardingPage, state: OnboardingUiState = previewState) {
    NovilaTheme {
        OnboardingScreen(state = state, actions = OnboardingActions(), initialPage = page)
    }
}

@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun WelcomePreview() = OnboardingContent(OnboardingPage.Welcome)

@Preview(widthDp = 400, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun WelcomeDarkPreview() = OnboardingContent(OnboardingPage.Welcome)

@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun TrainingPreview() = OnboardingContent(OnboardingPage.Training)

@Preview(widthDp = 400, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun SleepDarkPreview() = OnboardingContent(OnboardingPage.Sleep)

@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun NutritionPreview() = OnboardingContent(OnboardingPage.Nutrition)

@Preview(widthDp = 400, heightDp = 1300)
@Composable
private fun YouPreview() = OnboardingContent(OnboardingPage.You)

@Preview(widthDp = 400, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun MovementDarkPreview() = OnboardingContent(
    OnboardingPage.Movement,
    previewState.copy(movement = MovementState.Connected(TodayMovement(steps = 4210, activeKilocalories = 160))),
)

@Preview(widthDp = 400, heightDp = 900)
@Composable
private fun ReadyPreview() = OnboardingContent(OnboardingPage.Ready)

@Preview(widthDp = 400, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ReadyDarkPreview() = OnboardingContent(OnboardingPage.Ready)
