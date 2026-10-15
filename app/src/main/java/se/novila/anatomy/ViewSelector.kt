package se.novila.anatomy

import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Picks the camera that shows an exercise best, the way an illustrator would: the trained muscles are
 * visible (after the near side of the body hides the far side), the movement plays across the screen
 * rather than towards the viewer, and the limbs are seen at their full length instead of foreshortened.
 * Each exercise stores its view, and a unit test checks it is still the best one, so the choice costs
 * nothing at runtime.
 */
object ViewSelector {
    /** Around the body in 30° steps, from slightly above and at eye level (for exercises close to the floor). */
    val CANDIDATES: List<Camera> = listOf(Camera.DEFAULT_PITCH, 0f).flatMap { pitch ->
        listOf(0f, 30f, 60f, 90f, 120f, 150f, 180f, -150f, -120f, -90f, -60f, -30f).map { Camera(it, pitch) }
    }

    // Calibrated on the library so a squat, push-up and bridge are seen from the side and jumping jacks
    // from the front, like in a physiotherapist's handout. Foreshortened limbs are the hardest to read.
    private const val MUSCLE_WEIGHT = 0.15f
    private const val MOTION_WEIGHT = 0.25f
    private const val LENGTH_WEIGHT = 0.5f
    private const val FACING_WEIGHT = 0.1f
    private const val TIE = 0.02f
    private const val SECONDARY_WEIGHT = 0.2f
    private const val CELL = 0.01f
    private val SAMPLES = listOf(0.25f, 0.5f, 0.75f)

    /** The best scoring candidate. Near ties go to the earlier one, so symmetric exercises face right. */
    fun best(motion: ExerciseMotion): Camera {
        val scores = scores(motion)
        val top = scores.values.max()
        return CANDIDATES.first { scores.getValue(it) >= top - TIE }
    }

    /** Each candidate's score, highest for the best view. */
    fun scores(motion: ExerciseMotion): Map<Camera, Float> {
        val muscles = CANDIDATES.associateWith { visibleMuscle(motion, it) }
        val motions = CANDIDATES.associateWith { screenMotion(motion, it) }
        val maxMuscle = muscles.values.max().coerceAtLeast(1e-6f)
        val maxMotion = motions.values.max().coerceAtLeast(1e-6f)
        return CANDIDATES.associateWith {
            MUSCLE_WEIGHT * muscles.getValue(it) / maxMuscle + MOTION_WEIGHT * motions.getValue(it) / maxMotion +
                LENGTH_WEIGHT * visibleLength(motion, it).pow(2) + FACING_WEIGHT * facing(it)
        }
    }

    /**
     * 1 for the front, 0 for the back. The figure has no face, so a view from behind only wins when it
     * shows clearly more of the trained muscles.
     */
    internal fun facing(camera: Camera) = (1f + cos(Math.toRadians(camera.yaw.toDouble())).toFloat()) / 2f

    /** Visible area of the trained muscles, summed over a few moments of the repetition. */
    fun visibleMuscle(motion: ExerciseMotion, camera: Camera): Float = SAMPLES.sumOf { progress ->
        val visible = visibleArea(motion.sceneAt(progress, camera = camera))
        visible.entries.sumOf { (emphasis, area) ->
            (if (emphasis == Emphasis.Primary) area else area * SECONDARY_WEIGHT).toDouble()
        }
    }.toFloat()

    /** How far the body's joints travel on screen from the start of the repetition, on average. */
    internal fun screenMotion(motion: ExerciseMotion, camera: Camera): Float {
        val start = motion.bodyAt(0f).segments
        return SAMPLES.sumOf { progress ->
            motion.bodyAt(progress).segments.zip(start).sumOf { (now, then) ->
                distance(camera.project(now.end), camera.project(then.end)).toDouble()
            } / start.size
        }.toFloat()
    }

    /** How much of the arms', legs' and torso's length shows on screen, from 0 (end-on) to 1 (side-on). */
    internal fun visibleLength(motion: ExerciseMotion, camera: Camera): Float = SAMPLES.map { progress ->
        val long = motion.bodyAt(progress).segments.filter { it.part in LONG_PARTS }
        long.sumOf { segment ->
            val full = (segment.end - segment.start).length
            (distance(camera.project(segment.start), camera.project(segment.end)) / full).toDouble()
        }.toFloat() / long.size
    }.average().toFloat()

    private val LONG_PARTS = setOf(
        BodyPart.UpperTorso,
        BodyPart.LowerTorso,
        BodyPart.UpperArm,
        BodyPart.Forearm,
        BodyPart.Thigh,
        BodyPart.Shank,
    )

    /** Muscle area that is not covered by parts drawn later, by emphasis, from a coarse raster. */
    internal fun visibleArea(scene: Scene): Map<Emphasis, Float> {
        val bounds = scene.layers.map { Bounds.of(it.outline) }.reduce(Bounds::plus)
        val columns = ceil(bounds.width / CELL).toInt() + 1
        val rows = ceil(bounds.height / CELL).toInt() + 1
        val owner = IntArray(columns * rows) { -1 }
        scene.layers.forEachIndexed { index, layer ->
            val box = Bounds.of(layer.outline)
            val c0 = floor((box.left - bounds.left) / CELL).toInt().coerceAtLeast(0)
            val c1 = ceil((box.right - bounds.left) / CELL).toInt().coerceAtMost(columns - 1)
            val r0 = floor((box.bottom - bounds.bottom) / CELL).toInt().coerceAtLeast(0)
            val r1 = ceil((box.top - bounds.bottom) / CELL).toInt().coerceAtMost(rows - 1)
            for (r in r0..r1) {
                for (c in c0..c1) {
                    val point = Point2(bounds.left + (c + 0.5f) * CELL, bounds.bottom + (r + 0.5f) * CELL)
                    if (contains(layer.outline, point)) owner[r * columns + c] = index
                }
            }
        }
        val area = mutableMapOf<Emphasis, Float>()
        owner.forEach { index ->
            val layer = scene.layers.getOrNull(index) as? Layer.Muscle ?: return@forEach
            area[layer.emphasis] = (area[layer.emphasis] ?: 0f) + CELL * CELL
        }
        return area
    }

    /** Even-odd point-in-polygon test. */
    internal fun contains(polygon: List<Point2>, point: Point2): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.y > point.y) != (b.y > point.y) && point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun distance(a: Point2, b: Point2): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
