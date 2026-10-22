package se.novila.sleep

import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.RegularityLevel
import se.novila.model.SleepFactor
import se.novila.model.SleepRegularity
import se.novila.model.sleepRegularity
import java.time.LocalTime
import kotlin.math.abs

/**
 * One step of a wind-down routine before bed, with how many [minutesBeforeBedtime] it belongs. The steps are the
 * evening routine in cognitive behavioural therapy for insomnia: a calm wind-down, the bed kept for sleep.
 */
enum class WindDownStep(val minutesBeforeBedtime: Int) {
    PrepareTomorrow(120),
    Shower(90),
    ScreensOff(60),
    Quiet(45),
    WorryNote(40),
    Read(30),
}

/**
 * What the user wants from the sleep coach: a [bedtime] and [wakeTime] to aim for, and the [windDown] steps they
 * want in the evening. Null preferences mean the coach isn't set up, like the training coach's answers.
 */
data class SleepCoachPreferences(
    val bedtime: LocalTime = DEFAULT_BEDTIME,
    val wakeTime: LocalTime = DEFAULT_WAKE_TIME,
    val windDown: Set<WindDownStep> = emptySet(),
) {
    fun withBedtime(time: LocalTime) = copy(bedtime = time)

    fun withWakeTime(time: LocalTime) = copy(wakeTime = time)

    fun toggle(step: WindDownStep) =
        copy(windDown = if (step in windDown) windDown - step else windDown + step)

    companion object {
        val DEFAULT_BEDTIME: LocalTime = LocalTime.of(22, 30)
        val DEFAULT_WAKE_TIME: LocalTime = LocalTime.of(6, 30)
    }
}

/** A wind-down step with the clock time it belongs, [bedtime] minus its offset. */
data class WindDownMoment(val step: WindDownStep, val time: LocalTime)

/** Of the last seven nights with a bedtime logged, [nearGoal] were within half an hour of the goal. */
data class SleepCoachWeek(val nearGoal: Int, val logged: Int)

/** Something the coach has seen in the user's own nights, as advice inspired by CBT-I. Never a diagnosis. */
enum class SleepCoachTip {
    RegularBedtime,
    FixedWakeTime,

    /** The weekend moves the sleep an hour or more (social jetlag): get up close to the weekday time then too. */
    WeekendShift,
    OutOfBed,
    LateCaffeine,
    Screens,
    Alcohol,
    Stress,
    LateMeal,
    LateExercise,
    ShortNights,
}

/**
 * What the coach makes of the answers and the logs: the [routine] at its times before bedtime, [week] near the
 * bedtime goal, the [tips] the nights point to (strongest first, at most [MAX_TIPS]) and the next [tonight] step
 * still ahead of [now], or null when the evening's steps are done.
 */
data class SleepCoachPlan(
    val routine: List<WindDownMoment>,
    val week: SleepCoachWeek,
    val tips: List<SleepCoachTip>,
    val nightsLogged: Int,
    val tonight: WindDownMoment?,
) {
    val hasSteps: Boolean get() = routine.isNotEmpty()
}

/** Bedtime within this of the goal counts as near it. */
const val NEAR_BEDTIME_MINUTES = 30

/** A wake time this far from the goal is worth a word about keeping it steady. */
const val FAR_WAKE_MINUTES = 60

/** Nights with the same logged factor before the coach mentions it. */
const val FACTOR_NIGHTS = 3

/** Nights of each kind needed before a tip is worth showing. */
const val MIN_TIP_NIGHTS = 5

/** Below this share of the bed time actually asleep, the coach suggests getting out of bed instead. */
const val MIN_SLEEP_EFFICIENCY = 0.85

/** Nights the screens read for the coach: a little more than the two weeks the tips look at, ending today. */
const val COACH_NIGHTS = 15

private const val MAX_TIPS = 3
private const val MINUTES_PER_DAY = 24 * 60
private const val NOON = 12 * 60
private const val WEEK_NIGHTS = 7
private const val WINDOW_NIGHTS = 14

/**
 * The sleep coach's plan from the user's [days] (oldest first), their [goals] and the [preferences] the coach runs
 * on. Everything comes from the logs on the phone: which nights are near the bedtime goal, whether enough of the
 * bed time is spent asleep, how regular the nights are ([sleepRegularity]) and how often each evening factor was
 * logged. Deterministic, so it is unit tested.
 */
fun sleepCoachPlan(
    days: List<DayEntry>,
    preferences: SleepCoachPreferences,
    goals: Goals = Goals(),
    now: LocalTime = LocalTime.now(),
): SleepCoachPlan {
    val nights = days.takeLast(WINDOW_NIGHTS)
    val withBedtime = nights.filter { it.bedtime != null }
    return SleepCoachPlan(
        routine = routine(preferences),
        week = week(nights, preferences),
        tips = tips(withBedtime, preferences, goals, regularity(days)),
        nightsLogged = nights.count { it.sleepHours > 0f },
        tonight = routine(preferences).firstOrNull { minutesFrom(it.time) > minutesFrom(now) },
    )
}

/** The wind-down steps with their clock times, earliest in the evening first. */
private fun routine(preferences: SleepCoachPreferences): List<WindDownMoment> = preferences.windDown
    .map { step -> WindDownMoment(step, preferences.bedtime.minusMinutes(step.minutesBeforeBedtime.toLong())) }
    .sortedBy { minutesFrom(it.time) }

/** The last week's bedtimes against the goal. */
private fun week(nights: List<DayEntry>, preferences: SleepCoachPreferences): SleepCoachWeek {
    val logged = nights.takeLast(WEEK_NIGHTS).mapNotNull { it.bedtime }
    val nearGoal = logged.count { distance(it, preferences.bedtime) <= NEAR_BEDTIME_MINUTES }
    return SleepCoachWeek(nearGoal = nearGoal, logged = logged.size)
}

/** The tips the logged nights point to, strongest evidence first, at most [MAX_TIPS]. */
private fun tips(
    nights: List<DayEntry>,
    preferences: SleepCoachPreferences,
    goals: Goals,
    regularity: SleepRegularity,
): List<SleepCoachTip> {
    val bedtime = regularityTip(nights, preferences, regularity)
    val wake = wakeTip(nights, preferences)
    val weekend = SleepCoachTip.WeekendShift.takeIf { regularity.notableJetlag }
    val efficiency = efficiencyTip(nights)
    val factors = factorTips(nights)
    val short = shortTip(nights, goals)
    return listOfNotNull(bedtime, wake, weekend, efficiency).plus(factors).plus(listOfNotNull(short)).take(MAX_TIPS)
}

/**
 * The bedtime goal is only half the nights away, when there are enough of them, or the nights are irregular from one
 * to the next ([RegularityLevel.Irregular]).
 */
private fun regularityTip(
    nights: List<DayEntry>,
    preferences: SleepCoachPreferences,
    regularity: SleepRegularity,
): SleepCoachTip? {
    if (regularity.level == RegularityLevel.Irregular) return SleepCoachTip.RegularBedtime
    val bedtimes = nights.mapNotNull { it.bedtime }
    if (bedtimes.size < MIN_TIP_NIGHTS) return null
    val near = bedtimes.count { distance(it, preferences.bedtime) <= NEAR_BEDTIME_MINUTES }
    return SleepCoachTip.RegularBedtime.takeIf { near * 2 < bedtimes.size }
}

/** The wake-up goal is more than an hour away at least half the nights. */
private fun wakeTip(nights: List<DayEntry>, preferences: SleepCoachPreferences): SleepCoachTip? {
    val wakeTimes = nights.mapNotNull { it.wakeTime }
    if (wakeTimes.size < MIN_TIP_NIGHTS) return null
    val far = wakeTimes.count { distance(it, preferences.wakeTime) > FAR_WAKE_MINUTES }
    return SleepCoachTip.FixedWakeTime.takeIf { far * 2 >= wakeTimes.size }
}

/**
 * Too little of the time spent in bed is actually asleep: the KBT-I advice is to get out of bed when sleep won't
 * come and keep the bed for sleep.
 */
private fun efficiencyTip(nights: List<DayEntry>): SleepCoachTip? {
    val slept = nights.filter { it.bedtime != null && it.wakeTime != null && it.sleepHours > 0f }
    if (slept.size < MIN_TIP_NIGHTS) return null
    val efficiency = slept.map { day ->
        val inBed = minutesBetween(day.bedtime!!, day.wakeTime!!).toDouble()
        (day.sleepHours.toDouble() * 60).coerceAtMost(inBed) / inBed
    }.average()
    return SleepCoachTip.OutOfBed.takeIf { efficiency < MIN_SLEEP_EFFICIENCY }
}

/**
 * The most often logged habit, when it was logged before three or more nights. A period is not a habit to change, so
 * it never becomes a tip.
 */
private fun factorTips(nights: List<DayEntry>): List<SleepCoachTip> {
    val detailed = nights.filter { it.sleepQuality > 0 || it.sleepFactors.isNotEmpty() }
    if (detailed.size < MIN_TIP_NIGHTS) return emptyList()
    return detailed.flatMap { it.sleepFactors - SleepFactor.Period }
        .groupingBy { it }.eachCount()
        .filterValues { it >= FACTOR_NIGHTS }
        .entries
        .sortedWith(compareByDescending<Map.Entry<SleepFactor, Int>> { it.value }.thenBy { it.key.ordinal })
        .firstOrNull()
        ?.let { SleepCoachTip.entries.first { tip -> tip.name == it.key.name } }
        ?.let { listOf(it) }
        .orEmpty()
}

/** The last week's nights have been at least half an hour shorter than the goal. */
private fun shortTip(nights: List<DayEntry>, goals: Goals): SleepCoachTip? {
    val slept = nights.takeLast(WEEK_NIGHTS).map { it.sleepHours }.filter { it > 0f }
    if (slept.size < MIN_TIP_NIGHTS) return null
    return SleepCoachTip.ShortNights.takeIf { slept.average() < goals.sleepHours - 0.5f }
}

/** The regularity of the coach's nights, up to the last of [days] (the coach's two weeks, so two weekends). */
private fun regularity(days: List<DayEntry>): SleepRegularity {
    val today = days.lastOrNull()?.date ?: return SleepRegularity(null, 0, null, 0, 0)
    return sleepRegularity(days.takeLast(WINDOW_NIGHTS), today)
}

/** The shorter way around the clock between two times of day, in minutes. */
private fun distance(a: LocalTime, b: LocalTime): Int {
    val apart = abs(minutesFrom(a) - minutesFrom(b))
    return minOf(apart, MINUTES_PER_DAY - apart)
}

/** Minutes since noon, so times around midnight sit next to each other. */
private fun minutesFrom(time: LocalTime) = (time.hour * 60 + time.minute - NOON + MINUTES_PER_DAY) % MINUTES_PER_DAY

private fun minutesBetween(bedtime: LocalTime, wakeTime: LocalTime): Int =
    (minutesFrom(wakeTime) - minutesFrom(bedtime) + MINUTES_PER_DAY) % MINUTES_PER_DAY

/**
 * Stores [SleepCoachPreferences] as text: "s1;bedtime;wakeTime;steps", where the times are minutes after midnight
 * and the steps are comma-separated names. Plain Kotlin, so it is unit tested. Unknown names (from a later
 * version) are skipped, and anything unreadable gives null: the coach is then simply not set up.
 */
object SleepCoachPreferencesCodec {
    private const val VERSION = "s1"

    fun encode(preferences: SleepCoachPreferences): String = with(preferences) {
        listOf(
            VERSION,
            bedtime.hour * 60 + bedtime.minute,
            wakeTime.hour * 60 + wakeTime.minute,
            windDown.sorted().joinToString(",") { it.name },
        ).joinToString(";")
    }

    fun decode(raw: String?): SleepCoachPreferences? {
        val parts = raw?.split(';') ?: return null
        if (parts.size != 4 || parts[0] != VERSION) return null
        val bedtime = time(parts[1]) ?: return null
        val wakeTime = time(parts[2]) ?: return null
        val steps = parts[3].split(',')
            .mapNotNull { name -> WindDownStep.entries.firstOrNull { it.name == name } }
            .toSet()
        return SleepCoachPreferences(bedtime = bedtime, wakeTime = wakeTime, windDown = steps)
    }

    private fun time(raw: String?): LocalTime? =
        raw?.toIntOrNull()?.takeIf { it in 0 until MINUTES_PER_DAY }?.let { LocalTime.of(it / 60, it % 60) }
}
