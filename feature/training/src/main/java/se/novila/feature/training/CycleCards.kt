package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaSheetContent
import se.novila.designsystem.component.NovilaSwitchRow
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.training.R
import se.novila.model.CycleDay
import se.novila.model.CycleSymptom
import se.novila.model.SessionChoice
import se.novila.ui.R as UiR

/** What the cycle sheet and the offer of a lighter session do. */
data class CycleActions(
    val onPeriod: (Boolean) -> Unit = {},
    val onSymptom: (CycleSymptom, Boolean) -> Unit = { _, _ -> },
    val onChooseSession: (SessionChoice) -> Unit = {},
)

/**
 * Today in *Follow your cycle*, as the sheet logs it: whether the user has their period, and how the body feels.
 * Both are optional, and the sheet says plainly that they stay on the phone.
 */
@Composable
internal fun CycleSheet(day: CycleDay, actions: CycleActions) {
    val training = NovilaTheme.pillars.training
    NovilaSwitchRow(
        title = stringResource(R.string.cycle_period_today),
        checked = day.period,
        onCheckedChange = actions.onPeriod,
        role = training,
    )
    NovilaText(
        stringResource(R.string.cycle_symptoms_title),
        style = NovilaTheme.typography.labelLarge,
        color = NovilaTheme.colors.onSurfaceVariant,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
    ) {
        CycleSymptom.entries.forEach { symptom ->
            val on = symptom in day.symptoms
            NovilaChoiceChip(
                label = stringResource(symptom.label),
                selected = on,
                onClick = { actions.onSymptom(symptom, !on) },
                role = training,
            )
        }
    }
    NovilaText(
        stringResource(R.string.cycle_sheet_footer),
        style = NovilaTheme.typography.bodySmall,
        color = NovilaTheme.colors.onSurfaceVariant,
    )
}

/**
 * A day with symptoms and a session planned: the coach offers a lighter session, and the user chooses. It never
 * switches by itself.
 */
@Composable
internal fun LighterSessionCard(onChoose: (SessionChoice) -> Unit, modifier: Modifier = Modifier) {
    val training = NovilaTheme.pillars.training
    NovilaCard(
        modifier = modifier,
        title = stringResource(R.string.cycle_lighter_title),
        icon = painterResource(UiR.drawable.ic_training),
        role = training,
    ) {
        NovilaText(
            stringResource(R.string.cycle_lighter_body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            NovilaButton(
                text = stringResource(R.string.cycle_lighter_yes),
                onClick = { onChoose(SessionChoice.Lighter) },
                modifier = Modifier.weight(1f),
                role = training,
            )
            NovilaButton(
                text = stringResource(R.string.cycle_lighter_no),
                onClick = { onChoose(SessionChoice.AsPlanned) },
                modifier = Modifier.weight(1f),
                role = training,
            )
        }
    }
}

private val CycleSymptom.label: Int
    get() = when (this) {
        CycleSymptom.Cramps -> R.string.cycle_symptom_cramps
        CycleSymptom.Tiredness -> R.string.cycle_symptom_tiredness
        CycleSymptom.Headache -> R.string.cycle_symptom_headache
        CycleSymptom.Bloating -> R.string.cycle_symptom_bloating
        CycleSymptom.LowMood -> R.string.cycle_symptom_low_mood
    }

/** The sheet the cycle is logged in, on a day with a period and cramps. */
@Preview(showBackground = true, heightDp = 600)
@Composable
private fun CycleSheetPreview() {
    NovilaTheme {
        NovilaSheetContent(stringResource(R.string.cycle_sheet_title)) {
            CycleSheet(CycleDay(period = true, symptoms = setOf(CycleSymptom.Cramps)), CycleActions())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LighterSessionCardPreview() {
    NovilaTheme { LighterSessionCard(onChoose = {}) }
}
