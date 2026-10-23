package lt.marcomultiservices.kimba.ui.scene.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.SceneColors
import lt.marcomultiservices.kimba.ui.scene.SceneGeometry
import lt.marcomultiservices.kimba.ui.scene.drawArt
import kotlin.math.sin

/*
 * Night fishing: the angler's headlamp throws a beam onto the float and a lantern burns beside
 * them, so the dark lake stays playable and looks like a real night out. Picture slot (see
 * docs/GRAFIKA.md): `lantern` (an old paraffin lantern standing on the ground, transparent
 * around it; its flame and glow are added by code).
 */

private val Beam = Color(0xFFFFF1D2)
private val Flame = Color(0xFFFFC46B)

/** How much the night lights show: nothing by day, fully once it is properly dark. */
fun nightStrength(colors: SceneColors): Float = ((1f - colors.daylight - 0.35f) / 0.5f).coerceIn(0f, 1f)

/**
 * The headlamp beam from the angler (just below the bottom edge) to [target], the float or where
 * the line enters, with a bright pool of light on the water around it.
 */
fun DrawScope.drawHeadlamp(geo: SceneGeometry, colors: SceneColors, target: Offset, t: Float) {
    val k = nightStrength(colors)
    if (k <= 0f) return
    // The head moves a little as the angler breathes and watches.
    val sway = sin(t * 0.45f) * geo.w * 0.006f
    val at = Offset(target.x + sway, target.y)
    val from = Offset(geo.w * 0.6f, geo.h * 1.04f)
    val s = geo.scaleAt(at.y)
    val spread = geo.w * 0.16f * s
    val cone = Path().apply {
        moveTo(from.x - geo.w * 0.02f, from.y)
        lineTo(at.x - spread, at.y)
        lineTo(at.x + spread, at.y)
        lineTo(from.x + geo.w * 0.02f, from.y)
        close()
    }
    drawPath(
        cone,
        Brush.linearGradient(listOf(Beam.copy(alpha = 0.16f * k), Beam.copy(alpha = 0.05f * k)), from, at),
        blendMode = BlendMode.Screen,
    )
    // The pool on the water, flattened by the angle.
    val r = geo.w * 0.2f * s
    withTransform({ scale(1f, 0.36f, pivot = at) }) {
        drawCircle(
            Brush.radialGradient(listOf(Beam.copy(alpha = 0.42f * k), Beam.copy(alpha = 0.12f * k), Color.Transparent), at, r),
            r, at, blendMode = BlendMode.Screen,
        )
    }
}

/** A lantern standing at [base] (its bottom), its flame flickering and lighting the boards around. */
fun DrawScope.drawLantern(geo: SceneGeometry, colors: SceneColors, base: Offset, t: Float) {
    val k = nightStrength(colors)
    if (k <= 0f) return
    val s = geo.scaleAt(base.y)
    val hgt = geo.h * 0.055f * s
    val flicker = 0.88f + 0.08f * sin(t * 13f) + 0.04f * sin(t * 29f + 1f)
    // Warm light on whatever it stands on.
    val glowR = hgt * 3.2f
    withTransform({ scale(1f, 0.5f, pivot = base) }) {
        drawCircle(
            Brush.radialGradient(listOf(Flame.copy(alpha = 0.5f * k * flicker), Flame.copy(alpha = 0.12f * k), Color.Transparent), base, glowR),
            glowR, base, blendMode = BlendMode.Screen,
        )
    }
    val art = ArtPack["lantern"]
    // The flame sits lower in the painted lantern than in the drawn one.
    val flame = Offset(base.x, base.y - hgt * (if (art != null) 0.38f else 0.5f))
    if (art != null) {
        val w = hgt * art.width / art.height
        drawArt(art, Rect(base.x - w / 2, base.y - hgt, base.x + w / 2, base.y), colors.lit(Color.White, 0.4f))
    } else {
        val metal = colors.lit(Color(0xFF3A3530), 0.3f)
        // Base, glass, cap and handle.
        drawRect(metal, Offset(base.x - hgt * 0.24f, base.y - hgt * 0.2f), Size(hgt * 0.48f, hgt * 0.2f))
        drawRect(Flame.copy(alpha = 0.35f + 0.3f * k), Offset(base.x - hgt * 0.17f, base.y - hgt * 0.72f), Size(hgt * 0.34f, hgt * 0.52f))
        drawRect(metal, Offset(base.x - hgt * 0.2f, base.y - hgt * 0.82f), Size(hgt * 0.4f, hgt * 0.1f))
        drawArc(metal, 180f, 180f, false, Offset(base.x - hgt * 0.16f, base.y - hgt * 1.02f), Size(hgt * 0.32f, hgt * 0.36f), style = androidx.compose.ui.graphics.drawscope.Stroke(hgt * 0.04f))
    }
    // The flame itself and its halo.
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f * k), Flame.copy(alpha = 0.6f * k * flicker), Color.Transparent), flame, hgt * 0.9f),
        hgt * 0.9f, flame, blendMode = BlendMode.Screen,
    )
}
