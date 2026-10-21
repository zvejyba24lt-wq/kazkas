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

// Layer: WEATHER. Drawn over everything: rain, snow, fireflies on summer nights, dragonflies
// on warm days, the lightning flash, and a soft vignette framing the screen.

internal fun DrawScope.drawWeather(geo: SceneGeometry, colors: SceneColors, c: Conditions, t: Float) {
    if (colors.rain > 0.05f && colors.snow < 1f) {
        val n = (geo.drops.size * colors.rain).toInt()
        val slant = 0.05f + colors.wind * 0.012f
        val col = Palette.Rain.copy(alpha = 0.18f + 0.22f * colors.daylight)
        for (i in 0 until n) {
            val d = geo.drops[i]
            val y = ((d.y + t * d.speed) % 1f) * geo.h * 1.1f - geo.h * 0.05f
            val x = (d.x + y / geo.h * slant) * geo.w
            drawLine(col, Offset(x, y), Offset(x - slant * d.len * 2, y - d.len), strokeWidth = 1.3f)
        }
        // Drops landing on the water make little rings.
        for (i in 0 until (n / 6)) {
            val d = geo.drops[i]
            val ph = (t * 1.5f + d.y * 7f) % 1f
            val x = d.x * geo.w
            val y = geo.hy + (geo.h - geo.hy) * ((d.y * 3.7f) % 1f)
            val r = geo.w * 0.012f * geo.scaleAt(y) * (0.3f + ph)
            drawOval(Palette.Rain.copy(alpha = (1 - ph) * 0.25f), Offset(x - r, y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(1f))
        }
    }
    if (colors.snow > 0f) {
        val n = (geo.flakes.size * colors.snow).toInt()
        for (i in 0 until n) {
            val f = geo.flakes[i]
            val y = ((f.y + t * f.speed) % 1f) * geo.h
            val x = ((f.x + sin(t * 0.7f + i) * 0.015f + y / geo.h * colors.wind * 0.01f) % 1f) * geo.w
            drawCircle(Palette.Snow.copy(alpha = 0.85f), f.len * (0.6f + y / geo.h * 0.8f), Offset(x, y))
        }
    }
    // Fireflies on summer nights.
    if (c.month in 6..8 && colors.daylight < 0.2f && colors.rain < 0.05f) {
        for (f in geo.flies) {
            val pulse = (sin(t * 2.3f + f.phase * 3) * 0.5f + 0.5f).let { it * it }
            val p = Offset(f.x + sin(t * 0.4f + f.phase) * f.ax, f.y + cos(t * 0.53f + f.phase) * f.ay)
            drawCircle(Brush.radialGradient(listOf(Palette.FireflyGlow, Palette.FireflyGlow.copy(alpha = 0f)), p, 14f), 14f, p, alpha = pulse)
            drawCircle(Palette.FireflyCore, 2f, p, alpha = pulse)
        }
    }
    // Dragonflies darting over the water on warm summer days.
    if (c.month in 5..8 && colors.daylight > 0.6f && colors.rain < 0.05f && c.weather.tempC > 16 && colors.wind < 7f) {
        for (k in 0 until 3) {
            val f = geo.flies[k]
            // Hover, then dart to a new spot.
            val cycle = 3.2f + k * 0.7f
            val n = floor((t + f.phase) / cycle)
            val ph = ((t + f.phase) % cycle) / cycle
            val r0 = Random(n.toInt() * 7 + k)
            val r1 = Random(n.toInt() * 7 + k + 7)
            val a = Offset(geo.w * (0.1f + r0.nextFloat() * 0.8f), geo.hy + geo.h * (0.03f + r0.nextFloat() * 0.18f))
            val b = Offset(geo.w * (0.1f + r1.nextFloat() * 0.8f), geo.hy + geo.h * (0.03f + r1.nextFloat() * 0.18f))
            val m = ((ph - 0.8f) / 0.2f).coerceIn(0f, 1f)
            val e = m * m * (3 - 2 * m)
            val p = Offset(a.x + (b.x - a.x) * e + sin(t * 9f + k) * 1.5f, a.y + (b.y - a.y) * e + cos(t * 7f + k) * 1.5f)
            val sc = geo.scaleAt(p.y) * geo.w * 0.012f
            val dir = if (b.x >= a.x) 1f else -1f
            drawLine(Palette.Dragonfly, Offset(p.x - dir * sc * 1.6f, p.y), Offset(p.x + dir * sc * 1.0f, p.y), strokeWidth = sc * 0.35f, cap = StrokeCap.Round)
            // Wings blur as they beat.
            val beat = 0.5f + 0.5f * sin(t * 60f + k)
            val wing = Palette.DragonflyWing.copy(alpha = 0.35f + 0.25f * beat)
            for (side in listOf(-1f, 1f)) {
                drawOval(wing, Offset(p.x - sc * 0.9f, p.y + side * sc * (0.1f + 0.5f * beat) - sc * 0.25f), Size(sc * 1.1f, sc * 0.5f))
                drawOval(wing, Offset(p.x - sc * 0.1f, p.y + side * sc * (0.1f + 0.5f * beat) - sc * 0.25f), Size(sc * 1.1f, sc * 0.5f))
            }
        }
    }
    if (colors.storm) {
        val cycle = 6.5f
        val ph = t % cycle
        val flash = when {
            ph < 0.08f -> 1f
            ph < 0.14f -> 0.2f
            ph < 0.22f -> 0.8f
            ph < 0.6f -> 0.8f * (1 - (ph - 0.22f) / 0.38f)
            else -> 0f
        }
        if (flash > 0f) drawRect(Palette.StormFlash.copy(alpha = flash * 0.35f), size = size)
    }
}

/** Darkens the corners a little so the eye goes to the water. */
internal fun DrawScope.drawVignette(geo: SceneGeometry) {
    drawRect(
        Brush.radialGradient(listOf(Color.Transparent, Palette.Vignette), Offset(geo.w / 2, geo.h * 0.48f), geo.h * 0.75f),
        size = size,
    )
}
