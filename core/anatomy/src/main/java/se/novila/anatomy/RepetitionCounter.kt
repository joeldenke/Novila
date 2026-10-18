package se.novila.anatomy

import kotlin.math.sqrt

/**
 * One attempt at a repetition, from leaving the start pose until back at it. [depth] is how far towards the
 * turning point it went (1: all the way, as in the [ExerciseMotion]), [durationMillis] how long it was away
 * from the start, and [rushed] whether that was less than half as long as at the exercise's calm tempo
 * ([ExerciseMotion.repetitionMillis]).
 */
data class Repetition(val depth: Float, val durationMillis: Long, val rushed: Boolean) {
    val complete: Boolean get() = depth >= RepetitionCounter.FULL_DEPTH
}

/**
 * Counts repetitions of [motion] from observed joint angles, e.g. what a pose estimator sees through the
 * camera, converted to a [Pose]. Each observation is placed on the line from the motion's first keyframe to
 * its turning point (the keyframe farthest from it), measured only in the angles the motion moves, so joints
 * the exercise doesn't use, or the camera can't see, don't matter. A repetition is counted when the body
 * reaches near the turning point and comes back to the start. Poses far off that line (walking up to the
 * phone, the other leg in mountain climbers) are ignored.
 *
 * Pure and deterministic: the same observations always give the same count.
 */
class RepetitionCounter(val motion: ExerciseMotion) {
    private val active: List<Int>
    private val start: FloatArray
    private val axis: FloatArray
    private val axisLengthSquared: Float

    /** How long a repetition at the calm tempo is away from the start. */
    private val calmMillis: Int

    init {
        require(!motion.isHold) { "${motion.id} is a hold, it has no repetitions" }
        val keyframes = motion.movement.keyframes.map { angles(it.pose) }
        active = keyframes.first().indices.filter { i ->
            keyframes.maxOf { it[i] } - keyframes.minOf { it[i] } > ACTIVE_RANGE
        }
        start = keyframes.first().pick()
        val turn = keyframes.map { it.pick() }.maxBy { distanceSquared(it, start) }
        axis = FloatArray(active.size) { turn[it] - start[it] }
        axisLengthSquared = axis.sumOf { (it * it).toDouble() }.toFloat()
        val away = (0 until TEMPO_SAMPLES).count { sample ->
            (progressOf(motion.movement.poseAt(sample / TEMPO_SAMPLES.toFloat())) ?: 0f) > LEAVE
        }
        calmMillis = motion.repetitionMillis * away / TEMPO_SAMPLES
    }

    /** Complete repetitions so far. */
    var count: Int = 0
        private set

    private var smoothed: Float? = null
    private var attemptStart: Long? = null
    private var depth = 0f

    /**
     * Adds what was observed at [timeMillis]. Returns the [Repetition] when an attempt ends back at the
     * start, or null. Small wobbles that never go [MIN_DEPTH] of the way aren't attempts.
     */
    fun add(timeMillis: Long, pose: Pose): Repetition? {
        val progress = progressOf(pose) ?: return null
        val value = smoothed?.let { it + SMOOTHING * (progress - it) } ?: progress
        smoothed = value
        val began = attemptStart
        if (began == null) {
            if (value > LEAVE) {
                attemptStart = timeMillis
                depth = value
            }
            return null
        }
        depth = maxOf(depth, value)
        if (value >= RETURN) return null
        attemptStart = null
        if (depth < MIN_DEPTH) return null
        val duration = timeMillis - began
        val repetition = Repetition(minOf(depth, 1f), duration, rushed = duration < calmMillis * RUSHED_FRACTION)
        if (repetition.complete) count++
        return repetition
    }

    /** How far [pose] is from the start towards the turning point (0–1), or null when off the movement. */
    fun progressOf(pose: Pose): Float? {
        val observed = angles(pose).pick()
        val offset = FloatArray(active.size) { observed[it] - start[it] }
        val progress = offset.indices.sumOf { (offset[it] * axis[it]).toDouble() }.toFloat() / axisLengthSquared
        val off = offset.indices.sumOf {
            val residual = offset[it] - progress * axis[it]
            (residual * residual).toDouble()
        }
        return progress.takeIf { sqrt(off / axisLengthSquared) <= MAX_OFF_PATH }
    }

    private fun FloatArray.pick() = FloatArray(active.size) { this[active[it]] }

    companion object {
        /** Deep enough to count, as a share of the way to the turning point. */
        const val FULL_DEPTH = 0.8f

        /** Shallower attempts than this are wobbles, not repetitions. */
        const val MIN_DEPTH = 0.5f

        /** Leaving the start and returning to it, with a gap so noise doesn't count twice. */
        private const val LEAVE = 0.3f
        private const val RETURN = 0.2f

        /** Less than this share of the calm time away from the start is rushed. */
        private const val RUSHED_FRACTION = 0.5f
        private const val TEMPO_SAMPLES = 200

        /** How much of each new observation the smoothed progress takes, against noisy estimates. */
        private const val SMOOTHING = 0.4f

        /** Angles that change less than this (degrees) through the motion aren't part of it. */
        private const val ACTIVE_RANGE = 5f

        /** Distance from the movement's line, relative to its length, beyond which a pose is ignored. */
        private const val MAX_OFF_PATH = 0.5

        private fun angles(pose: Pose) = with(pose) {
            floatArrayOf(lean, trunk, neck, leftFoot, rightFoot) +
                listOf(leftLeg, rightLeg, leftArm, rightArm).flatMap { listOf(it.raise, it.spread, it.bend) }
        }

        private fun distanceSquared(a: FloatArray, b: FloatArray) =
            a.indices.sumOf { ((a[it] - b[it]) * (a[it] - b[it])).toDouble() }
    }
}
