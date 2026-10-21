package se.novila.anatomy

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An orthographic camera around the body. [yaw] 0 looks at the front, 90 at the body's right side with
 * the body facing right on screen, and 180 at the back. [pitch] looks down from slightly above, so the
 * floor and the depth of the body read without perspective.
 */
data class Camera(val yaw: Float, val pitch: Float = DEFAULT_PITCH) {
    private val cy = cos(Math.toRadians(yaw.toDouble())).toFloat()
    private val sy = sin(Math.toRadians(yaw.toDouble())).toFloat()
    private val cp = cos(Math.toRadians(pitch.toDouble())).toFloat()
    private val sp = sin(Math.toRadians(pitch.toDouble())).toFloat()

    /** Camera space: x right and y up on screen, z towards the viewer. */
    fun view(v: Vec3): Vec3 {
        val towards = -v.x * sy + v.z * cy
        return Vec3(v.x * cy + v.z * sy, v.y * cp - towards * sp, v.y * sp + towards * cp)
    }

    fun project(v: Vec3): Point2 = view(v).let { Point2(it.x, it.y) }

    /** How flat the floor looks: a circle on it is drawn this many times as tall as it is wide. */
    val floorSquash: Float get() = sp

    companion object {
        const val DEFAULT_PITCH = 10f
    }
}

enum class Emphasis { Primary, Secondary }

/** One thing to draw, back to front, in body heights with y up. */
sealed interface Layer {
    val outline: List<Point2>

    /** A body part. [shade] is 0 nearest the viewer and up to 1 for parts on the far side. */
    data class Part(override val outline: List<Point2>, val part: BodyPart, val shade: Float) : Layer

    data class Muscle(override val outline: List<Point2>, val group: MuscleGroup, val emphasis: Emphasis) : Layer

    /** A piece of gear: a handle, a weight plate or a band. */
    data class Gear(override val outline: List<Point2>, val kind: PropKind) : Layer
}

/** A soft shadow on the floor, as an ellipse. */
data class FloorShadow(val center: Point2, val radiusX: Float, val radiusY: Float)

data class Bounds(val left: Float, val bottom: Float, val right: Float, val top: Float) {
    val width: Float get() = right - left
    val height: Float get() = top - bottom

    operator fun plus(other: Bounds) = Bounds(
        min(left, other.left),
        min(bottom, other.bottom),
        max(right, other.right),
        max(top, other.top),
    )

    companion object {
        fun of(points: List<Point2>) = Bounds(
            points.minOf { it.x },
            points.minOf { it.y },
            points.maxOf { it.x },
            points.maxOf { it.y },
        )
    }
}

/** A posed body seen through a camera, ready to draw. */
class Scene(val layers: List<Layer>, val shadow: FloorShadow?) {
    val bounds: Bounds
        get() {
            val body = layers.map { Bounds.of(it.outline) }.reduce(Bounds::plus)
            return shadow?.let {
                body + Bounds(
                    it.center.x - it.radiusX,
                    it.center.y - it.radiusY,
                    it.center.x + it.radiusX,
                    it.center.y + it.radiusY,
                )
            } ?: body
        }
}

/**
 * Turns a posed body into flat shapes: each part's silhouette, sorted back to front, followed by the
 * visible part of each highlighted muscle on it. Muscles are curved patches on the parts' oval
 * cross-sections, so they follow the movement and turn away out of sight like real muscles do.
 * Gear ([Prop]) is sorted in among the parts, so a dumbbell on the far side hides behind the body.
 */
object SceneBuilder {
    private const val CAP_POINTS = 10
    private const val ALONG_SAMPLES = 9
    private const val AROUND_SAMPLES = 19
    private const val MUSCLE_INSET = 0.86f
    private const val SHADE_DEPTH = 0.14f
    private const val SHADOW_MARGIN = 0.07f

    fun build(
        body: PosedBody,
        camera: Camera,
        highlights: Map<MuscleGroup, Emphasis>,
        contacts: Set<Landmark> = emptySet(),
        shadowUnder: Set<Landmark> = contacts,
        props: List<Prop> = emptyList(),
    ): Scene {
        fun depth(start: Vec3, end: Vec3) = (camera.view(start).z + camera.view(end).z) / 2f
        val depths = body.segments.associateWith { depth(it.start, it.end) }
        val nearest = depths.values.max()
        val parts = body.segments.map { segment ->
            depths.getValue(segment) to {
                val shade = ((nearest - depths.getValue(segment)) / SHADE_DEPTH).coerceIn(0f, 1f)
                listOf(Layer.Part(silhouette(segment, camera), segment.part, shade)) +
                    muscles(segment, camera, highlights)
            }
        }
        val gear = props.map { prop ->
            depth(prop.start, prop.end) to { listOf(Layer.Gear(propOutline(prop, camera), prop.kind)) }
        }
        val layers = (parts + gear).sortedBy { it.first }.flatMap { it.second() }
        return Scene(layers, shadow(body, camera, contacts, shadowUnder))
    }

    /** Silhouette half width on screen: the widest the oval cross-section reaches across the projected axis. */
    private fun screenRadius(segment: Segment, radii: Radii, across: Vec3): Float {
        val a = radii.lateral * (segment.lateral dot across)
        val b = radii.depth * (segment.frame.zAxis dot across)
        return sqrt(a * a + b * b)
    }

    private class Axis(val start: Point2, val end: Point2, val along: Point2, val across: Vec3)

    /** The segment's axis on screen. Seen end-on, any direction across will do. */
    private fun axis(segment: Segment, camera: Camera): Axis {
        val start = camera.project(segment.start)
        val end = camera.project(segment.end)
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)
        val along = if (length > 1e-4f) Point2(dx / length, dy / length) else Point2(0f, 1f)
        // `across` is the screen's left-hand normal of the axis, as a world direction.
        val acrossScreen = Point2(-along.y, along.x)
        return Axis(start, end, along, unview(camera, Vec3(acrossScreen.x, acrossScreen.y, 0f)))
    }

    /** Camera space back to body space, for directions. The camera is a rotation, so its inverse is its transpose. */
    private fun unview(camera: Camera, v: Vec3): Vec3 {
        val x = camera.view(Vec3(1f, 0f, 0f))
        val y = camera.view(Vec3(0f, 1f, 0f))
        val z = camera.view(Vec3(0f, 0f, 1f))
        return Vec3(x dot v, y dot v, z dot v)
    }

    private fun silhouette(segment: Segment, camera: Camera): List<Point2> {
        val axis = axis(segment, camera)
        val r0 = screenRadius(segment, segment.startRadii, axis.across)
        val r1 = screenRadius(segment, segment.endRadii, axis.across)
        val points = ArrayList<Point2>(CAP_POINTS * 2 + 2)

        // Each end is a half ellipse: as wide as the part, and as deep along the axis as its roundness.
        fun cap(center: Point2, radius: Float, forward: Float) {
            for (i in 0..CAP_POINTS) {
                val angle = PI * i / CAP_POINTS
                val side = cos(angle).toFloat() * radius * forward
                val out = sin(angle).toFloat() * radius * segment.roundness * forward
                points += Point2(
                    center.x - axis.along.y * side + axis.along.x * out,
                    center.y + axis.along.x * side + axis.along.y * out,
                )
            }
        }
        cap(axis.end, r1, 1f)
        cap(axis.start, r0, -1f)
        return points
    }

    private fun muscles(segment: Segment, camera: Camera, highlights: Map<MuscleGroup, Emphasis>): List<Layer> =
        MuscleMap.regions
            .filter { it.part == segment.part && it.group in highlights }
            .flatMap { region ->
                val arounds = if (region.mirrored) listOf(region.around, -region.around) else listOf(region.around)
                arounds.mapNotNull { around ->
                    musclePatch(segment, camera, region, around)?.let {
                        Layer.Muscle(it, region.group, highlights.getValue(region.group))
                    }
                }
            }

    /**
     * The visible part of a muscle region as a spindle-shaped patch. At each step along the part it
     * takes the band of the region's arc that faces the viewer, then narrows the band towards the
     * muscle's ends like a muscle belly.
     */
    private fun musclePatch(segment: Segment, camera: Camera, region: MuscleRegion, around: Float): List<Point2>? {
        val axis = axis(segment, camera)
        val towardsViewer = unview(camera, Vec3(0f, 0f, 1f))
        val front = segment.frame.zAxis
        val lateral = segment.lateral
        val left = ArrayList<Point2>(ALONG_SAMPLES)
        val right = ArrayList<Point2>(ALONG_SAMPLES)
        for (i in 0 until ALONG_SAMPLES) {
            val tau = (i + 0.5f) / ALONG_SAMPLES
            val t = lerp(region.from, region.to, tau)
            val radii = segment.radiiAt(t)
            var lo = Float.MAX_VALUE
            var hi = -Float.MAX_VALUE
            for (j in 0 until AROUND_SAMPLES) {
                val degrees = around - region.halfWidth + 2f * region.halfWidth * j / (AROUND_SAMPLES - 1)
                val phi = Math.toRadians(degrees.toDouble())
                val s = sin(phi).toFloat()
                val c = cos(phi).toFloat()
                val normal = lateral * (s / radii.lateral) + front * (c / radii.depth)
                if ((normal dot towardsViewer) <= 0.08f * normal.length) continue
                val offset = (lateral * (radii.lateral * s) + front * (radii.depth * c)) dot axis.across
                lo = min(lo, offset)
                hi = max(hi, offset)
            }
            if (lo > hi) continue
            val limit = screenRadius(segment, radii, axis.across) * MUSCLE_INSET
            lo = lo.coerceIn(-limit, limit)
            hi = hi.coerceIn(-limit, limit)
            val belly = sin(PI * tau).toFloat().pow(0.55f)
            val mid = (lo + hi) / 2f
            val half = (hi - lo) / 2f * belly
            if (half < 0.0015f) continue
            val center = Point2(lerp(axis.start.x, axis.end.x, t), lerp(axis.start.y, axis.end.y, t))
            val across = Point2(-axis.along.y, axis.along.x)
            left += Point2(center.x + across.x * (mid - half), center.y + across.y * (mid - half))
            right += Point2(center.x + across.x * (mid + half), center.y + across.y * (mid + half))
        }
        if (left.size < 3) return null
        val tip = Point2((left.first().x + right.first().x) / 2f, (left.first().y + right.first().y) / 2f)
        val tail = Point2((left.last().x + right.last().x) / 2f, (left.last().y + right.last().y) / 2f)
        return listOf(tip) + left + tail + right.asReversed()
    }

    private fun shadow(body: PosedBody, camera: Camera, contacts: Set<Landmark>, under: Set<Landmark>): FloorShadow? {
        if (contacts.isEmpty()) return null
        val floor = floorHeight(body, contacts)
        val points = under.map { body.landmarks.getValue(it) }
        val cx = points.map { it.x }.average().toFloat()
        val cz = points.map { it.z }.average().toFloat()
        val reach = points.maxOf { sqrt((it.x - cx).pow(2) + (it.z - cz).pow(2)) } + SHADOW_MARGIN
        return FloorShadow(camera.project(Vec3(cx, floor, cz)), reach, max(reach * camera.floorSquash, 0.012f))
    }

    /** The floor touches the lowest contact landmark's skin. */
    fun floorHeight(body: PosedBody, contacts: Set<Landmark>): Float =
        contacts.minOf { body.landmarks.getValue(it).y - Skeleton.CONTACT_RADIUS.getValue(it) }
}
