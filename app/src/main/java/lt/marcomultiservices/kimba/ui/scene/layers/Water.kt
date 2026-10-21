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

// Layer: ON THE WATER. The water surface itself is the AGSL shader in WaterShader.kt; this
// file draws what floats on it or flies just above: lilies, river stones and leaves, gulls,
// a sea buoy, and the plain water used on phones too old for the shader.

/** Plain gradient water with a glitter path, for Android 12 and older (no AGSL). */
internal fun DrawScope.drawFallbackWater(geo: SceneGeometry, colors: SceneColors, t: Float, glowX: Float) {
    drawRect(
        Brush.verticalGradient(listOf(colors.skyHorizon, lerp(colors.skyHorizon, colors.waterDeep, 0.6f), colors.waterDeep), geo.hy, geo.h),
        topLeft = Offset(0f, geo.hy), size = Size(geo.w, geo.h - geo.hy),
    )
    for (i in 0 until 40) {
        val f = i / 40f
        val y = geo.hy + (geo.h - geo.hy) * f * f
        val x = glowX + sin(t * 1.3f + i) * geo.w * 0.03f * (1 + f * 3)
        drawLine(colors.glow.copy(alpha = colors.glowStrength * 0.5f * (1 - f)), Offset(x - geo.w * 0.03f * (1 + f * 2), y), Offset(x + geo.w * 0.03f * (1 + f * 2), y), strokeWidth = 1.5f + f * 3)
    }
}

/** Water lily pads, some with a flower. Art slots `lily` and `lily_flower` (seen from the side, about 3:1). */
internal fun DrawScope.drawLilies(geo: SceneGeometry, colors: SceneColors, t: Float, lilies: List<SceneGeometry.Lily>) {
    val padArt = ArtPack["lily"]
    val flowerArt = ArtPack["lily_flower"]
    for (l in lilies) {
        val bob = sin(t * 0.8f + l.rot) * 1.5f
        val rx = l.r
        val ry = l.r * 0.34f
        val rect = Rect(l.x - rx, l.y - ry + bob, l.x + rx, l.y + ry + bob)
        val art = if (l.flower) flowerArt ?: padArt else padArt
        if (art != null) {
            val ht = rect.width * art.height / art.width
            drawArt(art, Rect(rect.left, rect.bottom - ht, rect.right, rect.bottom), colors.lit(Color.White))
            continue
        }
        val start = (l.rot * 57.3f) % 360f
        val pad = Path().apply {
            moveTo(l.x, l.y + bob)
            arcTo(rect, start + 14f, 332f, false)
            close()
        }
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(rect.left + 2, rect.top + 3), rect.size)
        drawPath(pad, Brush.linearGradient(listOf(colors.lit(Palette.LilyLight), colors.lit(Palette.LilyDark)), Offset(rect.left, rect.top), Offset(rect.right, rect.bottom)))
        drawPath(pad, colors.lit(Palette.LilyRim).copy(alpha = 0.5f), style = Stroke(1.5f))
        // Veins from the notch.
        for (k in 0 until 5) {
            val a = (start + 50f + k * 60f) * (PI.toFloat() / 180f)
            drawLine(colors.lit(Palette.LilyDark).copy(alpha = 0.45f), Offset(l.x, l.y + bob), Offset(l.x + cos(a) * rx * 0.8f, l.y + bob + sin(a) * ry * 0.8f), strokeWidth = 1f)
        }
        if (l.flower) {
            val c = Offset(l.x + rx * 0.2f, l.y - ry * 0.4f + bob)
            for (k in 0 until 7) {
                val a = k / 7f * 360f
                rotate(a, c) {
                    drawOval(colors.lit(Palette.LilyPetal), Offset(c.x - rx * 0.06f, c.y - rx * 0.24f), Size(rx * 0.12f, rx * 0.24f))
                }
            }
            drawCircle(colors.lit(Palette.LilyHeart), rx * 0.06f, c)
        }
    }
}

/** River: stones in the shallows with water piling up against them, and leaves drifting downstream. */
internal fun DrawScope.drawRiverBits(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val stoneArt = ArtPack["stone"]
    for (st in geo.stones) {
        val rect = Rect(st.x - st.r, st.y - st.r * 0.45f, st.x + st.r, st.y + st.r * 0.45f)
        // Foam on the upstream side and a short wake trailing downstream.
        val foam = (sin(t * 4f + st.x) * 0.5f + 0.5f) * 0.3f + 0.3f
        val day = colors.daylight.coerceAtLeast(0.3f)
        drawOval(Palette.Foam.copy(alpha = foam * day), Offset(rect.left - st.r * 0.25f, rect.center.y), Size(st.r * 2.3f, st.r * 0.4f), style = Stroke(2f))
        for (k in 0 until 3) {
            val ph = (t * 0.8f + k / 3f) % 1f
            val x = rect.right + st.r * (0.1f + ph * 1.6f)
            drawLine(Palette.Foam.copy(alpha = (1 - ph) * 0.35f * day), Offset(x, rect.center.y + st.r * 0.1f * (k - 1)), Offset(x + st.r * 0.5f, rect.center.y + st.r * 0.1f * (k - 1)), strokeWidth = 1.5f)
        }
        if (stoneArt != null) {
            drawArt(stoneArt, rect, colors.lit(Color.White))
            continue
        }
        drawOval(Brush.verticalGradient(listOf(colors.lit(Palette.StoneLight), colors.lit(Palette.StoneDark)), rect.top, rect.bottom), rect.topLeft, rect.size)
        drawOval(colors.lit(Palette.StoneLight).copy(alpha = 0.5f), Offset(rect.left + st.r * 0.35f, rect.top + st.r * 0.06f), Size(st.r * 0.7f, st.r * 0.18f))
        drawOval(colors.lit(Palette.StoneMoss).copy(alpha = 0.5f), Offset(rect.left + st.r * 0.9f, rect.top + st.r * 0.05f), Size(st.r * 0.8f, st.r * 0.22f))
    }
    for (lf in geo.leaves) {
        val x = ((lf.x + t * lf.speed) % 1.2f - 0.1f) * geo.w
        val y = lf.y + sin(t * 1.3f + lf.phase) * geo.h * 0.002f
        val s = geo.scaleAt(y) * geo.w * 0.018f
        rotate(lf.phase * 57f + t * 20f * lf.speed, Offset(x, y)) {
            drawOval(colors.lit(if (lf.phase > 3f) Palette.LeafOrange else Palette.LeafBrown), Offset(x - s, y - s * 0.35f), Size(s * 2, s * 0.7f))
            drawLine(colors.lit(Palette.LeafBrown), Offset(x - s * 0.9f, y), Offset(x + s * 0.9f, y), strokeWidth = 1f)
        }
    }
}

/** Gulls gliding over the sea. */
internal fun DrawScope.drawGulls(geo: SceneGeometry, colors: SceneColors, t: Float) {
    if (colors.rain > 0.6f) return
    val col = lerp(Palette.GullNight, Palette.GullDay, colors.daylight * 0.3f)
    for (i in 0 until 4) {
        val ph = i * 1.7f
        val x = ((0.1f + i * 0.27f + t * (0.006f + i * 0.002f)) % 1.2f - 0.1f) * geo.w
        val y = geo.hy * (0.35f + 0.12f * i) + sin(t * 0.5f + ph) * geo.h * 0.01f
        val s = geo.w * (0.022f - i * 0.003f)
        val flap = sin(t * 3f + ph) * 0.35f
        val path = Path().apply {
            moveTo(x - s, y - s * (0.2f + flap))
            quadraticTo(x - s * 0.45f, y - s * (0.45f + flap * 0.5f), x, y)
            quadraticTo(x + s * 0.45f, y - s * (0.45f + flap * 0.5f), x + s, y - s * (0.2f + flap))
        }
        drawPath(path, col, style = Stroke(s * 0.16f, cap = StrokeCap.Round))
    }
}

/** A red and white fairway buoy rocking on the sea swell. Art slot `buoy` (about 1:2, waterline at the bottom). */
internal fun DrawScope.drawBuoy(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val base = Offset(geo.w * 0.8f, geo.hy + geo.h * 0.075f + sin(t * 1.1f) * geo.h * 0.002f)
    val s = geo.scaleAt(base.y) * geo.w * 0.03f
    val tilt = sin(t * 1.1f + 0.8f) * (6f + colors.wind)
    drawOval(Color.Black.copy(alpha = 0.2f), Offset(base.x - s * 1.1f, base.y - s * 0.15f), Size(s * 2.2f, s * 0.4f))
    rotate(tilt, base) {
        val art = ArtPack["buoy"]
        if (art != null) {
            drawArt(art, Rect(base.x - s, base.y - s * 2f, base.x + s, base.y), colors.lit(Color.White))
        } else {
            val body = Path().apply {
                moveTo(base.x - s, base.y)
                lineTo(base.x - s * 0.55f, base.y - s * 1.7f)
                lineTo(base.x + s * 0.55f, base.y - s * 1.7f)
                lineTo(base.x + s, base.y)
                close()
            }
            drawPath(body, Brush.horizontalGradient(listOf(colors.lit(Palette.BuoyRed), colors.lit(Palette.BuoyRed, 0.35f)), base.x - s, base.x + s))
            clipRect(top = base.y - s * 1.15f, bottom = base.y - s * 0.6f) {
                drawPath(body, Brush.horizontalGradient(listOf(colors.lit(Palette.BuoyWhite), colors.lit(Palette.BuoyWhite, 0.3f)), base.x - s, base.x + s))
            }
            drawLine(colors.lit(Palette.LampPost), Offset(base.x, base.y - s * 1.7f), Offset(base.x, base.y - s * 2.3f), strokeWidth = s * 0.12f)
            drawCircle(colors.lit(Palette.BuoyRed), s * 0.2f, Offset(base.x, base.y - s * 2.35f))
        }
    }
    // A light blinks on top at night.
    if (colors.daylight < 0.4f && (t % 3f) < 0.5f) {
        val c = Offset(base.x, base.y - s * 2.35f)
        drawCircle(Brush.radialGradient(listOf(Palette.BuoyRed.copy(alpha = 0.8f), Palette.BuoyRed.copy(alpha = 0f)), c, s * 2f), s * 2f, c)
    }
    // Water slapping round its foot.
    val ph = (t * 0.7f) % 1f
    drawOval(Palette.Foam.copy(alpha = (1 - ph) * 0.35f), Offset(base.x - s * (1f + ph), base.y - s * 0.2f), Size(s * 2f * (1f + ph), s * 0.4f), style = Stroke(1.5f))
}
