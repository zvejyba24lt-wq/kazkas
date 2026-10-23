package lt.marcomultiservices.kimba.ui.scene

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.FloatStyle
import lt.marcomultiservices.kimba.core.Stand
import lt.marcomultiservices.kimba.core.Tackle
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
    /** The rod in hand while no cast is out (a cast keeps its own in [LakeState.tackle]). */
    tackle: Tackle = Tackle.FLOAT,
    hasAlarm: Boolean = false,
    /** Ice on this water in cm: a rim along the shores while it forms, fishing through a hole once it holds. */
    iceCm: Double = 0.0,
    /** Where the player stands: the jetty, the reed bank or the boat (on the ice it is always the hole). */
    stand: Stand = Stand.PIER,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val geo = remember(w, h) { SceneGeometry(w, h) }
        val colors = remember(conditions) { SceneColors(conditions) }
        // How eagerly fish feed right now, from the real conditions: the lake shows it with rises.
        val activity = remember(conditions, waterKind) { lt.marcomultiservices.kimba.core.Fish.biteIndex(conditions, lt.marcomultiservices.kimba.core.Spot(waterKind)).toFloat() }
        val water = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { WaterShader() }.getOrNull() else null }

        val time = rememberSceneTime()
        // Layers that stand still are drawn once into a picture and reused every frame.
        val shoreCache = remember { LayerCache() }
        val pierCache = remember { LayerCache() }

        val castId by remember { derivedStateOf { lake.value.castId } }
        val dipTarget by remember { derivedStateOf { lake.value.dip } }
        val dip by animateFloatAsState(dipTarget, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow), label = "dip")
        val flight = remember { Animatable(1f) }
        val splash = remember { Animatable(1f) }
        LaunchedEffect(castId) {
            if (castId == 0) return@LaunchedEffect
            flight.snapTo(0f)
            flight.animateTo(1f, tween(GameViewModel.FLIGHT_MS.toInt(), easing = LinearEasing))
            // The float hits the water: a splash that settles.
            splash.snapTo(0f)
            splash.animateTo(1f, tween(900, easing = LinearEasing))
        }
        val stageNow by remember { derivedStateOf { lake.value.stage } }
        // Lifting a landed fish out of the water.
        val lift = remember { Animatable(0f) }
        LaunchedEffect(stageNow) {
            if (stageNow == Stage.LANDING) {
                lift.snapTo(0f)
                lift.animateTo(1f, tween(GameViewModel.LANDING_MS.toInt() - 200, easing = FastOutSlowInEasing))
            } else {
                lift.snapTo(0f)
            }
        }
        // On a bite the fish drags the float sideways as it goes under.
        val pull = remember { Animatable(0f) }
        LaunchedEffect(stageNow) {
            if (stageNow == Stage.BITE) { pull.snapTo(0f); pull.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) } else pull.snapTo(0f)
        }
        val tensionTarget by remember { derivedStateOf { lake.value.tension } }
        val tension by animateFloatAsState(tensionTarget, tween(90), label = "tension")
        val sideTarget by remember { derivedStateOf { lake.value.fishSide } }
        val fishSide by animateFloatAsState(sideTarget, tween(140), label = "side")
        val aimTarget by remember { derivedStateOf { lake.value.rodAim } }
        val rodAim by animateFloatAsState(aimTarget, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), label = "aim")
        // The view closes in on the water while a fish is played and lifted out.
        val zoomTarget by remember { derivedStateOf { if (lake.value.stage == Stage.FIGHT || lake.value.stage == Stage.LANDING) 1.22f else 1f } }
        val zoom by animateFloatAsState(zoomTarget, tween(700, easing = FastOutSlowInEasing), label = "zoom")
        // At the bank the angler looks down: the view tilts to the landing net and the fish in it.
        val frozen = iceCm >= lt.marcomultiservices.kimba.core.Ice.SAFE_CM
        val lookDownTarget by remember(frozen) { derivedStateOf { val st = lake.value; if (frozen) 0f else if ((st.stage == Stage.FIGHT && st.fightDistance < 0.22f) || st.stage == Stage.LANDING) 1f else 0f } }
        val lookDown by animateFloatAsState(lookDownTarget, tween(900, easing = FastOutSlowInEasing), label = "look")
        // The camera follows the cast out and settles close on the float (closer the farther it
        // went), then keeps the hooked fish in view; between casts it pulls back to the whole lake.
        val followTarget by remember(frozen) {
            derivedStateOf {
                val st = lake.value
                when (st.stage) {
                    Stage.FLYING, Stage.WAITING, Stage.BITE -> if (frozen) 1.22f else 1.08f + 0.34f * st.castDist.toFloat()
                    else -> 1f
                }
            }
        }
        val follow by animateFloatAsState(followTarget, tween(1800, easing = FastOutSlowInEasing), label = "follow")
        val focusTarget by remember { derivedStateOf { when (lake.value.stage) { Stage.FLYING, Stage.WAITING, Stage.BITE -> 1f; Stage.FIGHT -> 0.5f; else -> 0f } } }
        val focus by animateFloatAsState(focusTarget, tween(1800, easing = FastOutSlowInEasing), label = "focus")

        Canvas(Modifier.fillMaxSize()) {
            val t = time.floatValue
            val s = lake.value
            val sun = sunPosition(geo, conditions)
            val moon = Offset(geo.w * 0.74f, geo.hy * 0.26f)
            val glowX = if (conditions.sunElevation > -3) sun.x else moon.x

            // Float and line state.
            val kind = if (s.stage == Stage.IDLE || s.stage == Stage.CHARGING) tackle else s.tackle
            // On the ice the bait always goes down the same hole.
            // From the boat even a short cast lands clear of the bow.
            val boat = stand == Stand.BOAT && !frozen
            val target = when {
                frozen -> geo.floatTarget(GameViewModel.ICE_DIST, 0)
                boat -> geo.floatTarget(0.3 + 0.7 * s.castDist, s.castId)
                else -> geo.floatTarget(s.castDist, s.castId)
            }
            // A spinning lure comes back from where it landed towards the bank below the angler.
            val spinEnd = Offset(geo.w * 0.6f, geo.h * 0.9f)
            val spinFrac = when {
                kind != Tackle.SPIN -> 0f
                s.stage == Stage.WAITING -> s.spin
                s.stage == Stage.BITE || s.stage == Stage.FIGHT -> s.takenAt
                else -> 0f
            }
            val hookAt = Offset(target.x + (spinEnd.x - target.x) * spinFrac, target.y + (spinEnd.y - target.y) * spinFrac)
            // The bottom rod lies on its rest with the tip up against the far shore.
            val resting = kind == Tackle.FEEDER && (s.stage == Stage.WAITING || s.stage == Stage.BITE)
            val bend = when (s.stage) {
                Stage.FIGHT -> tension.coerceIn(0f, 1.2f)
                Stage.LANDING -> 0.7f - 0.3f * lift.value
                else -> 0.08f + dip * (if (kind == Tackle.SPIN) 0.4f else 0.15f)
            }
            // The rod swings back while charging a cast and whips forward on release.
            val swing = when (s.stage) {
                Stage.CHARGING -> -GameViewModel.castPower(android.os.SystemClock.uptimeMillis() - s.chargeStart) * 0.9f
                Stage.FLYING -> {
                    val f = (flight.value / 0.28f).coerceIn(0f, 1f)
                    -0.9f * s.castDist.toFloat() * (1f - f) + sin(f * PI.toFloat()) * 0.7f
                }
                else -> 0f
            }
            val nibbleAge = android.os.SystemClock.uptimeMillis() - s.nibbleAt
            val tremble = if (s.stage == Stage.WAITING && s.nibbleAt > 0 && nibbleAge < 900) 1f - nibbleAge / 900f else 0f
            val tip = if (resting) {
                // The quiver tip nods towards the bait on a nibble and pulls right round on a bite.
                val d = dip + tremble * sin(t * 48f) * 0.08f
                Offset(geo.w * (0.62f - 0.04f * d), geo.h * (0.40f + 0.07f * d))
            } else Offset(
                geo.rodTip.x - geo.w * 0.05f * bend - geo.w * 0.06f * swing + (if (kind == Tackle.SPIN) tremble * sin(t * 50f) * geo.w * 0.004f else 0f) +
                    (if (s.stage == Stage.FIGHT) rodAim * geo.w * 0.16f else 0f),
                geo.rodTip.y + geo.h * 0.11f * bend + geo.h * 0.07f * swing,
            )
            val hanging = s.stage == Stage.IDLE || s.stage == Stage.CHARGING
            val floatPos = when {
                hanging -> Offset(tip.x + sin(t * 1.8f) * geo.w * 0.006f, tip.y + geo.h * 0.075f)
                s.stage == Stage.FLYING -> {
                    // Thrown out fast, slowing down, on a high arc that drops onto the water.
                    val f = flight.value
                    val e = 1f - (1f - f) * (1f - f)
                    val from = Offset(tip.x, tip.y + geo.h * 0.075f)
                    val arc = geo.h * (0.12f + 0.18f * s.castDist.toFloat())
                    Offset(from.x + (target.x - from.x) * e, from.y + (target.y - from.y) * f - sin(PI.toFloat() * f) * arc)
                }
                s.stage == Stage.FIGHT && frozen -> {
                    // Under the ice only the line shows, leaning in the hole the way the fish swims.
                    Offset(target.x + fishSide * geo.w * 0.04f * geo.scaleAt(target.y), target.y)
                }
                s.stage == Stage.LANDING && frozen -> Offset(target.x, target.y - geo.h * 0.2f * lift.value)
                s.stage == Stage.FIGHT -> {
                    // From where it was hooked in to the bank below the angler; a fish that wins line
                    // swims on out past that point towards the far shore.
                    val bank = Offset(geo.w * 0.57f, geo.h * 0.885f)
                    val start = s.fightStart.coerceAtLeast(0.05f)
                    val along = if (s.fightDistance <= start) {
                        val k = s.fightDistance / start
                        Offset(bank.x + (hookAt.x - bank.x) * k, bank.y + (hookAt.y - bank.y) * k)
                    } else {
                        val far = Offset(hookAt.x, geo.hy + geo.h * 0.04f)
                        val k = ((s.fightDistance - start) / (1f - start).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                        Offset(hookAt.x + (far.x - hookAt.x) * k, hookAt.y + (far.y - hookAt.y) * k)
                    }
                    // It swims from side to side across the water, less so close in.
                    Offset(along.x + fishSide * geo.w * 0.34f * geo.scaleAt(along.y) * s.fightDistance.coerceIn(0.25f, 1f), along.y)
                }
                s.stage == Stage.LANDING -> {
                    val l = lift.value
                    Offset(geo.w * (0.58f - 0.06f * l), geo.h * (0.9f - 0.36f * l))
                }
                kind == Tackle.SPIN -> hookAt
                s.stage == Stage.BITE && kind == Tackle.FLOAT -> Offset(target.x + geo.w * 0.025f * geo.scaleAt(target.y) * pull.value, target.y)
                else -> target
            }
            // Bubbles rise where the fish feeds.
            val bubbles = when (s.stage) {
                Stage.WAITING -> if (s.nibbles == 0) 0.08f else (0.2f + 0.18f * s.nibbles).coerceAtMost(0.75f)
                Stage.BITE -> 1f
                else -> 0f
            }
            val onWater = s.stage == Stage.WAITING || s.stage == Stage.BITE
            val ripple = when {
                s.stage == Stage.FIGHT -> 0.8f + (if (s.running) 0.6f else 0f)
                onWater -> 0.25f + dip * 0.9f
                s.stage == Stage.FLYING && flight.value > 0.95f -> 1f
                else -> 0f
            }

            // Layers, back to front, through the camera, which closes in during the fight.
            val sea = waterKind == Water.SEA
            // Looking down at the bank the view closes in further around the bottom edge, so
            // nothing past the picture ever shows.
            val base = if (frozen) target else Offset(geo.w * (0.55f + 0.02f * lookDown), geo.h * (0.78f + 0.22f * lookDown))
            // A spinning lure is watched on its way back; anything else where it landed.
            val seen = if (s.stage == Stage.FIGHT || (kind == Tackle.SPIN && s.stage != Stage.FLYING)) floatPos else target
            val pivot = Offset(base.x + (seen.x - base.x) * focus, base.y + (seen.y - base.y) * focus)
            var cam = maxOf(zoom, follow) + 0.3f * lookDown
            // A jolt on the strike, a run and the landing, dying away quickly.
            val shakeAge = (android.os.SystemClock.uptimeMillis() - s.shakeAt).toFloat()
            val jolt = if (s.shakeAt > 0 && shakeAge < 420f) s.shake * (1f - shakeAge / 420f) else 0f
            val jx = jolt * geo.w * 0.011f * sin(shakeAge * 0.11f)
            val jy = jolt * geo.w * 0.008f * kotlin.math.cos(shakeAge * 0.13f)
            // A touch of extra zoom so a jolt never shows past the edge of the picture.
            cam *= 1f + jolt * 0.03f
            drawContext.transform.translate(jx, jy)
            // The boat rides the swell: the whole view sways a little (zoomed in so no corner shows).
            val rock = if (boat) sin(t * 0.9f) * (0.6f + (colors.wind / 10f).coerceAtMost(0.8f)) else 0f
            if (boat) cam *= 1.05f
            drawContext.transform.scale(cam, cam, pivot)
            if (boat) drawContext.transform.rotate(rock * 0.9f, Offset(geo.w * 0.5f, geo.h * 0.6f))
            drawSky(geo, colors, conditions, t, sun, moon)
            if (sea) {
                drawShores(geo, colors, waterKind, t)
            } else {
                shoreCache.draw(this, Triple(geo, colors, waterKind), Rect(0f, 0f, geo.w, geo.hy + geo.h * 0.01f)) {
                    drawShores(geo, colors, waterKind, 0f)
                    if (waterKind == Water.POND) drawWillows(geo, colors, 0f)
                }
            }
            if (frozen) {
                drawIce(geo, colors)
                drawIceAnglers(geo, colors, t)
                drawIceHole(geo, colors, target, t, if (onWater) 0.2f + dip else if (s.stage == Stage.FIGHT) 0.8f else 0f)
            } else if (water != null) {
                water.update(
                    geo.w, geo.h, geo.hy, t, colors, glowX, floatPos.x, floatPos.y, if (onWater || s.stage == Stage.FIGHT) ripple else 0f,
                    flow = if (waterKind == Water.RIVER) 0.35f else 0f,
                    swell = if (sea) 0.6f + (colors.wind / 14f).coerceAtMost(0.6f) else 0f,
                )
                drawRect(ShaderBrush(water.shader), topLeft = Offset(0f, geo.hy), size = Size(geo.w, geo.h - geo.hy))
            } else {
                drawFallbackWater(geo, colors, t, glowX)
            }
            if (!frozen) {
                drawReflections(geo, colors, t, waterKind)
                drawWindStreaks(geo, colors, t)
                drawRises(geo, colors, t, activity)
                if (!sea) drawSwallows(geo, colors, t)
            }
            drawMist(geo, colors, conditions)
            val month = conditions.month
            if (!frozen) when (waterKind) {
                Water.LAKE -> if (liliesIn(month)) drawLilies(geo, colors, t, geo.lilies)
                Water.POND -> if (liliesIn(month)) drawLilies(geo, colors, t, geo.pondLilies)
                Water.RIVER -> drawRiverBits(geo, colors, t)
                Water.SEA -> { drawBuoy(geo, colors, t); drawGulls(geo, colors, t) }
            }
            if (!frozen && iceCm >= lt.marcomultiservices.kimba.core.Ice.RIM_CM) drawIceRim(geo, colors, (iceCm / lt.marcomultiservices.kimba.core.Ice.SAFE_CM).toFloat())
            if (s.stage == Stage.CHARGING) {
                // Where the float will land if released now.
                val power = GameViewModel.castPower(android.os.SystemClock.uptimeMillis() - s.chargeStart)
                drawAim(geo, geo.floatTarget(power.toDouble(), s.castId + 1), t)
            }
            if (onWater && kind == Tackle.FLOAT && dip > 0.05f && !frozen) drawFishShadow(geo, floatPos, dip, t, s.bite?.let { (it.cm / 60.0).toFloat() } ?: 0.6f)
            if (onWater && kind != Tackle.SPIN) drawBubbles(geo, floatPos, t, bubbles)
            when {
                !onWater -> Unit
                kind == Tackle.FLOAT -> drawFloatOnWater(geo, floatPos, dip, t, s.stage == Stage.BITE, floatStyle, colors.daylight, tremble)
                kind == Tackle.FEEDER -> drawLineEntry(geo, floatPos, t, dip)
                s.stage == Stage.BITE -> drawFightSplash(geo, floatPos, t, true)
                s.winding -> drawSpinWake(geo, floatPos, t)
                else -> drawBubbles(geo, floatPos, t, 0.25f)
            }
            if (s.stage == Stage.WAITING && splash.value < 1f) drawCastSplash(geo, floatPos, splash.value)
            if (s.stage == Stage.FIGHT && s.fightDistance < 0.22f && !frozen) drawLandingNet(geo, colors, t)
            if (s.stage == Stage.FIGHT && frozen) {
                drawBubbles(geo, floatPos, t, if (s.running) 0.9f else 0.35f)
            } else if (s.stage == Stage.FIGHT) {
                s.bite?.let { b -> drawHookedFish(geo, floatPos, t, b.species.id, lt.marcomultiservices.kimba.core.Fish.gold(b.golden?.id), (b.cm / 60.0).toFloat(), s.fishHeading, s.running, colors) }
                drawFightSplash(geo, floatPos, t, s.running)
                val b = s.bite
                if (s.running && b != null) drawFishJump(geo, floatPos, t, b.species.id, lt.marcomultiservices.kimba.core.Fish.gold(b.golden?.id), (b.cm / 60.0).toFloat())
            }
            val reeds = !sea && !boat
            when {
                boat -> drawBoat(geo, colors, rock)
                stand == Stand.BANK && !frozen -> drawBank(geo, colors, conditions.weather.sky == lt.marcomultiservices.kimba.core.Sky.SNOW)
                else -> pierCache.draw(this, Triple(geo, colors, sea), pierRect(geo, sea)) { drawPier(geo, colors, sea, 0f) }
            }
            // At night a lantern burns beside the angler (the sea pier has its own lamps).
            when {
                sea -> Unit
                boat -> drawLantern(geo, colors, Offset(geo.w * 0.66f, geo.h * 0.905f), t)
                stand == Stand.BANK && !frozen -> drawLantern(geo, colors, Offset(geo.w * 0.16f, geo.h * 0.87f), t)
                else -> drawLantern(geo, colors, Offset(geo.w * 0.34f, geo.h * 0.9f), t)
            }
            // No reeds out on a sea pier or around the boat.
            if (reeds) drawReeds(geo, colors, t, back = true)
            if (resting) drawRodRest(geo, colors, tip, hasAlarm, s.alarm, t)
            drawRod(geo, colors, tip, quiver = kind == Tackle.FEEDER)
            drawLine(geo, tip, floatPos, s.stage, tension, slack = when (kind) { Tackle.FEEDER -> 0.12f; Tackle.SPIN -> 0.25f; else -> 1f })
            if (s.stage == Stage.LANDING) s.bite?.let { b -> drawLandedFish(geo, floatPos, t, lift.value, b.species.id, lt.marcomultiservices.kimba.core.Fish.gold(b.golden?.id), (b.cm / 60.0).toFloat()) }
            if (hanging || s.stage == Stage.FLYING) {
                val u = geo.h * 0.016f * geo.scaleAt(geo.h)
                when (kind) {
                    Tackle.FLOAT -> drawFloatBody(floatPos, u, 0f, floatStyle, colors.daylight)
                    Tackle.FEEDER -> drawFeederCage(floatPos, u, colors)
                    Tackle.SPIN -> drawSpoon(floatPos, u, t, colors)
                }
            }
            if (reeds) drawReeds(geo, colors, t, back = false)
            drawAutumn(geo, colors, t, month, onWater = !frozen && waterKind != Water.SEA && waterKind != Water.RIVER)
            drawFluff(geo, colors, t, month, java.time.Instant.ofEpochMilli(conditions.epochMillis).atZone(java.time.ZoneId.systemDefault()).dayOfMonth)
            drawWeather(geo, colors, conditions, t)
            // The headlamp follows the float; with nothing out it lights the water in front.
            drawHeadlamp(geo, colors, when {
                onWater || s.stage == Stage.FIGHT || s.stage == Stage.LANDING -> floatPos
                frozen -> target
                else -> Offset(geo.w * 0.5f, geo.h * 0.68f)
            }, t)
            if (boat) drawContext.transform.rotate(-rock * 0.9f, Offset(geo.w * 0.5f, geo.h * 0.6f))
            drawContext.transform.scale(1f / cam, 1f / cam, pivot)
            drawContext.transform.translate(-jx, -jy)
            drawVignette(geo)
        }
    }
}
