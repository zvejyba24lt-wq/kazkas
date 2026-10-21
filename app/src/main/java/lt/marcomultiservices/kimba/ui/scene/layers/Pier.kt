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

// Layer: PIER AND REEDS. The wooden jetty the player stands on (a long Palanga style pier at
// the sea), its posts, railing and lamps, the bait tin, and the reeds in front of and behind it.

/** Where a `pier` / `pier_sea` picture goes: the jetty's bounding box with room above for railing and lamps. */
internal fun pierRect(geo: SceneGeometry, sea: Boolean): Rect {
    val p = if (sea) geo.seaPier else geo.pier
    return Rect(p.near.first, p.farY - geo.h * (if (sea) 0.2f else 0.12f), p.near.second, geo.h)
}

internal fun DrawScope.drawPier(geo: SceneGeometry, colors: SceneColors, sea: Boolean, t: Float) {
    val art = ArtPack[if (sea) "pier_sea" else "pier"]
    if (art != null) {
        drawArt(art, pierRect(geo, sea), colors.lit(Color.White))
    } else {
        drawDeck(geo, colors, sea)
        if (sea) drawSeaPierRailing(geo, colors, t)
    }
    drawBaitTin(geo, colors)
}

/** Posts, side beam and boards of a jetty, each board with its own tone, seams, grain and nails. */
private fun DrawScope.drawDeck(geo: SceneGeometry, colors: SceneColors, sea: Boolean) {
    val shape = if (sea) geo.seaPier else geo.pier
    val pal = if (sea) Palette.SeaPier else Palette.Pier
    fun thick(y: Float) = geo.h * 0.018f * geo.scaleAt(y)

    // Posts standing in the water, with their reflections: both far corners, more along a long pier.
    val postDepths = if (sea) listOf(0f, 0.12f, 0.28f) else listOf(0f)
    for (d in postDepths) {
        val (l, r) = shape.edges(d)
        val corners = if (d == 0f) listOf(l, r) else listOf(r)
        for (c in corners) {
            val s = geo.scaleAt(c.y)
            val pw = geo.w * 0.026f * s
            val top = c.y - (if (!sea && c == r) geo.h * 0.035f * s else geo.h * 0.01f * s)
            val bottom = c.y + thick(c.y) + geo.h * 0.045f * s
            val x = if (c == r) c.x - pw * 0.6f else c.x + pw * 0.6f
            drawRect(Brush.horizontalGradient(listOf(colors.lit(pal.post), colors.lit(pal.post, 0.4f)), x - pw / 2, x + pw / 2), Offset(x - pw / 2, top), Size(pw, bottom - top))
            drawRect(colors.lit(pal.edgeLight).copy(alpha = 0.35f), Offset(x - pw / 2, top), Size(pw, geo.h * 0.003f))
            drawRect(Brush.verticalGradient(listOf(colors.lit(pal.post).copy(alpha = 0.45f), Color.Transparent), bottom, bottom + geo.h * 0.05f * s), Offset(x - pw / 2, bottom), Size(pw, geo.h * 0.05f * s))
            drawOval(Color.White.copy(alpha = 0.18f), Offset(x - pw, bottom - geo.h * 0.002f), Size(pw * 2, geo.h * 0.005f), style = Stroke(1.2f))
        }
    }

    // Side beam along the right edge and the face of the far end, so the deck has thickness.
    val (fl, fr) = shape.edges(0f)
    val (_, nr) = shape.edges(1f)
    val side = Path().apply {
        moveTo(fr.x, fr.y); lineTo(nr.x, nr.y); lineTo(nr.x, nr.y + thick(nr.y)); lineTo(fr.x, fr.y + thick(fr.y)); close()
    }
    drawPath(side, colors.lit(pal.side, 0.15f))
    drawRect(Brush.verticalGradient(listOf(colors.lit(pal.side), colors.lit(pal.side, 0.3f)), fl.y, fl.y + thick(fl.y)), Offset(fl.x, fl.y), Size(fr.x - fl.x, thick(fl.y)))

    // Boards laid across, far to near.
    val n = if (sea) 26 else 13
    for (i in 0 until n) {
        val (a0, b0) = shape.plank(i, n)
        val (a1, b1) = shape.plank(i + 1, n)
        val depth = shape.depth(i, n)
        val v = sin(i * 12.9898f + (if (sea) 3f else 0f)) // a fixed tone per board, -1..1
        var col = lerp(pal.boardFar, pal.boardNear, depth)
        col = lerp(col, if (v > 0) pal.edgeLight else pal.seam, abs(v) * 0.14f)
        val board = Path().apply { moveTo(a0.x, a0.y); lineTo(b0.x, b0.y); lineTo(b1.x, b1.y); lineTo(a1.x, a1.y); close() }
        drawPath(board, Brush.verticalGradient(listOf(colors.lit(col, 0.06f), colors.lit(col)), a0.y, a1.y))
        val bh = a1.y - a0.y
        // Grain: two broken lines along each board.
        for (g in 1..2) {
            val f = g / 3f
            val y = a0.y + bh * f
            val xl = a0.x + (a1.x - a0.x) * f
            val xr = b0.x + (b1.x - b0.x) * f
            val start = 0.08f + 0.3f * abs(sin(i * 3.1f + g))
            val end = 0.55f + 0.4f * abs(sin(i * 1.7f + g * 2f))
            drawLine(colors.lit(pal.grain).copy(alpha = 0.3f), Offset(xl + (xr - xl) * start, y), Offset(xl + (xr - xl) * end, y), strokeWidth = 0.8f + depth * 1.2f)
        }
        // Light catches the near edge of each board; the gap after it is dark.
        drawLine(colors.lit(pal.edgeLight).copy(alpha = 0.3f), Offset(a1.x, a1.y - 1.5f - depth * 2f), Offset(b1.x, b1.y - 1.5f - depth * 2f), strokeWidth = 1f + depth)
        drawLine(colors.lit(pal.seam).copy(alpha = 0.9f), a1, b1, strokeWidth = 1.2f + 3f * depth)
        // Nails over the joists near both edges.
        val nailR = 0.8f + 1.8f * depth
        val my = a0.y + bh * 0.5f
        val mxl = (a0.x + a1.x) / 2
        val mxr = (b0.x + b1.x) / 2
        for (k in listOf(0.07f, 0.5f, 0.93f)) {
            val x = mxl + (mxr - mxl) * k
            drawCircle(colors.lit(pal.nail).copy(alpha = 0.8f), nailR, Offset(x - nailR * 1.5f, my))
            drawCircle(colors.lit(pal.nail).copy(alpha = 0.8f), nailR, Offset(x + nailR * 1.5f, my))
        }
    }
    // A worn path down the middle where people walk.
    val (na, nb) = shape.edges(1f)
    drawLine(colors.lit(pal.edgeLight).copy(alpha = 0.08f), Offset((fl.x + fr.x) / 2, fl.y), Offset((na.x + nb.x) / 2, na.y), strokeWidth = geo.w * 0.08f)
}

/** The sea pier's white railing on both sides and its lamp posts, lit at night. */
private fun DrawScope.drawSeaPierRailing(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val shape = geo.seaPier
    val rail = colors.lit(Palette.SeaPier.rail)
    for (right in listOf(false, true)) {
        val posts = 10
        var prevTop: Offset? = null
        var prevMid: Offset? = null
        for (k in 0..posts) {
            val d = (k / posts.toFloat()).let { it * it }
            val (l, r) = shape.edges(d)
            val base = if (right) r else l
            val s = geo.scaleAt(base.y)
            val hgt = geo.h * 0.075f * s
            val inset = geo.w * 0.012f * s * (if (right) -1f else 1f)
            val b = Offset(base.x + inset, base.y)
            val top = Offset(b.x, b.y - hgt)
            val mid = Offset(b.x, b.y - hgt * 0.5f)
            drawLine(rail, b, top, strokeWidth = 1.5f + 4f * s, cap = StrokeCap.Round)
            prevTop?.let { drawLine(rail, it, top, strokeWidth = 2f + 5f * s, cap = StrokeCap.Round) }
            prevMid?.let { drawLine(rail.copy(alpha = 0.85f), it, mid, strokeWidth = 1f + 3f * s, cap = StrokeCap.Round) }
            prevTop = top
            prevMid = mid
        }
    }
    // Lamp posts on the right railing.
    for (d in listOf(0.08f, 0.45f)) {
        val (_, r) = shape.edges(d)
        val s = geo.scaleAt(r.y)
        val x = r.x - geo.w * 0.012f * s
        val hgt = geo.h * 0.2f * s
        val pole = colors.lit(Palette.LampPost)
        drawLine(pole, Offset(x, r.y), Offset(x, r.y - hgt), strokeWidth = 2f + 4f * s, cap = StrokeCap.Round)
        val head = Offset(x - geo.w * 0.02f * s, r.y - hgt)
        drawLine(pole, Offset(x, r.y - hgt), head, strokeWidth = 2f + 3f * s, cap = StrokeCap.Round)
        val lampW = geo.w * 0.03f * s
        val lamp = Path().apply {
            moveTo(head.x - lampW / 2, head.y + lampW * 0.1f)
            lineTo(head.x + lampW / 2, head.y + lampW * 0.1f)
            lineTo(head.x + lampW * 0.3f, head.y + lampW * 0.6f)
            lineTo(head.x - lampW * 0.3f, head.y + lampW * 0.6f)
            close()
        }
        drawPath(lamp, pole)
        if (colors.daylight < 0.5f) {
            val on = 1f - colors.daylight * 2f
            val c = Offset(head.x, head.y + lampW * 0.6f)
            drawCircle(Brush.radialGradient(listOf(Palette.LampLight.copy(alpha = 0.75f), Palette.LampLight.copy(alpha = 0f)), c, lampW * 4f), lampW * 4f, c, alpha = on)
            drawCircle(Palette.LampLight, lampW * 0.25f, c, alpha = on)
            // A pool of light on the boards below.
            drawOval(Palette.LampLight.copy(alpha = 0.12f * on), Offset(x - lampW * 4f, r.y - lampW * 0.8f), Size(lampW * 6f, lampW * 1.6f))
        }
    }
}

/** A worm tin standing on the boards. Art slot `bait_tin` (about 1:1). */
private fun DrawScope.drawBaitTin(geo: SceneGeometry, colors: SceneColors) {
    val tin = Offset(geo.w * 0.14f, geo.h * 0.9f)
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(tin.x - geo.w * 0.035f, tin.y + geo.h * 0.008f), Size(geo.w * 0.075f, geo.h * 0.016f))
    ArtPack["bait_tin"]?.let { img ->
        drawArt(img, Rect(tin.x - geo.w * 0.04f, tin.y - geo.h * 0.04f, tin.x + geo.w * 0.04f, tin.y + geo.h * 0.014f), colors.lit(Color.White))
        return
    }
    drawRect(Brush.horizontalGradient(listOf(colors.lit(Palette.TinMid), colors.lit(Palette.TinLight), colors.lit(Palette.TinDark)), tin.x - geo.w * 0.03f, tin.x + geo.w * 0.03f), Offset(tin.x - geo.w * 0.03f, tin.y - geo.h * 0.02f), Size(geo.w * 0.06f, geo.h * 0.03f))
    drawOval(colors.lit(Palette.TinSoil), Offset(tin.x - geo.w * 0.03f, tin.y - geo.h * 0.026f), Size(geo.w * 0.06f, geo.h * 0.012f))
    drawOval(colors.lit(Palette.TinLight).copy(alpha = 0.6f), Offset(tin.x - geo.w * 0.03f, tin.y - geo.h * 0.026f), Size(geo.w * 0.06f, geo.h * 0.012f), style = Stroke(1.5f))
}

/** Reeds swaying in the wind; [back] ones are drawn before the rod, the others in front of it. */
internal fun DrawScope.drawReeds(geo: SceneGeometry, colors: SceneColors, t: Float, back: Boolean) {
    val sway = 0.012f + colors.wind * 0.004f
    // Art slot `reeds` (about 1:1.85 portrait, stems standing on the bottom edge) replaces both
    // rows with one painted clump in the bottom right corner, swaying with the wind.
    ArtPack["reeds"]?.let { img ->
        if (back) return
        val r = Rect(geo.w * 0.62f, geo.h * 0.66f, geo.w * 1.04f, geo.h * 1.01f)
        val lean = sin(t * 1.1f) * (1.2f + colors.wind * 0.4f)
        rotate(lean, pivot = Offset(r.center.x, r.bottom)) {
            drawArt(img, r, light = colors.lit(Color.White))
        }
        return
    }
    for (r in geo.reeds) {
        if (r.back != back) continue
        val base = Offset(r.x, geo.h * 1.02f)
        val bend = (r.lean + sin(t * 1.3f + r.phase) * sway) * r.height
        val top = Offset(r.x + bend, base.y - r.height)
        val ctrl = Offset(r.x + bend * 0.2f, base.y - r.height * 0.55f)
        val col = colors.lit(if (back) Palette.ReedBack else Palette.ReedFront, extraDark = if (back) 0.2f else 0f)
        val path = Path().apply { moveTo(base.x, base.y); quadraticTo(ctrl.x, ctrl.y, top.x, top.y) }
        drawPath(path, col, style = Stroke(r.width, cap = StrokeCap.Round))
        drawPath(path, colors.lit(Palette.ReedShine).copy(alpha = 0.25f), style = Stroke(r.width * 0.3f, cap = StrokeCap.Round))
        if (r.head) {
            val hb = Offset(base.x + (top.x - base.x) * 0.8f + bend * 0.05f, base.y - r.height * 0.8f)
            val ht = Offset(base.x + (top.x - base.x) * 0.92f, base.y - r.height * 0.92f)
            drawLine(colors.lit(Palette.ReedHead), hb, ht, strokeWidth = r.width * 2.6f, cap = StrokeCap.Round)
            drawLine(colors.lit(Palette.ReedHeadShine).copy(alpha = 0.5f), Offset(hb.x - r.width * 0.5f, hb.y), Offset(ht.x - r.width * 0.5f, ht.y), strokeWidth = r.width * 0.8f, cap = StrokeCap.Round)
        } else {
            // A leaf peeling off the stem.
            val lb = Offset(base.x + (top.x - base.x) * 0.35f, base.y - r.height * 0.35f)
            val dir = if (sin(r.phase * 3f) > 0) 1f else -1f
            val leaf = Path().apply { moveTo(lb.x, lb.y); quadraticTo(lb.x + dir * r.height * 0.12f + bend * 0.3f, lb.y - r.height * 0.2f, lb.x + dir * r.height * 0.2f + bend * 0.5f, lb.y - r.height * 0.12f) }
            drawPath(leaf, col, style = Stroke(r.width * 0.7f, cap = StrokeCap.Round))
        }
    }
}
