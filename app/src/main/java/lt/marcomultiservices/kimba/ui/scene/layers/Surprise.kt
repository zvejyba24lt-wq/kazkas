package lt.marcomultiservices.kimba.ui.scene.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import lt.marcomultiservices.kimba.core.Surprise
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.Palette
import lt.marcomultiservices.kimba.ui.scene.drawArt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * What else comes up on the line. Picture slots (see docs/GRAFIKA.md): `surprise_boot`,
 * `surprise_crayfish`, `surprise_rod`, `surprise_bottle`, each on a transparent background,
 * hanging from the line at the top centre of the picture.
 */

fun surpriseSlot(s: Surprise) = when (s) {
    Surprise.BOOT -> "surprise_boot"
    Surprise.CRAYFISH -> "surprise_crayfish"
    Surprise.SUNKEN_ROD -> "surprise_rod"
    Surprise.BOTTLE -> "surprise_bottle"
}

/** The object hanging from the line at [mouth] as it is lifted out ([l] 0..1), swaying and dripping. */
fun DrawScope.drawSurpriseLifted(mouth: Offset, w: Float, h: Float, t: Float, l: Float, s: Surprise) {
    val size = w * (0.16f + 0.24f * l)
    if (l < 0.5f) {
        val base = Offset(w * 0.58f, h * 0.9f)
        for (i in 0 until 10) {
            val k = (l * 2f + i * 0.09f) % 1f
            drawCircle(Palette.Spray.copy(alpha = (1 - k) * 0.7f), 3f + 4f * (1 - k), Offset(base.x + cos(i * 2.1f) * size * 0.4f * k, base.y - sin(k * PI.toFloat()) * size * 0.4f))
        }
    }
    val sway = sin(t * 3f) * 8f
    rotate(sway, mouth) {
        drawSurprise(Rect(mouth.x - size / 2, mouth.y, mouth.x + size / 2, mouth.y + size), s, t)
    }
    for (i in 0 until 4) {
        val k = (t * 1.6f + i * 0.25f) % 1f
        drawCircle(Palette.Spray.copy(alpha = (1f - k) * 0.7f), 2f + 2f * l, Offset(mouth.x + sin(i * 1.9f) * size * 0.2f, mouth.y + size * (1f + k * 0.4f)))
    }
}

/** The object fitted into [r], its top centre where the line holds it. */
fun DrawScope.drawSurprise(r: Rect, s: Surprise, t: Float) {
    ArtPack[surpriseSlot(s)]?.let { img ->
        val h = r.width * img.height / img.width
        drawArt(img, Rect(r.left, r.top, r.right, r.top + h))
        return
    }
    val u = r.width
    val x = r.left
    val y = r.top
    when (s) {
        Surprise.BOOT -> {
            // A rubber boot hanging heel down, with weed on it.
            val boot = Path().apply {
                moveTo(x + u * 0.35f, y + u * 0.05f); lineTo(x + u * 0.62f, y + u * 0.05f)
                lineTo(x + u * 0.62f, y + u * 0.62f); quadraticTo(x + u * 0.95f, y + u * 0.66f, x + u * 0.95f, y + u * 0.84f)
                lineTo(x + u * 0.95f, y + u * 0.92f); lineTo(x + u * 0.3f, y + u * 0.92f)
                quadraticTo(x + u * 0.33f, y + u * 0.5f, x + u * 0.35f, y + u * 0.05f); close()
            }
            drawPath(boot, Brush.horizontalGradient(listOf(Color(0xFF2D3A2A), Color(0xFF4F6146), Color(0xFF2A3326)), x + u * 0.3f, x + u * 0.95f))
            drawRect(Color(0xFF1C241A), Offset(x + u * 0.3f, y + u * 0.88f), Size(u * 0.65f, u * 0.06f))
            drawLine(Color(0xFF6F8F3A), Offset(x + u * 0.4f, y + u * 0.1f), Offset(x + u * 0.36f, y + u * 0.45f), strokeWidth = u * 0.03f, cap = StrokeCap.Round)
            drawLine(Color(0xFF6F8F3A), Offset(x + u * 0.56f, y + u * 0.08f), Offset(x + u * 0.62f, y + u * 0.36f), strokeWidth = u * 0.025f, cap = StrokeCap.Round)
        }
        Surprise.CRAYFISH -> {
            // Hanging by a claw: body, tail fan, two claws, feelers that twitch.
            val c = Offset(x + u * 0.5f, y + u * 0.55f)
            val red = Color(0xFF6E3B24)
            drawOval(red, Offset(c.x - u * 0.12f, c.y - u * 0.22f), Size(u * 0.24f, u * 0.4f))
            for (i in 0 until 4) drawOval(Color(0xFF5A2F1C), Offset(c.x - u * 0.1f, c.y + u * (0.14f + i * 0.07f)), Size(u * 0.2f, u * 0.08f))
            drawPath(Path().apply { moveTo(c.x, c.y + u * 0.42f); lineTo(c.x - u * 0.12f, c.y + u * 0.5f); lineTo(c.x + u * 0.12f, c.y + u * 0.5f); close() }, red)
            for (side in listOf(-1f, 1f)) {
                drawLine(red, Offset(c.x + side * u * 0.08f, c.y - u * 0.18f), Offset(c.x + side * u * 0.2f, c.y - u * 0.36f), strokeWidth = u * 0.05f, cap = StrokeCap.Round)
                drawOval(red, Offset(c.x + side * u * 0.2f - u * 0.07f, c.y - u * 0.5f), Size(u * 0.14f, u * 0.2f))
                val tw = sin(t * 7f + side) * u * 0.04f
                drawLine(Color(0xFF8A5A3A), Offset(c.x + side * u * 0.03f, c.y - u * 0.22f), Offset(c.x + side * u * 0.3f + tw, c.y - u * 0.55f), strokeWidth = u * 0.012f)
            }
        }
        Surprise.SUNKEN_ROD -> {
            // A length of old rod with a reel and a weedy line, a lure dangling at the end.
            drawLine(Color(0xFF3A2C22), Offset(x + u * 0.5f, y), Offset(x + u * 0.42f, y + u), strokeWidth = u * 0.05f, cap = StrokeCap.Round)
            drawCircle(Color(0xFF7A7F86), u * 0.09f, Offset(x + u * 0.47f, y + u * 0.62f))
            drawCircle(Color(0xFF4A4E54), u * 0.05f, Offset(x + u * 0.47f, y + u * 0.62f))
            drawLine(Color(0xFF6F8F3A), Offset(x + u * 0.48f, y + u * 0.3f), Offset(x + u * 0.6f, y + u * 0.42f), strokeWidth = u * 0.02f)
            val lure = Offset(x + u * 0.72f + sin(t * 4f) * u * 0.03f, y + u * 0.85f)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(x + u * 0.5f, y + u * 0.05f), lure, strokeWidth = 1.2f)
            drawOval(Brush.horizontalGradient(listOf(Color(0xFFE8C04A), Color(0xFFD0553A))), Offset(lure.x - u * 0.07f, lure.y - u * 0.03f), Size(u * 0.14f, u * 0.06f))
        }
        Surprise.BOTTLE -> {
            // A green glass bottle with a cork and a rolled letter inside.
            val glass = Brush.horizontalGradient(listOf(Color(0xCC1F5A3A), Color(0xCC5FA07A), Color(0xCC1F5A3A)), x + u * 0.3f, x + u * 0.7f)
            drawRect(Color(0xFF9C7448), Offset(x + u * 0.45f, y), Size(u * 0.1f, u * 0.08f))
            drawRect(glass, Offset(x + u * 0.44f, y + u * 0.08f), Size(u * 0.12f, u * 0.18f))
            drawRoundRect(glass, Offset(x + u * 0.3f, y + u * 0.26f), Size(u * 0.4f, u * 0.68f), androidx.compose.ui.geometry.CornerRadius(u * 0.12f))
            drawRect(Color(0xFFEFE2C0), Offset(x + u * 0.42f, y + u * 0.38f), Size(u * 0.16f, u * 0.42f))
            drawLine(Color(0xFFB0895A), Offset(x + u * 0.42f, y + u * 0.55f), Offset(x + u * 0.58f, y + u * 0.55f), strokeWidth = u * 0.02f)
            drawLine(Color.White.copy(alpha = 0.5f), Offset(x + u * 0.35f, y + u * 0.32f), Offset(x + u * 0.35f, y + u * 0.8f), strokeWidth = u * 0.025f, cap = StrokeCap.Round)
        }
    }
}

/** A dark shape and swirl under the water where something heavy but lifeless is dragged in. */
fun DrawScope.drawDraggedThing(pos: Offset, scale: Float, t: Float) {
    val len = 60f * scale
    drawOval(Color(0x55101A1E), Offset(pos.x - len / 2, pos.y + len * 0.05f), Size(len, len * 0.3f))
    val k = (t * 1.1f) % 1f
    drawOval(Color.White.copy(alpha = (1 - k) * 0.3f), Offset(pos.x - len * k, pos.y - len * 0.25f * k), Size(len * 2 * k, len * 0.5f * k), style = Stroke(1.5f))
}
