package se.novila.anatomy

/**
 * How an exercise looks: one repetition as a [movement], the muscle groups it trains and the
 * [camera] that shows it best (chosen by [ViewSelector], kept in sync by a unit test). Independent of
 * any data source or body: the same motion plays on every [BodyShape].
 *
 * @param contacts landmarks that touch the floor; they get a shadow and must stay planted.
 * @param anchor the landmark that never moves, so the rest of the body moves around it. Without one,
 *   the body keeps its lowest contact on the floor (for exercises where the feet move).
 * @param peak the moment of the repetition that thumbnails show, where the muscles work hardest.
 */
class ExerciseMotion(
    val id: String,
    val movement: Movement,
    val primary: Set<MuscleGroup>,
    val secondary: Set<MuscleGroup>,
    val contacts: Set<Landmark>,
    val anchor: Landmark?,
    val camera: Camera,
    val peak: Float = 0.5f,
    val repetitionMillis: Int = 3200,
) {
    val highlights: Map<MuscleGroup, Emphasis> =
        secondary.associateWith { Emphasis.Secondary } + primary.associateWith { Emphasis.Primary }

    /** The posed body at [progress] repetitions, moved so the [anchor] or the floor stays put. */
    fun bodyAt(progress: Float, shape: BodyShape = BodyShape.NEUTRAL): PosedBody {
        val start = Skeleton.pose(movement.keyframes.first().pose, shape)
        val body = Skeleton.pose(movement.poseAt(progress), shape)
        val offset = if (anchor != null) {
            start.landmarks.getValue(anchor) - body.landmarks.getValue(anchor)
        } else {
            Vec3(0f, SceneBuilder.floorHeight(start, contacts) - SceneBuilder.floorHeight(body, contacts), 0f)
        }
        return body.moved(offset)
    }

    fun sceneAt(
        progress: Float,
        shape: BodyShape = BodyShape.NEUTRAL,
        camera: Camera = this.camera,
        highlights: Map<MuscleGroup, Emphasis> = this.highlights,
    ): Scene = SceneBuilder.build(bodyAt(progress, shape), camera, highlights, contacts)

    /** Room for the whole repetition, so the figure doesn't jump around or get cropped while it moves. */
    fun bounds(shape: BodyShape = BodyShape.NEUTRAL, camera: Camera = this.camera): Bounds =
        (0 until BOUNDS_SAMPLES).map { sceneAt(it / BOUNDS_SAMPLES.toFloat(), shape, camera).bounds }
            .reduce(Bounds::plus)

    private companion object {
        const val BOUNDS_SAMPLES = 16
    }
}
