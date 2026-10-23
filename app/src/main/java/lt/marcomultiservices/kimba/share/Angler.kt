package lt.marcomultiservices.kimba.share

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader

/*
 * The angler in the trophy photo: from the waist up, smiling, both arms held out with the fish
 * across the hands. Used by the catch card in the game and by the picture players share.
 *
 * Picture slot (see docs/GRAFIKA.md): `angler` — an angler from the waist up, facing us, forearms
 * held forward with open hands at 15 % and 85 % of the width and 63 % of the height, transparent
 * around him. The fish is laid across the hands by code. Without it a simple painted figure is
 * drawn here.
 */
object Angler {
    /** Where the hands are in the `angler` picture, as fractions of its width and height. */
    const val HAND_L = 0.15f
    const val HAND_R = 0.855f
    const val HAND_Y = 0.63f

    /** The `angler` picture, its seasonal version first (`angler_winter` wears winter clothes). */
    @Suppress("UNUSED_PARAMETER")
    private fun art(context: Context): Bitmap? = runCatching { lt.marcomultiservices.kimba.ui.scene.ArtPack["angler"]?.asAndroidBitmap() }.getOrNull()

    /** Hand positions for a figure in [box]: left x, right x, y. */
    fun hands(box: RectF): Triple<Float, Float, Float> =
        Triple(box.left + box.width() * HAND_L, box.left + box.width() * HAND_R, box.top + box.height() * HAND_Y)

    /** Draws the angler's body in [box] (its bottom is the waist, cut by the frame). */
    fun body(context: Context, cv: Canvas, box: RectF, night: Float = 0f) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        art(context)?.let {
            cv.drawBitmap(it, null, box, p)
            return
        }
        val u = box.width() / 10f
        val cx = box.centerX()
        val top = box.top
        val (hl, hr, hy) = hands(box)
        val jacket = shade(0xFF56653F.toInt(), night)
        val jacketDark = shade(0xFF3C482B.toInt(), night)
        val skin = shade(0xFFE2B08A.toInt(), night)
        val skinDark = shade(0xFFB98563.toInt(), night)

        // Torso: a padded jacket with rounded shoulders, cut off by the bottom of the box.
        val torso = Path().apply {
            moveTo(cx - 2.0f * u, top + 3.3f * u)
            cubicTo(cx - 3.3f * u, top + 3.4f * u, cx - 3.6f * u, top + 4.6f * u, cx - 3.5f * u, top + 6.0f * u)
            lineTo(cx - 3.3f * u, box.bottom)
            lineTo(cx + 3.3f * u, box.bottom)
            lineTo(cx + 3.5f * u, top + 6.0f * u)
            cubicTo(cx + 3.6f * u, top + 4.6f * u, cx + 3.3f * u, top + 3.4f * u, cx + 2.0f * u, top + 3.3f * u)
            close()
        }
        p.color = jacket
        cv.drawPath(torso, p)
        // Zip, collar and chest pockets.
        p.color = jacketDark
        p.strokeWidth = u * 0.12f
        p.style = Paint.Style.STROKE
        cv.drawLine(cx, top + 3.6f * u, cx, box.bottom, p)
        cv.drawRect(cx - 2.4f * u, top + 5.0f * u, cx - 0.8f * u, top + 6.2f * u, p)
        cv.drawRect(cx + 0.8f * u, top + 5.0f * u, cx + 2.4f * u, top + 6.2f * u, p)
        p.style = Paint.Style.FILL
        val collar = Path().apply {
            moveTo(cx - 1.4f * u, top + 3.1f * u); lineTo(cx, top + 4.0f * u); lineTo(cx + 1.4f * u, top + 3.1f * u)
            lineTo(cx + 1.0f * u, top + 3.6f * u); lineTo(cx, top + 4.4f * u); lineTo(cx - 1.0f * u, top + 3.6f * u); close()
        }
        cv.drawPath(collar, p)

        // Arms: upper arm down the side, forearm forward to the hand.
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = u * 1.25f
        p.color = jacket
        for (side in listOf(-1f, 1f)) {
            val hx = if (side < 0) hl else hr
            val arm = Path().apply {
                moveTo(cx + side * 2.6f * u, top + 3.9f * u)
                quadTo(cx + side * 3.6f * u, hy - 0.4f * u, cx + side * 3.1f * u, hy + 0.2f * u)
                lineTo(hx, hy + 0.15f * u)
            }
            cv.drawPath(arm, p)
        }
        p.style = Paint.Style.FILL

        // Neck and head.
        p.color = skinDark
        cv.drawRect(cx - 0.6f * u, top + 2.4f * u, cx + 0.6f * u, top + 3.5f * u, p)
        p.shader = RadialGradient(cx - 0.3f * u, top + 1.6f * u, 1.6f * u, skin, skinDark, Shader.TileMode.CLAMP)
        cv.drawOval(RectF(cx - 1.05f * u, top + 0.55f * u, cx + 1.05f * u, top + 3.0f * u), p)
        p.shader = null
        // A short beard along the jaw.
        p.color = shade(0xFF6B4A33.toInt(), night)
        val beard = Path().apply {
            moveTo(cx - 1.02f * u, top + 1.9f * u)
            quadTo(cx - 0.9f * u, top + 3.15f * u, cx, top + 3.15f * u)
            quadTo(cx + 0.9f * u, top + 3.15f * u, cx + 1.02f * u, top + 1.9f * u)
            quadTo(cx + 0.6f * u, top + 2.55f * u, cx, top + 2.5f * u)
            quadTo(cx - 0.6f * u, top + 2.55f * u, cx - 1.02f * u, top + 1.9f * u)
            close()
        }
        cv.drawPath(beard, p)
        // Eyes and a broad smile: a good fish.
        p.color = 0xFF2A1E16.toInt()
        cv.drawCircle(cx - 0.4f * u, top + 1.65f * u, u * 0.09f, p)
        cv.drawCircle(cx + 0.4f * u, top + 1.65f * u, u * 0.09f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = u * 0.1f
        cv.drawArc(RectF(cx - 0.45f * u, top + 1.85f * u, cx + 0.45f * u, top + 2.45f * u), 20f, 140f, false, p)
        p.style = Paint.Style.FILL
        // Cap with a peak.
        p.color = shade(0xFF2F3D2A.toInt(), night)
        cv.drawArc(RectF(cx - 1.12f * u, top + 0.2f * u, cx + 1.12f * u, top + 2.0f * u), 180f, 180f, true, p)
        cv.drawOval(RectF(cx - 1.3f * u, top + 0.95f * u, cx + 1.5f * u, top + 1.35f * u), p)
    }

    /** The hands, drawn over the fish so the fingers close round it. */
    fun hands(context: Context, cv: Canvas, box: RectF, night: Float = 0f) {
        if (art(context) != null) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val u = box.width() / 10f
        val (hl, hr, hy) = hands(box)
        for (x in listOf(hl, hr)) {
            p.color = shade(0xFFD9A47E.toInt(), night)
            cv.drawOval(RectF(x - 0.55f * u, hy - 0.45f * u, x + 0.55f * u, hy + 0.55f * u), p)
            // Fingers curled over the top of the fish.
            p.color = shade(0xFFC48E69.toInt(), night)
            for (i in 0..2) cv.drawOval(RectF(x - 0.5f * u + i * 0.34f * u, hy - 0.62f * u, x - 0.2f * u + i * 0.34f * u, hy - 0.18f * u), p)
        }
    }

    /** Darkens a colour towards night blue by [night] (0 day, 1 night). */
    private fun shade(c: Int, night: Float): Int {
        if (night <= 0f) return c
        val k = 1f - 0.55f * night
        val r = ((c shr 16 and 0xFF) * k + 10 * night).toInt().coerceIn(0, 255)
        val g = ((c shr 8 and 0xFF) * k + 18 * night).toInt().coerceIn(0, 255)
        val b = ((c and 0xFF) * k + 40 * night).toInt().coerceIn(0, 255)
        return (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }
}
