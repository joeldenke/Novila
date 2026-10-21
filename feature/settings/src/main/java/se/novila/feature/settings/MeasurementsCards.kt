package se.novila.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaLineChart
import se.novila.designsystem.component.NovilaMetric
import se.novila.designsystem.component.NovilaSheetContent
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextField
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.BodyMeasurements
import se.novila.model.MeasurementEntry
import se.novila.model.MeasurementForm
import se.novila.model.NumberInput
import se.novila.model.Profile
import se.novila.model.on
import se.novila.ui.common.DayPicker
import se.novila.ui.common.longDateText
import se.novila.ui.common.shortDateLabels
import se.novila.ui.preview.PreviewData
import java.time.LocalDate
import se.novila.ui.R as UiR

/**
 * The newest of each measurement and when it was taken, as the profile shows them, with the button that opens the
 * sheet to log a day ([onLog]). Before anything is measured, the dashes show what will be there.
 */
@Composable
internal fun LatestMeasurementsCard(entries: List<MeasurementEntry>, onLog: () -> Unit) {
    val missing = stringResource(UiR.string.value_missing)
    val kg = stringResource(R.string.profile_unit_kg)
    val cm = stringResource(R.string.profile_unit_cm)

    // Newest first, so the first day with a value is the newest one.
    fun latest(value: (BodyMeasurements) -> Int?) = entries.firstNotNullOfOrNull { value(it.measurements) }
    NovilaCard(
        title = stringResource(R.string.settings_measurements),
        icon = painterResource(UiR.drawable.ic_trend),
        role = NovilaTheme.pillars.training,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            NovilaMetric(
                latest { it.weightKg }?.let { stringResource(R.string.measure_value, it, kg) },
                stringResource(R.string.measure_weight),
                missing,
                Modifier.weight(1f),
            )
            NovilaMetric(
                latest { it.waistCm }?.let { stringResource(R.string.measure_value, it, cm) },
                stringResource(R.string.measure_waist),
                missing,
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            NovilaMetric(
                latest { it.neckCm }?.let { stringResource(R.string.measure_value, it, cm) },
                stringResource(R.string.measure_neck),
                missing,
                Modifier.weight(1f),
            )
            NovilaMetric(
                latest { it.hipsCm }?.let { stringResource(R.string.measure_value, it, cm) },
                stringResource(R.string.measure_hips),
                missing,
                Modifier.weight(1f),
            )
        }
        entries.firstOrNull()?.let { newest ->
            NovilaText(
                stringResource(R.string.measure_latest, longDateText(newest.date)),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        NovilaAccentButton(text = stringResource(R.string.measure_log), onClick = onLog)
    }
}

/**
 * The sheet that logs a day's measurements: the day ([onSelectDay] goes back within the editable days) and the four
 * fields for it, with the button that saves them.
 */
@Composable
internal fun MeasurementsSheet(
    state: MeasurementsUiState,
    onChange: (MeasurementForm) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    DayPicker(state.day, state.today, onSelectDay, NovilaTheme.pillars.training)
    LogFields(state.form, saved = state.entries.on(state.day), onChange, onSave)
    NovilaText(
        stringResource(R.string.measure_privacy),
        style = NovilaTheme.typography.bodySmall,
        color = NovilaTheme.colors.onSurfaceVariant,
    )
}

/** The four measurements of the chosen day. Saving offers nothing new until something was typed or cleared. */
@Composable
private fun LogFields(
    form: MeasurementForm,
    saved: BodyMeasurements,
    onChange: (MeasurementForm) -> Unit,
    onSave: () -> Unit,
) {
    val centimetres = stringResource(R.string.profile_unit_cm)
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
        NovilaTextField(
            value = form.weight,
            onValueChange = { onChange(form.copy(weight = it)) },
            label = stringResource(R.string.measure_weight),
            suffix = stringResource(R.string.profile_unit_kg),
            error = form.weightInput.errorOrNull {
                stringResource(R.string.profile_weight_error, Profile.WEIGHT_KG.first, Profile.WEIGHT_KG.last)
            },
            numeric = true,
        )
        NovilaTextField(
            value = form.waist,
            onValueChange = { onChange(form.copy(waist = it)) },
            label = stringResource(R.string.measure_waist),
            suffix = centimetres,
            error = form.waistInput.errorOrNull {
                stringResource(
                    R.string.measure_waist_error,
                    BodyMeasurements.WAIST_CM.first,
                    BodyMeasurements.WAIST_CM.last,
                )
            },
            numeric = true,
        )
        NovilaTextField(
            value = form.neck,
            onValueChange = { onChange(form.copy(neck = it)) },
            label = stringResource(R.string.measure_neck),
            suffix = centimetres,
            error = form.neckInput.errorOrNull {
                stringResource(
                    R.string.measure_neck_error,
                    BodyMeasurements.NECK_CM.first,
                    BodyMeasurements.NECK_CM.last,
                )
            },
            numeric = true,
        )
        NovilaTextField(
            value = form.hips,
            onValueChange = { onChange(form.copy(hips = it)) },
            label = stringResource(R.string.measure_hips),
            suffix = centimetres,
            error = form.hipsInput.errorOrNull {
                stringResource(
                    R.string.measure_hips_error,
                    BodyMeasurements.HIPS_CM.first,
                    BodyMeasurements.HIPS_CM.last,
                )
            },
            numeric = true,
            last = true,
        )
        NovilaAccentButton(
            text = stringResource(R.string.measure_save),
            onClick = onSave,
            enabled = form != MeasurementForm.of(saved),
        )
    }
}

/** What has been measured, newest first: the weight over the logged days, and one row per day with all four. */
@Composable
internal fun MeasurementsHistoryCard(entries: List<MeasurementEntry>) {
    NovilaCard(title = stringResource(R.string.measure_history)) {
        val weights = remember(entries) {
            entries.mapNotNull { entry -> entry.measurements.weightKg?.let { entry.date to it } }.sortedBy { it.first }
        }
        if (weights.size >= 2) WeightChart(weights)
        if (entries.isEmpty()) {
            NovilaText(
                stringResource(R.string.measure_history_empty),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        } else {
            entries.forEach { entry -> DayRow(entry) }
        }
    }
}

/** The weight of each logged day as a curve, with air above and below so a flat history stays a line in the middle. */
@Composable
private fun WeightChart(weights: List<Pair<LocalDate, Int>>) {
    val values = weights.map { it.second.toFloat() }
    val lowest = values.min()
    val highest = values.max()
    val pad = ((highest - lowest) / 5).coerceAtLeast(1f)
    NovilaLineChart(
        values = values,
        labels = shortDateLabels(weights.map { it.first }),
        valueRange = (lowest - pad)..(highest + pad),
        role = NovilaTheme.pillars.training,
        contentDescription = stringResource(R.string.measure_chart_description, lowest.toInt(), highest.toInt()),
    )
}

/** One logged day: the date, then weight, waist, neck and hips left to right, or a dash when one is missing. */
@Composable
private fun DayRow(entry: MeasurementEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(
            longDateText(entry.date),
            style = NovilaTheme.typography.labelMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            MeasurementValue(
                label = stringResource(R.string.measure_weight),
                value = entry.measurements.weightKg,
                unit = stringResource(R.string.profile_unit_kg),
                modifier = Modifier.weight(1f),
            )
            MeasurementValue(
                label = stringResource(R.string.measure_waist),
                value = entry.measurements.waistCm,
                unit = stringResource(R.string.profile_unit_cm),
                modifier = Modifier.weight(1f),
            )
            MeasurementValue(
                label = stringResource(R.string.measure_neck),
                value = entry.measurements.neckCm,
                unit = stringResource(R.string.profile_unit_cm),
                modifier = Modifier.weight(1f),
            )
            MeasurementValue(
                label = stringResource(R.string.measure_hips),
                value = entry.measurements.hipsCm,
                unit = stringResource(R.string.profile_unit_cm),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One number of a logged day: what it was and which measurement it is, or a dash when that one was not measured. */
@Composable
private fun MeasurementValue(label: String, value: Int?, unit: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        NovilaText(
            value?.let { stringResource(R.string.measure_value, it, unit) } ?: stringResource(UiR.string.value_missing),
            style = NovilaTheme.typography.bodyMedium,
        )
        NovilaText(
            label,
            style = NovilaTheme.typography.labelSmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NumberInput.errorOrNull(message: @Composable () -> String): String? =
    if (this == NumberInput.Invalid) message() else null

/** A few weeks of measurements: the weight falls a little, the waist with it. */
internal val sampleMeasurements = listOf(
    MeasurementEntry(PreviewData.date, BodyMeasurements(weightKg = 73, waistCm = 81, neckCm = 37, hipsCm = 97)),
    MeasurementEntry(
        PreviewData.date.minusDays(7),
        BodyMeasurements(weightKg = 74, waistCm = 82, neckCm = 37, hipsCm = 98),
    ),
    MeasurementEntry(
        PreviewData.date.minusDays(16),
        BodyMeasurements(weightKg = 75, waistCm = 84, neckCm = 38, hipsCm = 99),
    ),
    MeasurementEntry(
        PreviewData.date.minusDays(23),
        BodyMeasurements(weightKg = 76, waistCm = 85, neckCm = 38, hipsCm = 100),
    ),
)

/** The sheet as it slides up: the day and its four measurements. A waist still being typed shows its error. */
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MeasurementsSheetPreview() {
    NovilaTheme {
        NovilaSheetContent(stringResource(R.string.measure_log)) {
            MeasurementsSheet(
                state = MeasurementsUiState(
                    form = MeasurementForm(weight = "73", waist = "8"),
                    day = PreviewData.date,
                    today = PreviewData.date,
                    entries = sampleMeasurements,
                ),
                onChange = {},
                onSelectDay = {},
                onSave = {},
            )
        }
    }
}
