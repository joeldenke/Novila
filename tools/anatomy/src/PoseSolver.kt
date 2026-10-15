import se.novila.anatomy.Landmark
import se.novila.anatomy.Pose
import se.novila.anatomy.PosedBody
import se.novila.anatomy.Skeleton
import se.novila.anatomy.Vec3

/**
 * Helpers for solving keyframe angles so planted hands, feet or knees stay put, used from a small
 * file passed to `anatomy.sh run`. See .claude/skills/add-exercise/SKILL.md for an example.
 */
object PoseSolver {
    /** Pattern search: nudges each parameter up and down, halving the step until nothing improves. */
    fun minimize(start: DoubleArray, cost: (DoubleArray) -> Double): DoubleArray {
        var x = start.copyOf()
        var best = cost(x)
        var step = 8.0
        while (step > 0.005) {
            var improved = false
            for (i in x.indices) {
                for (delta in listOf(step, -step)) {
                    val y = x.copyOf().also { it[i] += delta }
                    val value = cost(y)
                    if (value < best) {
                        best = value
                        x = y
                        improved = true
                    }
                }
            }
            if (!improved) step /= 2
        }
        println("residual %.6f (0 means every constraint is met)".format(best))
        return x
    }

    /** The posed body moved so [anchor] sits at the origin, as ExerciseMotion.bodyAt does. */
    fun body(pose: Pose, anchor: Landmark): PosedBody =
        Skeleton.pose(pose).let { it.moved(Vec3.ZERO - it.landmarks.getValue(anchor)) }

    fun landmark(pose: Pose, landmark: Landmark, anchor: Landmark): Vec3 = body(
        pose,
        anchor,
    ).landmarks.getValue(landmark)

    /** How far [landmark]'s skin is above the floor that [anchor] stands on. */
    fun heightAboveFloor(pose: Pose, landmark: Landmark, anchor: Landmark): Float =
        landmark(pose, landmark, anchor).y - Skeleton.CONTACT_RADIUS.getValue(landmark) +
            Skeleton.CONTACT_RADIUS.getValue(anchor) - landmark(pose, anchor, anchor).y

    fun squared(value: Number): Double = value.toDouble() * value.toDouble()
}
