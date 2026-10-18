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
        camera = Camera(90f, pitch = -8f),
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

    val calfRaise = ExerciseMotion(
        id = "calf_raise",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.4f, Pose.symmetric(feet = 32f)),
                Keyframe(0.6f, Pose.symmetric(feet = 32f)),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Calves),
        secondary = emptySet(),
        contacts = setOf(Landmark.LeftToe, Landmark.RightToe),
        anchor = Landmark.LeftToe,
        camera = Camera(120f, pitch = 0f),
        repetitionMillis = 2400,
    )

    val plank = ExerciseMotion(
        id = "plank",
        movement = Movement(listOf(Keyframe(0f, PUSH_UP[0]), Keyframe(1f, PUSH_UP[0]))),
        primary = setOf(MuscleGroup.Abs),
        secondary = setOf(MuscleGroup.Shoulders, MuscleGroup.Glutes),
        contacts = setOf(Landmark.LeftToe, Landmark.RightToe, Landmark.LeftHand, Landmark.RightHand),
        anchor = Landmark.LeftToe,
        camera = Camera(90f, pitch = -8f),
    )

    val mountainClimber = ExerciseMotion(
        id = "mountain_climber",
        movement = Movement(
            listOf(
                Keyframe(0f, PUSH_UP[0]),
                Keyframe(0.25f, PUSH_UP[0].copy(leftLeg = KNEE_DRIVE, leftFoot = 15f)),
                Keyframe(0.5f, PUSH_UP[0]),
                Keyframe(0.75f, PUSH_UP[0].copy(rightLeg = KNEE_DRIVE, rightFoot = 15f)),
                Keyframe(1f, PUSH_UP[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Abs),
        secondary = setOf(MuscleGroup.Shoulders, MuscleGroup.Quads),
        contacts = setOf(Landmark.LeftHand, Landmark.RightHand),
        anchor = Landmark.LeftHand,
        shadowUnder = setOf(Landmark.LeftHand, Landmark.RightHand, Landmark.LeftToe, Landmark.RightToe),
        camera = Camera(90f, pitch = -8f),
        peak = 0.25f,
        repetitionMillis = 1600,
    )

    val superman = ExerciseMotion(
        id = "superman",
        movement = Movement(
            listOf(
                Keyframe(0f, SUPERMAN[0]),
                Keyframe(0.4f, SUPERMAN[1]),
                Keyframe(0.6f, SUPERMAN[1]),
                Keyframe(1f, SUPERMAN[0]),
            ),
        ),
        primary = setOf(MuscleGroup.LowerBack, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.UpperBack, MuscleGroup.Hamstrings),
        contacts = setOf(Landmark.Belly),
        anchor = Landmark.Belly,
        camera = Camera(90f),
    )

    val reverseLunge = ExerciseMotion(
        id = "reverse_lunge",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.2f, STEP_BACK),
                Keyframe(0.45f, SPLIT[3]),
                Keyframe(0.58f, SPLIT[3]),
                Keyframe(0.8f, STEP_BACK),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Quads, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.Hamstrings),
        contacts = setOf(Landmark.LeftHeel, Landmark.LeftToe),
        anchor = Landmark.LeftHeel,
        camera = Camera(60f),
    )

    val goodMorning = ExerciseMotion(
        id = "good_morning",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.45f, HINGE),
                Keyframe(0.58f, HINGE),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Hamstrings, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.LowerBack),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f),
    )

    val birdDog = ExerciseMotion(
        id = "bird_dog",
        movement = Movement(
            listOf(
                Keyframe(0f, ALL_FOURS),
                Keyframe(0.4f, BIRD_DOG),
                Keyframe(0.6f, BIRD_DOG),
                Keyframe(1f, ALL_FOURS),
            ),
        ),
        primary = setOf(MuscleGroup.Abs, MuscleGroup.LowerBack),
        secondary = setOf(MuscleGroup.Glutes, MuscleGroup.Shoulders),
        contacts = setOf(Landmark.LeftKnee, Landmark.RightHand),
        anchor = Landmark.LeftKnee,
        camera = Camera(90f, pitch = -8f),
    )

    val crunch = ExerciseMotion(
        id = "crunch",
        movement = Movement(
            listOf(
                Keyframe(0f, CRUNCH[0]),
                Keyframe(0.4f, CRUNCH[1]),
                Keyframe(0.55f, CRUNCH[1]),
                Keyframe(1f, CRUNCH[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Abs),
        secondary = emptySet(),
        contacts = feet + Landmark.Belly,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f),
    )

    val armRaise = ExerciseMotion(
        id = "arm_raise",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.45f, Pose.symmetric(arm = LimbPose(raise = 88f))),
                Keyframe(0.58f, Pose.symmetric(arm = LimbPose(raise = 88f))),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Shoulders),
        secondary = setOf(MuscleGroup.UpperBack),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(60f, pitch = 10f),
    )

    val neckNod = ExerciseMotion(
        id = "neck_nod",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.45f, Pose.symmetric(neck = 28f)),
                Keyframe(0.58f, Pose.symmetric(neck = 28f)),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Neck),
        secondary = setOf(MuscleGroup.UpperBack),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(60f, pitch = 0f),
    )

    val all: List<ExerciseMotion> = listOf(
        squat,
        pushUp,
        gluteBridge,
        splitSquat,
        jumpingJack,
        calfRaise,
        plank,
        mountainClimber,
        superman,
        reverseLunge,
        goodMorning,
        birdDog,
        crunch,
        armRaise,
        neckNod,
    )

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

/** Pulling a knee towards the chest from a plank, foot off the floor. */
private val KNEE_DRIVE = LimbPose(raise = 104f, bend = 124f)

/** Lying face down with the arms reaching overhead, then lifting arms, chest and legs. */
private val SUPERMAN = listOf(
    Pose.symmetric(arm = LimbPose(raise = 176f), lean = 90f, feet = 170f),
    Pose.symmetric(
        leg = LimbPose(raise = -18f),
        arm = LimbPose(raise = 182f),
        lean = 90f,
        trunk = -22f,
        neck = -14f,
        feet = 170f,
    ),
)

/** Halfway through stepping back into a lunge, the back foot in the air. */
private val STEP_BACK = Pose(
    leftLeg = LimbPose(raise = 22f, bend = 24f),
    rightLeg = LimbPose(raise = -24f, bend = 62f),
    leftArm = LimbPose(bend = 15f),
    rightArm = LimbPose(bend = 15f),
    rightFoot = 45f,
)

/** Hinged forward at the hips with a flat back and soft knees; the arms hang straight down. */
private val HINGE = Pose.symmetric(
    leg = LimbPose(raise = 70f, bend = 14f),
    arm = LimbPose(raise = 62f),
    lean = 62f,
    neck = -22f,
)

/** On hands and knees: hands under the shoulders, knees under the hips (solved, see AnatomyTest). */
private val ALL_FOURS = Pose.symmetric(
    leg = LimbPose(raise = 77.3f, bend = 90f),
    arm = LimbPose(raise = 77.3f),
    lean = 77.3f,
    feet = 165f,
    flatHands = true,
)

/** From all fours, the left arm reaches forward and the right leg back, both level with the back. */
private val BIRD_DOG = ALL_FOURS.copy(
    leftArm = LimbPose(raise = 167.3f),
    rightLeg = LimbPose(raise = -12.7f),
    rightFoot = 180f,
)

/**
 * Lying with bent knees, then the shoulders curl off the floor while the lower back stays down. The
 * arms reach for the knees above the body, so they don't hide the belly.
 */
private val CRUNCH = listOf(
    BRIDGE[0],
    BRIDGE[0].copy(trunk = 32f, neck = 18f, leftArm = LimbPose(raise = 55f), rightArm = LimbPose(raise = 55f)),
)
