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
 * The other places to stand on the home water, each its own foreground instead of the jetty.
 * Picture slots (see docs/GRAFIKA.md): `bank` (grassy reed bank across the bottom of the
 * screen, transparent above the waterline) and `boat` (the bow of a wooden rowing boat seen from
 * the stern seat, transparent around it).
 */

private fun standRandom(k: Int, salt: Int): Float {
    var x = k * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFFFF) / 16777215f
}

/** Where a lantern stands on the reed bank: in the grass among the stones, left of the cast button. */
fun bankLanternBase(geo: SceneGeometry): Offset {
    val art = ArtPack["bank"] ?: return Offset(geo.w * 0.16f, geo.h * 0.87f)
    val h = geo.w * art.height / art.width
    return Offset(geo.w * 0.22f, geo.h - h * 0.2f)
}

/** The reed bank: a grassy slope with stones at the water's edge, along the bottom of the view. */
fun DrawScope.drawBank(geo: SceneGeometry, colors: SceneColors, snow: Boolean) {
    val top = geo.h * 0.8f
    val art = ArtPack["bank"]
    if (art != null) {
        val h = geo.w * art.height / art.width
        drawArt(art, Rect(0f, geo.h - h, geo.w, geo.h), colors.lit(Color.White))
        return
    }
    val grass = if (snow) Color(0xFFE8EEF2) else Color(0xFF4F6B2E)
    val grassDark = if (snow) Color(0xFFB9C6D0) else Color(0xFF2F4419)
    // The slope rises from the water on the left and runs along the bottom.
    val slope = Path().apply {
        moveTo(0f, top - geo.h * 0.06f)
        cubicTo(geo.w * 0.25f, top - geo.h * 0.05f, geo.w * 0.45f, top + geo.h * 0.07f, geo.w * 0.7f, top + geo.h * 0.1f)
        lineTo(geo.w, top + geo.h * 0.12f)
        lineTo(geo.w, geo.h)
        lineTo(0f, geo.h)
        close()
    }
    drawPath(slope, Brush.verticalGradient(listOf(colors.lit(grass), colors.lit(grassDark, 0.2f)), top - geo.h * 0.06f, geo.h))
    // Wet mud and stones along the water line.
    for (k in 0 until 9) {
        val f = k / 8f
        val x = geo.w * (0.05f + 0.7f * f) + geo.w * 0.03f * standRandom(k, 3)
        val y = top - geo.h * 0.05f + geo.h * 0.15f * f * f + geo.h * 0.01f * standRandom(k, 5)
        val r = geo.w * (0.02f + 0.025f * standRandom(k, 7))
        drawOval(colors.lit(Color(0xFF6D6A62)), Offset(x - r, y - r * 0.5f), Size(r * 2f, r))
        drawOval(colors.lit(Color(0xFFA5A196)).copy(alpha = 0.5f), Offset(x - r * 0.6f, y - r * 0.45f), Size(r * 1.1f, r * 0.35f))
    }
    // Tufts of grass.
    for (k in 0 until 60) {
        val x = geo.w * standRandom(k, 11)
        val y = top + geo.h * (0.04f + 0.18f * standRandom(k, 13)) + geo.h * 0.08f * (x / geo.w)
        if (y > geo.h) continue
        val len = geo.h * (0.02f + 0.03f * standRandom(k, 17))
        val lean = (standRandom(k, 19) - 0.5f) * len * 0.8f
        drawLine(colors.lit(if (snow) Color(0xFF8C8A70) else lerp(grass, Color(0xFF9CB45A), standRandom(k, 23))), Offset(x, y), Offset(x + lean, y - len), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

/**
 * The bow of the rowing boat, pointing out over the water, with an oar resting on each side.
 * [rock] is the boat's sway (-1..1) so it rides the swell with the camera.
 */
/** Where a lantern stands on the boat's seat, riding the swell with the boat. */
fun boatLanternBase(geo: SceneGeometry, rock: Float): Offset {
    val lift = rock * geo.h * 0.006f
    val art = ArtPack["boat"]
    if (art != null) {
        // The seat's top edge in the picture, about three quarters down and right of centre.
        val w = geo.w * 1.1f
        val h = w * art.height / art.width
        return Offset(-geo.w * 0.05f + w * 0.64f, geo.h - h * (1f - 0.745f) + lift)
    }
    return Offset(geo.w * 0.66f, geo.h * 0.9f + lift)
}

fun DrawScope.drawBoat(geo: SceneGeometry, colors: SceneColors, rock: Float) {
    val art = ArtPack["boat"]
    val lift = rock * geo.h * 0.006f
    if (art != null) {
        val w = geo.w * 1.1f
        val h = w * art.height / art.width
        drawArt(art, Rect(-geo.w * 0.05f, geo.h - h + lift, geo.w * 1.05f, geo.h + lift), colors.lit(Color.White))
        return
    }
    val wood = Color(0xFF7A4A28)
    val woodLight = Color(0xFFA4693A)
    val paint = Color(0xFF2E5A6E)
    val tipY = geo.h * 0.72f + lift
    val bow = Offset(geo.w * 0.42f, tipY)
    val left = Offset(-geo.w * 0.12f, geo.h + lift)
    val right = Offset(geo.w * 0.98f, geo.h + lift)
    // Painted outer hull along the sides.
    val hull = Path().apply {
        moveTo(bow.x, bow.y - geo.h * 0.012f)
        cubicTo(geo.w * 0.2f, geo.h * 0.8f + lift, geo.w * 0.02f, geo.h * 0.9f + lift, left.x, left.y - geo.h * 0.02f)
        lineTo(right.x, right.y - geo.h * 0.02f)
        cubicTo(geo.w * 0.82f, geo.h * 0.9f + lift, geo.w * 0.62f, geo.h * 0.8f + lift, bow.x, bow.y - geo.h * 0.012f)
        close()
    }
    drawPath(hull, colors.lit(paint, 0.1f))
    // Inside: the floor boards running to the bow.
    val inside = Path().apply {
        moveTo(bow.x, bow.y + geo.h * 0.012f)
        cubicTo(geo.w * 0.24f, geo.h * 0.82f + lift, geo.w * 0.08f, geo.h * 0.92f + lift, geo.w * 0.0f, geo.h + lift)
        lineTo(geo.w * 0.86f, geo.h + lift)
        cubicTo(geo.w * 0.76f, geo.h * 0.92f + lift, geo.w * 0.6f, geo.h * 0.82f + lift, bow.x, bow.y + geo.h * 0.012f)
        close()
    }
    drawPath(inside, Brush.verticalGradient(listOf(colors.lit(woodLight), colors.lit(wood, 0.25f)), tipY, geo.h))
    for (k in 1..6) {
        val f = k / 7f
        drawLine(colors.lit(Color(0xFF4A2C17)).copy(alpha = 0.6f), bow, Offset(geo.w * (0.0f + 0.86f * f), geo.h + lift), strokeWidth = 1.5f)
    }
    // A thwart (seat) across the boat and the gunwale rails catching the light.
    val seatY = geo.h * 0.9f + lift
    drawRect(colors.lit(woodLight), Offset(geo.w * 0.06f, seatY), Size(geo.w * 0.74f, geo.h * 0.022f))
    drawRect(colors.lit(Color(0xFF4A2C17)), Offset(geo.w * 0.06f, seatY + geo.h * 0.022f), Size(geo.w * 0.74f, geo.h * 0.006f))
    drawPath(hull, colors.lit(Color(0xFFC08A55)), style = Stroke(geo.w * 0.012f))
    // Oars resting on the sides, blades in the water.
    val oarCol = colors.lit(Color(0xFFB98A55))
    drawLine(oarCol, Offset(geo.w * 0.18f, geo.h * 0.86f + lift), Offset(-geo.w * 0.05f, geo.h * 0.66f + lift), strokeWidth = geo.w * 0.014f, cap = StrokeCap.Round)
    drawOval(oarCol, Offset(-geo.w * 0.1f, geo.h * 0.63f + lift), Size(geo.w * 0.08f, geo.h * 0.025f))
    // The bow's iron ring.
    drawCircle(colors.lit(Color(0xFF3A3A3A)), geo.w * 0.008f, bow, style = Stroke(2.5f))
    // Ripples where the bow meets the water.
    val r = sin(rock * 3f) * 0.5f + 0.5f
    drawOval(Color.White.copy(alpha = 0.18f + 0.1f * r), Offset(bow.x - geo.w * 0.06f, bow.y + geo.h * 0.004f), Size(geo.w * 0.12f, geo.h * 0.008f), style = Stroke(1.5f))
}
