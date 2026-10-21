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
            lt.marcomultiservices.kimba.ui.fish.FishArt.draw(this, species, golden, Rect(x - len / 2, y - len * 0.2f, x + len / 2, y + len * 0.2f), t)
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
