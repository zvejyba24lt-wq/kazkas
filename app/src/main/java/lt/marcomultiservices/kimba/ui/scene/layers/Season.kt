package lt.marcomultiservices.kimba.ui.scene.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.withTransform
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.SceneColors
import lt.marcomultiservices.kimba.ui.scene.drawArt
import lt.marcomultiservices.kimba.ui.scene.SceneGeometry
import kotlin.math.sin

/*
 * The season on the lake, from the real month: autumn leaves on the water and drifting down,
 * willow and poplar fluff in late spring. Water lilies only show from May to September.
 */

private fun seasonRandom(k: Int, salt: Int): Float {
    var x = k * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFFFF) / 16777215f
}

private val autumn = listOf(Color(0xFFD9822B), Color(0xFFC4472B), Color(0xFFE8B13A), Color(0xFF9C5A2A))

/** Lilies grow only in the warm months. */
fun liliesIn(month: Int): Boolean = month in 5..9

/**
 * September to November: leaves lying on the water and a few falling, more in the wind. Picture
 * slots `leaf_1`, `leaf_2`, ... (any number: maple, oak, birch, aspen in red, yellow, orange and
 * brown) are mixed at random; without them simple coloured leaves are drawn.
 */
fun DrawScope.drawAutumn(geo: SceneGeometry, colors: SceneColors, t: Float, month: Int, onWater: Boolean) {
    if (month !in 9..11) return
    val leaves = ArtPack.variants("leaf")
    val amount = when (month) { 9 -> 0.4f; 10 -> 1f; else -> 0.7f }
    if (onWater) {
        val n = (26 * amount).toInt()
        for (k in 0 until n) {
            val y = geo.hy + (geo.h - geo.hy) * (0.05f + 0.85f * seasonRandom(k, 3))
            val s = geo.scaleAt(y)
            val drift = (t * 0.004f * (0.5f + seasonRandom(k, 5))) % 1f
            val x = geo.w * ((seasonRandom(k, 7) + drift) % 1.1f - 0.05f)
            val r = geo.w * 0.012f * s
            val col = colors.lit(autumn[k % autumn.size])
            if (leaves.isNotEmpty()) {
                // Lying flat on the water: squashed by the angle of view.
                val img = leaves[(seasonRandom(k, 21) * leaves.size).toInt().coerceAtMost(leaves.size - 1)]
                withTransform({ scale(1f, 0.45f, Offset(x, y)); rotate(seasonRandom(k, 9) * 360f, Offset(x, y)) }) {
                    drawArt(img, Rect(x - r * 1.3f, y - r * 1.3f, x + r * 1.3f, y + r * 1.3f), colors.lit(Color.White))
                }
            } else rotate(seasonRandom(k, 9) * 360f, Offset(x, y)) {
                drawOval(col, Offset(x - r, y - r * 0.35f), Size(r * 2f, r * 0.7f))
            }
        }
    }
    // Falling leaves, swaying as they come down.
    val falling = (8 * amount + colors.wind * 0.8f).toInt().coerceAtMost(18)
    for (k in 0 until falling) {
        val speed = 0.05f + 0.04f * seasonRandom(k, 11)
        val ph = (t * speed + seasonRandom(k, 13)) % 1f
        val y = geo.h * (ph * 0.95f)
        val x = geo.w * ((seasonRandom(k, 17) + ph * colors.wind * 0.03f) % 1f) + sin(t * 2f + k) * geo.w * 0.02f
        val r = geo.w * 0.009f * (0.6f + 0.8f * (y / geo.h))
        val spin = t * 90f * (0.5f + seasonRandom(k, 19)) + k * 40f
        if (leaves.isNotEmpty()) {
            // Tumbling: it turns in the air and flips over as it falls.
            val img = leaves[k % leaves.size]
            val flip = kotlin.math.cos(t * (2f + seasonRandom(k, 23) * 2f) + k).coerceIn(-1f, 1f)
            withTransform({ rotate(spin, Offset(x, y)); scale(flip.let { if (kotlin.math.abs(it) < 0.15f) 0.15f else it }, 1f, Offset(x, y)) }) {
                drawArt(img, Rect(x - r * 1.4f, y - r * 1.4f, x + r * 1.4f, y + r * 1.4f), colors.lit(Color.White))
            }
        } else rotate(spin, Offset(x, y)) {
            drawOval(colors.lit(autumn[(k + 1) % autumn.size]), Offset(x - r, y - r * 0.4f), Size(r * 2f, r * 0.8f))
        }
    }
}

/** Late May and June: white fluff from willows and poplars floating in the air. */
fun DrawScope.drawFluff(geo: SceneGeometry, colors: SceneColors, t: Float, month: Int, day: Int) {
    if (!(month == 6 || (month == 5 && day >= 15))) return
    for (k in 0 until 24) {
        val ph = (t * (0.015f + 0.01f * seasonRandom(k, 23)) + seasonRandom(k, 29)) % 1f
        val x = geo.w * ((seasonRandom(k, 31) + ph * 0.6f) % 1f)
        val y = geo.h * (0.1f + 0.8f * seasonRandom(k, 37)) + sin(t * 0.8f + k) * geo.h * 0.03f
        drawCircle(Color.White.copy(alpha = 0.55f * colors.daylight + 0.15f), 1.5f + 2.5f * seasonRandom(k, 41), Offset(x, y))
    }
}
