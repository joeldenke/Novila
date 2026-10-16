package se.novila.anatomy

/**
 * The body's build, as factors on an average adult's proportions. The figure is always measured in
 * body heights, so height itself never changes the drawing. Neutral by default; a user profile can
 * widen the shoulders or hips or make the figure fuller or slimmer.
 */
data class BodyShape(
    val shoulders: Float = 1f,
    val hips: Float = 1f,
    val girth: Float = 1f,
) {
    companion object {
        val NEUTRAL = BodyShape()
    }
}

enum class Side(val sign: Float) { Left(1f), Right(-1f) }

/** The drawn parts of the body. Arms and legs come in pairs, see [Segment.side]. */
enum class BodyPart(val sided: Boolean) {
    Head(false),
    Neck(false),
    UpperTorso(false),
    LowerTorso(false),
    UpperArm(true),
    Forearm(true),
    Hand(true),
    Thigh(true),
    Shank(true),
    Foot(true),
}

/** Points on the body that exercises plant on the floor, see [ExerciseMotion.contacts]. */
enum class Landmark {
    LeftToe,
    RightToe,
    LeftHeel,
    RightHeel,
    LeftHand,
    RightHand,
    LeftKnee,
    RightKnee,
    Belly,
    UpperBack,
    Head,
}

/** The half widths of a body part's oval cross-section: to the side and front to back. */
data class Radii(val lateral: Float, val depth: Float) {
    operator fun times(scale: Float) = Radii(lateral * scale, depth * scale)
}

/**
 * A body part as a tapered tube from [start] to [end]. [frame] orients it: its y axis runs along the
 * part and its z axis faces the front of the body. [roundness] is how round the ends are, from flat (0)
 * to a full half circle (1).
 */
data class Segment(
    val part: BodyPart,
    val side: Side?,
    val start: Vec3,
    val end: Vec3,
    val frame: Rotation,
    val startRadii: Radii,
    val endRadii: Radii,
    val roundness: Float,
) {
    /** The cross-section's side axis, pointing away from the body's midline on arms and legs. */
    val lateral: Vec3 get() = frame.xAxis * (side?.sign ?: 1f)

    fun radiiAt(t: Float) = Radii(
        lerp(startRadii.lateral, endRadii.lateral, t.coerceIn(0f, 1f)),
        lerp(startRadii.depth, endRadii.depth, t.coerceIn(0f, 1f)),
    )
}

/** A posed body: its segments and the landmarks exercises plant on the floor. */
class PosedBody(val segments: List<Segment>, val landmarks: Map<Landmark, Vec3>) {
    fun moved(offset: Vec3) = PosedBody(
        segments.map { it.copy(start = it.start + offset, end = it.end + offset) },
        landmarks.mapValues { it.value + offset },
    )
}

/**
 * Average adult proportions in body heights (after Drillis and Contini's segment lengths), and the
 * forward kinematics that turn a [Pose] into a [PosedBody]. The pelvis is the root, at the origin.
 */
object Skeleton {
    private const val THIGH = 0.245f
    private const val SHANK = 0.246f
    private const val FOOT = 0.115f
    private const val ANKLE_HEIGHT = 0.039f
    private const val HIP_HALF_WIDTH = 0.085f
    private const val LOWER_TORSO = 0.1f
    private const val UPPER_TORSO = 0.19f
    private const val SHOULDER_HALF_WIDTH = 0.112f
    private const val SHOULDER_DROP = 0.022f
    private const val UPPER_ARM = 0.186f
    private const val FOREARM = 0.146f
    private const val HAND = 0.07f
    private const val NECK = 0.05f
    private const val HEAD = 0.075f

    /** Contact landmarks sit this far inside the skin, so a planted point touches the floor at its radius. */
    val CONTACT_RADIUS = mapOf(
        Landmark.LeftToe to 0.014f,
        Landmark.RightToe to 0.014f,
        Landmark.LeftHeel to 0.02f,
        Landmark.RightHeel to 0.02f,
        Landmark.LeftHand to 0.02f,
        Landmark.RightHand to 0.02f,
        Landmark.LeftKnee to 0.036f,
        Landmark.RightKnee to 0.036f,
        Landmark.Belly to 0.06f,
        Landmark.UpperBack to 0.06f,
        Landmark.Head to 0.06f,
    )

    fun pose(pose: Pose, shape: BodyShape = BodyShape.NEUTRAL): PosedBody {
        val g = shape.girth
        val segments = mutableListOf<Segment>()
        val landmarks = mutableMapOf<Landmark, Vec3>()
        val root = Rotation.aboutX(pose.lean)
        val pelvis = Vec3.ZERO

        val waist = pelvis + root * Vec3(0f, LOWER_TORSO, 0f)
        // The middle of the lower torso; with its contact radius it is the belly or the lower back.
        landmarks[Landmark.Belly] = pelvis + root * Vec3(0f, 0.045f, 0f)
        segments += Segment(
            BodyPart.LowerTorso,
            null,
            pelvis + root * Vec3(0f, -0.035f, 0f),
            waist + root * Vec3(0f, 0.02f, 0f),
            root,
            Radii(0.098f * shape.hips, 0.064f) * g,
            Radii(0.082f, 0.056f) * g,
            roundness = 0.55f,
        )

        val chest = root * Rotation.aboutX(pose.trunk)
        val neckBase = waist + chest * Vec3(0f, UPPER_TORSO, 0f)
        segments += Segment(
            BodyPart.UpperTorso,
            null,
            waist + chest * Vec3(0f, -0.01f, 0f),
            neckBase,
            chest,
            Radii(0.084f, 0.058f) * g,
            Radii(0.1f * shape.shoulders, 0.062f) * g,
            roundness = 0.5f,
        )
        landmarks[Landmark.UpperBack] = neckBase + chest * Vec3(0f, -0.06f, 0f)

        val neckFrame = chest * Rotation.aboutX(pose.neck)
        val neckTop = neckBase + neckFrame * Vec3(0f, NECK, 0.006f)
        segments += Segment(
            BodyPart.Neck,
            null,
            neckBase + neckFrame * Vec3(0f, -0.02f, 0f),
            neckTop,
            neckFrame,
            Radii(0.03f, 0.032f) * g,
            Radii(0.027f, 0.029f) * g,
            roundness = 1f,
        )
        val headStart = neckTop + neckFrame * Vec3(0f, 0.035f, 0.01f)
        val headEnd = headStart + neckFrame * Vec3(0f, HEAD - 0.045f, -0.004f)
        segments += Segment(
            BodyPart.Head,
            null,
            headStart,
            headEnd,
            neckFrame,
            Radii(0.05f, 0.058f),
            Radii(0.05f, 0.058f),
            roundness = 1f,
        )
        landmarks[Landmark.Head] = (headStart + headEnd) * 0.5f

        for (side in Side.entries) {
            val left = side == Side.Left
            val shank = leg(side, if (left) pose.leftLeg else pose.rightLeg, root, shape, segments)
            landmarks[if (left) Landmark.LeftKnee else Landmark.RightKnee] = shank.start
            segments += foot(side, shank.end, if (left) pose.leftFoot else pose.rightFoot, g, landmarks)
            val hand =
                arm(side, if (left) pose.leftArm else pose.rightArm, pose.flatHands, waist, chest, shape, segments)
            landmarks[if (left) Landmark.LeftHand else Landmark.RightHand] = hand
        }
        return PosedBody(segments, landmarks)
    }

    /** Adds the thigh and shank and returns the shank, which ends at the ankle. */
    private fun leg(
        side: Side,
        limb: LimbPose,
        root: Rotation,
        shape: BodyShape,
        segments: MutableList<Segment>,
    ): Segment {
        val g = shape.girth
        val hip = root * Vec3(side.sign * HIP_HALF_WIDTH * shape.hips, 0f, 0f)
        val thigh = root * Rotation.aboutZ(side.sign * limb.spread) * Rotation.aboutX(-limb.raise)
        val knee = hip + thigh * Vec3(0f, -THIGH, 0f)
        segments += Segment(
            BodyPart.Thigh,
            side,
            hip,
            knee,
            thigh,
            Radii(0.056f, 0.058f) * g,
            Radii(0.034f, 0.036f) * g,
            roundness = 1f,
        )
        val shankFrame = thigh * Rotation.aboutX(limb.bend)
        val shank = Segment(
            BodyPart.Shank,
            side,
            knee,
            knee + shankFrame * Vec3(0f, -SHANK, 0f),
            shankFrame,
            Radii(0.036f, 0.039f) * g,
            Radii(0.022f, 0.024f) * g,
            roundness = 1f,
        )
        segments += shank
        return shank
    }

    /**
     * The foot hangs off the [ankle] but keeps its own angle to the floor, [toesDown], so planted feet
     * stay flat whatever the legs do. Its frame's y axis runs from heel to toe and its z axis is the sole.
     */
    private fun foot(
        side: Side,
        ankle: Vec3,
        toesDown: Float,
        g: Float,
        landmarks: MutableMap<Landmark, Vec3>,
    ): Segment {
        val frame = Rotation.aboutX(90f + toesDown)
        val heel = ankle + frame * Vec3(0f, -0.03f, ANKLE_HEIGHT - 0.02f)
        val toe = heel + frame * Vec3(0f, FOOT + 0.03f, 0.006f)
        landmarks[if (side == Side.Left) Landmark.LeftToe else Landmark.RightToe] = toe
        landmarks[if (side == Side.Left) Landmark.LeftHeel else Landmark.RightHeel] = heel
        return Segment(
            BodyPart.Foot,
            side,
            heel,
            toe,
            frame,
            Radii(0.026f, 0.02f) * g,
            Radii(0.03f, 0.014f) * g,
            roundness = 1f,
        )
    }

    /**
     * Adds the arm and returns the hand's contact landmark: the fingertips, or with [flatHands] a point
     * just above the palm, so either touches the floor at the same contact radius.
     */
    private fun arm(
        side: Side,
        limb: LimbPose,
        flatHands: Boolean,
        waist: Vec3,
        chest: Rotation,
        shape: BodyShape,
        segments: MutableList<Segment>,
    ): Vec3 {
        val g = shape.girth
        val shoulder = waist +
            chest * Vec3(side.sign * SHOULDER_HALF_WIDTH * shape.shoulders, UPPER_TORSO - SHOULDER_DROP, -0.004f)
        val upper = chest * Rotation.aboutZ(side.sign * limb.spread) * Rotation.aboutX(-limb.raise)
        val elbow = shoulder + upper * Vec3(0f, -UPPER_ARM, 0f)
        segments += Segment(
            BodyPart.UpperArm,
            side,
            shoulder,
            elbow,
            upper,
            Radii(0.036f, 0.038f) * g,
            Radii(0.024f, 0.026f) * g,
            roundness = 1f,
        )
        val fore = upper * Rotation.aboutX(-limb.bend)
        val wrist = elbow + fore * Vec3(0f, -FOREARM, 0f)
        segments += Segment(
            BodyPart.Forearm,
            side,
            elbow,
            wrist,
            fore,
            Radii(0.025f, 0.026f) * g,
            Radii(0.018f, 0.016f) * g,
            roundness = 1f,
        )
        val handRadii = Radii(0.022f, 0.012f) * g
        if (flatHands) {
            // Palm down, fingers forward: y runs to the fingertips and z points down through the palm.
            val palm = Rotation.aboutX(90f)
            segments += Segment(
                BodyPart.Hand,
                side,
                wrist,
                wrist + palm * Vec3(0f, HAND - 0.015f, 0f),
                palm,
                handRadii,
                handRadii,
                roundness = 1f,
            )
            val contact = Skeleton.CONTACT_RADIUS.getValue(Landmark.LeftHand)
            return wrist + palm * Vec3(0f, 0.03f, 0f) + Vec3(0f, contact - handRadii.depth, 0f)
        }
        segments += Segment(
            BodyPart.Hand,
            side,
            wrist,
            wrist + fore * Vec3(0f, -(HAND - 0.02f), 0f),
            fore,
            handRadii,
            Radii(0.02f, 0.011f) * g,
            roundness = 1f,
        )
        return wrist + fore * Vec3(0f, -HAND, 0f)
    }
}
