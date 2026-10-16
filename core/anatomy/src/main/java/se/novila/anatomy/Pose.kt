package se.novila.anatomy

import kotlin.math.floor

/**
 * One arm or leg, in degrees from standing with the limb hanging straight down.
 * [raise] swings it forward (negative: backward), [spread] out to the side and [bend] flexes the knee or
 * elbow. Angles are relative to the torso, the way a physiotherapist measures them.
 */
data class LimbPose(
    val raise: Float = 0f,
    val spread: Float = 0f,
    val bend: Float = 0f,
) {
    fun lerpTo(other: LimbPose, fraction: Float) = LimbPose(
        raise = lerp(raise, other.raise, fraction),
        spread = lerp(spread, other.spread, fraction),
        bend = lerp(bend, other.bend, fraction),
    )
}

/**
 * The whole body as joint angles in degrees, so a pose fits any body shape and any camera.
 * [lean] tips the whole body forward around the hips (90: face down, -90: lying on the back), [trunk]
 * bends the upper body forward and [neck] the head. [leftFoot] and [rightFoot] point the toes down
 * from flat on the floor, regardless of the legs, so planted feet stay flat. With [flatHands] the hands
 * lie flat on the floor, fingers forward, as in a push-up.
 */
data class Pose(
    val lean: Float = 0f,
    val trunk: Float = 0f,
    val neck: Float = 0f,
    val leftLeg: LimbPose = LimbPose(),
    val rightLeg: LimbPose = LimbPose(),
    val leftArm: LimbPose = LimbPose(),
    val rightArm: LimbPose = LimbPose(),
    val leftFoot: Float = 0f,
    val rightFoot: Float = 0f,
    val flatHands: Boolean = false,
) {
    fun lerpTo(other: Pose, fraction: Float) = Pose(
        lean = lerp(lean, other.lean, fraction),
        trunk = lerp(trunk, other.trunk, fraction),
        neck = lerp(neck, other.neck, fraction),
        leftLeg = leftLeg.lerpTo(other.leftLeg, fraction),
        rightLeg = rightLeg.lerpTo(other.rightLeg, fraction),
        leftArm = leftArm.lerpTo(other.leftArm, fraction),
        rightArm = rightArm.lerpTo(other.rightArm, fraction),
        leftFoot = lerp(leftFoot, other.leftFoot, fraction),
        rightFoot = lerp(rightFoot, other.rightFoot, fraction),
        flatHands = if (fraction < 0.5f) flatHands else other.flatHands,
    )

    companion object {
        val STANDING = Pose()

        /** The same [leg] and [arm] pose on both sides, for symmetric exercises. */
        fun symmetric(
            leg: LimbPose = LimbPose(),
            arm: LimbPose = LimbPose(),
            lean: Float = 0f,
            trunk: Float = 0f,
            neck: Float = 0f,
            feet: Float = 0f,
            flatHands: Boolean = false,
        ) = Pose(lean, trunk, neck, leg, leg, arm, arm, feet, feet, flatHands)
    }
}

/** A [pose] reached at [at] (0–1) of one repetition. */
data class Keyframe(val at: Float, val pose: Pose)

/**
 * One repetition of an exercise as keyframes from [Keyframe.at] 0 to 1. The last keyframe should
 * return to the first pose, so repetitions loop seamlessly. Between keyframes the pose eases in and
 * out, for a calm, controlled tempo.
 */
class Movement(val keyframes: List<Keyframe>) {
    init {
        require(keyframes.size >= 2) { "A movement needs at least two keyframes" }
        require(keyframes.first().at == 0f && keyframes.last().at == 1f) { "Keyframes must span 0 to 1" }
        require(keyframes.zipWithNext().all { (a, b) -> a.at < b.at }) { "Keyframes must be in order" }
    }

    /** The pose at [progress] repetitions; whole numbers are the start of a repetition. */
    fun poseAt(progress: Float): Pose {
        val fraction = progress - floor(progress)
        val next = keyframes.indexOfFirst { it.at > fraction }.takeIf { it > 0 } ?: return keyframes.last().pose
        val from = keyframes[next - 1]
        val to = keyframes[next]
        val local = (fraction - from.at) / (to.at - from.at)
        return from.pose.lerpTo(to.pose, smoothstep(local))
    }

    private fun smoothstep(t: Float) = t * t * (3f - 2f * t)
}
