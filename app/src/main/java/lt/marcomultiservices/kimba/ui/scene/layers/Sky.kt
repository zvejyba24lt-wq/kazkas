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

// Layer: SKY. Gradient, stars, sun, moon with its real phase, clouds, lightning.
// Everything above the horizon line geo.hy except the shores.

/** Where the sun stands on screen for its real height and direction. */
internal fun sunPosition(geo: SceneGeometry, c: Conditions): Offset {
    val x = geo.w * (0.5f + (c.sunHourAngle / 120.0).toFloat()).coerceIn(0.06f, 0.94f)
    val y = (geo.hy - (c.sunElevation / 40.0).toFloat() * geo.hy * 0.95f).coerceAtLeast(geo.h * 0.06f)
    return Offset(x, y)
}

internal fun DrawScope.drawSky(geo: SceneGeometry, colors: SceneColors, c: Conditions, t: Float, sun: Offset, moon: Offset) {
    drawRect(Brush.verticalGradient(listOf(colors.skyTop, lerp(colors.skyTop, colors.skyHorizon, 0.55f), colors.skyHorizon), 0f, geo.hy), size = Size(geo.w, geo.hy + 2))

    if (colors.starAlpha > 0.02f) {
        for (st in geo.stars) {
            val tw = 0.55f + 0.45f * sin(t * 1.7f + st.phase)
            drawCircle(Palette.Star.copy(alpha = colors.starAlpha * tw * (0.4f + 0.6f * (1f - st.y / geo.hy))), st.r, Offset(st.x, st.y))
        }
    }

    // Sun glow and disc.
    if (c.sunElevation > -4) {
        val clear = (1f - colors.cloud * 0.9f).coerceIn(0.1f, 1f)
        drawCircle(
            Brush.radialGradient(listOf(colors.glow.copy(alpha = 0.55f * clear), colors.glow.copy(alpha = 0f)), sun, geo.w * 0.45f),
            geo.w * 0.45f, sun,
        )
        drawCircle(Palette.SunDisc.copy(alpha = clear), geo.w * 0.032f, sun)
    }

    // Moon with its real phase.
    if (c.sunElevation < 3) {
        val vis = ((3 - c.sunElevation) / 10).toFloat().coerceIn(0f, 1f) * (1f - colors.cloud * 0.85f)
        if (vis > 0.03f) {
            val r = geo.w * 0.03f
            drawCircle(Brush.radialGradient(listOf(Palette.MoonHalo.copy(alpha = 0.33f), Palette.MoonHalo.copy(alpha = 0f)), moon, r * 6), r * 6, moon, alpha = vis * c.moonIllumination.toFloat())
            drawCircle(Palette.MoonDark.copy(alpha = vis * 0.5f), r, moon)
            drawPath(moonPath(moon, r, c.moonAge), Palette.MoonLit.copy(alpha = vis))
        }
    }

    drawClouds(geo, colors, t)

    // Lightning bolt during a thunderstorm (the flash itself is in the weather layer).
    if (colors.storm) {
        val cycle = 6.5f
        val n = floor(t / cycle)
        val ph = t - n * cycle
        if (ph < 0.18f) {
            val r = Random(n.toInt())
            val path = Path()
            var x = geo.w * (0.25f + r.nextFloat() * 0.5f)
            var y = 0f
            path.moveTo(x, y)
            while (y < geo.hy * 0.9f) {
                x += (r.nextFloat() - 0.5f) * geo.w * 0.06f
                y += geo.hy * (0.06f + r.nextFloat() * 0.08f)
                path.lineTo(x, y)
            }
            drawPath(path, Palette.Lightning, style = Stroke(geo.w * 0.008f, cap = StrokeCap.Round))
            drawPath(path, Palette.LightningGlow, style = Stroke(geo.w * 0.03f, cap = StrokeCap.Round))
        }
    }
}

/**
 * Cumulus billows lit from above with shaded bases, drifting with the wind. Their number
 * follows the real cloud cover; a heavy sky also gets a grey overcast layer behind them.
 * Art slots `cloud_1`, `cloud_2`, `cloud_3` replace the billows with pictures.
 */
private fun DrawScope.drawClouds(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val base = lerp(Palette.CloudNight, Palette.CloudDay, colors.daylight)
    val shade = lerp(lerp(base, Palette.CloudShade, 0.35f + colors.rain * 0.3f), Palette.CloudStorm, colors.cloud * 0.5f)
    val warm = lerp(base, Palette.CloudSunset, colors.warmth * 0.55f * (1f - colors.cloud * 0.5f))
    if (colors.cloud > 0.55f) {
        val o = ((colors.cloud - 0.55f) / 0.45f).coerceIn(0f, 1f)
        drawRect(
            Brush.verticalGradient(listOf(shade.copy(alpha = 0.85f * o), shade.copy(alpha = 0.35f * o), shade.copy(alpha = 0f)), 0f, geo.hy * 0.95f),
            size = Size(geo.w, geo.hy),
        )
    }
    val pictures = listOfNotNull(ArtPack["cloud_1"], ArtPack["cloud_2"], ArtPack["cloud_3"])
    val count = (colors.cloud * geo.puffs.size).toInt()
    for (i in 0 until count) {
        val p = geo.puffs[i]
        val x = ((p.x0 + t * 0.003f * (1f + colors.wind * 0.25f)) % 1.4f - 0.2f) * geo.w
        // Far clouds near the horizon are smaller and fainter.
        val far = (p.y / geo.hy).coerceIn(0f, 1f)
        val size = geo.w * 0.13f * p.scale * (1.1f - far * 0.5f)
        val haze = 1f - far * 0.35f
        if (pictures.isNotEmpty()) {
            val img = pictures[i % pictures.size]
            val wd = size * 3f
            val ht = wd * img.height / img.width
            drawArt(img, Rect(x - wd / 2, p.y - ht * 0.8f, x + wd / 2, p.y + ht * 0.2f), lerp(shade, warm, 0.7f), haze)
            continue
        }
        // Shadowed underside first, then the billows from the bottom up so the lit tops stay on top.
        drawOval(
            Brush.radialGradient(listOf(shade.copy(alpha = 0.7f * haze), shade.copy(alpha = 0f)), Offset(x, p.y), size * 1.2f),
            Offset(x - size * 1.2f, p.y - size * 0.28f), Size(size * 2.4f, size * 0.56f),
        )
        for ((dx, dy, r) in p.parts) {
            val center = Offset(x + dx * size, p.y + dy * size)
            val rad = size * r
            val lit = ((-dy) / 0.6f).coerceIn(0f, 1f)
            val col = lerp(shade, warm, 0.35f + 0.65f * lit)
            drawCircle(
                Brush.radialGradient(
                    0f to col.copy(alpha = 0.95f * haze), 0.6f to col.copy(alpha = 0.85f * haze), 1f to col.copy(alpha = 0f),
                    center = Offset(center.x - rad * 0.15f, center.y - rad * 0.2f), radius = rad * 1.1f,
                ),
                rad, center,
            )
        }
    }
}

/** The lit part of the moon for its age (0 new, 0.5 full): a half disc plus or minus a half ellipse. */
private fun moonPath(c: Offset, r: Float, age: Double): Path {
    val k = cos(2 * PI * age).toFloat()
    val waxing = age < 0.5
    val circle = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    val ex = r * abs(k)
    val ellipse = Rect(c.x - ex, c.y - r, c.x + ex, c.y + r)
    return Path().apply {
        moveTo(c.x, c.y - r)
        if (waxing) {
            arcTo(circle, -90f, 180f, false)
            arcTo(ellipse, 90f, if (k > 0) -180f else 180f, false)
        } else {
            arcTo(circle, -90f, -180f, false)
            arcTo(ellipse, 90f, if (k > 0) 180f else -180f, false)
        }
        close()
    }
}
