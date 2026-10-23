package lt.marcomultiservices.kimba.ui.scene.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.SceneColors
import lt.marcomultiservices.kimba.ui.scene.SceneGeometry
import lt.marcomultiservices.kimba.ui.scene.drawArt
import kotlin.math.sin

/*
 * The frozen lake: snow-dusted ice instead of water, old holes of other anglers, a few of them
 * out on the ice, and the player's own hole with dark water in it. Picture slots (see
 * docs/GRAFIKA.md): `ice` (the whole ice surface from the horizon down), `ice_hole` (one hole
 * with its rim of chipped ice and snow, seen from above at an angle), `ice_angler` (a distant
 * angler sitting on a box, from behind).
 */

private fun iceRandom(k: Int, salt: Int): Float {
    var x = k * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFFFF) / 16777215f
}

/** Snow and clear ice from the horizon down, lit by the real sky. */
fun DrawScope.drawIce(geo: SceneGeometry, colors: SceneColors) {
    val top = geo.hy
    val rect = Rect(0f, top, geo.w, geo.h)
    val art = ArtPack["ice"]
    if (art != null) {
        drawArt(art, rect, colors.lit(Color.White))
        return
    }
    // Snow takes the sky's colour: blue in the shade of dusk, warm at sunset, grey under cloud.
    val far = lerp(colors.skyHorizon, Color(0xFFE6EEF4), 0.45f * colors.daylight)
    val near = colors.lit(Color(0xFFF2F6FA))
    drawRect(Brush.verticalGradient(listOf(far, near), top, geo.h), rect.topLeft, rect.size)
    // Wind-swept patches of clear grey-blue ice between the snow.
    for (k in 0 until 14) {
        val y = top + (geo.h - top) * (0.04f + 0.9f * iceRandom(k, 3))
        val s = geo.scaleAt(y)
        val x = geo.w * (iceRandom(k, 5) * 1.2f - 0.1f)
        val w = geo.w * (0.18f + 0.3f * iceRandom(k, 7)) * s
        val h = geo.h * 0.012f * s * (1f + iceRandom(k, 9))
        drawOval(colors.lit(Color(0xFF8FA9BC)).copy(alpha = 0.35f), Offset(x - w / 2, y - h / 2), Size(w, h))
        drawOval(colors.lit(Color(0xFFDDEAF4)).copy(alpha = 0.35f * colors.daylight), Offset(x - w * 0.3f, y - h * 0.4f), Size(w * 0.5f, h * 0.25f))
    }
    // Long pressure cracks.
    for (k in 0 until 5) {
        val y0 = top + (geo.h - top) * (0.1f + 0.8f * iceRandom(k, 11))
        val p = Path()
        var x = geo.w * iceRandom(k, 13)
        var y = y0
        p.moveTo(x, y)
        repeat(6) { i ->
            x += geo.w * (0.05f + 0.06f * iceRandom(k * 7 + i, 17)) * geo.scaleAt(y)
            y += geo.h * (iceRandom(k * 7 + i, 19) - 0.5f) * 0.02f
            p.lineTo(x, y)
        }
        drawPath(p, colors.lit(Color(0xFF6E8799)).copy(alpha = 0.5f), style = Stroke(1.2f + 1.5f * geo.scaleAt(y0), cap = StrokeCap.Round))
    }
    // Old holes, frozen over and dusted with snow (out in the distance, clear of the player's own).
    for (k in 0 until 9) {
        val y = top + (geo.h - top) * (0.06f + 0.55f * iceRandom(k, 23))
        val x = geo.w * (0.06f + 0.88f * iceRandom(k, 29))
        drawOldHole(geo, colors, Offset(x, y))
    }
}

private fun DrawScope.drawOldHole(geo: SceneGeometry, colors: SceneColors, at: Offset) {
    val s = geo.scaleAt(at.y)
    val r = geo.w * 0.022f * s
    drawOval(colors.lit(Color(0xFFFFFFFF)), Offset(at.x - r * 1.5f, at.y - r * 0.55f), Size(r * 3f, r * 1.1f))
    drawOval(colors.lit(Color(0xFF7E95A6)), Offset(at.x - r, at.y - r * 0.33f), Size(r * 2f, r * 0.66f))
}

/** Distant anglers on their boxes, each over a hole. */
fun DrawScope.drawIceAnglers(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val art = ArtPack["ice_angler"]
    val spots = listOf(Offset(0.18f, 0.035f), Offset(0.78f, 0.05f), Offset(0.36f, 0.1f))
    for ((i, p) in spots.withIndex()) {
        val y = geo.hy + geo.h * p.y
        val s = geo.scaleAt(y)
        val x = geo.w * p.x
        val hgt = geo.h * 0.06f * s
        if (art != null) {
            val w = hgt * art.width / art.height
            drawArt(art, Rect(x - w / 2, y - hgt, x + w / 2, y), colors.far(Color.White, 0.25f))
            continue
        }
        val dark = colors.far(Color(0xFF2A2E36), 0.3f)
        // Box, body in a dark coat, hood, and a short rod over the hole.
        drawRect(colors.far(Color(0xFF4A6E8A), 0.3f), Offset(x - hgt * 0.22f, y - hgt * 0.32f), Size(hgt * 0.44f, hgt * 0.32f))
        drawOval(dark, Offset(x - hgt * 0.2f, y - hgt * 0.86f), Size(hgt * 0.4f, hgt * 0.62f))
        drawCircle(dark, hgt * 0.13f, Offset(x, y - hgt * 0.92f))
        val jig = sin(t * 2.2f + i * 2f) * hgt * 0.04f
        drawLine(dark, Offset(x + hgt * 0.12f, y - hgt * 0.5f), Offset(x + hgt * 0.5f, y - hgt * 0.42f + jig), strokeWidth = 1.5f)
        drawOval(colors.far(Color(0xFF55697A), 0.3f), Offset(x + hgt * 0.38f, y - hgt * 0.06f), Size(hgt * 0.26f, hgt * 0.1f))
    }
}

/** The player's hole: chipped ice and snow around dark water that trembles with the float. */
fun DrawScope.drawIceHole(geo: SceneGeometry, colors: SceneColors, at: Offset, t: Float, stir: Float) {
    val s = geo.scaleAt(at.y)
    val r = geo.w * 0.1f * s
    val art = ArtPack["ice_hole"]
    if (art != null) {
        val w = r * 3.2f
        val h = w * art.height / art.width
        drawArt(art, Rect(at.x - w / 2, at.y - h / 2, at.x + w / 2, at.y + h / 2), colors.lit(Color.White))
    } else {
        // Drill spoil: a ring of slushy snow around the hole.
        drawOval(colors.lit(Color(0xFFFFFFFF)), Offset(at.x - r * 1.6f, at.y - r * 0.62f), Size(r * 3.2f, r * 1.24f))
        drawOval(colors.lit(Color(0xFFCFDCE6)), Offset(at.x - r * 1.18f, at.y - r * 0.44f), Size(r * 2.36f, r * 0.88f))
        // The cut wall of the ice, blue-green where the light goes through it.
        drawOval(colors.lit(Color(0xFF6FA3B8)), Offset(at.x - r, at.y - r * 0.36f), Size(r * 2f, r * 0.76f))
    }
    // Black water in the hole.
    val water = Brush.radialGradient(listOf(colors.lit(Color(0xFF1C3442)), Color(0xFF050B10)), Offset(at.x, at.y - r * 0.05f), r)
    drawOval(water, Offset(at.x - r * 0.86f, at.y - r * 0.28f), Size(r * 1.72f, r * 0.6f))
    // Rings when the float moves.
    if (stir > 0.02f) {
        val k = (t * 1.4f) % 1f
        drawOval(Color.White.copy(alpha = 0.35f * stir * (1f - k)), Offset(at.x - r * 0.8f * k, at.y - r * 0.26f * k), Size(r * 1.6f * k, r * 0.52f * k), style = Stroke(1.5f))
    }
    // Skin of new ice glinting at the edge.
    drawArc(Color.White.copy(alpha = 0.5f * colors.daylight + 0.15f), 200f, 110f, false, Offset(at.x - r * 0.8f, at.y - r * 0.26f), Size(r * 1.6f, r * 0.52f), style = Stroke(2f))
}

/** Thin ice forming along the shores before the lake freezes over. */
fun DrawScope.drawIceRim(geo: SceneGeometry, colors: SceneColors, amount: Float) {
    val a = amount.coerceIn(0f, 1f)
    val col = colors.lit(Color(0xFFEAF2F8)).copy(alpha = 0.92f)
    // Along the far shore.
    drawRect(Brush.verticalGradient(listOf(col, col.copy(alpha = 0f)), geo.hy, geo.hy + geo.h * 0.03f * (0.4f + a)), Offset(0f, geo.hy), Size(geo.w, geo.h * 0.03f * (0.4f + a)))
    // Around the jetty and the near bank.
    val nearTop = geo.h * (0.9f - 0.1f * a)
    drawRect(Brush.verticalGradient(listOf(col.copy(alpha = 0f), col), nearTop, geo.h), Offset(0f, nearTop), Size(geo.w, geo.h - nearTop))
    for (k in 0 until 10) {
        val x = geo.w * iceRandom(k, 41)
        val y = nearTop + (geo.h - nearTop) * (0.2f + 0.6f * iceRandom(k, 43))
        val w = geo.w * 0.08f * (0.5f + iceRandom(k, 47))
        drawOval(col, Offset(x - w / 2, y - w * 0.08f), Size(w, w * 0.16f))
    }
}
