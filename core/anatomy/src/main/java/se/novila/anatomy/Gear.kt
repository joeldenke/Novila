package se.novila.anatomy

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Equipment an exercise needs. Without any, it is a bodyweight exercise. */
enum class Equipment { Dumbbells, Band, Barbell }

/** What a piece of gear is made of, so it can be drawn in its own color. */
enum class PropKind {
    /** A handle or a bar. */
    Bar,

    /** A weight plate. */
    Weight,

    /** A resistance band. */
    Band,
}

/**
 * One piece of gear as a cylinder from [start] to [end] with a [radius]: a handle, a weight plate or a
 * band. Measured in body heights, like the body.
 */
data class Prop(val kind: PropKind, val start: Vec3, val end: Vec3, val radius: Float)

/**
 * The gear an exercise is drawn with, held in the hands of the posed body. It follows the hands, so the
 * same keyframes work for every body, and needs no angles of its own.
 */
sealed interface Gear {
    val equipment: Equipment

    /** The pieces to draw for [body]. */
    fun props(body: PosedBody): List<Prop>

    /** A dumbbell in each hand, the handle across the palm. */
    data object Dumbbells : Gear {
        override val equipment = Equipment.Dumbbells

        override fun props(body: PosedBody): List<Prop> = Side.entries.flatMap { side ->
            val hand = body.hand(side)
            dumbbell(hand.grip, hand.segment.frame.xAxis)
        }
    }

    /** One dumbbell held upright against the chest in both hands, as in a goblet squat. */
    data object Goblet : Gear {
        override val equipment = Equipment.Dumbbells

        override fun props(body: PosedBody): List<Prop> {
            val center = (body.hand(Side.Left).grip + body.hand(Side.Right).grip) * 0.5f
            val torso = body.segments.first { it.part == BodyPart.UpperTorso }
            return dumbbell(center, torso.frame.yAxis)
        }
    }

    /** A barbell across both hands, with a plate on each end. */
    data object Barbell : Gear {
        override val equipment = Equipment.Barbell

        override fun props(body: PosedBody): List<Prop> {
            val left = body.hand(Side.Left).grip
            val right = body.hand(Side.Right).grip
            val center = (left + right) * 0.5f
            val across = (left - right).normalized()
            return listOf(
                Prop(PropKind.Bar, center - across * BAR_HALF_LENGTH, center + across * BAR_HALF_LENGTH, BAR_RADIUS),
            ) + listOf(1f, -1f).map { sign ->
                val middle = center + across * (sign * PLATE_OFFSET)
                Prop(
                    PropKind.Weight,
                    middle - across * PLATE_HALF_THICKNESS,
                    middle + across * PLATE_HALF_THICKNESS,
                    PLATE_RADIUS,
                )
            }
        }
    }

    /**
     * A resistance band from each hand to a fixed [anchor], such as a door anchor. The anchor is where the left
     * hand's end is tied, a point in the space of the exercise's first keyframe (where the body starts), so it
     * never moves; the right hand's end is tied at its mirror image. With x at 0 both ends meet in one point.
     */
    data class Band(val anchor: Vec3) : Gear {
        override val equipment = Equipment.Band

        fun anchorFor(side: Side) = Vec3(anchor.x * side.sign, anchor.y, anchor.z)

        override fun props(body: PosedBody): List<Prop> =
            Side.entries.map { Prop(PropKind.Band, body.hand(it).grip, anchorFor(it), BAND_RADIUS) }
    }

    private companion object {
        const val HANDLE_HALF_LENGTH = 0.05f
        const val HANDLE_RADIUS = 0.008f
        const val BELL_OFFSET = 0.055f
        const val BELL_HALF_THICKNESS = 0.014f
        const val BELL_RADIUS = 0.032f
        const val BAR_HALF_LENGTH = 0.5f
        const val BAR_RADIUS = 0.008f
        const val PLATE_OFFSET = 0.4f
        const val PLATE_HALF_THICKNESS = 0.02f
        const val PLATE_RADIUS = 0.06f
        const val BAND_RADIUS = 0.006f

        fun dumbbell(center: Vec3, axis: Vec3): List<Prop> {
            val along = axis.normalized()
            return listOf(
                Prop(
                    PropKind.Bar,
                    center - along * HANDLE_HALF_LENGTH,
                    center + along * HANDLE_HALF_LENGTH,
                    HANDLE_RADIUS,
                ),
            ) + listOf(1f, -1f).map { sign ->
                val middle = center + along * (sign * BELL_OFFSET)
                Prop(
                    PropKind.Weight,
                    middle - along * BELL_HALF_THICKNESS,
                    middle + along * BELL_HALF_THICKNESS,
                    BELL_RADIUS,
                )
            }
        }
    }
}

/** A hand and the point in its palm where it grips a handle. */
internal class Hand(val segment: Segment, val grip: Vec3)

internal fun PosedBody.hand(side: Side): Hand {
    val segment = segments.first { it.part == BodyPart.Hand && it.side == side }
    return Hand(segment, (segment.start + segment.end) * 0.5f)
}

/**
 * The outline of a cylinder on screen: the convex hull of its two end circles, so a weight plate seen
 * edge-on is a thin bar and seen face-on a round disc.
 */
internal fun propOutline(prop: Prop, camera: Camera): List<Point2> {
    val axis = prop.end - prop.start
    val along = if (axis.length > 1e-6f) axis.normalized() else Vec3(0f, 1f, 0f)
    val helper = if (abs(along.y) < 0.9f) Vec3(0f, 1f, 0f) else Vec3(1f, 0f, 0f)
    val u = along.cross(helper).normalized()
    val v = along.cross(u)
    val points = listOf(prop.start, prop.end).flatMap { center ->
        (0 until PROP_POINTS).map { i ->
            val angle = 2 * PI * i / PROP_POINTS
            camera.project(center + u * (prop.radius * cos(angle).toFloat()) + v * (prop.radius * sin(angle).toFloat()))
        }
    }
    return convexHull(points)
}

private const val PROP_POINTS = 16

/** Andrew's monotone chain, counterclockwise. */
internal fun convexHull(points: List<Point2>): List<Point2> {
    val sorted = points.sortedWith(compareBy<Point2> { it.x }.thenBy { it.y })
    if (sorted.size < 3) return sorted
    fun cross(o: Point2, a: Point2, b: Point2) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val lower = mutableListOf<Point2>()
    for (p in sorted) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), p) <= 0f) lower.removeAt(lower.lastIndex)
        lower += p
    }
    val upper = mutableListOf<Point2>()
    for (p in sorted.asReversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), p) <= 0f) upper.removeAt(upper.lastIndex)
        upper += p
    }
    return lower.dropLast(1) + upper.dropLast(1)
}
