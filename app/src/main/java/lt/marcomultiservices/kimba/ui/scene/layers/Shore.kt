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

// Layer: SHORES. Everything standing on the horizon line geo.hy: hills, forests, willows,
// the river bank, the sea coast with its lighthouse and ship, and their mirror images.

/** Where a `shore_<water>` picture goes: the full width, from above the tree tops down to the waterline. */
internal fun shoreRect(geo: SceneGeometry) = Rect(0f, geo.hy - geo.h * 0.24f, geo.w, geo.hy + geo.h * 0.006f)

/** Art slot for the shore of [kind], e.g. `shore_lake`. */
internal fun shoreSlot(kind: Water) = "shore_" + kind.name.lowercase()

internal fun DrawScope.drawShores(geo: SceneGeometry, colors: SceneColors, kind: Water, t: Float) {
    ArtPack[shoreSlot(kind)]?.let { img ->
        drawArt(img, shoreRect(geo), colors.lit(Color.White))
        drawShoreFog(geo, colors)
        return
    }
    when (kind) {
        Water.SEA -> { drawSeaHorizon(geo, colors, t); return }
        Water.POND -> drawForests(geo, colors, ridge = false)
        else -> drawForests(geo, colors, ridge = true)
    }
    if (kind == Water.RIVER) drawRiverBank(geo, colors)
}

private fun DrawScope.drawShoreFog(geo: SceneGeometry, colors: SceneColors, height: Float = 0.08f) {
    if (colors.fog <= 0f) return
    drawRect(
        Brush.verticalGradient(listOf(colors.skyHorizon.copy(alpha = 0f), colors.skyHorizon.copy(alpha = colors.fog * 0.75f)), geo.hy - geo.h * height, geo.hy),
        topLeft = Offset(0f, geo.hy - geo.h * height), size = Size(geo.w, geo.h * height),
    )
}

/** Hills, the far forest line and the near forest, each layer paler with distance. */
private fun DrawScope.drawForests(geo: SceneGeometry, colors: SceneColors, ridge: Boolean) {
    if (ridge) drawPath(geo.ridge, colors.far(Palette.HillFar, 0.62f))
    drawPath(geo.forestFar, colors.far(Palette.ForestFar, 0.42f))
    drawShoreFog(geo, colors)
    // Near forest: tree tops catch the light, the foot of the forest is in shade.
    val haze = 0.18f + colors.fog * 0.45f
    drawPath(
        geo.forestMid,
        Brush.verticalGradient(
            listOf(colors.far(Palette.ForestNearLit, haze), colors.far(Palette.ForestNear, haze), colors.far(Palette.ForestNear, haze + 0.05f)),
            geo.hy - geo.h * 0.16f, geo.hy,
        ),
    )
}

/** A river's steep earth bank with a grass lip, under the forest across the water. */
private fun DrawScope.drawRiverBank(geo: SceneGeometry, colors: SceneColors) {
    val top = ArrayList<Offset>()
    var x = 0f
    while (x <= geo.w + 1) {
        val u = x / geo.w
        top += Offset(x, geo.hy - geo.h * (0.007f + 0.003f * sin(u * 17f) + 0.0015f * sin(u * 43f + 1f)))
        x += geo.w / 90
    }
    val bank = Path().apply {
        moveTo(0f, geo.hy + 2)
        top.forEach { lineTo(it.x, it.y) }
        lineTo(geo.w, geo.hy + 2)
        close()
    }
    val haze = 0.2f + colors.fog * 0.4f
    drawPath(bank, Brush.verticalGradient(listOf(colors.far(Palette.BankEarth, haze), colors.far(Palette.BankEarthDark, haze)), top.minOf { it.y }, geo.hy))
    val lip = Path().apply { top.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
    drawPath(lip, colors.far(Palette.BankGrass, haze), style = Stroke(geo.h * 0.003f, cap = StrokeCap.Round))
}

/** Mirror image of the shore in calm water, broken by thin ripple lines, and a bright waterline. */
internal fun DrawScope.drawReflections(geo: SceneGeometry, colors: SceneColors, t: Float, kind: Water) {
    if (kind == Water.SEA) return
    val a = 0.5f * (1f - colors.rain * 0.4f) * (1f - colors.fog * 0.4f)
    clipRect(top = geo.hy) {
        withTransform({ scale(1f, -1f, pivot = Offset(0f, geo.hy)) }) {
            val art = ArtPack[shoreSlot(kind)]
            if (art != null) {
                drawArt(art, shoreRect(geo), lerp(colors.waterDeep, colors.lit(Color.White), 0.55f), a)
            } else {
                drawPath(geo.forestFar, lerp(colors.waterDeep, colors.far(Palette.ForestFar, 0.42f), 0.6f), alpha = a * 0.6f)
                drawPath(geo.forestMid, lerp(colors.waterDeep, colors.far(Palette.ForestNear, 0.18f), 0.75f), alpha = a)
            }
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
        // Where water meets the shore it catches the sky.
        drawLine(colors.skyHorizon.copy(alpha = 0.18f + 0.2f * colors.daylight), Offset(0f, geo.hy + 1.5f), Offset(geo.w, geo.hy + 1.5f), strokeWidth = 2f)
    }
}

/** Morning mist and fog lying on the water. */
internal fun DrawScope.drawMist(geo: SceneGeometry, colors: SceneColors, c: Conditions) {
    val dawnMist = if (c.phase == DayPhase.DAWN && colors.wind < 3f) 0.4f else 0f
    val m = maxOf(colors.fog * 0.8f, dawnMist)
    if (m <= 0f) return
    val col = lerp(Palette.MistNight, Palette.MistDay, colors.daylight)
    drawRect(
        Brush.verticalGradient(listOf(col.copy(alpha = m * 0.8f), col.copy(alpha = 0f)), geo.hy - geo.h * 0.01f, geo.hy + geo.h * 0.14f),
        topLeft = Offset(0f, geo.hy - geo.h * 0.01f), size = Size(geo.w, geo.h * 0.15f),
    )
}

/** Open sea: a clean horizon, a low dune coast with a lighthouse on the left, a passing ship. */
private fun DrawScope.drawSeaHorizon(geo: SceneGeometry, colors: SceneColors, t: Float) {
    val far = colors.far(Palette.Dunes, 0.75f)
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
    drawPath(tower, colors.far(Palette.LighthouseWall, 0.5f))
    drawRect(colors.far(Palette.LighthouseBand, 0.5f), Offset(lx - lh * 0.075f, lb - lh * 0.55f), Size(lh * 0.15f, lh * 0.15f))
    drawRect(colors.far(Palette.LighthouseLantern, 0.5f), Offset(lx - lh * 0.07f, lb - lh * 1.12f), Size(lh * 0.14f, lh * 0.12f))
    if (colors.daylight < 0.5f) {
        // The light sweeps round at night.
        val beam = (sin(t * 1.2f) * 0.5f + 0.5f)
        val c = Offset(lx, lb - lh * 1.06f)
        drawCircle(Brush.radialGradient(listOf(Palette.LighthouseBeam, Palette.LighthouseBeam.copy(alpha = 0f)), c, geo.w * 0.08f), geo.w * 0.08f, c, alpha = (1f - colors.daylight * 2f) * (0.4f + 0.6f * beam))
    }
    // A cargo ship slowly crossing the horizon: hull, then the bridge at the stern.
    val sx = ((0.15f + t * 0.002f) % 1.3f) * geo.w
    val sy = geo.hy - 1f
    val ship = Path().apply {
        moveTo(sx, sy); lineTo(sx + geo.w * 0.09f, sy); lineTo(sx + geo.w * 0.1f, sy - geo.h * 0.007f); lineTo(sx - geo.w * 0.005f, sy - geo.h * 0.007f); close()
        moveTo(sx + geo.w * 0.065f, sy - geo.h * 0.007f); lineTo(sx + geo.w * 0.065f, sy - geo.h * 0.014f)
        lineTo(sx + geo.w * 0.085f, sy - geo.h * 0.014f); lineTo(sx + geo.w * 0.085f, sy - geo.h * 0.007f); close()
    }
    drawPath(ship, colors.far(Palette.Ship, 0.65f))
    drawShoreFog(geo, colors, 0.06f)
}

/** Weeping willows framing the pond: a crown of leaf clumps with long curtains of twigs. */
internal fun DrawScope.drawWillows(geo: SceneGeometry, colors: SceneColors, t: Float) {
    if (ArtPack[shoreSlot(Water.POND)] != null) return
    val sway = sin(t * 0.6f) * geo.w * 0.004f * (1f + colors.wind * 0.15f)
    val pal = Palette.Willow
    for (w in geo.willows) {
        val near = w.depth < 0.3f
        val cx = w.x + w.lean
        val domeTop = w.top
        val domeBottom = w.top + w.size * 0.42f
        val half = w.size * 0.62f
        // Trunk forking into two leaders under the crown.
        val tr = Path().apply {
            val b = w.size * 0.05f
            moveTo(w.x - b, geo.hy + geo.h * 0.004f)
            quadraticTo(w.x - b * 0.6f, domeBottom + w.size * 0.3f, cx - half * 0.35f, domeBottom - w.size * 0.05f)
            lineTo(cx - half * 0.2f, domeBottom - w.size * 0.03f)
            lineTo(cx, domeBottom + w.size * 0.12f)
            lineTo(cx + half * 0.3f, domeBottom - w.size * 0.04f)
            lineTo(cx + half * 0.42f, domeBottom - w.size * 0.02f)
            quadraticTo(w.x + b * 0.6f, domeBottom + w.size * 0.3f, w.x + b, geo.hy + geo.h * 0.004f)
            close()
        }
        drawPath(tr, colors.far(pal.trunk, w.depth))

        val dark = colors.far(if (near) pal.dark else pal.far, w.depth)
        val mid = lerp(dark, colors.lit(pal.mid), if (near) 0.5f else 0.25f)
        val light = colors.lit(pal.light).copy(alpha = if (near) 0.45f else 0.22f)
        val leafDash = PathEffect.dashPathEffect(floatArrayOf(w.size * 0.02f, w.size * 0.01f))
        fun strand(dx: Float, len: Float, k: Int, lift: Float): Path {
            val sx = cx + dx * half
            // Twigs start inside the crown, higher in the middle, and fall almost straight down.
            val sy = domeBottom - (1f - dx * dx) * (domeBottom - domeTop) * lift
            val ey = (sy + len * (geo.hy - sy)).coerceAtMost(geo.hy + geo.h * 0.003f)
            val ex = sx + dx * w.size * 0.16f + sway * (1f + len) * (if (k % 2 == 0) 1f else 0.7f)
            return Path().apply { moveTo(sx, sy); quadraticTo(sx + dx * w.size * 0.2f, sy + (ey - sy) * 0.25f, ex, ey) }
        }
        // Back curtain in shade.
        w.strands.forEachIndexed { k, (dx, len) ->
            drawPath(strand(dx * 0.92f, len * 0.92f, k, 0.55f), dark, style = Stroke(w.size * 0.045f, cap = StrokeCap.Round, pathEffect = leafDash))
        }
        // Crown: overlapping clumps of leaves along an arch, each lit from the upper left.
        val clumps = 9
        for (k in 0 until clumps) {
            val f = k / (clumps - 1f) * 2f - 1f
            val arch = 1f - f * f
            val r = w.size * (0.13f + 0.07f * arch) * (0.9f + 0.2f * abs(sin(k * 2.3f + w.x)))
            val c = Offset(cx + f * half * 0.9f, domeBottom - arch * (domeBottom - domeTop) * 0.75f)
            drawCircle(Brush.radialGradient(listOf(mid, lerp(mid, dark, 0.6f), dark), Offset(c.x - r * 0.3f, c.y - r * 0.35f), r * 1.3f), r, c)
        }
        // Front curtain: every twig, with light catching the leaves.
        w.strands.forEachIndexed { k, (dx, len) ->
            val p = strand(dx, len * (if (k % 3 == 0) 1f else 0.82f), k, 0.8f)
            drawPath(p, mid, style = Stroke(w.size * 0.032f, cap = StrokeCap.Round, pathEffect = leafDash))
            if (k % 2 == 1) drawPath(p, light, style = Stroke(w.size * 0.012f, cap = StrokeCap.Round, pathEffect = leafDash))
        }
    }
}
