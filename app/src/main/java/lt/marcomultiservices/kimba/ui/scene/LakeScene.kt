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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.FloatStyle
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.game.LakeState
import lt.marcomultiservices.kimba.game.Stage
import lt.marcomultiservices.kimba.ui.scene.layers.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * The living lake: sky, forest, water, jetty, reeds, rod and float, all reacting to the real
 * conditions. This function only works out where things are (float, rod tip, ripples) and
 * paints the layers in order, back to front. Each layer lives in its own file in `layers/`,
 * its colours in [Palette], and most parts can be replaced by pictures through [ArtPack].
 * See docs/GRAFIKA.md.
 */
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

            // Layers, back to front.
            val sea = waterKind == Water.SEA
            drawSky(geo, colors, conditions, t, sun, moon)
            drawShores(geo, colors, waterKind, t)
            if (waterKind == Water.POND) drawWillows(geo, colors, t)
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
            drawReflections(geo, colors, t, waterKind)
            drawMist(geo, colors, conditions)
            when (waterKind) {
                Water.LAKE -> drawLilies(geo, colors, t, geo.lilies)
                Water.POND -> drawLilies(geo, colors, t, geo.pondLilies)
                Water.RIVER -> drawRiverBits(geo, colors, t)
                Water.SEA -> { drawBuoy(geo, colors, t); drawGulls(geo, colors, t) }
            }
            if (s.stage == Stage.CHARGING) {
                // Where the float will land if released now.
                val power = GameViewModel.castPower(android.os.SystemClock.uptimeMillis() - s.chargeStart)
                drawAim(geo, geo.floatTarget(power.toDouble(), s.castId + 1), t)
            }
            if (onWater && dip > 0.05f) drawFishShadow(geo, floatPos, dip, t, s.bite?.let { (it.cm / 60.0).toFloat() } ?: 0.6f)
            if (onWater) drawFloatOnWater(geo, floatPos, dip, t, s.stage == Stage.BITE, floatStyle, colors.daylight)
            if (s.stage == Stage.FIGHT) {
                drawFightSplash(geo, floatPos, t, s.running)
                val b = s.bite
                if (s.running && b != null) drawFishJump(geo, floatPos, t, b.species.id, b.golden != null, (b.cm / 60.0).toFloat())
            }
            drawPier(geo, colors, sea, t)
            // No reeds out on a sea pier.
            if (!sea) drawReeds(geo, colors, t, back = true)
            drawRod(geo, colors, tip)
            drawLine(geo, tip, floatPos, s.stage, tension)
            if (hanging || s.stage == Stage.FLYING) drawFloatBody(floatPos, geo.h * 0.016f * geo.scaleAt(geo.h), 0f, floatStyle, colors.daylight)
            if (!sea) drawReeds(geo, colors, t, back = false)
            drawWeather(geo, colors, conditions, t)
            drawVignette(geo)
        }
    }
}
