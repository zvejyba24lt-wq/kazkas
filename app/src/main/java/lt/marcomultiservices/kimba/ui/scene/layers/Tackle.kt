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

/**
 * A tapered carbon rod from the bottom right corner, bending towards the line under tension:
 * a cork handle, a spinning reel hanging below it, guides with thread whippings, and on a
 * feeder rod ([quiver]) a bright orange quiver tip with white bands that shows the bite.
 */
internal fun DrawScope.drawRod(geo: SceneGeometry, colors: SceneColors, tip: Offset, quiver: Boolean = false) {
    val base = geo.rodBase
    val ctrl = Offset(geo.w * 0.92f, geo.h * 0.70f)
    val n = 48
    val pts = Array(n + 1) { i ->
        val u = i / n.toFloat()
        Offset(
            (1 - u) * (1 - u) * base.x + 2 * (1 - u) * u * ctrl.x + u * u * tip.x,
            (1 - u) * (1 - u) * base.y + 2 * (1 - u) * u * ctrl.y + u * u * tip.y,
        )
    }
    fun normal(i: Int): Offset {
        val a = pts[(i - 1).coerceAtLeast(0)]
        val b = pts[(i + 1).coerceAtMost(n)]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val l = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        return Offset(-dy / l, dx / l)
    }
    fun width(u: Float): Float = geo.w * (if (u < 0.2f) 0.034f - 0.01f * u else 0.022f - 0.0195f * ((u - 0.2f) / 0.8f))
    // The blank as one smooth filled shape: down one edge and back up the other.
    fun band(from: Int, to: Int): Path = Path().apply {
        for (i in from..to) {
            val q = pts[i] + normal(i) * (width(i / n.toFloat()) / 2)
            if (i == from) moveTo(q.x, q.y) else lineTo(q.x, q.y)
        }
        for (i in to downTo from) {
            val q = pts[i] - normal(i) * (width(i / n.toFloat()) / 2)
            lineTo(q.x, q.y)
        }
        close()
    }
    val handleEnd = (n * 0.2f).toInt()
    val tipStart = if (quiver) (n * 0.84f).toInt() else n
    drawPath(band(0, handleEnd), colors.lit(Palette.RodCork))
    // Cork rings.
    for (i in 1 until handleEnd step 2) {
        val c = pts[i]
        val nn = normal(i) * (width(i / n.toFloat()) / 2)
        drawLine(Color.Black.copy(alpha = 0.16f), c - nn, c + nn, strokeWidth = 1.2f)
    }
    drawPath(band(handleEnd, tipStart), colors.lit(Palette.RodBlank))
    if (quiver) drawPath(band(tipStart, n), colors.lit(Palette.QuiverTip))
    // Light along the upper edge of the blank.
    val shine = Path().apply {
        for (i in handleEnd..n) {
            val q = pts[i] + normal(i) * (width(i / n.toFloat()) * 0.22f)
            if (i == handleEnd) moveTo(q.x, q.y) else lineTo(q.x, q.y)
        }
    }
    drawPath(shine, colors.lit(Palette.RodShine).copy(alpha = 0.55f), style = Stroke(geo.w * 0.0025f, cap = StrokeCap.Round))
    // Guides: a thread whipping and a ring standing off the blank.
    for (u in floatArrayOf(0.34f, 0.48f, 0.6f, 0.71f, 0.81f, 0.9f, 0.97f)) {
        val i = (u * n).toInt()
        val c = pts[i]
        val nn = normal(i)
        val wdt = width(u)
        val along = Offset(nn.y, -nn.x)
        drawLine(colors.lit(Palette.RodWhip), c - along * (wdt * 0.9f), c + along * (wdt * 0.9f), strokeWidth = wdt * 1.25f)
        val ring = c - nn * (wdt * 1.6f)
        drawLine(colors.lit(Palette.RodGuide), c, ring, strokeWidth = 1.4f)
        drawCircle(colors.lit(Palette.RodGuide), wdt * 0.75f + 2f, ring, style = Stroke(1.4f))
    }
    if (quiver) for (u in floatArrayOf(0.88f, 0.93f)) {
        val i = (u * n).toInt()
        val nn = normal(i) * (width(u) / 2 + 0.5f)
        drawLine(colors.lit(Palette.QuiverBand), pts[i] - nn, pts[i] + nn, strokeWidth = geo.w * 0.006f)
    }
    drawReel(geo, colors, pts[(n * 0.13f).toInt()], pts[(n * 0.16f).toInt()] - pts[(n * 0.1f).toInt()])
}

/** A spinning reel hanging under the rod at [at]; [along] points up the rod. */
private fun DrawScope.drawReel(geo: SceneGeometry, colors: SceneColors, at: Offset, along: Offset) {
    val angle = kotlin.math.atan2(along.y, along.x) * 180f / PI.toFloat()
    val s = geo.w * 0.03f
    rotate(angle, pivot = at) {
        // Rod pointing along +x here: the reel hangs on its foot below (+y).
        drawLine(colors.lit(Palette.ReelBody), at, Offset(at.x, at.y + s * 1.3f), strokeWidth = s * 0.35f, cap = StrokeCap.Round)
        val body = Rect(at.x - s * 0.9f, at.y + s * 1.1f, at.x + s * 0.5f, at.y + s * 2.4f)
        drawOval(Brush.verticalGradient(listOf(colors.lit(Palette.ReelRim), colors.lit(Palette.ReelBody)), body.top, body.bottom), body.topLeft, body.size)
        // Spool with line, facing up the rod.
        val spool = Rect(at.x + s * 0.4f, at.y + s * 1.05f, at.x + s * 1.25f, at.y + s * 2.45f)
        drawRoundRect(Brush.horizontalGradient(listOf(colors.lit(Palette.ReelRim), colors.lit(Palette.ReelSpool)), spool.left, spool.right), spool.topLeft, spool.size, androidx.compose.ui.geometry.CornerRadius(s * 0.25f))
        drawRoundRect(colors.lit(Palette.Line).copy(alpha = 0.7f), Offset(spool.left + s * 0.15f, spool.top + s * 0.25f), Size(spool.width - s * 0.3f, spool.height - s * 0.5f), androidx.compose.ui.geometry.CornerRadius(s * 0.15f))
        // Bail arm.
        drawArc(colors.lit(Palette.ReelRim), -80f, 160f, false, Offset(spool.right - s * 0.4f, spool.top - s * 0.15f), Size(s * 0.8f, spool.height + s * 0.3f), style = Stroke(s * 0.1f))
        // Handle and knob.
        val hub = Offset(body.center.x - s * 0.1f, body.center.y)
        val knob = Offset(hub.x - s * 0.6f, hub.y + s * 1.1f)
        drawLine(colors.lit(Palette.ReelRim), hub, knob, strokeWidth = s * 0.16f, cap = StrokeCap.Round)
        drawCircle(colors.lit(Palette.ReelBody), s * 0.18f, hub)
        drawOval(colors.lit(Palette.ReelKnob), Offset(knob.x - s * 0.22f, knob.y - s * 0.32f), Size(s * 0.44f, s * 0.64f))
    }
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

/**
 * A cage feeder packed with brown groundbait, hanging from the line or flying: a mesh cylinder
 * with metal rims and a lead strip along one side. Art slot `feeder_cage` (1:1.6 portrait)
 * replaces it; the line is tied at its top centre.
 */
internal fun DrawScope.drawFeederCage(pos: Offset, u: Float, colors: SceneColors) {
    val w = u * 1.0f
    val h = u * 2.0f
    val top = pos.y + u * 0.3f
    ArtPack["feeder_cage"]?.let { img ->
        drawArt(img, Rect(pos.x - w * 0.8f, pos.y, pos.x + w * 0.8f, pos.y + h * 1.3f), light = colors.lit(Color.White))
        return
    }
    // Swivel and short link to the line.
    drawLine(colors.lit(Palette.FeederCage), pos, Offset(pos.x, top), strokeWidth = u * 0.12f)
    val body = Rect(pos.x - w / 2, top, pos.x + w / 2, top + h)
    val r = androidx.compose.ui.geometry.CornerRadius(w * 0.3f)
    drawRoundRect(Brush.horizontalGradient(listOf(colors.lit(Palette.Groundbait), colors.lit(Palette.GroundbaitDark)), body.left, body.right), body.topLeft, body.size, r)
    // Crumbs of groundbait showing through.
    for (k in 0 until 7) {
        val gx = body.left + w * (0.2f + 0.6f * ((k * 37) % 10) / 10f)
        val gy = body.top + h * (0.15f + 0.7f * ((k * 53) % 10) / 10f)
        drawCircle(colors.lit(Palette.GroundbaitDark).copy(alpha = 0.7f), u * 0.08f, Offset(gx, gy))
    }
    // Mesh: vertical wires and rings.
    val wire = colors.lit(Palette.FeederCage)
    for (k in 1..3) drawLine(wire, Offset(body.left + w * k / 4f, body.top), Offset(body.left + w * k / 4f, body.bottom), strokeWidth = u * 0.07f)
    for (k in 1..3) drawLine(wire, Offset(body.left, body.top + h * k / 4f), Offset(body.right, body.top + h * k / 4f), strokeWidth = u * 0.07f)
    drawRoundRect(wire, body.topLeft, body.size, r, style = Stroke(u * 0.14f))
    // Lead strip down one side, shiny metal rims top and bottom.
    drawRoundRect(colors.lit(Palette.FeederLead), Offset(body.left - u * 0.08f, body.top + h * 0.1f), Size(u * 0.28f, h * 0.8f), androidx.compose.ui.geometry.CornerRadius(u * 0.1f))
    drawLine(colors.lit(Palette.ReelRim), Offset(body.left, body.top), Offset(body.right, body.top), strokeWidth = u * 0.16f, cap = StrokeCap.Round)
    drawLine(colors.lit(Palette.ReelRim), Offset(body.left, body.bottom), Offset(body.right, body.bottom), strokeWidth = u * 0.16f, cap = StrokeCap.Round)
}

/** Three small hook points round a shank end at [at], [s] across: a treble hook. */
internal fun DrawScope.drawTreble(at: Offset, s: Float, color: Color) {
    drawLine(color, at, Offset(at.x, at.y + s * 0.9f), strokeWidth = s * 0.1f)
    for (side in listOf(-1f, 1f)) {
        val hook = Path().apply {
            moveTo(at.x, at.y + s * 0.6f)
            quadraticTo(at.x + side * s * 0.05f, at.y + s * 1.25f, at.x + side * s * 0.45f, at.y + s * 0.95f)
            lineTo(at.x + side * s * 0.42f, at.y + s * 0.7f)
        }
        drawPath(hook, color, style = Stroke(s * 0.09f, cap = StrokeCap.Round))
    }
    drawCircle(color, s * 0.08f, Offset(at.x, at.y + s * 1.05f))
}

/**
 * A casting spoon (blizgė): a curved silver blade with a polished highlight and a red spot,
 * on a split ring with a treble hook, flashing as it wobbles. Art slot `spoon` (1:2.4
 * portrait, ring at the top) replaces it.
 */
internal fun DrawScope.drawSpoon(pos: Offset, u: Float, t: Float, colors: SceneColors) {
    val turn = 0.55f + 0.45f * abs(sin(t * 9f))
    ArtPack["spoon"]?.let { img ->
        val w = u * 0.9f * turn
        drawArt(img, Rect(pos.x - w, pos.y - u * 0.4f, pos.x + w, pos.y + u * 3.9f), light = colors.lit(Color.White))
        return
    }
    val w = u * 0.85f * turn
    val h = u * 2.0f
    val top = pos.y + u * 0.35f
    // Split ring.
    drawCircle(colors.lit(Palette.SpoonShade), u * 0.18f, Offset(pos.x, pos.y + u * 0.15f), style = Stroke(u * 0.07f))
    val blade = Path().apply {
        moveTo(pos.x, top)
        cubicTo(pos.x + w * 0.9f, top + h * 0.1f, pos.x + w * 1.1f, top + h * 0.75f, pos.x, top + h)
        cubicTo(pos.x - w * 1.1f, top + h * 0.75f, pos.x - w * 0.9f, top + h * 0.1f, pos.x, top)
        close()
    }
    drawPath(blade, Brush.linearGradient(listOf(colors.lit(Palette.Spoon), colors.lit(Palette.SpoonShade), colors.lit(Palette.Spoon)), Offset(pos.x - w, top), Offset(pos.x + w, top + h)))
    drawPath(blade, colors.lit(Palette.SpoonShade).copy(alpha = 0.8f), style = Stroke(u * 0.06f))
    // Hammered highlight that flashes with the wobble.
    drawOval(Color.White.copy(alpha = 0.35f + 0.5f * turn), Offset(pos.x - w * 0.45f, top + h * 0.18f), Size(w * 0.45f, h * 0.38f))
    drawOval(colors.lit(Palette.SpoonRed), Offset(pos.x - w * 0.22f, top + h * 0.62f), Size(w * 0.44f, h * 0.16f))
    drawTreble(Offset(pos.x, top + h), u * 0.9f, colors.lit(Palette.SpoonShade))
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
 * The bank stick the feeder rod lies on, at the point under the rod: a telescopic metal pole
 * pushed into the ground with a rubber V head, or, once bought, an electronic bite alarm on top
 * whose red light flashes while [alarm] is on. Art slots `rod_rest` and `bite_alarm` (both
 * 1:1, the rod rests on their top centre) replace the drawings.
 */
internal fun DrawScope.drawRodRest(geo: SceneGeometry, colors: SceneColors, tip: Offset, hasAlarm: Boolean, alarm: Boolean, t: Float) {
    val base = geo.rodBase
    val ctrl = Offset(geo.w * 0.92f, geo.h * 0.70f)
    val u = 0.36f
    val at = Offset(
        (1 - u) * (1 - u) * base.x + 2 * (1 - u) * u * ctrl.x + u * u * tip.x,
        (1 - u) * (1 - u) * base.y + 2 * (1 - u) * u * ctrl.y + u * u * tip.y,
    )
    val s = geo.w * 0.022f
    val ground = geo.h * 0.985f
    // Telescopic pole: a thicker lower part and a thinner upper one.
    drawLine(Brush.horizontalGradient(listOf(colors.lit(Palette.ReelRim), colors.lit(Palette.ReelBody)), at.x - s * 0.3f, at.x + s * 0.3f), Offset(at.x, ground), Offset(at.x, at.y + s * 4f), strokeWidth = s * 0.55f)
    drawLine(Brush.horizontalGradient(listOf(colors.lit(Palette.ReelRim), colors.lit(Palette.ReelBody)), at.x - s * 0.2f, at.x + s * 0.2f), Offset(at.x, at.y + s * 4.2f), Offset(at.x, at.y + s * 1.2f), strokeWidth = s * 0.36f)
    drawRoundRect(colors.lit(Palette.AlarmBody), Offset(at.x - s * 0.4f, at.y + s * 3.8f), Size(s * 0.8f, s * 0.5f), androidx.compose.ui.geometry.CornerRadius(s * 0.15f))
    if (!hasAlarm) {
        val img = ArtPack["rod_rest"]
        if (img != null) { drawArt(img, Rect(at.x - s * 2f, at.y - s * 0.6f, at.x + s * 2f, at.y + s * 3.4f), light = colors.lit(Color.White)); return }
        // Rubber V head cradling the rod.
        val head = Path().apply {
            moveTo(at.x - s * 1.4f, at.y - s * 0.9f)
            quadraticTo(at.x - s * 1.2f, at.y + s * 1.3f, at.x, at.y + s * 1.3f)
            quadraticTo(at.x + s * 1.2f, at.y + s * 1.3f, at.x + s * 1.4f, at.y - s * 0.9f)
            lineTo(at.x + s * 0.9f, at.y - s * 0.9f)
            quadraticTo(at.x + s * 0.8f, at.y + s * 0.5f, at.x, at.y + s * 0.5f)
            quadraticTo(at.x - s * 0.8f, at.y + s * 0.5f, at.x - s * 0.9f, at.y - s * 0.9f)
            close()
        }
        drawPath(head, colors.lit(Palette.AlarmBody))
        return
    }
    val img = ArtPack["bite_alarm"]
    val on = alarm && sin(t * 30f) > -0.3f
    val led = Offset(at.x - s * 1.3f, at.y + s * 1.6f)
    if (img != null) {
        drawArt(img, Rect(at.x - s * 2.2f, at.y - s * 0.6f, at.x + s * 2.2f, at.y + s * 3.8f), light = colors.lit(Color.White))
    } else {
        // Sleek alarm body with the rod lying in its groove, a sensor wheel and the light at the front.
        val body = Path().apply {
            moveTo(at.x - s * 1.9f, at.y + s * 0.3f)
            lineTo(at.x + s * 1.7f, at.y + s * 0.1f)
            quadraticTo(at.x + s * 2.1f, at.y + s * 0.2f, at.x + s * 1.9f, at.y + s * 1.0f)
            lineTo(at.x + s * 0.6f, at.y + s * 2.9f)
            quadraticTo(at.x, at.y + s * 3.4f, at.x - s * 0.6f, at.y + s * 2.9f)
            lineTo(at.x - s * 1.9f, at.y + s * 1.2f)
            quadraticTo(at.x - s * 2.2f, at.y + s * 0.7f, at.x - s * 1.9f, at.y + s * 0.3f)
            close()
        }
        drawPath(body, Brush.verticalGradient(listOf(colors.lit(Palette.AlarmTop), colors.lit(Palette.AlarmBody)), at.y, at.y + s * 3f))
        drawCircle(colors.lit(Palette.ReelRim), s * 0.35f, Offset(at.x + s * 0.4f, at.y + s * 0.5f), style = Stroke(s * 0.12f))
        drawRoundRect(colors.lit(Palette.AlarmTop), Offset(at.x - s * 0.5f, at.y + s * 1.6f), Size(s * 1.4f, s * 0.35f), androidx.compose.ui.geometry.CornerRadius(s * 0.15f))
    }
    if (on) drawCircle(Brush.radialGradient(listOf(Palette.AlarmLed.copy(alpha = 0.75f), Palette.AlarmLed.copy(alpha = 0f)), led, s * 4f), s * 4f, led)
    drawCircle(if (on) Palette.AlarmLed else Palette.AlarmLed.copy(alpha = 0.35f), s * 0.38f, led)
    if (on) drawCircle(Color.White.copy(alpha = 0.8f), s * 0.14f, led)
}
