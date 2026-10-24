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
import androidx.compose.ui.graphics.drawscope.clipPath
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
import lt.marcomultiservices.kimba.ui.scene.drawBentArt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// Layer: FISH EFFECTS. The fish's shadow circling the float, the splashing fight, and leaps
// during a run (drawn with the fish picture from ui/fish, so a fish photo shows up here too).

/** A dark fish shape circling under the float while it nibbles and bites. */
internal fun DrawScope.drawFishShadow(geo: SceneGeometry, pos: Offset, dip: Float, t: Float, size: Float) {
    val s = geo.scaleAt(pos.y)
    val len = geo.w * 0.09f * s * size.coerceIn(0.4f, 2.2f)
    val a = t * 1.6f
    val rad = len * (1.1f - dip * 0.8f)
    val c = Offset(pos.x + cos(a) * rad, pos.y + len * 0.25f + sin(a) * rad * 0.25f)
    rotate(a * 57.3f + 90f, c) {
        val body = Path().apply {
            // A spindle with a forked tail, so it reads as a fish and not a blob.
            moveTo(c.x - len / 2, c.y)
            quadraticTo(c.x - len * 0.1f, c.y - len * 0.16f, c.x + len * 0.3f, c.y - len * 0.03f)
            lineTo(c.x + len / 2, c.y - len * 0.12f)
            lineTo(c.x + len * 0.44f, c.y)
            lineTo(c.x + len / 2, c.y + len * 0.12f)
            lineTo(c.x + len * 0.3f, c.y + len * 0.03f)
            quadraticTo(c.x - len * 0.1f, c.y + len * 0.16f, c.x - len / 2, c.y)
            close()
        }
        drawPath(body, Palette.FishShadow.copy(alpha = (0.12f + 0.28f * dip).coerceAtMost(0.4f)))
    }
}

/**
 * A swimming fish's body wave: the head stays nearly still, the wave grows towards the tail and
 * travels back along the body. [u] is 0 at the head (left of the picture) and 1 at the tail.
 */
internal fun swimWave(u: Float, t: Float, freq: Float, amp: Float): Float {
    val grow = u * u * 0.85f + u * 0.15f
    return amp * grow * sin(t * freq - u * 4.2f)
}

/**
 * The hooked fish seen through the water while it is played: its own picture, dimmed and
 * tinted by the water, facing the way it swims. Its body flexes and the tail beats, it swings
 * from side to side on the line, now and then shakes its head hard and rolls, showing its flank.
 */
internal fun DrawScope.drawHookedFish(geo: SceneGeometry, mouth: Offset, t: Float, species: String, golden: Boolean, size: Float, heading: Float, running: Boolean, colors: SceneColors) {
    val s = geo.scaleAt(mouth.y)
    val len = geo.w * 0.26f * s * size.coerceIn(0.5f, 2f)
    // Pictures face left; the body trails behind the mouth, a little under the surface.
    val c = Offset(mouth.x - heading * len * 0.42f, mouth.y + len * 0.1f)
    // Bursts of head shaking every few seconds, the way a pike or a carp fights the hook.
    val burst = ((sin(t * 0.9f + 1.3f) - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val shake = sin(t * 31f) * 9f * burst
    // The whole fish swings on the line, wider when it runs.
    val swing = sin(t * (if (running) 5.5f else 2.6f)) * (if (running) 11f else 7f)
    // A slow roll: the body narrows as it turns its flank or back to us.
    val roll = 1f - 0.22f * (0.5f + 0.5f * sin(t * 1.7f)) - 0.12f * burst
    val freq = if (running) 17f else 9f + 8f * burst
    val amp = len * (if (running) 0.13f else 0.08f + 0.06f * burst)
    val tint = lerp(colors.lit(Color(0xFF7FA9A0)), Color.White, 0.25f)
    withTransform({
        rotate(swing + shake, mouth)
        scale(-heading, roll, c)
    }) {
        val photo = ArtPack.fish(species)
        val rect = Rect(c.x - len / 2, c.y - len * 0.3f, c.x + len / 2, c.y + len * 0.3f)
        if (photo != null) {
            val ht = rect.width * photo.height / photo.width
            val r = Rect(rect.left, c.y - ht / 2, rect.right, c.y + ht / 2)
            drawBentArt(
                photo, r, { u -> swimWave(u, t, freq, amp) },
                light = tint, alpha = if (running) 0.85f else 0.7f, filter = if (golden) lt.marcomultiservices.kimba.ui.fish.FishPhotos.goldFilter else null,
            )
        } else {
            lt.marcomultiservices.kimba.ui.fish.FishArt.draw(this, species, golden, rect, t)
        }
    }
    // Head shakes break the surface: a burst of spray at the mouth.
    if (burst > 0.2f) {
        for (i in 0 until 6) {
            val k = (t * 3.1f + i * 0.17f) % 1f
            drawCircle(Palette.Spray.copy(alpha = (1 - k) * 0.6f * burst), (1.5f + 2.5f * s) * (1 - k * 0.5f), Offset(mouth.x + cos(i * 2.2f) * len * 0.18f * k, mouth.y - sin(k * PI.toFloat()) * len * 0.22f))
        }
    }
    // A veil of water over the back half keeps it under the surface.
    drawOval(
        Brush.verticalGradient(listOf(Color.Transparent, colors.lit(Color(0xFF2E5A63)).copy(alpha = 0.35f)), c.y - len * 0.2f, c.y + len * 0.3f),
        Offset(c.x - len * 0.6f, c.y - len * 0.2f), Size(len * 1.2f, len * 0.5f),
    )
}

/**
 * Where the landing net's hoop is: on the surface below the angler, reaching a little towards
 * the fish, and on [lift] (0..1) raised out of the water and drawn in towards the angler.
 */
internal fun landingNetHoop(geo: SceneGeometry, fishX: Float, atY: Float, t: Float, lift: Float): Offset {
    val reach = ((fishX - geo.w * 0.56f) * 0.45f).coerceIn(-geo.w * 0.1f, geo.w * 0.1f) * (1f - lift)
    return Offset(geo.w * (0.56f - 0.06f * lift) + reach, atY - geo.h * 0.24f * lift + sin(t * 2.2f) * geo.h * 0.003f * (1f - lift))
}

/** How long the fish lies in the net: its own length, but bent to fit a hoop smaller than it. */
internal fun netFishLength(geo: SceneGeometry, hoop: Offset, lift: Float, size: Float): Float =
    minOf(geo.w * 0.26f * geo.scaleAt(hoop.y) * size.coerceIn(0.5f, 2f), netRadius(geo, lift) * 1.75f)

/** The middle of the fish lying in the bag. */
internal fun netFishCentre(geo: SceneGeometry, hoop: Offset, lift: Float, t: Float): Offset {
    val rx = netRadius(geo, lift)
    return Offset(hoop.x, hoop.y + rx * (0.3f + 0.25f * lift) * (0.9f + 0.7f * lift) + sin(t * 3f) * rx * 0.03f)
}

/** Where the line meets the fish in the net: its mouth, at the end towards the rod. */
internal fun netFishMouth(geo: SceneGeometry, hoop: Offset, lift: Float, size: Float, t: Float): Offset {
    val c = netFishCentre(geo, hoop, lift, t)
    val len = netFishLength(geo, hoop, lift, size)
    val a = netFishTilt(lift) * PI.toFloat() / 180f
    return Offset(c.x + cos(a) * len * 0.5f, c.y + sin(a) * len * 0.5f)
}

private fun netFishTilt(lift: Float) = -8f - 6f * lift

private fun netRadius(geo: SceneGeometry, lift: Float) = geo.w * 0.17f * (1f + 0.22f * lift)

/**
 * The landing net: a knotless rubber mesh bag under an aluminium hoop on a telescopic handle
 * coming in from the angler at the bottom left. The fish lies in the bag, between its back and
 * front mesh, seen through the water until the net is [lift]ed out, dripping.
 */
internal fun DrawScope.drawLandingNet(geo: SceneGeometry, colors: SceneColors, t: Float, hoop: Offset, lift: Float, fish: (DrawScope.() -> Unit)?) {
    // At night the net is in the headlamp's light, so it never sinks into the dark water.
    val lamp = nightStrength(colors) * 0.7f
    fun c(col: Color) = lerp(colors.lit(col), col, lamp)
    val rx = netRadius(geo, lift)
    // Lifted, the hoop tilts up towards the angler and shows more of its inside.
    val ry = rx * (0.3f + 0.25f * lift)
    val sway = sin(t * 1.4f) * rx * 0.06f * (1f - 0.5f * lift)
    val deep = ry * (3.3f - 0.9f * lift) + rx * 0.35f * lift
    val bottom = Offset(hoop.x + rx * 0.08f + sway, hoop.y + deep)
    val bag = Path().apply {
        moveTo(hoop.x - rx, hoop.y)
        cubicTo(hoop.x - rx * 0.98f, hoop.y + deep * 0.55f, bottom.x - rx * 0.55f, bottom.y, bottom.x, bottom.y)
        cubicTo(bottom.x + rx * 0.55f, bottom.y, hoop.x + rx * 0.98f, hoop.y + deep * 0.55f, hoop.x + rx, hoop.y)
        // Back along the far half of the hoop.
        arcTo(Rect(hoop.x - rx, hoop.y - ry, hoop.x + rx, hoop.y + ry), 0f, -180f, false)
        close()
    }
    val oval = Rect(hoop.x - rx, hoop.y - ry, hoop.x + rx, hoop.y + ry)
    val metal = c(Color(0xFF3A4048))
    val shine = c(Color(0xFFD4DAE0))
    val mesh = c(Color(0xFF16201D))
    val rim = geo.w * 0.011f

    // Far half of the hoop and the shadowy inside of the bag.
    drawArc(metal, 180f, 180f, false, oval.topLeft, oval.size, style = Stroke(rim, cap = StrokeCap.Round))
    drawArc(shine.copy(alpha = 0.55f), 200f, 140f, false, Offset(oval.left, oval.top - rim * 0.25f), oval.size, style = Stroke(rim * 0.25f, cap = StrokeCap.Round))
    drawPath(bag, mesh.copy(alpha = 0.32f))

    // The fish lies in the bag, head towards the line.
    fish?.invoke(this)

    // Front mesh: a fine diamond weave over the fish, darker along the folds.
    clipPath(bag) {
        val step = rx / 6.5f
        val strand = mesh.copy(alpha = 0.55f)
        val glint = c(Color(0xFF6C8079)).copy(alpha = 0.25f)
        val top = hoop.y - ry
        val span = deep + ry * 2f
        var x = hoop.x - rx - span
        while (x < hoop.x + rx + span) {
            val w = sin(t * 1.4f + x * 0.01f) * rx * 0.02f
            drawLine(strand, Offset(x, top), Offset(x + span * 0.7f + w, top + span), strokeWidth = 1.6f)
            drawLine(strand, Offset(x + span * 0.7f, top), Offset(x + w, top + span), strokeWidth = 1.6f)
            drawLine(glint, Offset(x + 1.2f, top), Offset(x + 1.2f + span * 0.7f + w, top + span), strokeWidth = 0.8f)
            x += step
        }
        // Shading towards the bottom of the bag and its sides.
        drawPath(bag, Brush.verticalGradient(listOf(Color.Transparent, mesh.copy(alpha = 0.45f)), hoop.y, bottom.y))
        // Under the surface everything is seen through the water.
        val under = (1f - lift * 1.6f).coerceIn(0f, 1f)
        if (under > 0f) {
            clipRect(top = hoop.y, bottom = geo.h * 2f) {
                drawRect(c(Color(0xFF1B3A42)).copy(alpha = 0.42f * under), Offset(hoop.x - rx * 1.2f, hoop.y), Size(rx * 2.4f, deep + rx))
            }
        }
    }
    drawPath(bag, mesh.copy(alpha = 0.7f), style = Stroke(1.6f))

    // Rings on the water around the hoop while it is in.
    if (lift < 0.4f) {
        for (k in 0 until 2) {
            val ph = (t * 0.5f + k / 2f) % 1f
            val r = 1f + ph * 0.25f
            drawOval(Color.White.copy(alpha = (1f - ph) * 0.22f * (1f - lift / 0.4f)), Offset(hoop.x - rx * r, hoop.y - ry * r), Size(rx * 2 * r, ry * 2 * r), style = Stroke(1.4f))
        }
    }

    // Near half of the hoop, over the bag.
    drawArc(metal, 0f, 180f, false, oval.topLeft, oval.size, style = Stroke(rim * 1.15f, cap = StrokeCap.Round))
    drawArc(shine.copy(alpha = 0.8f), 20f, 140f, false, Offset(oval.left, oval.top - rim * 0.3f), oval.size, style = Stroke(rim * 0.3f, cap = StrokeCap.Round))

    // Yoke and telescopic handle in from the angler, with a black grip at the end.
    val yoke = Offset(hoop.x - rx * 0.92f, hoop.y + ry * 0.45f)
    val end = Offset(geo.w * 0.04f, geo.h * 1.05f)
    val joint = Offset(yoke.x + (end.x - yoke.x) * 0.45f, yoke.y + (end.y - yoke.y) * 0.45f)
    val grip = Offset(yoke.x + (end.x - yoke.x) * 0.78f, yoke.y + (end.y - yoke.y) * 0.78f)
    drawLine(metal, yoke, joint, strokeWidth = geo.w * 0.013f, cap = StrokeCap.Round)
    drawLine(c(Color(0xFF2B3036)), joint, end, strokeWidth = geo.w * 0.018f, cap = StrokeCap.Round)
    drawLine(shine.copy(alpha = 0.5f), Offset(yoke.x, yoke.y - geo.w * 0.004f), Offset(joint.x, joint.y - geo.w * 0.004f), strokeWidth = geo.w * 0.003f, cap = StrokeCap.Round)
    drawLine(shine.copy(alpha = 0.35f), Offset(joint.x, joint.y - geo.w * 0.006f), Offset(end.x, end.y - geo.w * 0.006f), strokeWidth = geo.w * 0.004f, cap = StrokeCap.Round)
    drawLine(c(Color(0xFF111316)), grip, end, strokeWidth = geo.w * 0.024f, cap = StrokeCap.Round)
    drawCircle(shine.copy(alpha = 0.7f), geo.w * 0.011f, joint, style = Stroke(geo.w * 0.004f))
    drawCircle(metal, geo.w * 0.012f, yoke)

    // Lifted out, the water runs off the bottom of the bag.
    if (lift > 0.25f) {
        for (i in 0 until 9) {
            val k = (t * 1.8f + i * 0.17f) % 1f
            val x = bottom.x + sin(i * 2.3f) * rx * 0.35f
            drawCircle(Palette.Spray.copy(alpha = (1f - k) * 0.75f * lift), 2f + 2f * lift, Offset(x, bottom.y + k * geo.h * 0.12f))
        }
    }
}

/**
 * The fish lying in the net: its own picture, head towards the rod on the right, body curved
 * along the bag, flapping now and then. [centre] is the middle of the fish.
 */
internal fun DrawScope.drawFishInNet(centre: Offset, len: Float, t: Float, species: String, golden: Boolean, lift: Float) {
    val flap = ((sin(t * 0.8f + 0.7f) - 0.4f) / 0.6f).coerceIn(0f, 1f)
    val tilt = netFishTilt(lift) + sin(t * 9f) * 6f * flap
    rotate(tilt, centre) {
        withTransform({ scale(-1f, 1f, centre) }) {
            drawFishPicture(species, golden, Rect(centre.x - len / 2, centre.y - len * 0.2f, centre.x + len / 2, centre.y + len * 0.2f), t) { u ->
                // Sagging in the middle like a fish lying in mesh, the tail kicking on a flap.
                len * 0.06f * sin(u * PI.toFloat()) + swimWave(u, t, 12f, len * 0.07f * (0.3f + flap))
            }
        }
    }
}

private const val JUMP_PERIOD = 2.6f
private const val JUMP_SHARE = 0.32f

/** Whether the running fish is in the air right now (it is not drawn under the water meanwhile). */
internal fun fishAirborne(t: Float): Boolean = (t % JUMP_PERIOD) / JUMP_PERIOD <= JUMP_SHARE

/** Where the leaping fish's mouth is, so the line follows it up out of the water. */
internal fun fishJumpMouth(geo: SceneGeometry, pos: Offset, t: Float, size: Float, heading: Float): Offset {
    val f = ((t % JUMP_PERIOD) / JUMP_PERIOD / JUMP_SHARE).coerceIn(0f, 1f)
    val len = geo.w * 0.26f * geo.scaleAt(pos.y) * size.coerceIn(0.5f, 2f)
    val dir = if (heading >= 0f) 1f else -1f
    val x = pos.x + dir * (f - 0.5f) * len * 0.9f
    val y = pos.y - sin(f * PI.toFloat()) * len * 0.6f
    val a = dir * (f - 0.5f) * 80f * PI.toFloat() / 180f
    return Offset(x + dir * cos(a) * len * 0.5f, y + dir * sin(a) * len * 0.5f)
}

/**
 * During a run the hooked fish itself leaps clear of the water now and then: the same size as
 * under the water, out of where it swims and on the way it [heading]s.
 */
internal fun DrawScope.drawFishJump(geo: SceneGeometry, pos: Offset, t: Float, species: String, golden: Boolean, size: Float, heading: Float = 1f) {
    val ph = (t % JUMP_PERIOD) / JUMP_PERIOD
    if (ph > JUMP_SHARE) return
    val f = ph / JUMP_SHARE
    val s = geo.scaleAt(pos.y)
    val len = geo.w * 0.26f * s * size.coerceIn(0.5f, 2f)
    val dir = if (heading >= 0f) 1f else -1f
    val x = pos.x + dir * (f - 0.5f) * len * 0.9f
    val y = pos.y - sin(f * PI.toFloat()) * len * 0.6f
    // Head up on the way out, down on the way back in.
    val angle = dir * (f - 0.5f) * 80f
    rotate(angle, Offset(x, y)) {
        withTransform({ if (dir > 0) scale(-1f, 1f, Offset(x, y)) }) {
            // Arched in the air, tail kicking.
            drawFishPicture(species, golden, Rect(x - len / 2, y - len * 0.2f, x + len / 2, y + len * 0.2f), t) { u ->
                -len * 0.1f * sin(u * PI.toFloat()) + swimWave(u, t, 24f, len * 0.08f)
            }
        }
    }
    // Spray where it left and where it falls back.
    for (i in 0 until 8) {
        val k = (f * 1.5f + i * 0.13f) % 1f
        val spot = if (f < 0.5f) Offset(pos.x - dir * len * 0.7f, pos.y) else Offset(pos.x + dir * len * 0.7f, pos.y)
        drawCircle(Palette.Spray.copy(alpha = (1 - k) * 0.6f), (2f + 3f * s) * (1 - k * 0.5f), Offset(spot.x + cos(i * 2.4f) * len * 0.25f * k, spot.y - sin(k * PI.toFloat()) * len * 0.3f))
    }
}

/** The hooked fish thrashing just under the surface, with spray and rings. */
internal fun DrawScope.drawFightSplash(geo: SceneGeometry, pos: Offset, t: Float, running: Boolean) {
    val s = geo.scaleAt(pos.y)
    val len = geo.w * 0.07f * s
    val wig = sin(t * (if (running) 18f else 9f)) * 0.25f
    rotate(wig * 20f, pos) {
        drawOval(Palette.FightShadow.copy(alpha = 0.35f), Offset(pos.x - len / 2, pos.y + len * 0.05f), Size(len, len * 0.26f))
    }
    val n = if (running) 10 else 4
    for (i in 0 until n) {
        val ph = (t * 2.2f + i * 0.37f) % 1f
        val a = i * 2.39f
        val d = len * (0.2f + ph * 0.6f)
        val c = Offset(pos.x + cos(a) * d, pos.y - sin(ph * PI.toFloat()) * len * 0.35f - abs(sin(a)) * d * 0.15f)
        drawCircle(Palette.Spray.copy(alpha = (1 - ph) * 0.7f), (1.5f + 2.5f * s) * (1 - ph * 0.5f), c)
    }
    for (k in 0 until 2) {
        val ph = (t * 1.4f + k / 2f) % 1f
        val r = len * (0.3f + ph)
        drawOval(Palette.Spray.copy(alpha = (1 - ph) * 0.4f), Offset(pos.x - r, pos.y - r * 0.28f), Size(r * 2, r * 0.56f), style = Stroke(2f))
    }
}

/** The fish's photo if there is one (head left, fitted into [rect] by width), otherwise its drawing. */
internal fun DrawScope.drawFishPicture(species: String, golden: Boolean, rect: Rect, t: Float, bend: ((Float) -> Float)? = null) {
    val photo = ArtPack.fish(species)
    if (photo == null) {
        lt.marcomultiservices.kimba.ui.fish.FishArt.draw(this, species, golden, rect, t)
        return
    }
    val ht = rect.width * photo.height / photo.width
    val r = Rect(rect.left, rect.center.y - ht / 2, rect.right, rect.center.y + ht / 2)
    val filter = if (golden) lt.marcomultiservices.kimba.ui.fish.FishPhotos.goldFilter else null
    if (bend != null) drawBentArt(photo, r, bend, filter = filter) else drawArt(photo, r, filter = filter)
}

/** The float landing after a cast: a crown of spray and a ring spreading out. */
internal fun DrawScope.drawCastSplash(geo: SceneGeometry, pos: Offset, p: Float) {
    val s = geo.scaleAt(pos.y)
    val size = geo.w * 0.05f * s
    for (i in 0 until 10) {
        val a = i / 10f * 2f * PI.toFloat()
        val up = sin(p * PI.toFloat()) * size * (0.8f + 0.4f * ((i * 7) % 3) / 2f)
        val c = Offset(pos.x + cos(a) * size * 0.9f * p, pos.y - up - sin(a) * size * 0.15f * p)
        drawCircle(Palette.Spray.copy(alpha = (1f - p) * 0.8f), (1.5f + 2.5f * s) * (1f - p * 0.5f), c)
    }
    for (k in 0 until 2) {
        val r = size * (0.4f + p * (1.6f + k))
        drawOval(Palette.Spray.copy(alpha = (1f - p) * 0.5f), Offset(pos.x - r, pos.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(2f))
    }
}

/**
 * A landed fish lifted out of the water on the line: it hangs head up from the hook,
 * wriggles and drips, growing as it comes towards the angler ([l] goes 0..1).
 */
internal fun DrawScope.drawLandedFish(geo: SceneGeometry, mouth: Offset, t: Float, l: Float, species: String, golden: Boolean, size: Float) {
    val len = geo.w * (0.16f + 0.3f * l) * size.coerceIn(0.6f, 1.6f)
    // Splash where it left the water.
    if (l < 0.5f) {
        val base = Offset(geo.w * 0.58f, geo.h * 0.9f)
        for (i in 0 until 12) {
            val k = (l * 2f + i * 0.08f) % 1f
            drawCircle(Palette.Spray.copy(alpha = (1 - k) * 0.7f), 3f + 4f * (1 - k), Offset(base.x + cos(i * 2.1f) * len * 0.4f * k, base.y - sin(k * PI.toFloat()) * len * 0.4f))
        }
    }
    val wiggle = sin(t * 11f) * 10f * (1f - l * 0.6f)
    // Head up: the picture's head (left side) points to the hook.
    rotate(90f + wiggle, mouth) {
        drawFishPicture(species, golden, Rect(mouth.x, mouth.y - len * 0.25f, mouth.x + len, mouth.y + len * 0.25f), t) { u ->
            swimWave(u, t, 14f, len * 0.09f * (1f - l * 0.4f))
        }
    }
    // Drops running off the tail.
    for (i in 0 until 5) {
        val k = (t * 1.6f + i * 0.21f) % 1f
        val x = mouth.x + sin(i * 1.7f) * len * 0.08f
        val y = mouth.y + len * (0.9f + k * 0.5f)
        drawCircle(Palette.Spray.copy(alpha = (1f - k) * 0.7f), 2f + 2f * l, Offset(x, y))
    }
}

/**
 * Air bubbles rising around the float where a fish is feeding: each one swells on the surface
 * and pops into a tiny ring. [amount] 0..1 is how busy the fish is (more after nibbles).
 */
internal fun DrawScope.drawBubbles(geo: SceneGeometry, pos: Offset, t: Float, amount: Float) {
    if (amount <= 0f) return
    val u = geo.h * 0.016f * geo.scaleAt(pos.y)
    for (i in 0 until 7) {
        val period = 1.3f + i * 0.31f
        val x = (t + i * 0.73f) / period
        val cycle = kotlin.math.floor(x).toInt()
        val ph = x - cycle
        if (bubbleHash(cycle * 31 + i * 7) > amount) continue
        val ang = bubbleHash(cycle * 17 + i * 13 + 5) * 2f * PI.toFloat()
        val dist = u * (1.0f + bubbleHash(cycle * 11 + i * 3 + 9) * 3.2f)
        val c = Offset(pos.x + kotlin.math.cos(ang) * dist, pos.y + kotlin.math.sin(ang) * dist * 0.3f)
        if (ph < 0.6f) {
            val k = ph / 0.6f
            val r = u * (0.1f + 0.2f * k)
            drawCircle(Color.White.copy(alpha = 0.18f + 0.3f * k), r, c)
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.35f, Offset(c.x - r * 0.35f, c.y - r * 0.35f))
        } else {
            val k = (ph - 0.6f) / 0.4f
            val r = u * (0.3f + 1.1f * k)
            drawOval(Color.White.copy(alpha = (1f - k) * 0.45f), Offset(c.x - r, c.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(1.2f))
        }
    }
}

/** A repeatable pseudo-random 0..1 from an integer, so bubbles need no allocation per frame. */
private fun bubbleHash(n: Int): Float {
    var h = n * 374761393 + 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFF) / 65535f
}
