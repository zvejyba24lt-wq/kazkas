package lt.marcomultiservices.kimba.ui.fish

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Side-view fish illustrations drawn from a handful of shape and colour parameters per species:
 * body profile, fins with rays, bars, spots, scales, gloss, eye and barbels.
 */
object FishArt {
    enum class Tail { FORK, SHALLOW, ROUND, EEL }
    enum class Dorsal { SINGLE, SPINY, REAR, LONG, EEL, TINY }
    enum class Spots { NONE, PIKE, MOTTLE, MARBLE, DOTS }

    class Look(
        val top: Color, val side: Color, val belly: Color, val fin: Color, val eye: Color,
        /** Body height / length. */
        val depth: Float,
        /** Where the body is highest along its length, 0..1 (exponent of the profile). */
        val peak: Float = 1f,
        val hump: Float = 1f,
        val tail: Tail = Tail.FORK,
        val dorsal: Dorsal = Dorsal.SINGLE,
        val bars: Int = 0,
        val barColor: Color = Color.Black,
        val spots: Spots = Spots.NONE,
        val spotColor: Color = Color.White,
        val scales: Float = 0.15f,
        val barbels: Int = 0,
        val longBarbels: Boolean = false,
        val longAnal: Boolean = false,
        val longSnout: Boolean = false,
        val bigHead: Boolean = false,
        val tailColor: Color? = null,
    )

    private val looks = mapOf(
        "roach" to Look(Color(0xFF3B575C), Color(0xFFC6D1D5), Color(0xFFF5F4EE), Color(0xFFD9573C), Color(0xFFD23A2A), 0.30f, scales = 0.22f),
        "bleak" to Look(Color(0xFF557A84), Color(0xFFDDE7EB), Color(0xFFFFFFFF), Color(0xFFC9D0D0), Color(0xFFE8ECEC), 0.21f, scales = 0.12f),
        "perch" to Look(Color(0xFF3C5628), Color(0xFF9EB05C), Color(0xFFEAE3C2), Color(0xFFE3623A), Color(0xFFE5BA3C), 0.31f, dorsal = Dorsal.SPINY, bars = 6, barColor = Color(0xFF26361A), scales = 0.12f),
        "rudd" to Look(Color(0xFF5A5730), Color(0xFFD9B865), Color(0xFFF3E7C2), Color(0xFFE5402B), Color(0xFFE39A2C), 0.34f, scales = 0.25f),
        "crucian" to Look(Color(0xFF4B4621), Color(0xFFCAA54B), Color(0xFFEBDCA8), Color(0xFF8C6B3A), Color(0xFFE0B040), 0.46f, hump = 1.1f, tail = Tail.SHALLOW, scales = 0.3f),
        "tench" to Look(Color(0xFF2B371C), Color(0xFF6E7B35), Color(0xFFC9B56B), Color(0xFF39421F), Color(0xFFD3412A), 0.30f, tail = Tail.SHALLOW, scales = 0.05f, barbels = 1),
        "bream" to Look(Color(0xFF474334), Color(0xFFB9A36F), Color(0xFFE7DDC2), Color(0xFF5B5141), Color(0xFFDCD8C8), 0.44f, hump = 1.2f, longAnal = true, scales = 0.22f),
        "carp" to Look(Color(0xFF473B20), Color(0xFFCA983F), Color(0xFFF0DAA2), Color(0xFF8C5C31), Color(0xFFE2B33F), 0.36f, hump = 1.05f, dorsal = Dorsal.LONG, scales = 0.45f, barbels = 2, tailColor = Color(0xFFB0552E)),
        "pike" to Look(Color(0xFF33432A), Color(0xFF7D8F4F), Color(0xFFEEE9D2), Color(0xFF7B6B3B), Color(0xFFE8C13C), 0.17f, peak = 0.9f, hump = 0.85f, tail = Tail.SHALLOW, dorsal = Dorsal.REAR, spots = Spots.PIKE, spotColor = Color(0xFFDCD8A2), scales = 0.05f, longSnout = true),
        "zander" to Look(Color(0xFF384446), Color(0xFFA9B3AF), Color(0xFFF1F1EB), Color(0xFFB9B9A9), Color(0xFFDFEAEA), 0.22f, dorsal = Dorsal.SPINY, bars = 8, barColor = Color(0x6638474A), spots = Spots.DOTS, spotColor = Color(0xFF3A4446), scales = 0.1f),
        "eel" to Look(Color(0xFF292D21), Color(0xFF5E5A39), Color(0xFFD8D0A1), Color(0xFF4A4631), Color(0xFFD7C878), 0.085f, peak = 0.6f, tail = Tail.EEL, dorsal = Dorsal.EEL, scales = 0f),
        "catfish" to Look(Color(0xFF25271D), Color(0xFF5B5945), Color(0xFFD7D1BD), Color(0xFF3A3A2E), Color(0xFFC9C090), 0.2f, peak = 0.6f, tail = Tail.ROUND, dorsal = Dorsal.TINY, spots = Spots.MARBLE, spotColor = Color(0xFF34352A), scales = 0f, barbels = 2, longBarbels = true, longAnal = true, bigHead = true),
        "burbot" to Look(Color(0xFF393120), Color(0xFF8B7545), Color(0xFFE1D3A9), Color(0xFF5B4B2F), Color(0xFFCDBB80), 0.17f, peak = 0.8f, tail = Tail.ROUND, dorsal = Dorsal.LONG, spots = Spots.MOTTLE, spotColor = Color(0xFF3E3320), scales = 0f, barbels = 1, longAnal = true),
    )

    private fun golden(l: Look) = Look(
        Color(0xFF8C5A12), Color(0xFFF2C24F), Color(0xFFFFF1BA), Color(0xFFFFD27C), Color(0xFFFFF6D8),
        l.depth, l.peak, l.hump, l.tail, l.dorsal, l.bars, Color(0x55A06810), l.spots, Color(0xFFFFE9A0), l.scales + 0.1f,
        l.barbels, l.longBarbels, l.longAnal, l.longSnout, l.bigHead, Color(0xFFFFB84A),
    )

    fun look(species: String, isGolden: Boolean): Look {
        val l = looks[species] ?: looks.getValue("roach")
        return if (isGolden) golden(l) else l
    }

    /** Body half-height at x (0 snout .. 0.82 tail root), in units of length. */
    private fun profile(l: Look, x: Float): Float {
        val u = (x / 0.82f).coerceIn(0f, 1f)
        val f = sin(PI.toFloat() * u.pow(l.peak * 0.75f)).coerceAtLeast(0f).pow(if (l.tail == Tail.EEL) 0.25f else 0.8f)
        var v = 0.16f + 0.84f * f
        if (l.longSnout && x < 0.22f) v *= 0.55f + 0.45f * (x / 0.22f)
        if (l.bigHead && x < 0.25f) v = v.coerceAtLeast(0.75f * (0.4f + x * 2.4f).coerceAtMost(1f))
        return l.depth / 2 * v
    }

    private fun topAt(l: Look, x: Float) = -profile(l, x) * l.hump
    private fun bottomAt(l: Look, x: Float) = profile(l, x)

    fun bodyPath(l: Look): Path = Path().apply {
        val n = 48
        moveTo(0f, 0f)
        for (i in 0..n) { val x = 0.82f * i / n; lineTo(x, topAt(l, x)) }
        for (i in n downTo 0) { val x = 0.82f * i / n; lineTo(x, bottomAt(l, x)) }
        close()
    }

    /**
     * Draws the fish facing left inside [rect] (its length fills the width).
     * [time] animates the golden shimmer and a gentle tail beat.
     */
    fun draw(scope: DrawScope, species: String, isGolden: Boolean, rect: Rect, time: Float = 0f, silhouette: Boolean = false) = with(scope) {
        val l = look(species, isGolden)
        val len = rect.width
        withTransform({
            translate(rect.left, rect.center.y)
            scale(len, len, pivot = Offset.Zero)
        }) {
            val body = bodyPath(l)
            if (silhouette) {
                drawFins(l, Color(0xFF1A2430), Color(0xFF1A2430), time, rays = false)
                drawPath(body, Color(0xFF1A2430))
                return@withTransform
            }
            // Soft shadow under the fish.
            drawOval(Color.Black.copy(alpha = 0.22f), Offset(0.05f, l.depth * 0.45f), Size(0.85f, l.depth * 0.25f))
            drawFins(l, l.fin.copy(alpha = 0.82f), l.tailColor ?: l.fin, time, rays = true)
            val h = l.depth * l.hump
            drawPath(body, Brush.verticalGradient(listOf(l.top, l.side, l.side, l.belly), -h / 2, l.depth / 2))
            clipPath(body) {
                if (l.scales > 0f) drawScales(l)
                for (i in 0 until l.bars) {
                    val x = 0.26f + i * (0.5f / l.bars.coerceAtLeast(1))
                    val bar = Path().apply {
                        moveTo(x - 0.018f, -h); lineTo(x + 0.02f, -h)
                        lineTo(x + 0.035f, l.depth * 0.1f); lineTo(x - 0.005f, l.depth * 0.12f); close()
                    }
                    drawPath(bar, l.barColor)
                }
                drawSpots(l)
                // Lateral line.
                val lat = Path().apply {
                    moveTo(0.2f, topAt(l, 0.2f) * 0.35f)
                    quadraticTo(0.5f, topAt(l, 0.5f) * 0.45f, 0.82f, 0f)
                }
                drawPath(lat, l.top.copy(alpha = 0.35f), style = Stroke(0.004f))
                // Gloss along the back.
                drawOval(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), -h * 0.45f, -h * 0.1f),
                    Offset(0.12f, -h * 0.45f), Size(0.55f, h * 0.4f),
                )
                // Dark rim along the top and belly shading for volume.
                drawPath(body, Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)), l.depth * 0.25f, l.depth / 2))
                if (isGolden) {
                    val x = ((time * 0.35f) % 1.6f) - 0.3f
                    drawRect(
                        Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent), Offset(x - 0.12f, -h), Offset(x + 0.12f, h)),
                        Offset(-0.1f, -h), Size(1.2f, h * 2),
                    )
                }
            }
            // Gill cover.
            val gx = if (l.bigHead) 0.24f else if (l.longSnout) 0.25f else 0.2f
            val gill = Path().apply {
                moveTo(gx - 0.01f, topAt(l, gx) * 0.75f)
                quadraticTo(gx + 0.035f, 0f, gx - 0.005f, bottomAt(l, gx) * 0.8f)
            }
            drawPath(gill, l.top.copy(alpha = 0.5f), style = Stroke(0.006f, cap = StrokeCap.Round))
            drawPectoral(l, time)
            // Eye.
            val ex = if (l.longSnout) 0.13f else if (l.tail == Tail.EEL) 0.05f else 0.075f
            val ey = topAt(l, ex) * 0.42f
            val er = (l.depth * (if (l.tail == Tail.EEL) 0.12f else 0.13f)).coerceIn(0.012f, 0.038f) * (if (species == "zander") 1.25f else 1f)
            drawCircle(Color(0xFF14110C), er * 1.12f, Offset(ex, ey))
            drawCircle(l.eye, er, Offset(ex, ey))
            drawCircle(Color(0xFF0B0B0B), er * 0.55f, Offset(ex - er * 0.08f, ey))
            drawCircle(Color.White.copy(alpha = 0.85f), er * 0.22f, Offset(ex - er * 0.3f, ey - er * 0.3f))
            // Mouth.
            val my = topAt(l, 0.01f) * 0.1f
            drawLine(Color(0xFF1A140E).copy(alpha = 0.6f), Offset(0.0f, my), Offset(if (l.longSnout || l.bigHead) 0.07f else 0.035f, my + l.depth * 0.06f), strokeWidth = 0.004f, cap = StrokeCap.Round)
            // Barbels.
            for (b in 0 until l.barbels) {
                val bl = if (l.longBarbels) (if (b == 0) 0.28f else 0.1f) else 0.04f
                val sway = sin(time * 1.5f + b) * 0.01f
                val p = Path().apply {
                    moveTo(0.02f + b * 0.01f, my + 0.004f)
                    quadraticTo(0.0f - bl * 0.2f, my + bl * 0.4f + sway, 0.03f + bl * 0.3f, my + bl * 0.9f + sway)
                }
                drawPath(p, l.top.copy(alpha = 0.9f), style = Stroke(if (l.longBarbels && b == 0) 0.006f else 0.0035f, cap = StrokeCap.Round))
            }
            // Fine outline.
            drawPath(body, Color.Black.copy(alpha = 0.25f), style = Stroke(0.003f))
            if (isGolden) drawSparkles(l, time)
        }
    }

    private fun DrawScope.drawScales(l: Look) {
        val step = 0.028f
        val h = l.depth * l.hump
        val col = Color.Black.copy(alpha = l.scales * 0.5f)
        val hi = Color.White.copy(alpha = l.scales * 0.6f)
        var row = 0
        var y = -h / 2
        while (y < l.depth / 2) {
            var x = 0.22f + (if (row % 2 == 0) 0f else step / 2)
            while (x < 0.84f) {
                drawArc(col, 300f, 120f, false, Offset(x - step * 0.6f, y - step * 0.5f), Size(step, step), style = Stroke(0.0025f))
                drawArc(hi, 290f, 60f, false, Offset(x - step * 0.65f, y - step * 0.55f), Size(step, step), style = Stroke(0.002f))
                x += step
            }
            y += step * 0.62f
            row++
        }
    }

    private fun DrawScope.drawSpots(l: Look) {
        if (l.spots == Spots.NONE) return
        val r = Random(l.spots.ordinal * 31 + 7)
        val h = l.depth * l.hump
        val n = when (l.spots) { Spots.PIKE -> 90; Spots.MOTTLE -> 70; Spots.MARBLE -> 55; Spots.DOTS -> 40; else -> 0 }
        repeat(n) {
            val x = 0.15f + r.nextFloat() * 0.7f
            val y = -h / 2 + r.nextFloat() * l.depth * 0.8f
            when (l.spots) {
                Spots.PIKE -> drawOval(l.spotColor.copy(alpha = 0.75f), Offset(x, y), Size(0.022f, 0.012f))
                Spots.MOTTLE -> drawOval(l.spotColor.copy(alpha = 0.5f), Offset(x, y), Size(0.03f + r.nextFloat() * 0.03f, 0.018f + r.nextFloat() * 0.02f))
                Spots.MARBLE -> drawOval(l.spotColor.copy(alpha = 0.55f), Offset(x, y), Size(0.04f + r.nextFloat() * 0.05f, 0.02f + r.nextFloat() * 0.03f))
                Spots.DOTS -> if (y < 0) drawCircle(l.spotColor.copy(alpha = 0.4f), 0.004f, Offset(x, y))
                else -> Unit
            }
        }
    }

    private fun DrawScope.fin(points: List<Offset>, color: Color, rays: Boolean, rayFrom: Offset? = null) {
        val p = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            close()
        }
        drawPath(p, Brush.linearGradient(listOf(color, color.copy(alpha = color.alpha * 0.55f)), points.first(), points[points.size / 2]))
        if (rays && rayFrom != null) {
            for (pt in points.drop(1).dropLast(1)) drawLine(Color.Black.copy(alpha = 0.18f), rayFrom, pt, strokeWidth = 0.0025f)
        }
        drawPath(p, Color.Black.copy(alpha = 0.12f), style = Stroke(0.002f))
    }

    private fun DrawScope.drawFins(l: Look, finColor: Color, tailColor: Color, time: Float, rays: Boolean) {
        val d = l.depth
        val beat = sin(time * 3f) * 0.02f
        // Tail.
        val tr = 0.8f
        val ty = profile(l, 0.82f)
        when (l.tail) {
            Tail.FORK, Tail.SHALLOW -> {
                val notch = if (l.tail == Tail.FORK) 0.92f else 0.97f
                val spread = (d * 0.75f).coerceAtLeast(0.11f)
                fin(listOf(Offset(tr, -ty), Offset(1.0f, -spread + beat), Offset(notch, beat * 0.5f), Offset(1.0f, spread + beat), Offset(tr, ty)), tailColor.copy(alpha = 0.88f), rays, Offset(tr, 0f))
            }
            Tail.ROUND -> {
                val spread = (d * 0.6f).coerceAtLeast(0.08f)
                fin(listOf(Offset(tr, -ty), Offset(0.93f, -spread + beat), Offset(1.0f, -spread * 0.4f + beat), Offset(1.01f, beat), Offset(1.0f, spread * 0.4f + beat), Offset(0.93f, spread + beat), Offset(tr, ty)), tailColor.copy(alpha = 0.88f), rays, Offset(tr, 0f))
            }
            Tail.EEL -> fin(listOf(Offset(0.75f, -ty), Offset(1.0f, beat), Offset(0.75f, ty)), tailColor, rays, null)
        }
        // Dorsal fins.
        fun top(x: Float) = topAt(l, x)
        when (l.dorsal) {
            Dorsal.SINGLE -> fin(listOf(Offset(0.38f, top(0.38f)), Offset(0.44f, top(0.44f) - d * 0.42f), Offset(0.5f, top(0.5f) - d * 0.3f), Offset(0.58f, top(0.58f))), finColor, rays, Offset(0.48f, top(0.48f)))
            Dorsal.SPINY -> {
                val spikes = buildList {
                    add(Offset(0.27f, top(0.27f)))
                    for (i in 0..6) { val x = 0.29f + i * 0.025f; add(Offset(x, top(x) - d * 0.42f)); add(Offset(x + 0.0125f, top(x) - d * 0.3f)) }
                    add(Offset(0.47f, top(0.47f)))
                }
                fin(spikes, finColor.copy(red = finColor.red * 0.6f, green = finColor.green * 0.7f), rays, Offset(0.37f, top(0.37f)))
                fin(listOf(Offset(0.5f, top(0.5f)), Offset(0.54f, top(0.54f) - d * 0.32f), Offset(0.64f, top(0.64f) - d * 0.22f), Offset(0.68f, top(0.68f))), finColor, rays, Offset(0.58f, top(0.58f)))
            }
            Dorsal.REAR -> fin(listOf(Offset(0.6f, top(0.6f)), Offset(0.66f, top(0.66f) - d * 0.55f), Offset(0.75f, top(0.75f) - d * 0.4f), Offset(0.79f, top(0.79f))), finColor, rays, Offset(0.7f, top(0.7f)))
            Dorsal.LONG -> {
                if (l.barbels == 1) fin(listOf(Offset(0.33f, top(0.33f)), Offset(0.37f, top(0.37f) - d * 0.3f), Offset(0.42f, top(0.42f))), finColor, rays, Offset(0.37f, top(0.37f)))
                val from = if (l.barbels == 1) 0.45f else 0.36f
                fin(listOf(Offset(from, top(from)), Offset(from + 0.04f, top(from) - d * 0.32f), Offset(0.74f, top(0.74f) - d * 0.22f), Offset(0.8f, top(0.8f))), finColor, rays, Offset((from + 0.8f) / 2, top((from + 0.8f) / 2)))
            }
            Dorsal.EEL -> fin(listOf(Offset(0.36f, top(0.36f)), Offset(0.45f, top(0.45f) - d * 0.35f), Offset(0.98f, -d * 0.3f + beat), Offset(1.0f, beat), Offset(0.8f, top(0.8f))), finColor, false)
            Dorsal.TINY -> fin(listOf(Offset(0.3f, top(0.3f)), Offset(0.32f, top(0.32f) - d * 0.3f), Offset(0.36f, top(0.36f))), finColor, rays, Offset(0.32f, top(0.32f)))
        }
        // Anal and pelvic fins.
        fun bot(x: Float) = bottomAt(l, x)
        if (l.tail == Tail.EEL) {
            fin(listOf(Offset(0.5f, bot(0.5f)), Offset(0.6f, bot(0.6f) + d * 0.3f), Offset(0.98f, d * 0.3f + beat), Offset(1.0f, beat), Offset(0.8f, bot(0.8f))), finColor, false)
        } else if (l.longAnal) {
            fin(listOf(Offset(0.48f, bot(0.48f)), Offset(0.52f, bot(0.52f) + d * 0.25f), Offset(0.76f, bot(0.76f) + d * 0.2f), Offset(0.8f, bot(0.8f))), finColor, rays, Offset(0.64f, bot(0.64f)))
        } else {
            fin(listOf(Offset(0.6f, bot(0.6f)), Offset(0.64f, bot(0.64f) + d * 0.3f), Offset(0.7f, bot(0.7f) + d * 0.22f), Offset(0.73f, bot(0.73f))), finColor, rays, Offset(0.66f, bot(0.66f)))
        }
        if (l.tail != Tail.EEL) {
            fin(listOf(Offset(0.4f, bot(0.4f)), Offset(0.44f, bot(0.44f) + d * 0.3f), Offset(0.49f, bot(0.49f) + d * 0.2f), Offset(0.5f, bot(0.5f))), finColor, rays, Offset(0.43f, bot(0.43f)))
        }
    }

    private fun DrawScope.drawPectoral(l: Look, time: Float) {
        val x = if (l.longSnout) 0.27f else 0.23f
        val y = bottomAt(l, x) * 0.45f
        val c = Offset(x, y)
        rotate(25f + sin(time * 2.2f) * 8f, c) {
            drawOval(l.fin.copy(alpha = 0.55f), Offset(x, y - l.depth * 0.06f), Size((l.depth * 0.45f).coerceAtLeast(0.06f), l.depth * 0.13f))
        }
    }

    private fun DrawScope.drawSparkles(l: Look, time: Float) {
        val r = Random(5)
        repeat(9) {
            val x = r.nextFloat() * 0.9f
            val y = (r.nextFloat() - 0.5f) * l.depth * 1.6f
            val tw = (sin(time * 3f + it * 1.7f) * 0.5f + 0.5f).pow(3f)
            val s = 0.02f * tw
            drawLine(Color.White.copy(alpha = tw), Offset(x - s, y), Offset(x + s, y), strokeWidth = 0.003f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = tw), Offset(x, y - s), Offset(x, y + s), strokeWidth = 0.003f, cap = StrokeCap.Round)
        }
    }
}
