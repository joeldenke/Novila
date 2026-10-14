package se.novila.ui.preview

import se.novila.DayEntry
import java.time.LocalDate

/** Fixed sample data for Compose previews. */
internal object PreviewData {
    private val date: LocalDate = LocalDate.of(2026, 10, 14)

    val today = DayEntry(date, waterGlasses = 5, sleepHours = 7.5f, mood = 4)

    val history: List<DayEntry> = (1..6).map { daysAgo ->
        DayEntry(
            date = date.minusDays(daysAgo.toLong()),
            waterGlasses = daysAgo + 2,
            sleepHours = 6f + daysAgo / 2f,
            mood = daysAgo % 5 + 1,
        )
    }
}
