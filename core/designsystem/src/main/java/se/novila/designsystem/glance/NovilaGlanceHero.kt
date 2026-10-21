package se.novila.designsystem.glance

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import se.novila.designsystem.R
import se.novila.designsystem.component.NovilaSky
import se.novila.designsystem.component.contourLines
import se.novila.designsystem.component.glow
import se.novila.designsystem.component.icon
import se.novila.designsystem.theme.NovilaDarkHero
import se.novila.designsystem.theme.NovilaHeroColors
import se.novila.designsystem.theme.NovilaLightHero
import kotlin.math.min
import kotlin.math.roundToInt

/** A part of the day, coloured like its pillar on the hero. */
enum class NovilaGlancePillar { Training, Sleep, Nutrition }

/** One ring on the widget's hero: [progress] in 0f..1f in its [pillar]'s colour. */
data class NovilaGlanceRing(val progress: Float, val pillar: NovilaGlancePillar)

/**
 * The app's hero card for widgets: the pine panel with the sky's glow and contour lines, and the balance rings.
 * RemoteViews can't draw, so the panel and the rings are bitmaps drawn with the same numbers as `NovilaHeroCard`
 * (the contours are shared code), and the score is set in Hanken Grotesk like in the app. Text and colours on top
 * come from here too, so they always match the panel: light and dark follow the phone when the widget is drawn.
 */
class NovilaGlanceHero private constructor(private val context: Context, private val hero: NovilaHeroColors) {
    /** Text on the panel. */
    val content: ColorProvider = fixed(hero.content)

    /** Labels and secondary text on the panel. */
    val contentVariant: ColorProvider = fixed(hero.content.copy(alpha = CONTENT_VARIANT_ALPHA))

    /** The sun or moon in the corner. */
    val sun: ColorProvider = fixed(hero.sun)

    /** A [pillar]'s colour, for its dot in the legend. */
    fun color(pillar: NovilaGlancePillar): ColorProvider = fixed(hero.colorOf(pillar))

    /** The [sky]'s sun or moon, tinted with [sun]. */
    @DrawableRes
    fun skyIcon(sky: NovilaSky): Int = sky.icon

    /** The panel at [width] × [height]: pine, the [sky]'s glow in the top corner and contour lines. */
    fun panel(width: Dp, height: Dp, sky: NovilaSky): Bitmap {
        // The panel is soft gradients and hairlines; twice the dp is sharp enough and keeps the widget's bitmaps
        // well under the system's memory limit for RemoteViews.
        val scale = min(context.resources.displayMetrics.density, PANEL_MAX_SCALE)
        val w = (width.value * scale).roundToInt().coerceAtLeast(1)
        val h = (height.value * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = NovilaGlance.widgetCornerRadius.value * scale
        // Rounded corners in the bitmap too: before Android 12 the widget's cornerRadius is ignored.
        val bounds = RectF(0f, 0f, w.toFloat(), h.toFloat())
        canvas.clipPath(Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) })

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f,
            0f,
            bounds.right,
            bounds.bottom,
            hero.start.toArgb(),
            hero.end.toArgb(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(bounds, paint)

        val glowX = w - SKY_INSET * scale
        val glowY = SKY_INSET * scale
        val glowRadius = GLOW_RADIUS * scale
        paint.shader = RadialGradient(
            glowX,
            glowY,
            glowRadius,
            hero.sun.copy(alpha = sky.glow).toArgb(),
            hero.sun.copy(alpha = 0f).toArgb(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, glowRadius, paint)

        val contours = Path()
        contourLines(
            center = Offset(w - CONTOUR_INSET_X * scale, h - CONTOUR_INSET_Y * scale),
            step = CONTOUR_STEP * scale,
            moveTo = contours::moveTo,
            lineTo = contours::lineTo,
            close = contours::close,
        )
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scale
        paint.color = hero.content.copy(alpha = CONTOUR_ALPHA).toArgb()
        canvas.drawPath(contours, paint)
        return bitmap
    }

    /**
     * The balance rings at [size], outermost first, with [value] and its [unit] in the middle, like the hero card.
     * Everything scales with the size, so a small widget gets the same rings, only smaller.
     */
    fun rings(size: Dp, rings: List<NovilaGlanceRing>, value: String, unit: String): Bitmap {
        val scale = min(context.resources.displayMetrics.density, RINGS_MAX_SCALE)
        val px = (size.value * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // The hero card's rings are 148 dp with 9 dp strokes and 5 dp gaps; k is a dp of that frame in pixels.
        val k = px / RING_FRAME
        val stroke = RING_STROKE * k
        val gap = RING_GAP * k

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        rings.forEachIndexed { i, ring ->
            val inset = stroke / 2f + i * (stroke + gap)
            val arc = RectF(inset, inset, px - inset, px - inset)
            paint.strokeCap = Paint.Cap.BUTT
            paint.color = hero.track.toArgb()
            canvas.drawOval(arc, paint)
            val sweep = 360f * ring.progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = hero.colorOf(ring.pillar).toArgb()
                canvas.drawArc(arc, -90f, sweep, false, paint)
            }
        }

        // The score in headlineLarge (32/40) over its unit in labelSmall (11/16), centred together.
        val center = px / 2f
        val valuePaint = textPaint(R.font.hanken_grotesk_semibold, VALUE_SIZE * k, hero.content)
        val unitPaint = textPaint(
            R.font.hanken_grotesk_medium,
            UNIT_SIZE * k,
            hero.content.copy(alpha = CONTENT_VARIANT_ALPHA),
        )
        val top = center - (VALUE_LINE + UNIT_LINE) * k / 2f
        canvas.drawText(value, center, baseline(valuePaint, top, VALUE_LINE * k), valuePaint)
        canvas.drawText(unit, center, baseline(unitPaint, top + VALUE_LINE * k, UNIT_LINE * k), unitPaint)
        return bitmap
    }

    private fun textPaint(@FontRes font: Int, size: Float, color: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, font) ?: Typeface.DEFAULT_BOLD
        textSize = size
        textAlign = Paint.Align.CENTER
        this.color = color.toArgb()
    }

    /** The baseline that centres [paint]'s text in a line of [height] starting at [top]. */
    private fun baseline(paint: Paint, top: Float, height: Float): Float {
        val metrics = paint.fontMetrics
        return top + height / 2f - (metrics.ascent + metrics.descent) / 2f
    }

    companion object {
        /** The hero as the phone is set right now: the dark hero in dark mode, otherwise the light one. */
        fun of(context: Context): NovilaGlanceHero {
            val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            return NovilaGlanceHero(context, if (night) NovilaDarkHero else NovilaLightHero)
        }

        private fun fixed(color: Color): ColorProvider = ColorProvider(day = color, night = color)

        private fun NovilaHeroColors.colorOf(pillar: NovilaGlancePillar) = when (pillar) {
            NovilaGlancePillar.Training -> training
            NovilaGlancePillar.Sleep -> sleep
            NovilaGlancePillar.Nutrition -> nutrition
        }

        private const val PANEL_MAX_SCALE = 2f
        private const val RINGS_MAX_SCALE = 3f
        private const val CONTENT_VARIANT_ALPHA = 0.8f

        // NovilaHeroCard's background, in dp.
        private const val SKY_INSET = 36f
        private const val GLOW_RADIUS = 120f
        private const val CONTOUR_INSET_X = 64f
        private const val CONTOUR_INSET_Y = 16f
        private const val CONTOUR_STEP = 18f
        private const val CONTOUR_ALPHA = 0.09f

        // NovilaHeroCard's rings and score, in dp of its 148 dp frame.
        private const val RING_FRAME = 148f
        private const val RING_STROKE = 9f
        private const val RING_GAP = 5f
        private const val VALUE_SIZE = 32f
        private const val VALUE_LINE = 40f
        private const val UNIT_SIZE = 11f
        private const val UNIT_LINE = 16f
    }
}
