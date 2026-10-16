package se.novila.anatomy

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A point or direction in body space, measured in body heights so the figure is the same whatever the
 * user's height: x points to the body's left, y up and z forward (the way the body faces).
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scale: Float) = Vec3(x * scale, y * scale, z * scale)

    infix fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    val length: Float get() = sqrt(this dot this)

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

/** A point on the screen plane, in body heights: x to the right and y up. */
data class Point2(val x: Float, val y: Float)

/** A rotation as an orthonormal basis: the columns are where the x, y and z axes end up. */
class Rotation(val xAxis: Vec3, val yAxis: Vec3, val zAxis: Vec3) {
    operator fun times(v: Vec3): Vec3 = xAxis * v.x + yAxis * v.y + zAxis * v.z

    operator fun times(other: Rotation) = Rotation(this * other.xAxis, this * other.yAxis, this * other.zAxis)

    companion object {
        val IDENTITY = Rotation(Vec3(1f, 0f, 0f), Vec3(0f, 1f, 0f), Vec3(0f, 0f, 1f))

        /** Positive [degrees] tip the y axis forward, towards z. */
        fun aboutX(degrees: Float): Rotation {
            val (c, s) = cosSin(degrees)
            return Rotation(Vec3(1f, 0f, 0f), Vec3(0f, c, s), Vec3(0f, -s, c))
        }

        /** Positive [degrees] turn z towards x (the body's left). */
        fun aboutY(degrees: Float): Rotation {
            val (c, s) = cosSin(degrees)
            return Rotation(Vec3(c, 0f, -s), Vec3(0f, 1f, 0f), Vec3(s, 0f, c))
        }

        /** Positive [degrees] tip the y axis towards -x, so a limb hanging down swings out to the left. */
        fun aboutZ(degrees: Float): Rotation {
            val (c, s) = cosSin(degrees)
            return Rotation(Vec3(c, s, 0f), Vec3(-s, c, 0f), Vec3(0f, 0f, 1f))
        }

        private fun cosSin(degrees: Float): Pair<Float, Float> {
            val radians = Math.toRadians(degrees.toDouble())
            return cos(radians).toFloat() to sin(radians).toFloat()
        }
    }
}

internal fun lerp(from: Float, to: Float, fraction: Float) = from + (to - from) * fraction
