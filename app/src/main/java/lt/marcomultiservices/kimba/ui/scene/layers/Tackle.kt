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

/**
 * The float sitting in the water with spreading rings; it dips with [dip] when a fish touches
 * the bait (below 0 it lifts) and shakes and tilts while [tremble] (1 right after a nibble,
 * fading to 0) lasts. On a bite it leans over as it is pulled under.
 */
internal fun DrawScope.drawFloatOnWater(geo: SceneGeometry, pos: Offset, dip: Float, t: Float, bite: Boolean, style: FloatStyle, daylight: Float, tremble: Float = 0f) {
    val u = geo.h * 0.016f * geo.scaleAt(pos.y)
    val bob = sin(t * 2.2f) * u * 0.12f
    val stir = dip.coerceAtLeast(0f)
    for (k in 0 until 3) {
        val ph = (t * 0.55f + k / 3f) % 1f
        val r = u * (1.2f + ph * 7f) * (1f + stir)
        drawOval(Color.White.copy(alpha = (1f - ph) * (0.18f + stir * 0.35f)), Offset(pos.x - r, pos.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(1.2f + stir * 1.5f))
    }
    // A sharp little ring from each nibble.
    if (tremble > 0f) {
        val k = 1f - tremble
        val r = u * (0.8f + k * 3.5f)
        drawOval(Color.White.copy(alpha = tremble * 0.6f), Offset(pos.x - r, pos.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(1.6f))
    }
    val shake = tremble * sin(t * 55f) * u * 0.32f
    val tilt = tremble * sin(t * 38f + 1f) * 10f + (if (bite) 18f * stir.coerceAtMost(1f) else 0f)
    val sink = dip * u * 3.2f + bob
    val at = Offset(pos.x + shake, pos.y)
    clipRect(bottom = pos.y + u * 0.15f) {
        rotate(tilt, pivot = Offset(at.x, pos.y + sink)) { drawFloatBody(at, u, sink, style, daylight) }
    }
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
internal fun DrawScope.drawRod(geo: SceneGeometry, colors: SceneColors, tip: Offset, quiver: Boolean = false) {
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
        // A feeder rod ends in a bright quiver tip that shows the bite against the far shore.
        val col = when {
            u < 0.22f -> colors.lit(Palette.RodCork)
            quiver && u > 0.8f -> colors.lit(if (i % 2 == 0) Palette.QuiverTip else Palette.QuiverBand)
            else -> colors.lit(Palette.RodBlank)
        }
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
internal fun DrawScope.drawLine(geo: SceneGeometry, tip: Offset, end: Offset, stage: Stage, tension: Float, slack: Float = 1f) {
    val sag = when (stage) {
        Stage.FIGHT -> geo.h * 0.03f * (1f - tension).coerceIn(0f, 1f)
        Stage.WAITING, Stage.BITE -> geo.h * 0.04f * slack
        else -> geo.h * 0.005f
    }
    val mid = Offset((tip.x + end.x) / 2, (tip.y + end.y) / 2 + sag)
    val path = Path().apply { moveTo(tip.x, tip.y); quadraticTo(mid.x, mid.y, end.x, end.y) }
    val col = if (stage == Stage.FIGHT && tension > 0.85f) Palette.LineStrain else Palette.Line
    drawPath(path, col, style = Stroke(if (stage == Stage.FIGHT) 2.2f else 1.4f))
}

/** A small metal feeder cage with a lead weight, hanging from the line or flying. */
internal fun DrawScope.drawFeederCage(pos: Offset, u: Float, colors: SceneColors) {
    val w = u * 1.1f
    val h = u * 2.2f
    val top = pos.y - h * 0.2f
    val body = Path().apply {
        moveTo(pos.x, top)
        cubicTo(pos.x + w, top + h * 0.1f, pos.x + w, top + h * 0.9f, pos.x, top + h)
        cubicTo(pos.x - w, top + h * 0.9f, pos.x - w, top + h * 0.1f, pos.x, top)
        close()
    }
    drawPath(body, colors.lit(Palette.FeederCage).copy(alpha = 0.35f))
    for (k in 1..3) drawLine(colors.lit(Palette.FeederCage), Offset(pos.x - w * 0.8f, top + h * k / 4f), Offset(pos.x + w * 0.8f, top + h * k / 4f), strokeWidth = u * 0.14f, cap = StrokeCap.Round)
    drawPath(body, colors.lit(Palette.FeederCage), style = Stroke(u * 0.18f))
    drawOval(colors.lit(Palette.FeederLead), Offset(pos.x - w * 0.55f, top + h * 0.82f), Size(w * 1.1f, h * 0.3f))
}

/** A spinning spoon, silver with a red dot, flashing as it turns. */
internal fun DrawScope.drawSpoon(pos: Offset, u: Float, t: Float, colors: SceneColors) {
    val turn = 0.45f + 0.55f * abs(sin(t * 9f))
    val w = u * 0.9f * turn
    val h = u * 2.0f
    drawLine(colors.lit(Palette.SpoonShade), Offset(pos.x, pos.y - h * 0.7f), Offset(pos.x, pos.y - h * 0.35f), strokeWidth = u * 0.12f)
    val body = Path().apply {
        moveTo(pos.x, pos.y - h * 0.35f)
        cubicTo(pos.x + w, pos.y - h * 0.2f, pos.x + w * 0.8f, pos.y + h * 0.5f, pos.x, pos.y + h * 0.6f)
        cubicTo(pos.x - w * 0.8f, pos.y + h * 0.5f, pos.x - w, pos.y - h * 0.2f, pos.x, pos.y - h * 0.35f)
        close()
    }
    drawPath(body, Brush.horizontalGradient(listOf(colors.lit(Palette.Spoon), colors.lit(Palette.SpoonShade)), pos.x - w, pos.x + w))
    drawCircle(colors.lit(Palette.SpoonRed), u * 0.22f * turn.coerceAtLeast(0.5f), Offset(pos.x, pos.y + h * 0.15f))
    drawCircle(Color.White.copy(alpha = 0.8f * turn), u * 0.18f, Offset(pos.x - w * 0.3f, pos.y - h * 0.05f))
    drawLine(colors.lit(Palette.SpoonShade), Offset(pos.x, pos.y + h * 0.6f), Offset(pos.x, pos.y + h * 0.85f), strokeWidth = u * 0.1f)
}

/** Where a feeder line goes into the water: small rings that pulse when the fish pulls. */
internal fun DrawScope.drawLineEntry(geo: SceneGeometry, pos: Offset, t: Float, dip: Float) {
    val u = geo.h * 0.016f * geo.scaleAt(pos.y)
    val stir = dip.coerceAtLeast(0f)
    for (k in 0 until 2) {
        val ph = (t * 0.45f + k / 2f) % 1f
        val r = u * (0.5f + ph * 3.5f) * (1f + stir)
        drawOval(Color.White.copy(alpha = (1f - ph) * (0.16f + stir * 0.4f)), Offset(pos.x - r, pos.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(1.1f + stir))
    }
}

/**
 * A spinning lure being wound back just under the surface: a soft V of ripples behind it and a
 * flash where it turns.
 */
internal fun DrawScope.drawSpinWake(geo: SceneGeometry, pos: Offset, t: Float) {
    val u = geo.h * 0.016f * geo.scaleAt(pos.y)
    for (k in 0 until 4) {
        val ph = (t * 1.6f + k / 4f) % 1f
        val back = u * (0.5f + ph * 4f)
        val spread = u * (0.4f + ph * 2.2f)
        val a = (1f - ph) * 0.35f
        // The lure comes towards the angler, so the wake trails behind it, further out.
        drawLine(Color.White.copy(alpha = a), Offset(pos.x, pos.y - back * 0.3f), Offset(pos.x - spread, pos.y - back * 0.55f), strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = a), Offset(pos.x, pos.y - back * 0.3f), Offset(pos.x + spread, pos.y - back * 0.55f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    }
    val flash = abs(sin(t * 9f))
    drawCircle(Color.White.copy(alpha = 0.25f + 0.45f * flash), u * (0.25f + 0.2f * flash), pos)
}

/**
 * The forked rod rest the feeder rod lies on, with the electronic bite alarm on it once
 * bought: its red light flashes while [alarm] is on.
 */
internal fun DrawScope.drawRodRest(geo: SceneGeometry, colors: SceneColors, hasAlarm: Boolean, alarm: Boolean, t: Float) {
    val at = Offset(geo.w * 0.93f, geo.h * 0.8f)
    val wood = colors.lit(Palette.RodRest)
    val s = geo.w * 0.012f
    drawLine(wood, Offset(at.x, at.y + s), Offset(at.x + s * 0.4f, geo.h * 0.97f), strokeWidth = s * 1.1f, cap = StrokeCap.Round)
    drawLine(wood, Offset(at.x, at.y + s), Offset(at.x - s * 1.6f, at.y - s * 1.2f), strokeWidth = s * 0.8f, cap = StrokeCap.Round)
    drawLine(wood, Offset(at.x, at.y + s), Offset(at.x + s * 1.6f, at.y - s * 1.0f), strokeWidth = s * 0.8f, cap = StrokeCap.Round)
    if (!hasAlarm) return
    val box = Rect(at.x - s * 2.2f, at.y + s * 1.6f, at.x + s * 2.2f, at.y + s * 4.6f)
    drawRoundRect(colors.lit(Palette.AlarmBody), box.topLeft, box.size, androidx.compose.ui.geometry.CornerRadius(s * 0.8f))
    val led = Offset(box.center.x + s * 1.1f, box.center.y)
    val on = alarm && sin(t * 30f) > -0.3f
    if (on) drawCircle(Brush.radialGradient(listOf(Palette.AlarmLed.copy(alpha = 0.7f), Palette.AlarmLed.copy(alpha = 0f)), led, s * 4f), s * 4f, led)
    drawCircle(if (on) Palette.AlarmLed else Palette.AlarmLed.copy(alpha = 0.3f), s * 0.6f, led)
}
