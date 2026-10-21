package lt.marcomultiservices.kimba.ui.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Everything about the lake that only depends on the screen size, built once. */
class SceneGeometry(val w: Float, val h: Float) {
    val hy = h * 0.44f

    class Star(val x: Float, val y: Float, val r: Float, val phase: Float)
    class Puff(val x0: Float, val y: Float, val scale: Float, val parts: List<Triple<Float, Float, Float>>)
    class Reed(val x: Float, val height: Float, val lean: Float, val width: Float, val phase: Float, val head: Boolean, val back: Boolean)
    class Lily(val x: Float, val y: Float, val r: Float, val rot: Float, val flower: Boolean)
    class Drop(val x: Float, val y: Float, val speed: Float, val len: Float)
    class Fly(val x: Float, val y: Float, val ax: Float, val ay: Float, val phase: Float)

    private val rnd = Random(20261020)

    /** Height of the far ridge at x. */
    private val ridgePhases = List(4) { rnd.nextFloat() * 6.28f }
    fun ridgeY(x: Float): Float {
        val u = x / w
        val n = sin(u * 5.1f + ridgePhases[0]) * 0.45f + sin(u * 11.3f + ridgePhases[1]) * 0.25f +
            sin(u * 23.7f + ridgePhases[2]) * 0.1f + sin(u * 2.3f + ridgePhases[3]) * 0.6f
        return hy - h * (0.03f + 0.018f * n)
    }

    val ridge: Path = Path().apply {
        moveTo(0f, hy + 1)
        var x = 0f
        while (x <= w) { lineTo(x, ridgeY(x)); x += w / 80 }
        lineTo(w, ridgeY(w)); lineTo(w, hy + 1); close()
    }

    val forestFar: Path = Path().apply {
        var x = -w * 0.01f
        while (x < w * 1.01f) {
            val th = h * (0.018f + rnd.nextFloat() * 0.022f)
            spruce(this, x, ridgeY(x) + h * 0.006f, th, th * (0.32f + rnd.nextFloat() * 0.1f), rnd)
            x += w * (0.006f + rnd.nextFloat() * 0.01f)
        }
        // Fill under the trees down to the water.
        moveTo(0f, hy + 1)
        var xx = 0f
        while (xx <= w) { lineTo(xx, ridgeY(xx) + h * 0.006f); xx += w / 60 }
        lineTo(w, hy + 1); close()
    }

    /** The near shores: tall forest on both sides, lower across the open water in the middle. */
    val forestMid: Path = Path().apply {
        var x = -w * 0.03f
        while (x < w * 1.03f) {
            val u = x / w
            val edge = (kotlin.math.abs(u - 0.5f) * 2f).pow(1.6f)
            val th = h * (0.025f + 0.11f * edge) * (0.75f + rnd.nextFloat() * 0.5f)
            val base = hy + h * 0.004f
            if (rnd.nextFloat() < 0.22f && edge > 0.25f) {
                roundTree(this, x, base, th * 0.85f, th * 0.55f, rnd)
            } else {
                spruce(this, x, base, th, th * (0.3f + rnd.nextFloat() * 0.1f), rnd)
            }
            x += w * (0.008f + rnd.nextFloat() * 0.014f) * (0.6f + edge)
        }
        moveTo(0f, hy + h * 0.006f)
        lineTo(0f, hy - h * 0.02f)
        lineTo(w * 0.25f, hy - h * 0.006f)
        lineTo(w * 0.75f, hy - h * 0.006f)
        lineTo(w, hy - h * 0.02f)
        lineTo(w, hy + h * 0.006f); close()
    }

    val stars: List<Star> = List(140) {
        Star(rnd.nextFloat() * w, rnd.nextFloat().pow(1.4f) * hy * 0.95f, 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f)
    }

    val puffs: List<Puff> = List(18) {
        val parts = List(6) { Triple((rnd.nextFloat() - 0.5f) * 2.4f, (rnd.nextFloat() - 0.5f) * 0.5f, 0.5f + rnd.nextFloat() * 0.6f) }
        Puff(rnd.nextFloat() * 1.4f, hy * (0.06f + rnd.nextFloat() * 0.6f), 0.6f + rnd.nextFloat() * 0.9f, parts)
    }

    val reeds: List<Reed> = buildList {
        repeat(26) { add(Reed(w * (0.79f + rnd.nextFloat() * 0.26f), h * (0.16f + rnd.nextFloat() * 0.17f), (rnd.nextFloat() - 0.6f) * 0.1f, 2.5f + rnd.nextFloat() * 3.5f, rnd.nextFloat() * 6.28f, rnd.nextFloat() < 0.3f, rnd.nextBoolean())) }
        repeat(12) { add(Reed(w * (0.24f + rnd.nextFloat() * 0.1f), h * (0.07f + rnd.nextFloat() * 0.08f), (rnd.nextFloat() - 0.4f) * 0.12f, 2f + rnd.nextFloat() * 2.5f, rnd.nextFloat() * 6.28f, rnd.nextFloat() < 0.25f, true)) }
    }.sortedBy { if (it.back) 0 else 1 }

    val lilies: List<Lily> = listOf(
        Lily(w * 0.70f, h * 0.745f, w * 0.040f, 0.4f, false),
        Lily(w * 0.77f, h * 0.775f, w * 0.050f, 2.1f, true),
        Lily(w * 0.66f, h * 0.80f, w * 0.055f, 4.0f, false),
        Lily(w * 0.84f, h * 0.73f, w * 0.033f, 1.2f, false),
        Lily(w * 0.36f, h * 0.66f, w * 0.026f, 5.2f, false),
    )

    /** A pond is full of lilies. */
    val pondLilies: List<Lily> = lilies + listOf(
        Lily(w * 0.58f, h * 0.70f, w * 0.035f, 3.0f, true),
        Lily(w * 0.47f, h * 0.62f, w * 0.022f, 0.9f, false),
        Lily(w * 0.28f, h * 0.62f, w * 0.02f, 2.6f, true),
        Lily(w * 0.62f, h * 0.60f, w * 0.018f, 4.4f, false),
        Lily(w * 0.9f, h * 0.68f, w * 0.03f, 5.6f, true),
        Lily(w * 0.4f, h * 0.555f, w * 0.014f, 1.7f, false),
    )

    class Willow(val x: Float, val top: Float, val size: Float, val lean: Float, val depth: Float, val strands: List<Pair<Float, Float>>)

    val willows: List<Willow> = listOf(
        Triple(w * 0.04f, h * 0.17f, 0f), Triple(w * 0.97f, h * 0.15f, 0f), Triple(w * 0.22f, h * 0.09f, 0.3f), Triple(w * 0.8f, h * 0.1f, 0.3f),
    ).map { (x, size, depth) ->
        Willow(x, hy - size * 1.15f, size, (if (x < w / 2) 1 else -1) * size * 0.15f, depth, List(14) { (rnd.nextFloat() - 0.5f) * 2f to (0.5f + rnd.nextFloat() * 0.5f) })
    }

    class Stone(val x: Float, val y: Float, val r: Float)
    class Leaf(val x: Float, val y: Float, val speed: Float, val phase: Float)

    val stones: List<Stone> = listOf(
        Stone(w * 0.72f, h * 0.79f, w * 0.05f), Stone(w * 0.83f, h * 0.75f, w * 0.032f), Stone(w * 0.62f, h * 0.83f, w * 0.03f),
        Stone(w * 0.35f, h * 0.65f, w * 0.018f), Stone(w * 0.9f, h * 0.71f, w * 0.022f),
    )
    val leaves: List<Leaf> = List(7) { Leaf(rnd.nextFloat() * 1.2f, hy + h * (0.04f + rnd.nextFloat() * 0.3f), 0.012f + rnd.nextFloat() * 0.01f, rnd.nextFloat() * 6.28f) }

    val drops: List<Drop> = List(220) { Drop(rnd.nextFloat() * 1.2f - 0.1f, rnd.nextFloat(), 1.5f + rnd.nextFloat() * 1.0f, h * (0.025f + rnd.nextFloat() * 0.03f)) }
    val flakes: List<Drop> = List(160) { Drop(rnd.nextFloat(), rnd.nextFloat(), 0.05f + rnd.nextFloat() * 0.07f, 1.2f + rnd.nextFloat() * 2.4f) }
    val flies: List<Fly> = List(16) { Fly(w * (0.05f + rnd.nextFloat() * 0.9f), hy + h * (0.02f + rnd.nextFloat() * 0.25f), w * (0.02f + rnd.nextFloat() * 0.05f), h * (0.01f + rnd.nextFloat() * 0.03f), rnd.nextFloat() * 6.28f) }

    // The jetty, in perspective from the bottom left corner out onto the lake.
    val pierNearY = h * 1.0f
    val pierFarY = h * 0.80f
    val pierNear = -w * 0.06f to w * 0.44f
    val pierFar = w * 0.03f to w * 0.25f
    val pier: Path = Path().apply {
        moveTo(pierNear.first, pierNearY + 4)
        lineTo(pierNear.second, pierNearY + 4)
        lineTo(pierFar.second, pierFarY)
        lineTo(pierFar.first, pierFarY)
        close()
    }

    /** Edge points of plank seam i of n, far to near. */
    fun plank(i: Int, n: Int): Pair<Offset, Offset> {
        val t = (i.toFloat() / n).pow(1.7f)
        val y = pierFarY + (pierNearY - pierFarY) * t
        val x1 = pierFar.first + (pierNear.first - pierFar.first) * t
        val x2 = pierFar.second + (pierNear.second - pierFar.second) * t
        return Offset(x1, y) to Offset(x2, y)
    }

    val rodBase = Offset(w * 1.03f, h * 1.02f)
    val rodTip = Offset(w * 0.66f, h * 0.575f)

    /** Where the float lands for a cast of [dist] 0..1. */
    fun floatTarget(dist: Double, castId: Int): Offset {
        val d = dist.coerceIn(0.0, 1.0).pow(0.8).toFloat()
        return Offset(w * (0.5f + 0.05f * sin(castId * 1.7f)), h * 0.86f + (hy + h * 0.035f - h * 0.86f) * d)
    }

    /** Perspective scale at screen height y: small far away, large close by. */
    fun scaleAt(y: Float): Float = 0.35f + 0.8f * ((y - hy) / (h - hy)).coerceIn(0f, 1f)

    companion object {
        /** A layered spruce silhouette with slightly ragged tiers. */
        fun spruce(p: Path, x: Float, base: Float, height: Float, width: Float, rnd: Random) {
            val tiers = 6
            val top = base - height
            p.moveTo(x, top)
            val right = ArrayList<Offset>()
            val left = ArrayList<Offset>()
            for (k in 1..tiers) {
                val f = k / tiers.toFloat()
                val ty = top + height * 0.92f * f
                val hw = width / 2 * f.pow(0.85f) * (0.85f + rnd.nextFloat() * 0.3f)
                right += Offset(x + hw * 0.45f, ty - height * 0.05f)
                right += Offset(x + hw, ty + height * 0.015f)
                left += Offset(x - hw * 0.45f, ty - height * 0.05f)
                left += Offset(x - hw, ty + height * 0.015f)
            }
            right.forEach { p.lineTo(it.x, it.y) }
            p.lineTo(x + width * 0.04f, base)
            p.lineTo(x - width * 0.04f, base)
            left.asReversed().forEach { p.lineTo(it.x, it.y) }
            p.close()
        }

        /** A broadleaf tree: a trunk and a crown of overlapping round lobes. */
        fun roundTree(p: Path, x: Float, base: Float, height: Float, width: Float, rnd: Random) {
            p.addRect(Rect(x - width * 0.04f, base - height * 0.4f, x + width * 0.04f, base))
            val cy = base - height * 0.62f
            repeat(6) {
                val a = it / 6f * 2 * PI.toFloat()
                val r = width * (0.22f + rnd.nextFloat() * 0.12f)
                val cx = x + kotlin.math.cos(a) * width * 0.22f
                val cyy = cy + sin(a) * height * 0.16f
                p.addOval(Rect(cx - r, cyy - r, cx + r, cyy + r))
            }
            val r = width * 0.3f
            p.addOval(Rect(x - r, cy - height * 0.2f - r, x + r, cy - height * 0.2f + r))
        }
    }
}
