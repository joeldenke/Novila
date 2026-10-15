package se.novila.anatomy

/**
 * The exercises Novila can show. Angles are in degrees, see [Pose]. Poses that plant both hands and
 * feet were solved so the planted points stay put through the repetition (checked by AnatomyTest).
 */
object ExerciseLibrary {
    private val feet = setOf(Landmark.LeftHeel, Landmark.LeftToe, Landmark.RightHeel, Landmark.RightToe)

    val squat = ExerciseMotion(
        id = "squat",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.45f, SQUAT_BOTTOM),
                Keyframe(0.58f, SQUAT_BOTTOM),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Quads, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.Hamstrings, MuscleGroup.LowerBack, MuscleGroup.Calves),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f),
    )

    val pushUp = ExerciseMotion(
        id = "push_up",
        movement = Movement(
            listOf(
                Keyframe(0f, PUSH_UP[0]),
                Keyframe(0.06f, PUSH_UP[1]),
                Keyframe(0.12f, PUSH_UP[2]),
                Keyframe(0.25f, PUSH_UP[3]),
                Keyframe(0.38f, PUSH_UP[4]),
                Keyframe(0.5f, PUSH_UP[5]),
                Keyframe(0.58f, PUSH_UP[5]),
                Keyframe(0.69f, PUSH_UP[4]),
                Keyframe(0.8f, PUSH_UP[3]),
                Keyframe(0.9f, PUSH_UP[2]),
                Keyframe(0.95f, PUSH_UP[1]),
                Keyframe(1f, PUSH_UP[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Chest, MuscleGroup.Triceps),
        secondary = setOf(MuscleGroup.Shoulders, MuscleGroup.Abs),
        contacts = setOf(Landmark.LeftToe, Landmark.RightToe, Landmark.LeftHand, Landmark.RightHand),
        anchor = Landmark.LeftToe,
        camera = Camera(90f),
        peak = 0.54f,
    )

    val gluteBridge = ExerciseMotion(
        id = "glute_bridge",
        movement = Movement(
            listOf(
                Keyframe(0f, BRIDGE[0]),
                Keyframe(0.22f, BRIDGE[1]),
                Keyframe(0.42f, BRIDGE[2]),
                Keyframe(0.6f, BRIDGE[2]),
                Keyframe(0.8f, BRIDGE[1]),
                Keyframe(1f, BRIDGE[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.Hamstrings, MuscleGroup.LowerBack),
        contacts = feet + Landmark.UpperBack,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f, pitch = 0f),
    )

    val splitSquat = ExerciseMotion(
        id = "split_squat",
        movement = Movement(
            listOf(
                Keyframe(0f, SPLIT[0]),
                Keyframe(0.12f, SPLIT[1]),
                Keyframe(0.24f, SPLIT[2]),
                Keyframe(0.45f, SPLIT[3]),
                Keyframe(0.58f, SPLIT[3]),
                Keyframe(0.78f, SPLIT[2]),
                Keyframe(0.89f, SPLIT[1]),
                Keyframe(1f, SPLIT[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Quads, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.Hamstrings, MuscleGroup.Calves),
        contacts = setOf(Landmark.LeftHeel, Landmark.LeftToe, Landmark.RightToe),
        anchor = Landmark.LeftHeel,
        camera = Camera(60f),
    )

    val jumpingJack = ExerciseMotion(
        id = "jumping_jack",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.5f, Pose.symmetric(leg = LimbPose(spread = 15f), arm = LimbPose(spread = 160f, bend = 12f))),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Shoulders),
        secondary = setOf(MuscleGroup.Glutes, MuscleGroup.Calves),
        contacts = feet,
        anchor = null,
        camera = Camera(0f),
        repetitionMillis = 1400,
    )

    val all: List<ExerciseMotion> = listOf(squat, pushUp, gluteBridge, splitSquat, jumpingJack)

    fun byId(id: String): ExerciseMotion? = all.firstOrNull { it.id == id }
}

private val SQUAT_BOTTOM = Pose.symmetric(
    leg = LimbPose(raise = 135f, bend = 125f),
    arm = LimbPose(raise = 115f),
    lean = 38f,
    trunk = -8f,
    neck = -28f,
)

/** Body straight from the toes, the feet at right angles to it and the hands flat on the floor. */
private fun pushUp(lean: Float, raise: Float, bend: Float) =
    Pose.symmetric(arm = LimbPose(raise = raise, bend = bend), lean = lean, feet = lean, flatHands = true)

/** From the top to the bottom of a push-up, pivoting on the toes with the hands planted. */
private val PUSH_UP = listOf(
    pushUp(lean = 74f, raise = 74f, bend = 0f),
    pushUp(lean = 75.7f, raise = 55.9f, bend = 41.3f),
    pushUp(lean = 77.4f, raise = 48.3f, bend = 58.8f),
    pushUp(lean = 80.7f, raise = 37.1f, bend = 84.1f),
    pushUp(lean = 84.1f, raise = 27.8f, bend = 104.2f),
    pushUp(lean = 87.4f, raise = 18.8f, bend = 121.5f),
)

/** Lying on the back with bent knees: the arms rest on the floor and the head stays down. */
private fun bridge(lean: Float, raise: Float, bend: Float, arms: Float, neck: Float) =
    Pose.symmetric(leg = LimbPose(raise = raise, bend = bend), arm = LimbPose(raise = arms), lean = lean, neck = neck)

/** From lying down to the hips lifted in line with the thighs, the upper back staying on the floor. */
private val BRIDGE = listOf(
    bridge(lean = -90f, raise = 46.3f, bend = 100f, arms = -5.1f, neck = 0f),
    bridge(lean = -102f, raise = 24.2f, bend = 96.4f, arms = -16f, neck = 11.1f),
    bridge(lean = -113.5f, raise = 2f, bend = 88.2f, arms = -26.6f, neck = 29.3f),
)

/** Left foot forward and flat, right foot back on its toes, arms relaxed. */
private fun split(front: LimbPose, back: LimbPose, toes: Float) = Pose(
    leftLeg = front,
    rightLeg = back,
    leftArm = LimbPose(bend = 15f),
    rightArm = LimbPose(bend = 15f),
    rightFoot = toes,
)

/** From standing in a split stance down until the back knee nearly touches the floor. */
private val SPLIT = listOf(
    split(front = LimbPose(26f, bend = 6f), back = LimbPose(-34f, bend = 12f), toes = 48f),
    split(front = LimbPose(41f, bend = 27f), back = LimbPose(-13.2f, bend = 51.6f), toes = 51.5f),
    split(front = LimbPose(56f, bend = 48f), back = LimbPose(-3.4f, bend = 74.2f), toes = 55f),
    split(front = LimbPose(86f, bend = 90f), back = LimbPose(-3.7f, bend = 102.9f), toes = 62f),
)
