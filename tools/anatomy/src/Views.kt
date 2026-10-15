import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.ViewSelector

/** Prints each exercise's best camera next to the stored one, and the runners-up. */
fun main(args: Array<String>) {
    ExerciseLibrary.all.filter { args.isEmpty() || it.id in args }.forEach { motion ->
        val best = ViewSelector.best(motion)
        val top = ViewSelector.scores(motion).entries.sortedByDescending { it.value }.take(4)
            .joinToString { "${it.key.yaw.toInt()}/${it.key.pitch.toInt()} ${"%.3f".format(it.value)}" }
        val status = if (best == motion.camera) "ok" else "CHANGE TO Camera(${best.yaw}f, pitch = ${best.pitch}f)"
        println("${motion.id}: $status  (top: $top)")
    }
}
