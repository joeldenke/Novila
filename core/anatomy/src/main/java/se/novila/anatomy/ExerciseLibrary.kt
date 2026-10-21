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

    val kneePushUp = ExerciseMotion(
        id = "knee_push_up",
        movement = Movement(
            listOf(
                Keyframe(0f, KNEE_PUSH_UP[0]),
                Keyframe(0.06f, KNEE_PUSH_UP[1]),
                Keyframe(0.12f, KNEE_PUSH_UP[2]),
                Keyframe(0.22f, KNEE_PUSH_UP[3]),
                Keyframe(0.36f, KNEE_PUSH_UP[4]),
                Keyframe(0.5f, KNEE_PUSH_UP[5]),
                Keyframe(0.58f, KNEE_PUSH_UP[5]),
                Keyframe(0.72f, KNEE_PUSH_UP[4]),
                Keyframe(0.85f, KNEE_PUSH_UP[3]),
                Keyframe(0.91f, KNEE_PUSH_UP[2]),
                Keyframe(0.96f, KNEE_PUSH_UP[1]),
                Keyframe(1f, KNEE_PUSH_UP[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Chest, MuscleGroup.Triceps),
        secondary = setOf(MuscleGroup.Shoulders, MuscleGroup.Abs),
        contacts = setOf(Landmark.LeftKnee, Landmark.RightKnee, Landmark.LeftHand, Landmark.RightHand),
        anchor = Landmark.LeftKnee,
        camera = Camera(90f, pitch = 0f),
        peak = 0.54f,
    )

    val singleLegBridge = ExerciseMotion(
        id = "single_leg_bridge",
        movement = Movement(
            listOf(
                Keyframe(0f, SINGLE_LEG_BRIDGE[0]),
                Keyframe(0.22f, SINGLE_LEG_BRIDGE[1]),
                Keyframe(0.42f, SINGLE_LEG_BRIDGE[2]),
                Keyframe(0.6f, SINGLE_LEG_BRIDGE[2]),
                Keyframe(0.8f, SINGLE_LEG_BRIDGE[1]),
                Keyframe(1f, SINGLE_LEG_BRIDGE[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.Hamstrings, MuscleGroup.Abs),
        contacts = setOf(Landmark.LeftHeel, Landmark.LeftToe, Landmark.UpperBack),
        anchor = Landmark.LeftHeel,
        camera = Camera(90f, pitch = 0f),
    )

    val deadBug = ExerciseMotion(
        id = "dead_bug",
        movement = Movement(
            listOf(
                Keyframe(0f, DEAD_BUG[0]),
                Keyframe(0.42f, DEAD_BUG[1]),
                Keyframe(0.58f, DEAD_BUG[1]),
                Keyframe(1f, DEAD_BUG[0]),
            ),
        ),
        primary = setOf(MuscleGroup.Abs),
        secondary = setOf(MuscleGroup.LowerBack),
        contacts = setOf(Landmark.Belly, Landmark.UpperBack),
        anchor = Landmark.Belly,
        camera = Camera(90f),
    )

    val highKnees = ExerciseMotion(
        id = "high_knees",
        movement = Movement(
            listOf(
                Keyframe(0f, RUN_MIDDLE),
                Keyframe(0.25f, HIGH_KNEE),
                Keyframe(0.5f, RUN_MIDDLE),
                Keyframe(0.75f, HIGH_KNEE.mirrored()),
                Keyframe(1f, RUN_MIDDLE),
            ),
        ),
        primary = setOf(MuscleGroup.Quads, MuscleGroup.Calves),
        secondary = setOf(MuscleGroup.Abs, MuscleGroup.Glutes),
        contacts = feet,
        anchor = null,
        camera = Camera(60f, pitch = 0f),
        peak = 0.25f,
        repetitionMillis = 1400,
    )

    val gobletSquat = ExerciseMotion(
        id = "goblet_squat",
        movement = Movement(
            listOf(
                Keyframe(0f, GOBLET_TOP),
                Keyframe(0.45f, GOBLET_BOTTOM),
                Keyframe(0.58f, GOBLET_BOTTOM),
                Keyframe(1f, GOBLET_TOP),
            ),
        ),
        primary = setOf(MuscleGroup.Quads, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.Hamstrings, MuscleGroup.Abs),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(60f, pitch = -8f),
        gear = Gear.Goblet,
    )

    val dumbbellRow = ExerciseMotion(
        id = "dumbbell_row",
        movement = Movement(
            listOf(
                Keyframe(0f, ROW_HANG),
                Keyframe(0.42f, ROW_PULL),
                Keyframe(0.58f, ROW_PULL),
                Keyframe(1f, ROW_HANG),
            ),
        ),
        primary = setOf(MuscleGroup.Lats, MuscleGroup.UpperBack),
        secondary = setOf(MuscleGroup.Biceps, MuscleGroup.LowerBack),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f),
        gear = Gear.Dumbbells,
    )

    val dumbbellPress = ExerciseMotion(
        id = "dumbbell_press",
        movement = Movement(
            listOf(
                Keyframe(0f, PRESS_RACK),
                Keyframe(0.42f, PRESS_TOP),
                Keyframe(0.58f, PRESS_TOP),
                Keyframe(1f, PRESS_RACK),
            ),
        ),
        primary = setOf(MuscleGroup.Shoulders, MuscleGroup.Triceps),
        secondary = setOf(MuscleGroup.UpperBack, MuscleGroup.Abs),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(0f, pitch = -8f),
        gear = Gear.Dumbbells,
    )

    val bicepsCurl = ExerciseMotion(
        id = "biceps_curl",
        movement = Movement(
            listOf(
                Keyframe(0f, Pose.STANDING),
                Keyframe(0.42f, CURL_TOP),
                Keyframe(0.58f, CURL_TOP),
                Keyframe(1f, Pose.STANDING),
            ),
        ),
        primary = setOf(MuscleGroup.Biceps),
        secondary = setOf(MuscleGroup.Shoulders),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(30f, pitch = -8f),
        gear = Gear.Dumbbells,
    )

    val dumbbellDeadlift = ExerciseMotion(
        id = "dumbbell_deadlift",
        movement = Movement(
            listOf(
                Keyframe(0f, DEADLIFT_TOP),
                Keyframe(0.45f, DEADLIFT_BOTTOM),
                Keyframe(0.58f, DEADLIFT_BOTTOM),
                Keyframe(1f, DEADLIFT_TOP),
            ),
        ),
        primary = setOf(MuscleGroup.Hamstrings, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.LowerBack, MuscleGroup.UpperBack),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f, pitch = 0f),
        gear = Gear.Dumbbells,
    )

    val bandRow = ExerciseMotion(
        id = "band_row",
        movement = Movement(
            listOf(
                Keyframe(0f, BAND_REACH),
                Keyframe(0.42f, BAND_ROW_PULL),
                Keyframe(0.58f, BAND_ROW_PULL),
                Keyframe(1f, BAND_REACH),
            ),
        ),
        primary = setOf(MuscleGroup.UpperBack, MuscleGroup.Lats),
        secondary = setOf(MuscleGroup.Biceps, MuscleGroup.Shoulders),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f),
        gear = Gear.Band(Vec3(0f, 0.24f, 0.75f)),
    )

    val bandPulldown = ExerciseMotion(
        id = "band_pulldown",
        movement = Movement(
            listOf(
                Keyframe(0f, BAND_OVERHEAD),
                Keyframe(0.42f, BAND_PULLED_DOWN),
                Keyframe(0.58f, BAND_PULLED_DOWN),
                Keyframe(1f, BAND_OVERHEAD),
            ),
        ),
        primary = setOf(MuscleGroup.Lats),
        secondary = setOf(MuscleGroup.Triceps, MuscleGroup.Abs),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f, pitch = 0f),
        gear = Gear.Band(Vec3(0f, 0.78f, 0.6f)),
    )

    val bandChestPress = ExerciseMotion(
        id = "band_chest_press",
        movement = Movement(
            listOf(
                Keyframe(0f, BAND_CHEST),
                Keyframe(0.42f, BAND_PRESSED),
                Keyframe(0.58f, BAND_PRESSED),
                Keyframe(1f, BAND_CHEST),
            ),
        ),
        primary = setOf(MuscleGroup.Chest, MuscleGroup.Triceps),
        secondary = setOf(MuscleGroup.Shoulders),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f, pitch = 0f),
        gear = Gear.Band(Vec3(0.22f, 0.16f, -0.45f)),
    )

    val frontSquat = ExerciseMotion(
        id = "front_squat",
        movement = Movement(
            listOf(
                Keyframe(0f, FRONT_SQUAT_TOP),
                Keyframe(0.45f, FRONT_SQUAT_BOTTOM),
                Keyframe(0.58f, FRONT_SQUAT_BOTTOM),
                Keyframe(1f, FRONT_SQUAT_TOP),
            ),
        ),
        primary = setOf(MuscleGroup.Quads, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.Abs, MuscleGroup.UpperBack, MuscleGroup.Hamstrings),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f),
        gear = Gear.Barbell,
    )

    val deadlift = ExerciseMotion(
        id = "deadlift",
        movement = Movement(
            listOf(
                Keyframe(0f, DEADLIFT_TOP),
                Keyframe(0.45f, DEADLIFT_BOTTOM),
                Keyframe(0.58f, DEADLIFT_BOTTOM),
                Keyframe(1f, DEADLIFT_TOP),
            ),
        ),
        primary = setOf(MuscleGroup.Hamstrings, MuscleGroup.Glutes),
        secondary = setOf(MuscleGroup.LowerBack, MuscleGroup.UpperBack),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f, pitch = 0f),
        gear = Gear.Barbell,
    )

    val barbellRow = ExerciseMotion(
        id = "barbell_row",
        movement = Movement(
            listOf(
                Keyframe(0f, ROW_HANG),
                Keyframe(0.42f, ROW_PULL),
                Keyframe(0.58f, ROW_PULL),
                Keyframe(1f, ROW_HANG),
            ),
        ),
        primary = setOf(MuscleGroup.Lats, MuscleGroup.UpperBack),
        secondary = setOf(MuscleGroup.Biceps, MuscleGroup.LowerBack),
        contacts = feet,
        anchor = Landmark.LeftHeel,
        camera = Camera(90f),
        gear = Gear.Barbell,
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
        kneePushUp,
        singleLegBridge,
        deadBug,
        highKnees,
        gobletSquat,
        dumbbellRow,
        dumbbellPress,
        bicepsCurl,
        dumbbellDeadlift,
        bandRow,
        bandPulldown,
        bandChestPress,
        frontSquat,
        deadlift,
        barbellRow,
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

/** On the knees, the shins resting on the floor and the body straight from the knees to the head. */
private fun kneePushUp(lean: Float, raise: Float, bend: Float) = Pose.symmetric(
    leg = LimbPose(bend = 90f - lean),
    arm = LimbPose(raise = raise, bend = bend),
    lean = lean,
    feet = 180f,
    flatHands = true,
)

/** From the top to the bottom of a push-up on the knees, pivoting on the knees with the hands planted (solved). */
private val KNEE_PUSH_UP = listOf(
    kneePushUp(lean = 53.5f, raise = 53.5f, bend = 0f),
    kneePushUp(lean = 56.3f, raise = 36.2f, bend = 40f),
    kneePushUp(lean = 59.7f, raise = 27.6f, bend = 60f),
    kneePushUp(lean = 64.7f, raise = 17.8f, bend = 82f),
    kneePushUp(lean = 70.6f, raise = 8f, bend = 102f),
    kneePushUp(lean = 76.1f, raise = -1.4f, bend = 118f),
)

/**
 * The glute bridge on the left foot, the right leg straight and in line with the left thigh. A straight leg's
 * foot is at right angles to it, which is the leg's own angle to the floor.
 */
private val SINGLE_LEG_BRIDGE = BRIDGE.map { pose ->
    val raise = pose.leftLeg.raise
    pose.copy(rightLeg = LimbPose(raise = raise), rightFoot = pose.lean - raise)
}

/**
 * Lying on the back with the hips and knees at right angles and the arms pointing up, then the left arm
 * reaches overhead and the right leg straightens out, both just above the floor.
 */
private val DEAD_BUG = listOf(
    Pose.symmetric(leg = LimbPose(raise = 90f, bend = 90f), arm = LimbPose(raise = 90f), lean = -90f, feet = -90f),
    Pose(
        lean = -90f,
        leftLeg = LimbPose(raise = 90f, bend = 90f),
        rightLeg = LimbPose(raise = 15f),
        leftArm = LimbPose(raise = 165f),
        rightArm = LimbPose(raise = 90f),
        leftFoot = -90f,
        rightFoot = -105f,
    ),
)

/** Running in place, both feet down and the elbows bent. */
private val RUN_MIDDLE = Pose.symmetric(arm = LimbPose(bend = 90f))

/** The left knee driven up to hip height, toes pointing down, the arms swinging the other way. */
private val HIGH_KNEE = Pose(
    leftLeg = LimbPose(raise = 85f, bend = 95f),
    leftArm = LimbPose(raise = -25f, bend = 90f),
    rightArm = LimbPose(raise = 40f, bend = 90f),
    leftFoot = 40f,
)

/** The same pose with left and right swapped. */
private fun Pose.mirrored() = copy(
    leftLeg = rightLeg,
    rightLeg = leftLeg,
    leftArm = rightArm,
    rightArm = leftArm,
    leftFoot = rightFoot,
    rightFoot = leftFoot,
)

/** Holding a dumbbell upright against the chest, elbows in. */
private val GOBLET_ARMS = LimbPose(raise = 5f, spread = -12f, bend = 125f)

private val GOBLET_TOP = Pose.symmetric(arm = GOBLET_ARMS)

/** A deep squat with an upright chest: the thighs level and the shins leaning forward. */
private val GOBLET_BOTTOM = Pose.symmetric(
    leg = LimbPose(raise = 125f, bend = 125f),
    arm = GOBLET_ARMS,
    lean = 30f,
    neck = -22f,
)

/** Hinged forward with soft knees and the weights hanging straight down from the shoulders. */
private val ROW_HANG = Pose.symmetric(
    leg = LimbPose(raise = 95f, bend = 25f),
    arm = LimbPose(raise = 70f),
    lean = 70f,
    neck = -30f,
)

/** The elbows pulled back past the body, the weights at the lower ribs. */
private val ROW_PULL = ROW_HANG.copy(
    leftArm = LimbPose(raise = 10f, bend = 70f),
    rightArm = LimbPose(raise = 10f, bend = 70f),
)

/** The weights at the shoulders, elbows forward and down. */
private val PRESS_RACK = Pose.symmetric(arm = LimbPose(raise = 30f, spread = 15f, bend = 140f))

/** The arms straight overhead. */
private val PRESS_TOP = Pose.symmetric(arm = LimbPose(raise = 172f, spread = 8f, bend = 4f))

/** The elbows by the sides and the forearms curled up. */
private val CURL_TOP = Pose.symmetric(arm = LimbPose(raise = 10f, bend = 135f))

/** Standing tall with the weight hanging in front of the thighs. */
private val DEADLIFT_TOP = Pose.symmetric(arm = LimbPose(raise = 12f, spread = 6f))

/**
 * Hinged at the hips with soft knees and a flat back, the hips pushed back so the weight hangs straight
 * down over the feet.
 */
private val DEADLIFT_BOTTOM = Pose.symmetric(
    leg = LimbPose(raise = 77f, bend = 20f),
    arm = LimbPose(raise = 54f, spread = 6f),
    lean = 52f,
    neck = -25f,
)

/** Soft knees for the standing band exercises. */
private val SOFT_KNEES = LimbPose(raise = 6f, bend = 12f)

/** Standing with the arms reaching straight forward at chest height. */
private val BAND_REACH = Pose.symmetric(leg = SOFT_KNEES, arm = LimbPose(raise = 85f))

/** The elbows pulled back along the sides, the hands at the ribs. */
private val BAND_ROW_PULL = Pose.symmetric(leg = SOFT_KNEES, arm = LimbPose(raise = -25f, bend = 95f))

/** The arms reaching up and forward, nearly straight. */
private val BAND_OVERHEAD = Pose.symmetric(leg = SOFT_KNEES, arm = LimbPose(raise = 150f, bend = 10f))

/** The straight arms pulled down to the thighs. */
private val BAND_PULLED_DOWN = Pose.symmetric(leg = SOFT_KNEES, arm = LimbPose(raise = 5f, bend = 10f))

/** The hands at the chest, the elbows just behind the body. */
private val BAND_CHEST = Pose.symmetric(leg = SOFT_KNEES, arm = LimbPose(raise = -10f, bend = 100f))

/** The arms pressed straight forward. */
private val BAND_PRESSED = Pose.symmetric(leg = SOFT_KNEES, arm = LimbPose(raise = 85f, bend = 4f))

/** The bar resting on the front of the shoulders, the elbows high and forward. */
private val FRONT_RACK = LimbPose(raise = 90f, spread = 10f, bend = 150f)

private val FRONT_SQUAT_TOP = Pose.symmetric(arm = FRONT_RACK)

/** Squatting down with an upright chest, the thighs level and the elbows kept high. */
private val FRONT_SQUAT_BOTTOM = Pose.symmetric(
    leg = LimbPose(raise = 120f, bend = 125f),
    arm = FRONT_RACK,
    lean = 25f,
    neck = -18f,
)
