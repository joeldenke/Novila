package se.novila.anatomy

/** Muscle groups as a physiotherapist or trainer names them in a program. */
enum class MuscleGroup {
    Chest,
    Shoulders,
    Biceps,
    Triceps,
    Abs,
    Neck,
    UpperBack,
    Lats,
    LowerBack,
    Glutes,
    Quads,
    Hamstrings,
    Calves,
}

/**
 * Where a muscle group lies on a [part]: from [from] to [to] along it (0 is the end nearest the torso,
 * or the bottom of a torso part; below 0 reaches past the joint, like the glutes over the hip) and
 * around it, centered on [around] degrees with [halfWidth] to each side. Around is measured from the front: 90 is the outer side of an arm or leg, or the body's left on the
 * torso, and 180 is the back. [mirrored] torso regions get a twin on the right side.
 */
data class MuscleRegion(
    val group: MuscleGroup,
    val part: BodyPart,
    val from: Float,
    val to: Float,
    val around: Float,
    val halfWidth: Float,
    val mirrored: Boolean = false,
)

/** A simplified, anatomically placed map of the muscle groups, the same for every body shape. */
object MuscleMap {
    val regions: List<MuscleRegion> = listOf(
        MuscleRegion(
            MuscleGroup.Chest,
            BodyPart.UpperTorso,
            0.6f,
            0.93f,
            around = 32f,
            halfWidth = 30f,
            mirrored = true,
        ),
        MuscleRegion(MuscleGroup.Abs, BodyPart.UpperTorso, 0.04f, 0.56f, around = 0f, halfWidth = 32f),
        MuscleRegion(MuscleGroup.Abs, BodyPart.LowerTorso, 0.3f, 0.98f, around = 0f, halfWidth = 34f),
        MuscleRegion(MuscleGroup.Neck, BodyPart.Neck, 0f, 1f, around = 0f, halfWidth = 180f),
        MuscleRegion(MuscleGroup.UpperBack, BodyPart.UpperTorso, 0.68f, 1f, around = 180f, halfWidth = 70f),
        MuscleRegion(
            MuscleGroup.Lats,
            BodyPart.UpperTorso,
            0.22f,
            0.7f,
            around = 132f,
            halfWidth = 36f,
            mirrored = true,
        ),
        MuscleRegion(MuscleGroup.LowerBack, BodyPart.UpperTorso, 0.02f, 0.3f, around = 180f, halfWidth = 26f),
        MuscleRegion(MuscleGroup.LowerBack, BodyPart.LowerTorso, 0.55f, 0.98f, around = 180f, halfWidth = 30f),
        MuscleRegion(MuscleGroup.Shoulders, BodyPart.UpperArm, 0f, 0.36f, around = 90f, halfWidth = 120f),
        MuscleRegion(MuscleGroup.Biceps, BodyPart.UpperArm, 0.3f, 0.88f, around = 0f, halfWidth = 55f),
        MuscleRegion(MuscleGroup.Triceps, BodyPart.UpperArm, 0.24f, 0.9f, around = 180f, halfWidth = 62f),
        MuscleRegion(MuscleGroup.Glutes, BodyPart.Thigh, -0.24f, 0.3f, around = 150f, halfWidth = 62f),
        MuscleRegion(MuscleGroup.Quads, BodyPart.Thigh, 0.1f, 0.9f, around = 18f, halfWidth = 78f),
        MuscleRegion(MuscleGroup.Hamstrings, BodyPart.Thigh, 0.14f, 0.88f, around = 180f, halfWidth = 62f),
        MuscleRegion(MuscleGroup.Calves, BodyPart.Shank, 0.08f, 0.6f, around = 180f, halfWidth = 76f),
    )
}
