package se.novila.coach

import se.novila.anatomy.Equipment
import se.novila.anatomy.MuscleGroup
import java.time.DayOfWeek

/**
 * What the user wants from their training. It decides how the week is split and how exercises are dosed.
 * [Rehab] is a calm, phased program for a [RehabArea] instead of the usual full-body sessions.
 */
enum class CoachGoal { Health, Strength, Endurance, Mobility, Rehab }

/** What a rehab program is for. Each has its own exercises per phase, see CoachPlanner. */
enum class RehabArea { Knee, LowerBack, Shoulder, Neck, Hip }

/** Parts of the body the user wants extra work on. */
enum class BodyArea(val muscles: Set<MuscleGroup>) {
    Legs(setOf(MuscleGroup.Quads, MuscleGroup.Glutes, MuscleGroup.Hamstrings, MuscleGroup.Calves)),
    Core(setOf(MuscleGroup.Abs, MuscleGroup.LowerBack)),
    UpperBody(
        setOf(
            MuscleGroup.Chest,
            MuscleGroup.Shoulders,
            MuscleGroup.Triceps,
            MuscleGroup.Biceps,
            MuscleGroup.UpperBack,
            MuscleGroup.Lats,
        ),
    ),
}

/** Things the program must take into account. Exercises that don't fit are left out. */
enum class Consideration {
    /** No jumps or lunges. */
    Knees,

    /** No exercises that bend or arch the back under load. */
    Back,

    /** No jumps, for an apartment with neighbors below. */
    Quiet,
}

/**
 * The user's answers to the coach: [goal], how many [daysPerWeek] they can train and for how many
 * [sessionMinutes], areas to [focus] on and [considerations]. [variation] is bumped for a new
 * suggestion with the same answers; 0 is the coach's first choice. With the [CoachGoal.Rehab] goal,
 * [rehabArea] and [rehabPhase] (1 to 3, from gentle to harder) choose the exercises. [equipment] is what the
 * user has at hand; without any, the sessions use bodyweight alone. [days] are the weekdays the user picked to train
 * on; empty lets the coach spread [daysPerWeek] days over the week ([trainingDays]). When there are days,
 * [daysPerWeek] is how many.
 */
data class CoachPreferences(
    val goal: CoachGoal = CoachGoal.Health,
    val daysPerWeek: Int = 4,
    val sessionMinutes: Int = 30,
    val focus: Set<BodyArea> = emptySet(),
    val considerations: Set<Consideration> = emptySet(),
    val variation: Int = 0,
    val rehabArea: RehabArea = RehabArea.Knee,
    val rehabPhase: Int = 1,
    val equipment: Set<Equipment> = emptySet(),
    val days: Set<DayOfWeek> = emptySet(),
) {
    /** The days to train on: the user's own, or [daysPerWeek] days spread over the week. */
    val trainingDays: List<DayOfWeek>
        get() = days.sorted().ifEmpty { SPREAD_DAYS.getValue(daysPerWeek.coerceIn(MIN_DAYS, MAX_DAYS)) }

    /** [days] days a week, which the coach spreads out again. */
    fun withDays(days: Int) = copy(daysPerWeek = days.coerceIn(MIN_DAYS, MAX_DAYS), days = emptySet())

    /**
     * Trains on [day] or not, starting from the [trainingDays]. At least [MIN_DAYS] and at most [MAX_DAYS] days stay,
     * so a change that would leave fewer or more does nothing.
     */
    fun toggle(day: DayOfWeek): CoachPreferences {
        val chosen = trainingDays.toSet().let { if (day in it) it - day else it + day }
        if (chosen.size !in MIN_DAYS..MAX_DAYS) return this
        return copy(days = chosen, daysPerWeek = chosen.size)
    }

    /** Back to as many days, spread out by the coach. */
    fun withSpreadDays() = copy(days = emptySet())

    fun withMinutes(minutes: Int) =
        copy(sessionMinutes = (minutes / MINUTES_STEP * MINUTES_STEP).coerceIn(MIN_MINUTES, MAX_MINUTES))

    fun toggle(area: BodyArea) = copy(focus = if (area in focus) focus - area else focus + area)

    fun toggle(consideration: Consideration) = copy(
        considerations = if (consideration in considerations) {
            considerations - consideration
        } else {
            considerations + consideration
        },
    )

    fun toggle(item: Equipment) = copy(equipment = if (item in equipment) equipment - item else equipment + item)

    fun withRehabPhase(phase: Int) = copy(rehabPhase = phase.coerceIn(1, MAX_REHAB_PHASE))

    /** A different program from the same answers. */
    fun nextVariation() = copy(variation = variation + 1)

    companion object {
        /** One day a week is a start; the coach says what WHO advises on top of it. */
        const val MIN_DAYS = 1
        const val MAX_DAYS = 6
        const val MIN_MINUTES = 15
        const val MAX_MINUTES = 60
        const val MINUTES_STEP = 5
        const val MAX_REHAB_PHASE = 3

        /** The days for each number of days a week, spread over the week so there is rest in between. */
        private val SPREAD_DAYS: Map<Int, List<DayOfWeek>> = mapOf(
            1 to listOf(DayOfWeek.WEDNESDAY),
            2 to listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            3 to listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            4 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
            5 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
            6 to DayOfWeek.entries.take(6),
        )
    }
}

/**
 * Stores [CoachPreferences] as text:
 * "c4;goal;days;minutes;focus;considerations;variation;rehabArea;rehabPhase;equipment;weekdays", where focus,
 * considerations, equipment and the weekdays the user picked are comma-separated names. c1 (before rehab), c2 (before
 * equipment) and c3 (before picked weekdays) are still read. Plain Kotlin, so it is
 * unit tested. Unknown names (from a later version) are skipped, and anything unreadable gives null: the coach is
 * then simply not set up.
 */
object CoachPreferencesCodec {
    private const val VERSION = "c4"
    private val FIELDS = mapOf("c1" to 7, "c2" to 9, "c3" to 10, VERSION to 11)

    fun encode(preferences: CoachPreferences): String = with(preferences) {
        listOf(
            VERSION,
            goal.name,
            daysPerWeek,
            sessionMinutes,
            focus.sorted().joinToString(",") { it.name },
            considerations.sorted().joinToString(",") { it.name },
            variation,
            rehabArea.name,
            rehabPhase,
            equipment.sorted().joinToString(",") { it.name },
            days.sorted().joinToString(",") { it.name },
        ).joinToString(";")
    }

    fun decode(raw: String?): CoachPreferences? {
        val parts = raw?.split(';') ?: return null
        if (FIELDS[parts[0]] != parts.size) return null
        val goal = CoachGoal.entries.firstOrNull { it.name == parts[1] } ?: return null
        val days = parts[2].toIntOrNull() ?: return null
        val minutes = parts[3].toIntOrNull() ?: return null
        return CoachPreferences(
            goal = goal,
            focus = names(parts[4], BodyArea.entries),
            considerations = names(parts[5], Consideration.entries),
            variation = parts[6].toIntOrNull()?.coerceAtLeast(0) ?: 0,
            rehabArea = RehabArea.entries.firstOrNull { it.name == parts.getOrNull(7) } ?: RehabArea.Knee,
            equipment = parts.getOrNull(9)?.let { names(it, Equipment.entries) }.orEmpty(),
        ).withDays(days).withMinutes(minutes).withRehabPhase(parts.getOrNull(8)?.toIntOrNull() ?: 1)
            .withPicked(parts.getOrNull(10)?.let { names(it, DayOfWeek.entries) }.orEmpty())
    }

    /** The picked weekdays, when there are as many as a week may have. */
    private fun CoachPreferences.withPicked(days: Set<DayOfWeek>): CoachPreferences =
        if (days.size in CoachPreferences.MIN_DAYS..CoachPreferences.MAX_DAYS) {
            copy(days = days, daysPerWeek = days.size)
        } else {
            this
        }

    private fun <T : Enum<T>> names(raw: String, entries: List<T>): Set<T> =
        raw.split(',').mapNotNull { name -> entries.firstOrNull { it.name == name } }.toSet()
}
