package lt.marcomultiservices.kimba.ui.scene.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.DayPhase
import lt.marcomultiservices.kimba.core.FloatStyle
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.game.Stage
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.Palette
import lt.marcomultiservices.kimba.ui.scene.SceneColors
import lt.marcomultiservices.kimba.ui.scene.SceneGeometry
import lt.marcomultiservices.kimba.ui.scene.drawArt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// Layer: TACKLE. The rod from the bottom right corner, the line, the float in the air and on
// the water, and the aim ring while charging a cast.

/**
 * Float body for each style, sinking by [sink] pixels. [u] is the size unit: the whole float
 * is about 5 u tall. The glow float shines in the dark. Art slot `float_<style>` (e.g.
 * `float_classic`, 1:2.6 portrait) replaces the drawing; its waterline is at 65 % height.
 */
internal fun DrawScope.drawFloatBody(pos: Offset, u: Float, sink: Float, style: FloatStyle = FloatStyle.CLASSIC, daylight: Float = 1f, alpha: Float = 1f) {
    val y = pos.y + sink
    val pal = Palette.Floats.getValue(style)
    if (style == FloatStyle.GLOW) {
        val g = 0.35f + 0.65f * (1f - daylight)
        drawCircle(Brush.radialGradient(listOf(Palette.GlowFloatHalo, Palette.GlowFloatHalo.copy(alpha = 0f)), Offset(pos.x, y - u * 2.2f), u * 3.5f), u * 3.5f, Offset(pos.x, y - u * 2.2f), alpha = g * alpha)
    }
    ArtPack["float_" + style.name.lowercase()]?.let { img ->
        drawArt(img, Rect(pos.x - u, y - u * 3.4f, pos.x + u, y + u * 1.8f), alpha = alpha)
        return
    }
    val slim = if (style == FloatStyle.FEATHER) 0.55f else 1f
    val body = Path().apply {
        moveTo(pos.x, y - u * 1.4f)
        cubicTo(pos.x + u * 0.75f * slim, y - u * 0.9f, pos.x + u * 0.55f * slim, y + u * 0.9f, pos.x, y + u * (if (style == FloatStyle.FEATHER) 2.4f else 1.6f))
        cubicTo(pos.x - u * 0.55f * slim, y + u * 0.9f, pos.x - u * 0.75f * slim, y - u * 0.9f, pos.x, y - u * 1.4f)
        close()
    }
    drawPath(body, Brush.horizontalGradient(listOf(pal.body, pal.bodyShade), pos.x - u * 0.6f, pos.x + u * 0.6f), alpha = alpha)
    clipRect(bottom = y - u * 0.35f) {
        drawPath(body, Brush.horizontalGradient(listOf(pal.tip, pal.tipShade), pos.x - u * 0.6f, pos.x + u * 0.6f), alpha = alpha)
    }
    drawLine(pal.tip, Offset(pos.x, y - u * 1.3f), Offset(pos.x, y - u * 3.2f), strokeWidth = u * 0.22f, cap = StrokeCap.Round, alpha = alpha)
    if (style == FloatStyle.GOLD) drawCircle(Color.White.copy(alpha = 0.9f * alpha), u * 0.25f, Offset(pos.x, y - u * 3.2f))
    drawCircle(Color.White.copy(alpha = 0.6f * alpha), u * 0.16f, Offset(pos.x - u * 0.22f, y - u * 0.8f))
}

/** A float for the shop: [height] is the whole float including its antenna. */
fun DrawScope.drawFloat(center: Offset, height: Float, style: FloatStyle, glow: Boolean = false, alpha: Float = 1f) {
    drawFloatBody(Offset(center.x, center.y), height / 5f, 0f, style, if (glow) 0f else 1f, alpha)
}

/** The float sitting in the water with spreading rings; it dips with [dip] when a fish touches the bait. */
internal fun DrawScope.drawFloatOnWater(geo: SceneGeometry, pos: Offset, dip: Float, t: Float, bite: Boolean, style: FloatStyle, daylight: Float) {
    val u = geo.h * 0.016f * geo.scaleAt(pos.y)
    val bob = sin(t * 2.2f) * u * 0.12f
    for (k in 0 until 3) {
        val ph = (t * 0.55f + k / 3f) % 1f
        val r = u * (1.2f + ph * 7f) * (1f + dip)
        drawOval(Color.White.copy(alpha = (1f - ph) * (0.18f + dip * 0.35f)), Offset(pos.x - r, pos.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(1.2f + dip * 1.5f))
    }
    val sink = dip * u * 3.2f + bob
    clipRect(bottom = pos.y + u * 0.15f) { drawFloatBody(pos, u, sink, style, daylight) }
    drawOval(Color.Black.copy(alpha = 0.15f), Offset(pos.x - u * 0.9f, pos.y - u * 0.15f), Size(u * 1.8f, u * 0.4f))
    if (bite) {
        drawOval(Color.White.copy(alpha = 0.5f), Offset(pos.x - u * 2.2f, pos.y - u * 0.5f), Size(u * 4.4f, u * 1.0f), style = Stroke(2.5f))
    }
}

/** A ring on the water where the cast will land, with a dotted line out to it. */
internal fun DrawScope.drawAim(geo: SceneGeometry, at: Offset, t: Float) {
    val s = geo.scaleAt(at.y)
    val r = geo.w * 0.07f * s * (1f + 0.08f * sin(t * 6f))
    val amber = Palette.Aim
    drawOval(amber.copy(alpha = 0.18f), Offset(at.x - r, at.y - r * 0.32f), Size(r * 2, r * 0.64f))
    drawOval(amber.copy(alpha = 0.9f), Offset(at.x - r, at.y - r * 0.32f), Size(r * 2, r * 0.64f), style = Stroke(2.5f))
    drawCircle(amber, 3f + 2f * s, at)
    val from = Offset(geo.w * 0.5f, geo.h * 0.93f)
    val dots = 14
    for (i in 1 until dots) {
        val f = i / dots.toFloat()
        val p = Offset(from.x + (at.x - from.x) * f, from.y + (at.y - from.y) * f)
        drawCircle(amber.copy(alpha = 0.25f + 0.35f * f), (1.5f + 2.5f * geo.scaleAt(p.y)) * 0.8f, p)
    }
}

/** A tapered carbon rod from the bottom right corner, bending towards the line under tension. */
internal fun DrawScope.drawRod(geo: SceneGeometry, colors: SceneColors, tip: Offset) {
    val base = geo.rodBase
    val ctrl = Offset(geo.w * 0.92f, geo.h * 0.70f)
    val steps = 24
    var prev = base
    for (i in 1..steps) {
        val u = i / steps.toFloat()
        val p = Offset(
            (1 - u) * (1 - u) * base.x + 2 * (1 - u) * u * ctrl.x + u * u * tip.x,
            (1 - u) * (1 - u) * base.y + 2 * (1 - u) * u * ctrl.y + u * u * tip.y,
        )
        val width = geo.w * (0.028f - 0.024f * u)
        val col = if (u < 0.22f) colors.lit(Palette.RodCork) else colors.lit(Palette.RodBlank)
        drawLine(col, prev, p, strokeWidth = width, cap = StrokeCap.Round)
        if (u >= 0.22f) drawLine(colors.lit(Palette.RodShine).copy(alpha = 0.6f), Offset(prev.x - width * 0.2f, prev.y - width * 0.2f), Offset(p.x - width * 0.2f, p.y - width * 0.2f), strokeWidth = width * 0.25f)
        if (i % 6 == 0 && u > 0.3f) drawCircle(colors.lit(Palette.RodGuide), width * 0.8f, p, style = Stroke(1.5f))
        prev = p
    }
    // Reel.
    val reel = Offset(geo.w * 0.955f, geo.h * 0.9f)
    drawCircle(colors.lit(Palette.ReelBody), geo.w * 0.03f, reel)
    drawCircle(colors.lit(Palette.ReelRim), geo.w * 0.03f, reel, style = Stroke(2f))
    drawCircle(colors.lit(Palette.ReelKnob), geo.w * 0.008f, reel)
}

/** The fishing line from the rod tip to the float: slack while waiting, tight and red when about to snap. */
internal fun DrawScope.drawLine(geo: SceneGeometry, tip: Offset, end: Offset, stage: Stage, tension: Float) {
    val sag = when (stage) {
        Stage.FIGHT -> geo.h * 0.03f * (1f - tension).coerceIn(0f, 1f)
        Stage.WAITING, Stage.BITE -> geo.h * 0.04f
        else -> geo.h * 0.005f
    }
    val mid = Offset((tip.x + end.x) / 2, (tip.y + end.y) / 2 + sag)
    val path = Path().apply { moveTo(tip.x, tip.y); quadraticTo(mid.x, mid.y, end.x, end.y) }
    val col = if (stage == Stage.FIGHT && tension > 0.85f) Palette.LineStrain else Palette.Line
    drawPath(path, col, style = Stroke(if (stage == Stage.FIGHT) 2.2f else 1.4f))
}
