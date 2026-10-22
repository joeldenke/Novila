package se.novila.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Symptoms around a period that the user can log for a day. What matters for training, not a diagnosis. */
enum class CycleSymptom { Cramps, Tiredness, Headache, Bloating, LowMood }

/** What the user said about today's session on a day with symptoms: a lighter one, or as planned. */
enum class SessionChoice { Lighter, AsPlanned }

/**
 * One day of the cycle as the user logged it: whether they had their [period], which [symptoms] they had, and, on a
 * day with symptoms, what they chose for the day's [session] (null until they choose).
 */
data class CycleDay(
    val period: Boolean = false,
    val symptoms: Set<CycleSymptom> = emptySet(),
    val session: SessionChoice? = null,
) {
    val isEmpty: Boolean get() = !period && symptoms.isEmpty() && session == null
}

/**
 * Everything the user logged in *Follow your cycle*, by date. Only days with something logged are kept. The cycle is
 * special category health data, so it lives in its own store on the phone and is deleted when the user turns the
 * cycle off. Novila never predicts ovulation or fertile days: the cycle only steers the training ([lighterOffered],
 * [lighter]) and which days recovery compares with ([cyclePhase]).
 */
data class CycleLog(val days: Map<LocalDate, CycleDay> = emptyMap()) {
    fun on(date: LocalDate): CycleDay = days[date] ?: CycleDay()

    /** The days the user had their period. */
    val periodDays: Set<LocalDate> get() = days.filterValues { it.period }.keys

    fun withPeriod(date: LocalDate, on: Boolean) = changed(date) { it.copy(period = on) }

    /** Turns [symptom] on or off. A day without symptoms has nothing to choose, so its choice goes with them. */
    fun withSymptom(date: LocalDate, symptom: CycleSymptom, on: Boolean) = changed(date) { day ->
        val symptoms = if (on) day.symptoms + symptom else day.symptoms - symptom
        day.copy(symptoms = symptoms, session = day.session.takeIf { symptoms.isNotEmpty() })
    }

    /** What the user chose for [date]'s session. Only a day with symptoms has a choice. */
    fun withChoice(date: LocalDate, choice: SessionChoice?) = changed(date) { day ->
        day.copy(session = choice.takeIf { day.symptoms.isNotEmpty() })
    }

    /** A day with symptoms where the user hasn't chosen yet: the coach offers a lighter session, never switches. */
    fun lighterOffered(date: LocalDate): Boolean = on(date).let { it.symptoms.isNotEmpty() && it.session == null }

    /** The user chose a lighter session for [date]. */
    fun lighter(date: LocalDate): Boolean = on(date).session == SessionChoice.Lighter

    private fun changed(date: LocalDate, change: (CycleDay) -> CycleDay): CycleLog {
        val day = change(on(date))
        val kept = if (day.isEmpty) days - date else days + (date to day)
        // Only the last CYCLE_KEPT_DAYS are kept: enough for the cycle's length, and no more data than needed.
        val newest = kept.keys.maxOrNull() ?: return CycleLog()
        return CycleLog(kept.filterKeys { it > newest.minusDays(CYCLE_KEPT_DAYS) })
    }
}

/**
 * The half of the cycle a day is in: [Early] from the first day of a period, [Late] from the middle of the user's own
 * cycle length. Resting heart rate is normally a few beats higher and HRV lower in the late half, so recovery
 * compares a day only with days in the same half. Never shown as ovulation or fertile days.
 */
enum class CyclePhase { Early, Late }

/**
 * The first days of each period in [periodDays], oldest first: a period day with no other period day in the
 * [PERIOD_GAP_DAYS] days before, so a day the user forgot to log doesn't start a new period.
 */
fun periodStarts(periodDays: Set<LocalDate>): List<LocalDate> = periodDays.sorted().filter { day ->
    (1..PERIOD_GAP_DAYS).none { day.minusDays(it) in periodDays }
}

/**
 * The user's usual cycle length in days: the median of the last [LENGTH_CYCLES] gaps between period starts that are
 * a plausible cycle ([MIN_CYCLE_DAYS] to [MAX_CYCLE_DAYS]), or [DEFAULT_CYCLE_DAYS] before there is one.
 */
fun cycleLength(starts: List<LocalDate>): Int {
    val lengths = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        .filter { it in MIN_CYCLE_DAYS..MAX_CYCLE_DAYS }
        .takeLast(LENGTH_CYCLES)
    return if (lengths.isEmpty()) DEFAULT_CYCLE_DAYS else lengths.sorted()[lengths.size / 2]
}

/**
 * Which half of the cycle [date] is in, from the periods logged on or before it: null when the last period began
 * more than [MAX_CYCLE_DAYS] days before (or none is logged), since the cycle then says nothing about the day.
 */
fun cyclePhase(periodDays: Set<LocalDate>, date: LocalDate): CyclePhase? {
    val starts = periodStarts(periodDays.filterTo(mutableSetOf()) { it <= date })
    val start = starts.lastOrNull() ?: return null
    val day = ChronoUnit.DAYS.between(start, date)
    if (day >= MAX_CYCLE_DAYS) return null
    return if (day < cycleLength(starts) / 2) CyclePhase.Early else CyclePhase.Late
}

/**
 * Simple versioned text format without Android dependencies, like `DayEntryCodec`. v1: "v1" followed by one
 * ";"-separated part per day, "date:period:symptoms:session", where the date is ISO (2026-10-22), period is 1 or 0,
 * the symptoms are comma-separated names and the session is the choice's name (empty when none). Unknown symptoms
 * and choices (from a later version) are skipped, and so are unreadable days.
 */
object CycleLogCodec {
    private const val VERSION = "v1"

    fun encode(log: CycleLog): String {
        val days = log.days.entries.sortedBy { it.key }.map { (date, day) ->
            listOf(
                date.toString(),
                if (day.period) "1" else "0",
                day.symptoms.sorted().joinToString(",") { it.name },
                day.session?.name.orEmpty(),
            ).joinToString(":")
        }
        return (listOf(VERSION) + days).joinToString(";")
    }

    fun decode(raw: String?): CycleLog {
        val parts = raw?.split(';') ?: return CycleLog()
        if (parts[0] != VERSION) return CycleLog()
        return CycleLog(parts.drop(1).mapNotNull(::day).toMap())
    }

    private fun day(raw: String): Pair<LocalDate, CycleDay>? {
        val fields = raw.split(':')
        if (fields.size != 4) return null
        val date = runCatching { LocalDate.parse(fields[0]) }.getOrNull() ?: return null
        val symptoms = fields[2].split(',').mapNotNull { name -> CycleSymptom.entries.firstOrNull { it.name == name } }
        val day = CycleDay(
            period = fields[1] == "1",
            symptoms = symptoms.toSet(),
            session = SessionChoice.entries.firstOrNull { it.name == fields[3] }.takeIf { symptoms.isNotEmpty() },
        )
        return if (day.isEmpty) null else date to day
    }
}

/** A day within this many days of an earlier period day belongs to the same period. */
const val PERIOD_GAP_DAYS = 3

/** Cycles from this many to [MAX_CYCLE_DAYS] days count when the usual length is worked out. */
const val MIN_CYCLE_DAYS = 21
const val MAX_CYCLE_DAYS = 45
const val DEFAULT_CYCLE_DAYS = 28

/** The usual length is the median of this many recent cycles. */
private const val LENGTH_CYCLES = 6

/** How far back the log keeps days: a little more than six of the longest cycles. */
const val CYCLE_KEPT_DAYS = 300L
