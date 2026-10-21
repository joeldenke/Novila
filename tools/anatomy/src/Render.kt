import se.novila.anatomy.Camera
import se.novila.anatomy.Emphasis
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.ExerciseMotion
import se.novila.anatomy.Layer
import se.novila.anatomy.Point2
import se.novila.anatomy.PropKind
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// The light anatomy colors of NovilaTheme.anatomy, approximately.
private val BODY = Color(0xDC, 0xE7, 0xF5)
private val BODY_SHADE = Color(0xC2, 0xD4, 0xEC)
private val OUTLINE = Color(0x2F, 0x58, 0x85)
private val SHADOW = Color(0x2F, 0x58, 0x85, 36)
private val MUSCLE = Color(0xBC, 0x4C, 0x08)
private val MUSCLE_SECONDARY = Color(0xF8, 0xD3, 0xB4)
private val GEAR = Color(0x4A, 0x4F, 0x55)
private val PLATE = Color(0x2E, 0x33, 0x38)
private val BAND = Color(0x3D, 0x6B, 0x3A)
private const val CELL = 260
private val FRAMES = listOf(0f, 0.25f, 0.5f, 0.75f)

/** Contact sheet: per exercise four moments of a repetition, then the thumbnail (its peak). */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "anatomy.png")
    val ids = args.drop(1).toSet()
    val motions = ExerciseLibrary.all.filter { ids.isEmpty() || it.id in ids }
    val image = BufferedImage(CELL * (FRAMES.size + 1), CELL * motions.size, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color.WHITE
    g.fillRect(0, 0, image.width, image.height)
    motions.forEachIndexed { row, motion ->
        (FRAMES + motion.peak).forEachIndexed { column, progress ->
            draw(g, motion, progress, column * CELL, row * CELL)
        }
        g.color = Color.GRAY
        g.drawString("${motion.id}  camera ${motion.camera.yaw}/${motion.camera.pitch}  last: peak", 4, row * CELL + 14)
    }
    ImageIO.write(image, "png", out)
    println("Wrote ${out.absolutePath}")
}

private fun draw(
    g: Graphics2D,
    motion: ExerciseMotion,
    progress: Float,
    x0: Int,
    y0: Int,
    camera: Camera = motion.camera,
) {
    val bounds = motion.bounds(camera = camera)
    val margin = 12f
    val scale = minOf((CELL - 2 * margin) / bounds.width, (CELL - 2 * margin) / bounds.height)
    val ox = x0 + CELL / 2f - (bounds.left + bounds.width / 2) * scale
    val oy = y0 + CELL / 2f + (bounds.bottom + bounds.height / 2) * scale
    fun path(points: List<Point2>) = Path2D.Float().apply {
        points.forEachIndexed { i, p ->
            if (i == 0) moveTo(ox + p.x * scale, oy - p.y * scale) else lineTo(ox + p.x * scale, oy - p.y * scale)
        }
        closePath()
    }
    val scene = motion.sceneAt(progress, camera = camera)
    scene.shadow?.let {
        g.color = SHADOW
        g.fill(
            Ellipse2D.Float(
                ox + (it.center.x - it.radiusX) * scale,
                oy - (it.center.y + it.radiusY) * scale,
                2 * it.radiusX * scale,
                2 * it.radiusY * scale,
            ),
        )
    }
    val outline = BasicStroke(0.0055f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    val edge = BasicStroke(0.0045f * scale)
    var pending: Path2D.Float? = null
    fun flush() {
        pending?.let {
            g.stroke = outline
            g.color = OUTLINE
            g.draw(it)
        }
        pending = null
    }
    for (layer in scene.layers) {
        val shape = path(layer.outline)
        when (layer) {
            is Layer.Part -> {
                flush()
                g.color = mix(BODY, BODY_SHADE, layer.shade)
                g.fill(shape)
                pending = shape
            }

            is Layer.Gear -> {
                flush()
                when (layer.kind) {
                    PropKind.Bar -> g.color = GEAR
                    PropKind.Weight -> g.color = Color(PLATE.red, PLATE.green, PLATE.blue, 64)
                    PropKind.Band -> g.color = BAND
                }
                g.fill(shape)
                if (layer.kind == PropKind.Weight) {
                    g.color = PLATE
                    g.stroke = BasicStroke(0.009f * scale)
                    g.draw(shape)
                }
            }

            is Layer.Muscle -> if (layer.emphasis == Emphasis.Primary) {
                g.color = MUSCLE
                g.fill(shape)
            } else {
                g.color = MUSCLE_SECONDARY
                g.fill(shape)
                g.stroke = edge
                g.color = MUSCLE
                g.draw(shape)
            }
        }
    }
    flush()
}

private fun mix(a: Color, b: Color, t: Float) = Color(
    (a.red + (b.red - a.red) * t).toInt(),
    (a.green + (b.green - a.green) * t).toInt(),
    (a.blue + (b.blue - a.blue) * t).toInt(),
)
