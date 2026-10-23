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
 * The landing net held out into the water at the bank, reaching after the fish as it swims
 * across. Picture slot `landing_net` (see docs/GRAFIKA.md): the net seen from above and behind,
 * hoop at the top right, handle running out of the bottom left corner.
 */
internal fun DrawScope.drawLandingNet(geo: SceneGeometry, colors: SceneColors, t: Float, fish: Offset) {
    val reach = ((fish.x - geo.w * 0.56f) * 0.45f).coerceIn(-geo.w * 0.12f, geo.w * 0.12f)
    val hoop = Offset(geo.w * 0.56f + reach, geo.h * 0.88f + sin(t * 2.2f) * geo.h * 0.003f)
    val rx = geo.w * 0.15f
    val ry = rx * 0.32f
    ArtPack["landing_net"]?.let { img ->
        // The hoop fills the top right of the picture; the rest is handle and mesh.
        val w = rx * 2 / 0.62f
        val h = w * img.height / img.width
        drawArt(img, androidx.compose.ui.geometry.Rect(hoop.x + rx - w, hoop.y - ry - h * 0.04f, hoop.x + rx + w * 0.02f, hoop.y - ry - h * 0.04f + h), colors.lit(Color.White))
        return
    }
    // The handle comes in from the angler at the bottom left.
    drawLine(colors.lit(Color(0xFF2B2B2E)), Offset(geo.w * 0.08f, geo.h * 1.02f), Offset(hoop.x - rx * 0.95f, hoop.y + ry * 0.2f), strokeWidth = geo.w * 0.016f, cap = StrokeCap.Round)
    drawLine(colors.lit(Color(0xFF6F6F78)).copy(alpha = 0.6f), Offset(geo.w * 0.09f, geo.h * 1.01f), Offset(hoop.x - rx * 0.95f, hoop.y + ry * 0.05f), strokeWidth = geo.w * 0.004f, cap = StrokeCap.Round)
    // The mesh hangs under the water, seen through it.
    val bag = Path().apply {
        moveTo(hoop.x - rx, hoop.y)
        quadraticTo(hoop.x - rx * 0.5f, hoop.y + ry * 3.2f, hoop.x + rx * 0.1f, hoop.y + ry * 3.0f)
        quadraticTo(hoop.x + rx * 0.8f, hoop.y + ry * 2.4f, hoop.x + rx, hoop.y)
        close()
    }
    drawPath(bag, colors.lit(Color(0xFF203833)).copy(alpha = 0.35f))
    for (i in 1..6) {
        val u = i / 7f
        val x = hoop.x - rx + 2 * rx * u
        drawLine(colors.lit(Color(0xFF9DB7AE)).copy(alpha = 0.22f), Offset(x, hoop.y), Offset(hoop.x - rx * 0.2f + (x - hoop.x) * 0.3f, hoop.y + ry * 2.9f), strokeWidth = 1.2f)
    }
    for (j in 1..3) {
        val k = j / 4f
        drawOval(colors.lit(Color(0xFF9DB7AE)).copy(alpha = 0.18f), Offset(hoop.x - rx * (1 - 0.45f * k), hoop.y + ry * 2.6f * k - ry * 0.3f), Size(rx * 2 * (1 - 0.45f * k), ry * 0.7f), style = Stroke(1.2f))
    }
    // The hoop itself on the surface.
    drawOval(colors.lit(Color(0xFF1E1E22)), Offset(hoop.x - rx, hoop.y - ry), Size(rx * 2, ry * 2), style = Stroke(geo.w * 0.007f))
    drawOval(colors.lit(Color(0xFF8A8A94)).copy(alpha = 0.5f), Offset(hoop.x - rx, hoop.y - ry * 1.06f), Size(rx * 2, ry * 2), style = Stroke(geo.w * 0.0018f))
}

/** During a run the fish leaps clear of the water now and then. */
internal fun DrawScope.drawFishJump(geo: SceneGeometry, pos: Offset, t: Float, species: String, golden: Boolean, size: Float) {
    val period = 2.6f
    val ph = (t % period) / period
    if (ph > 0.32f) return
    val f = ph / 0.32f
    val s = geo.scaleAt(pos.y)
    val len = geo.w * 0.12f * s * size.coerceIn(0.5f, 2f)
    val dir = if (((t / period).toInt() % 2) == 0) 1f else -1f
    val x = pos.x + dir * (f - 0.5f) * len * 1.4f
    val y = pos.y - sin(f * PI.toFloat()) * len * 0.9f
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
