package se.novila.model

/** Today's training as the week's program has it: [plannedMinutes] for today and the [doneMinutes] so far. */
data class TrainingToday(val plannedMinutes: Int = 0, val doneMinutes: Int = 0) {
    /** Nothing planned and nothing done: a rest day. */
    val isRest: Boolean get() = plannedMinutes == 0 && doneMinutes == 0
}

/** One thing to do today, and whether it is [done]. */
sealed interface DayTask {
    val done: Boolean

    /** Last night's sleep, [hours] logged or from a watch; 0 until it is. */
    data class Sleep(val hours: Float) : DayTask {
        override val done: Boolean get() = hours > 0f
    }

    /** Today's training. A rest day has nothing to do, so it counts as done. */
    data class Training(val today: TrainingToday) : DayTask {
        override val done: Boolean
            get() = today.isRest || (today.doneMinutes > 0 && today.doneMinutes >= today.plannedMinutes)
    }

    /** Today's nutrition [score] (null before anything is logged) and the [next] portion that raises it the most. */
    data class Nutrition(val score: Int?, val next: NextPortion?) : DayTask {
        override val done: Boolean get() = score != null && (score >= GOOD_NUTRITION_SCORE || next == null)
    }

    /** Glasses of water against the user's goal. */
    data class Water(val glasses: Int, val goal: Int) : DayTask {
        override val done: Boolean get() = glasses >= goal
    }

    /** How the user feels today, 0 until they say. */
    data class Mood(val mood: Int) : DayTask {
        override val done: Boolean get() = mood > 0
    }
}

/** Today's [tasks] in the order of the day: sleep, training, food, water and how the user feels. */
data class DayChecklist(val tasks: List<DayTask>) {
    val doneCount: Int get() = tasks.count { it.done }

    val allDone: Boolean get() = tasks.all { it.done }

    /** The first task still to do, the one Today puts forward. Null when everything is done. */
    val next: DayTask? get() = tasks.firstOrNull { !it.done }
}

/**
 * What [day] still needs, measured against [goals]: sleep, the [training] the week's program has today, food until
 * the nutrition score is a good day's, water up to the goal, and how the user feels. Training counts as done once
 * as many minutes as planned are trained, sleep once a night is logged or a watch recorded one.
 */
fun dayChecklist(day: DayEntry, goals: Goals, training: TrainingToday): DayChecklist = DayChecklist(
    listOf(
        DayTask.Sleep(day.sleepHours),
        DayTask.Training(training),
        DayTask.Nutrition(day.nutritionScore(goals)?.total, day.nextPortion(goals)),
        DayTask.Water(day.waterGlasses, goals.waterGlasses),
        DayTask.Mood(day.mood),
    ),
)
