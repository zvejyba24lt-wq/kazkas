package lt.marcomultiservices.kimba.ui.scene

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.FloatStyle
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.game.LakeState
import lt.marcomultiservices.kimba.game.Stage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** The living lake: sky, forest, water, jetty, reeds, rod and float, all reacting to the real conditions. */
@Composable
fun LakeScene(
    conditions: Conditions,
    lake: State<LakeState>,
    modifier: Modifier = Modifier,
    waterKind: Water = Water.LAKE,
    floatStyle: FloatStyle = FloatStyle.CLASSIC,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val geo = remember(w, h) { SceneGeometry(w, h) }
        val colors = remember(conditions) { SceneColors(conditions) }
        val water = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { WaterShader() }.getOrNull() else null }

        val time = rememberSceneTime()

        val castId by remember { derivedStateOf { lake.value.castId } }
        val dipTarget by remember { derivedStateOf { lake.value.dip } }
        val dip by animateFloatAsState(dipTarget, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow), label = "dip")
        val flight = remember { Animatable(1f) }
        LaunchedEffect(castId) {
            if (castId == 0) return@LaunchedEffect
            flight.snapTo(0f)
            flight.animateTo(1f, tween(GameViewModel.FLIGHT_MS.toInt(), easing = LinearEasing))
        }
        val tensionTarget by remember { derivedStateOf { lake.value.tension } }
        val tension by animateFloatAsState(tensionTarget, tween(90), label = "tension")

        Canvas(Modifier.fillMaxSize()) {
            val t = time.floatValue
            val s = lake.value
            val sun = sunPosition(geo, conditions)
            val moon = Offset(geo.w * 0.74f, geo.hy * 0.26f)
            val glowX = if (conditions.sunElevation > -3) sun.x else moon.x

            // Float and line state.
            val target = geo.floatTarget(s.castDist, s.castId)
            val bend = if (s.stage == Stage.FIGHT) tension.coerceIn(0f, 1.2f) else 0.08f + dip * 0.15f
            val tip = Offset(geo.rodTip.x - geo.w * 0.05f * bend, geo.rodTip.y + geo.h * 0.11f * bend)
            val hanging = s.stage == Stage.IDLE || s.stage == Stage.CHARGING
            val floatPos = when {
                hanging -> Offset(tip.x + sin(t * 1.8f) * geo.w * 0.006f, tip.y + geo.h * 0.075f)
                s.stage == Stage.FLYING -> {
                    val f = flight.value
                    val from = Offset(tip.x, tip.y + geo.h * 0.075f)
                    Offset(from.x + (target.x - from.x) * f, from.y + (target.y - from.y) * f - sin(PI.toFloat() * f) * geo.h * 0.2f)
                }
                s.stage == Stage.FIGHT -> {
                    val k = 1f - s.fightDistance
                    Offset(target.x + (geo.w * 0.58f - target.x) * k, target.y + (geo.h * 0.93f - target.y) * k)
                }
                else -> target
            }
            val onWater = s.stage == Stage.WAITING || s.stage == Stage.BITE
            val ripple = when {
                s.stage == Stage.FIGHT -> 0.8f + (if (s.running) 0.6f else 0f)
                onWater -> 0.25f + dip * 0.9f
                s.stage == Stage.FLYING && flight.value > 0.95f -> 1f
                else -> 0f
            }

            val sea = waterKind == Water.SEA
            drawSky(geo, colors, conditions, t, sun, moon)
            when (waterKind) {
                Water.SEA -> drawSeaHorizon(geo, colors, t)
                Water.POND -> { drawShores(geo, colors, ridge = false); drawWillows(geo, colors, t) }
                else -> drawShores(geo, colors)
            }
            if (water != null) {
                water.update(
                    geo.w, geo.h, geo.hy, t, colors, glowX, floatPos.x, floatPos.y, if (onWater || s.stage == Stage.FIGHT) ripple else 0f,
                    flow = if (waterKind == Water.RIVER) 0.35f else 0f,
                    swell = if (sea) 0.6f + (colors.wind / 14f).coerceAtMost(0.6f) else 0f,
                )
                drawRect(ShaderBrush(water.shader), topLeft = Offset(0f, geo.hy), size = Size(geo.w, geo.h - geo.hy))
            } else {
                drawFallbackWater(geo, colors, t, glowX)
            }
            if (!sea) drawReflections(geo, colors, t)
            drawMist(geo, colors, conditions)
            when (waterKind) {
                Water.LAKE -> drawLilies(geo, colors, t, geo.lilies)
                Water.POND -> drawLilies(geo, colors, t, geo.pondLilies)
                Water.RIVER -> drawRiverBits(geo, colors, t)
                Water.SEA -> drawGulls(geo, colors, t)
            }
            if (onWater) drawFloatOnWater(geo, floatPos, dip, t, s.stage == Stage.BITE, floatStyle, colors.daylight)
            if (s.stage == Stage.FIGHT) drawFightSplash(geo, floatPos, t, s.running)
            drawPier(geo, colors, sea)
            drawReeds(geo, colors, t, back = true, dune = sea)
            drawRod(geo, colors, tip)
            drawLine(geo, tip, floatPos, s.stage, tension)
            if (hanging || s.stage == Stage.FLYING) drawFloatBody(floatPos, geo.h * 0.016f * geo.scaleAt(geo.h), 0f, floatStyle, colors.daylight)
            drawReeds(geo, colors, t, back = false, dune = sea)
            drawWeather(geo, colors, conditions, t)
            drawVignette(geo)
        }
    }
}

private fun sunPosition(geo: SceneGeometry, c: Conditions): Offset {
    val x = geo.w * (0.5f + (c.sunHourAngle / 120.0).toFloat()).coerceIn(0.06f, 0.94f)
    val y = (geo.hy - (c.sunElevation / 40.0).toFloat() * geo.hy * 0.95f).coerceAtLeast(geo.h * 0.06f)
    return Offset(x, y)
}

private fun DrawScope.drawSky(geo: SceneGeometry, colors: SceneColors, c: Conditions, t: Float, sun: Offset, moon: Offset) {
    drawRect(Brush.verticalGradient(listOf(colors.skyTop, lerp(colors.skyTop, colors.skyHorizon, 0.55f), colors.skyHorizon), 0f, geo.hy), size = Size(geo.w, geo.hy + 2))

    if (colors.starAlpha > 0.02f) {
        for (st in geo.stars) {
            val tw = 0.55f + 0.45f * sin(t * 1.7f + st.phase)
            drawCircle(Color.White.copy(alpha = colors.starAlpha * tw * (0.4f + 0.6f * (1f - st.y / geo.hy))), st.r, Offset(st.x, st.y))
        }
    }

    // Sun glow and disc.
    if (c.sunElevation > -4) {
        val clear = (1f - colors.cloud * 0.9f).coerceIn(0.1f, 1f)
        drawCircle(
            Brush.radialGradient(listOf(colors.glow.copy(alpha = 0.55f * clear), colors.glow.copy(alpha = 0f)), sun, geo.w * 0.45f),
            geo.w * 0.45f, sun,
        )
        drawCircle(Color(0xFFFFF7E6).copy(alpha = clear), geo.w * 0.032f, sun)
    }

    // Moon with its real phase.
    if (c.sunElevation < 3) {
        val vis = ((3 - c.sunElevation) / 10).toFloat().coerceIn(0f, 1f) * (1f - colors.cloud * 0.85f)
        if (vis > 0.03f) {
            val r = geo.w * 0.03f
            drawCircle(Brush.radialGradient(listOf(Color(0x55DCE6FF), Color(0x00DCE6FF)), moon, r * 6), r * 6, moon, alpha = vis * c.moonIllumination.toFloat())
            drawCircle(Color(0xFF2A3348).copy(alpha = vis * 0.5f), r, moon)
            drawPath(moonPath(moon, r, c.moonAge), Color(0xFFF1F0E6).copy(alpha = vis))
        }
    }

    // Clouds: soft clusters drifting with the wind.
    val count = (colors.cloud * geo.puffs.size).toInt()
    if (count > 0) {
        val base = lerp(Color(0xFF262B36), Color(0xFFF4F6F8), colors.daylight)
        val shade = lerp(base, Color(0xFF707985), colors.cloud * 0.6f + colors.rain * 0.3f)
        val warm = lerp(shade, Color(0xFFF2A070), colors.warmth * 0.5f * (1f - colors.cloud * 0.5f))
        for (i in 0 until count) {
            val p = geo.puffs[i]
            val x = ((p.x0 + t * 0.003f * (1f + colors.wind * 0.25f)) % 1.4f - 0.2f) * geo.w
            val size = geo.w * 0.12f * p.scale
            for ((dx, dy, r) in p.parts) {
                val center = Offset(x + dx * size * 0.5f, p.y + dy * size * 0.5f)
                val rad = size * r
                val col = lerp(warm, shade, ((dy + 0.25f) * 2f).coerceIn(0f, 1f))
                drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.75f), col.copy(alpha = 0f)), center, rad), rad, center)
            }
        }
    }

    // Lightning bolt and flash during a thunderstorm.
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
            drawPath(path, Color(0xFFE8EEFF), style = Stroke(geo.w * 0.008f, cap = StrokeCap.Round))
            drawPath(path, Color(0x66BFD0FF), style = Stroke(geo.w * 0.03f, cap = StrokeCap.Round))
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

private val GreenFar = Color(0xFF2D4A3A)
private val GreenMid = Color(0xFF1B3626)
private val GreenNear = Color(0xFF14291C)

private fun DrawScope.drawShores(geo: SceneGeometry, colors: SceneColors, ridge: Boolean = true) {
    if (ridge) drawPath(geo.ridge, colors.far(GreenFar, 0.62f))
    drawPath(geo.forestFar, colors.far(GreenMid, 0.42f))
    if (colors.fog > 0f) {
        drawRect(
            Brush.verticalGradient(listOf(colors.skyHorizon.copy(alpha = 0f), colors.skyHorizon.copy(alpha = colors.fog * 0.75f)), geo.hy - geo.h * 0.08f, geo.hy),
            topLeft = Offset(0f, geo.hy - geo.h * 0.08f), size = Size(geo.w, geo.h * 0.08f),
        )
    }
    drawPath(geo.forestMid, colors.far(GreenNear, 0.18f + colors.fog * 0.45f))
}

private fun DrawScope.drawReflections(geo: SceneGeometry, colors: SceneColors, t: Float) {
    clipRect(top = geo.hy) {
        withTransform({ scale(1f, -1f, pivot = Offset(0f, geo.hy)) }) {
            val a = 0.5f * (1f - colors.rain * 0.4f) * (1f - colors.fog * 0.4f)
            drawPath(geo.forestFar, lerp(colors.waterDeep, colors.far(GreenMid, 0.42f), 0.6f), alpha = a * 0.6f)
            drawPath(geo.forestMid, lerp(colors.waterDeep, colors.far(GreenNear, 0.18f), 0.75f), alpha = a)
        }
        // Thin bright ripple lines break up the mirrored forest.
        val line = colors.skyHorizon.copy(alpha = 0.12f + 0.08f * colors.daylight)
        for (i in 0 until 22) {
            val f = (i / 22f)
            val y = geo.hy + (geo.h * 0.16f) * f * f + 2f
            val off = sin(t * (0.6f + f) + i * 1.9f) * geo.w * 0.05f
            val len = geo.w * (0.08f + 0.12f * ((i * 37) % 10) / 10f)
            val x = ((i * 0.137f) % 1f) * geo.w + off
            drawLine(line, Offset(x, y), Offset(x + len, y), strokeWidth = 1f + f * 1.5f)
        }
    }
}

private fun DrawScope.drawFallbackWater(geo: SceneGeometry, colors: SceneColors, t: Float, glowX: Float) {
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

private fun DrawScope.drawMist(geo: SceneGeometry, colors: SceneColors, c: Conditions) {
    val dawnMist = if (c.phase == lt.marcomultiservices.kimba.core.DayPhase.DAWN && colors.wind < 3f) 0.4f else 0f
    val m = maxOf(colors.fog * 0.8f, dawnMist)
    if (m <= 0f) return
    val col = lerp(Color(0xFF3A4048), Color(0xFFE6E9EC), colors.daylight)
    drawRect(
        Brush.verticalGradient(listOf(col.copy(alpha = m * 0.8f), col.copy(alpha = 0f)), geo.hy - geo.h * 0.01f, geo.hy + geo.h * 0.14f),
        topLeft = Offset(0f, geo.hy - geo.h * 0.01f), size = Size(geo.w, geo.h * 0.15f),
    )
}

private fun DrawScope.drawLilies(geo: SceneGeometry, colors: SceneColors, t: Float, lilies: List<SceneGeometry.Lily>) {
    for (l in lilies) {
        val bob = sin(t * 0.8f + l.rot) * 1.5f
        val rx = l.r
        val ry = l.r * 0.34f
        val rect = Rect(l.x - rx, l.y - ry + bob, l.x + rx, l.y + ry + bob)
        val start = (l.rot * 57.3f) % 360f
        val pad = Path().apply {
            moveTo(l.x, l.y + bob)
            arcTo(rect, start + 14f, 332f, false)
            close()
        }
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(rect.left + 2, rect.top + 3), rect.size)
        drawPath(pad, Brush.linearGradient(listOf(colors.lit(Color(0xFF4F8A3A)), colors.lit(Color(0xFF2D5A26))), Offset(rect.left, rect.top), Offset(rect.right, rect.bottom)))
        drawPath(pad, colors.lit(Color(0xFF7DB05A)).copy(alpha = 0.5f), style = Stroke(1.5f))
        if (l.flower) {
            val c = Offset(l.x + rx * 0.2f, l.y - ry * 0.4f + bob)
            for (k in 0 until 7) {
                val a = k / 7f * 360f
                rotate(a, c) {
                    drawOval(colors.lit(Color(0xFFFFF4F6)), Offset(c.x - rx * 0.06f, c.y - rx * 0.24f), Size(rx * 0.12f, rx * 0.24f))
                }
            }
            drawCircle(colors.lit(Color(0xFFF7D24A)), rx * 0.06f, c)
        }
    }
}

/** Float body for each style, sinking with [sink]. The glow float shines in the dark. */
private fun DrawScope.drawFloatBody(pos: Offset, u: Float, sink: Float, style: FloatStyle = FloatStyle.CLASSIC, daylight: Float = 1f, alpha: Float = 1f) {
    val y = pos.y + sink
    val slim = if (style == FloatStyle.FEATHER) 0.55f else 1f
    val body = Path().apply {
        moveTo(pos.x, y - u * 1.4f)
        cubicTo(pos.x + u * 0.75f * slim, y - u * 0.9f, pos.x + u * 0.55f * slim, y + u * 0.9f, pos.x, y + u * (if (style == FloatStyle.FEATHER) 2.4f else 1.6f))
        cubicTo(pos.x - u * 0.55f * slim, y + u * 0.9f, pos.x - u * 0.75f * slim, y - u * 0.9f, pos.x, y - u * 1.4f)
        close()
    }
    val (base, base2, tip, tip2) = when (style) {
        FloatStyle.CLASSIC -> listOf(Color(0xFFF7F3EA), Color(0xFFCFC8BA), Color(0xFFF2533F), Color(0xFFB72D24))
        FloatStyle.FEATHER -> listOf(Color(0xFFE8DCC0), Color(0xFFA89470), Color(0xFFF0A030), Color(0xFFB86A18))
        FloatStyle.GLOW -> listOf(Color(0xFF2A3A30), Color(0xFF141C18), Color(0xFFB8FF6A), Color(0xFF6ACC2A))
        FloatStyle.GOLD -> listOf(Color(0xFFFFE9A0), Color(0xFFC89030), Color(0xFFFFF4D0), Color(0xFFE0A830))
    }
    if (style == FloatStyle.GLOW) {
        val g = 0.35f + 0.65f * (1f - daylight)
        drawCircle(Brush.radialGradient(listOf(Color(0xAAB8FF6A), Color(0x00B8FF6A)), Offset(pos.x, y - u * 2.2f), u * 3.5f), u * 3.5f, Offset(pos.x, y - u * 2.2f), alpha = g * alpha)
    }
    drawPath(body, Brush.horizontalGradient(listOf(base, base2), pos.x - u * 0.6f, pos.x + u * 0.6f), alpha = alpha)
    clipRect(bottom = y - u * 0.35f) {
        drawPath(body, Brush.horizontalGradient(listOf(tip, tip2), pos.x - u * 0.6f, pos.x + u * 0.6f), alpha = alpha)
    }
    drawLine(tip, Offset(pos.x, y - u * 1.3f), Offset(pos.x, y - u * 3.2f), strokeWidth = u * 0.22f, cap = StrokeCap.Round, alpha = alpha)
    if (style == FloatStyle.GOLD) drawCircle(Color.White.copy(alpha = 0.9f * alpha), u * 0.25f, Offset(pos.x, y - u * 3.2f))
    drawCircle(Color.White.copy(alpha = 0.6f * alpha), u * 0.16f, Offset(pos.x - u * 0.22f, y - u * 0.8f))
}

/** A float for the shop: [height] is the whole float including its antenna. */
fun DrawScope.drawFloat(center: Offset, height: Float, style: FloatStyle, glow: Boolean = false, alpha: Float = 1f) {
    drawFloatBody(Offset(center.x, center.y), height / 5f, 0f, style, if (glow) 0f else 1f, alpha)
}

private fun DrawScope.drawFloatOnWater(geo: SceneGeometry, pos: Offset, dip: Float, t: Float, bite: Boolean, style: FloatStyle, daylight: Float) {
    val u = geo.h * 0.016f * geo.scaleAt(pos.y)
    val bob = sin(t * 2.2f) * u * 0.12f
    // Spreading rings, stronger when something touches the bait.
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

private fun DrawScope.drawFightSplash(geo: SceneGeometry, pos: Offset, t: Float, running: Boolean) {
    val s = geo.scaleAt(pos.y)
    // The fish as a dark shape just under the surface.
    val len = geo.w * 0.07f * s
    val wig = sin(t * (if (running) 18f else 9f)) * 0.25f
    rotate(wig * 20f, pos) {
        drawOval(Color(0xFF0B1410).copy(alpha = 0.35f), Offset(pos.x - len / 2, pos.y + len * 0.05f), Size(len, len * 0.26f))
    }
    val n = if (running) 10 else 4
    for (i in 0 until n) {
        val ph = (t * 2.2f + i * 0.37f) % 1f
        val a = i * 2.39f
        val d = len * (0.2f + ph * 0.6f)
        val c = Offset(pos.x + cos(a) * d, pos.y - sin(ph * PI.toFloat()) * len * 0.35f - abs(sin(a)) * d * 0.15f)
        drawCircle(Color.White.copy(alpha = (1 - ph) * 0.7f), (1.5f + 2.5f * s) * (1 - ph * 0.5f), c)
    }
    for (k in 0 until 2) {
        val ph = (t * 1.4f + k / 2f) % 1f
        val r = len * (0.3f + ph)
        drawOval(Color.White.copy(alpha = (1 - ph) * 0.4f), Offset(pos.x - r, pos.y - r * 0.28f), Size(r * 2, r * 0.56f), style = Stroke(2f))
    }
}

private fun DrawScope.drawPier(geo: SceneGeometry, colors: SceneColors, sea: Boolean = false) {
    // Posts going into the water at the far end, with reflections.
    val postW = geo.w * 0.022f
    for (x in listOf(geo.pierFar.first + postW, geo.pierFar.second - postW)) {
        val top = geo.pierFarY - geo.h * 0.012f
        val bottom = geo.pierFarY + geo.h * 0.05f
        drawRect(colors.lit(Color(0xFF3B2A1C)), Offset(x - postW / 2, top), Size(postW, bottom - top))
        drawRect(Brush.verticalGradient(listOf(colors.lit(Color(0xFF3B2A1C)).copy(alpha = 0.45f), Color.Transparent), bottom, bottom + geo.h * 0.05f), Offset(x - postW / 2, bottom), Size(postW, geo.h * 0.05f))
    }
    // Front face under the far edge.
    drawRect(colors.lit(Color(0xFF35261A)), Offset(geo.pierFar.first, geo.pierFarY), Size(geo.pierFar.second - geo.pierFar.first, geo.h * 0.014f))
    drawPath(
        geo.pier,
        Brush.verticalGradient(listOf(colors.lit(Color(0xFF6E5136)), colors.lit(Color(0xFF9A7650))), geo.pierFarY, geo.pierNearY),
    )
    val n = 13
    for (i in 1 until n) {
        val (a, b) = geo.plank(i, n)
        drawLine(colors.lit(Color(0xFF3A2A1B)).copy(alpha = 0.8f), a, b, strokeWidth = 1.5f + 2.5f * (i / n.toFloat()))
        drawLine(colors.lit(Color(0xFFC09A6A)).copy(alpha = 0.25f), Offset(a.x, a.y + 2f), Offset(b.x, b.y + 2f), strokeWidth = 1f)
    }
    // A worn grain line along the middle.
    val (fa, fb) = geo.plank(0, n)
    val (na, nb) = geo.plank(n, n)
    drawLine(colors.lit(Color(0xFF4A3524)).copy(alpha = 0.35f), Offset((fa.x + fb.x) / 2, fa.y), Offset((na.x + nb.x) / 2, na.y), strokeWidth = 2f)
    if (sea) {
        // Railing along the sea pier.
        val railCol = colors.lit(Color(0xFFD8D4CC))
        val top = geo.h * 0.045f
        for (i in 0..5) {
            val (a, _) = geo.plank(i * 13 / 5, 13)
            drawLine(railCol, a, Offset(a.x, a.y - top * (0.4f + 0.6f * i / 5f)), strokeWidth = 2f + i)
        }
        val (fa, _) = geo.plank(0, 13)
        val (na, _) = geo.plank(13, 13)
        drawLine(railCol, Offset(fa.x, fa.y - top * 0.4f), Offset(na.x, na.y - top), strokeWidth = 4f, cap = StrokeCap.Round)
    }
    // Bait tin and landing net handle as small props.
    val tin = Offset(geo.w * 0.14f, geo.h * 0.9f)
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(tin.x - geo.w * 0.035f, tin.y + geo.h * 0.008f), Size(geo.w * 0.075f, geo.h * 0.016f))
    drawRect(Brush.horizontalGradient(listOf(colors.lit(Color(0xFF5B6E7A)), colors.lit(Color(0xFFB8C6CE)), colors.lit(Color(0xFF4E5E68))), tin.x - geo.w * 0.03f, tin.x + geo.w * 0.03f), Offset(tin.x - geo.w * 0.03f, tin.y - geo.h * 0.02f), Size(geo.w * 0.06f, geo.h * 0.03f))
    drawOval(colors.lit(Color(0xFF6A4A32)), Offset(tin.x - geo.w * 0.03f, tin.y - geo.h * 0.026f), Size(geo.w * 0.06f, geo.h * 0.012f))
}

private fun DrawScope.drawReeds(geo: SceneGeometry, colors: SceneColors, t: Float, back: Boolean, dune: Boolean = false) {
    val sway = 0.012f + colors.wind * 0.004f
    for (r in geo.reeds) {
        if (r.back != back) continue
        val base = Offset(r.x, geo.h * 1.02f)
        val bend = (r.lean + sin(t * 1.3f + r.phase) * sway) * r.height
        val top = Offset(r.x + bend, base.y - r.height)
        val ctrl = Offset(r.x + bend * 0.2f, base.y - r.height * 0.55f)
        if (dune && r.head) continue
        val col = if (dune) colors.lit(if (back) Color(0xFF7A7A44) else Color(0xFFA8A060), extraDark = if (back) 0.15f else 0f)
            else colors.lit(if (back) Color(0xFF2E4A24) else Color(0xFF3F6230), extraDark = if (back) 0.2f else 0f)
        val path = Path().apply { moveTo(base.x, base.y); quadraticTo(ctrl.x, ctrl.y, top.x, top.y) }
        drawPath(path, col, style = Stroke(r.width, cap = StrokeCap.Round))
        drawPath(path, colors.lit(Color(0xFF8DB06A)).copy(alpha = 0.25f), style = Stroke(r.width * 0.3f, cap = StrokeCap.Round))
        if (r.head) {
            val hb = Offset(base.x + (top.x - base.x) * 0.8f + bend * 0.05f, base.y - r.height * 0.8f)
            val ht = Offset(base.x + (top.x - base.x) * 0.92f, base.y - r.height * 0.92f)
            drawLine(colors.lit(Color(0xFF5E3B22)), hb, ht, strokeWidth = r.width * 2.6f, cap = StrokeCap.Round)
            drawLine(colors.lit(Color(0xFF8A5C38)).copy(alpha = 0.5f), Offset(hb.x - r.width * 0.5f, hb.y), Offset(ht.x - r.width * 0.5f, ht.y), strokeWidth = r.width * 0.8f, cap = StrokeCap.Round)
        }
    }
}

/** Open sea: a clean horizon, a far lighthouse and a ship, distant dune line on the left. */
private fun DrawScope.drawSeaHorizon(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val far = colors.far(Color(0xFF4A5A50), 0.75f)
    // Low dune coast fading into the distance on the left.
    val coast = Path().apply {
        moveTo(0f, geo.hy + 1)
        lineTo(0f, geo.hy - geo.h * 0.022f)
        cubicTo(geo.w * 0.08f, geo.hy - geo.h * 0.03f, geo.w * 0.16f, geo.hy - geo.h * 0.012f, geo.w * 0.3f, geo.hy - geo.h * 0.004f)
        lineTo(geo.w * 0.42f, geo.hy + 1)
        close()
    }
    drawPath(coast, far)
    // Lighthouse on the coast.
    val lx = geo.w * 0.07f
    val lb = geo.hy - geo.h * 0.026f
    val lh = geo.h * 0.05f
    val tower = Path().apply { moveTo(lx - lh * 0.09f, lb); lineTo(lx + lh * 0.09f, lb); lineTo(lx + lh * 0.055f, lb - lh); lineTo(lx - lh * 0.055f, lb - lh); close() }
    drawPath(tower, colors.far(Color(0xFFE8E4DC), 0.5f))
    drawRect(colors.far(Color(0xFFB83A2A), 0.5f), Offset(lx - lh * 0.075f, lb - lh * 0.55f), Size(lh * 0.15f, lh * 0.15f))
    drawRect(colors.far(Color(0xFF2A2A2A), 0.5f), Offset(lx - lh * 0.07f, lb - lh * 1.12f), Size(lh * 0.14f, lh * 0.12f))
    if (colors.daylight < 0.5f) {
        // The light sweeps round at night.
        val beam = (sin(t * 1.2f) * 0.5f + 0.5f)
        val c = Offset(lx, lb - lh * 1.06f)
        drawCircle(Brush.radialGradient(listOf(Color(0xCCFFF2C0), Color(0x00FFF2C0)), c, geo.w * 0.08f), geo.w * 0.08f, c, alpha = (1f - colors.daylight * 2f) * (0.4f + 0.6f * beam))
    }
    // A cargo ship slowly crossing the horizon.
    val sx = ((0.15f + t * 0.002f) % 1.3f) * geo.w
    val sy = geo.hy - 1f
    val ship = Path().apply {
        moveTo(sx, sy); lineTo(sx + geo.w * 0.09f, sy); lineTo(sx + geo.w * 0.1f, sy - geo.h * 0.007f); lineTo(sx - geo.w * 0.005f, sy - geo.h * 0.007f); close()
        addRect(Rect(sx + geo.w * 0.065f, sy - geo.h * 0.014f, sx + geo.w * 0.085f, sy - geo.h * 0.007f))
    }
    drawPath(ship, colors.far(Color(0xFF2A3038), 0.65f))
    if (colors.fog > 0f) {
        drawRect(
            Brush.verticalGradient(listOf(colors.skyHorizon.copy(alpha = 0f), colors.skyHorizon.copy(alpha = colors.fog * 0.85f)), geo.hy - geo.h * 0.06f, geo.hy),
            topLeft = Offset(0f, geo.hy - geo.h * 0.06f), size = Size(geo.w, geo.h * 0.06f),
        )
    }
}

/** Weeping willows framing the pond on both sides. */
private fun DrawScope.drawWillows(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val sway = sin(t * 0.6f) * geo.w * 0.004f * (1f + colors.wind * 0.15f)
    for (w in geo.willows) {
        val trunk = colors.far(Color(0xFF2E2418), w.depth)
        drawLine(trunk, Offset(w.x, geo.hy + geo.h * 0.004f), Offset(w.x + w.lean, w.top + w.size * 0.25f), strokeWidth = w.size * 0.07f, cap = StrokeCap.Round)
        val crown = colors.far(if (w.depth > 0.3f) GreenFar else Color(0xFF3A5A2A), w.depth)
        drawOval(crown, Offset(w.x + w.lean - w.size * 0.55f, w.top), Size(w.size * 1.1f, w.size * 0.55f))
        // Hanging strands down to the water.
        for (k in 0 until w.strands.size) {
            val (dx, len) = w.strands[k]
            val sx = w.x + w.lean + dx * w.size * 0.5f
            val sy = w.top + w.size * 0.3f
            val end = Offset(sx + sway * (1f + len), (sy + len * (geo.hy - sy)).coerceAtMost(geo.hy + geo.h * 0.003f))
            val path = Path().apply { moveTo(sx, sy); quadraticTo(sx + dx * w.size * 0.12f, (sy + end.y) / 2, end.x, end.y) }
            drawPath(path, crown, style = Stroke(w.size * 0.05f, cap = StrokeCap.Round))
            drawPath(path, colors.lit(Color(0xFF9AC070)).copy(alpha = 0.18f), style = Stroke(w.size * 0.012f, cap = StrokeCap.Round))
        }
    }
}

/** River: stones in the shallows and leaves drifting with the current. */
private fun DrawScope.drawRiverBits(geo: SceneGeometry, colors: SceneColors, t: Float) {
    for (st in geo.stones) {
        val rect = Rect(st.x - st.r, st.y - st.r * 0.45f, st.x + st.r, st.y + st.r * 0.45f)
        // Water piling up against the stone on the upstream side.
        val foam = (sin(t * 4f + st.x) * 0.5f + 0.5f) * 0.3f + 0.3f
        drawOval(Color.White.copy(alpha = foam * colors.daylight.coerceAtLeast(0.3f)), Offset(rect.left - st.r * 0.25f, rect.center.y), Size(st.r * 2.3f, st.r * 0.4f), style = Stroke(2f))
        drawOval(Brush.verticalGradient(listOf(colors.lit(Color(0xFF8A8A80)), colors.lit(Color(0xFF3A3A34))), rect.top, rect.bottom), rect.topLeft, rect.size)
        drawOval(colors.lit(Color(0xFF4E6A3A)).copy(alpha = 0.5f), Offset(rect.left + st.r * 0.3f, rect.top), Size(st.r * 0.9f, st.r * 0.25f))
    }
    for (lf in geo.leaves) {
        val x = ((lf.x + t * lf.speed) % 1.2f - 0.1f) * geo.w
        val y = lf.y + sin(t * 1.3f + lf.phase) * geo.h * 0.002f
        val s = geo.scaleAt(y) * geo.w * 0.018f
        rotate(lf.phase * 57f + t * 20f * lf.speed, Offset(x, y)) {
            drawOval(colors.lit(if (lf.phase > 3f) Color(0xFFC88A2A) else Color(0xFF9A5A22)), Offset(x - s, y - s * 0.35f), Size(s * 2, s * 0.7f))
        }
    }
}

/** Gulls gliding over the sea. */
private fun DrawScope.drawGulls(geo: SceneGeometry, colors: SceneColors, t: Float) {
    if (colors.rain > 0.6f) return
    val col = lerp(Color(0xFF20242A), Color(0xFFF4F4F0), colors.daylight * 0.3f)
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

/** A tapered carbon rod from the bottom right corner, bending towards the line under tension. */
private fun DrawScope.drawRod(geo: SceneGeometry, colors: SceneColors, tip: Offset) {
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
        val col = if (u < 0.22f) colors.lit(Color(0xFF8E6A47)) else colors.lit(Color(0xFF1E2228))
        drawLine(col, prev, p, strokeWidth = width, cap = StrokeCap.Round)
        if (u >= 0.22f) drawLine(colors.lit(Color(0xFF6D7A88)).copy(alpha = 0.6f), Offset(prev.x - width * 0.2f, prev.y - width * 0.2f), Offset(p.x - width * 0.2f, p.y - width * 0.2f), strokeWidth = width * 0.25f)
        if (i % 6 == 0 && u > 0.3f) drawCircle(colors.lit(Color(0xFFB9C2CA)), width * 0.8f, p, style = Stroke(1.5f))
        prev = p
    }
    // Reel.
    val reel = Offset(geo.w * 0.955f, geo.h * 0.9f)
    drawCircle(colors.lit(Color(0xFF2A2F36)), geo.w * 0.03f, reel)
    drawCircle(colors.lit(Color(0xFF9AA5B0)), geo.w * 0.03f, reel, style = Stroke(2f))
    drawCircle(colors.lit(Color(0xFFD2B04A)), geo.w * 0.008f, reel)
}

private fun DrawScope.drawLine(geo: SceneGeometry, tip: Offset, end: Offset, stage: Stage, tension: Float) {
    val sag = when (stage) {
        Stage.FIGHT -> geo.h * 0.03f * (1f - tension).coerceIn(0f, 1f)
        Stage.WAITING, Stage.BITE -> geo.h * 0.04f
        else -> geo.h * 0.005f
    }
    val mid = Offset((tip.x + end.x) / 2, (tip.y + end.y) / 2 + sag)
    val path = Path().apply { moveTo(tip.x, tip.y); quadraticTo(mid.x, mid.y, end.x, end.y) }
    val col = if (stage == Stage.FIGHT && tension > 0.85f) Color(0xFFFF6A50) else Color(0xCCF2F0EA)
    drawPath(path, col, style = Stroke(if (stage == Stage.FIGHT) 2.2f else 1.4f))
}

private fun DrawScope.drawWeather(geo: SceneGeometry, colors: SceneColors, c: Conditions, t: Float) {
    if (colors.rain > 0.05f && colors.snow < 1f) {
        val n = (geo.drops.size * colors.rain).toInt()
        val slant = 0.05f + colors.wind * 0.012f
        val col = Color.White.copy(alpha = 0.18f + 0.22f * colors.daylight)
        for (i in 0 until n) {
            val d = geo.drops[i]
            val y = ((d.y + t * d.speed) % 1f) * geo.h * 1.1f - geo.h * 0.05f
            val x = (d.x + y / geo.h * slant) * geo.w
            drawLine(col, Offset(x, y), Offset(x - slant * d.len * 2, y - d.len), strokeWidth = 1.3f)
        }
    }
    if (colors.snow > 0f) {
        val n = (geo.flakes.size * colors.snow).toInt()
        for (i in 0 until n) {
            val f = geo.flakes[i]
            val y = ((f.y + t * f.speed) % 1f) * geo.h
            val x = ((f.x + sin(t * 0.7f + i) * 0.015f + y / geo.h * colors.wind * 0.01f) % 1f) * geo.w
            drawCircle(Color.White.copy(alpha = 0.85f), f.len * (0.6f + y / geo.h * 0.8f), Offset(x, y))
        }
    }
    // Fireflies on summer nights.
    if (c.month in 6..8 && colors.daylight < 0.2f && colors.rain < 0.05f) {
        for (f in geo.flies) {
            val pulse = (sin(t * 2.3f + f.phase * 3) * 0.5f + 0.5f).let { it * it }
            val p = Offset(f.x + sin(t * 0.4f + f.phase) * f.ax, f.y + cos(t * 0.53f + f.phase) * f.ay)
            drawCircle(Brush.radialGradient(listOf(Color(0xCCE8FF8A), Color(0x00E8FF8A)), p, 14f), 14f, p, alpha = pulse)
            drawCircle(Color(0xFFF6FFD0), 2f, p, alpha = pulse)
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
        if (flash > 0f) drawRect(Color(0xFFDDE6FF).copy(alpha = flash * 0.35f), size = size)
    }
}

private fun DrawScope.drawVignette(geo: SceneGeometry) {
    drawRect(
        Brush.radialGradient(listOf(Color.Transparent, Color(0x66000000)), Offset(geo.w / 2, geo.h * 0.48f), geo.h * 0.75f),
        size = size,
    )
}
