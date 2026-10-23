package lt.marcomultiservices.kimba.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import lt.marcomultiservices.kimba.core.Catch
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Lang
import lt.marcomultiservices.kimba.ui.S
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The picture a player shares after a good catch: the painted lake at dusk, the fish, its
 * weight and the real weather, and the player's friend code, so whoever sees it can join.
 */
object ShareCard {
    private const val PLAY_URL = "https://play.google.com/store/apps/details?id=lt.marcomultiservices.kimba"
    private const val W = 1080
    private const val H = 1350

    /** Store link that carries the friend code; Google Play hands it to the game on first start. */
    fun link(code: String): String = if (code.isEmpty()) PLAY_URL else "$PLAY_URL&referrer=kimba%3D$code"

    fun weight(c: Catch, lang: Lang): String {
        val locale = if (lang == Lang.EN) Locale.UK else Locale.GERMANY
        return if (c.grams >= 1000) String.format(locale, "%.2f kg", c.grams / 1000.0) else "${c.grams} g"
    }

    fun name(c: Catch, lang: Lang): String =
        c.golden?.let { Fish.goldenById(it)?.name?.get(lang) } ?: Fish.byId(c.species)?.name?.get(lang) ?: ""

    fun render(context: Context, c: Catch, lang: Lang, code: String): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val horizon = 760f

        // Dusk sky over the painted shore, and the water below with the shore mirrored in it.
        p.shader = LinearGradient(0f, 0f, 0f, horizon, intArrayOf(0xFF101C30.toInt(), 0xFF34486A.toInt(), 0xFFE2A25E.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, W.toFloat(), horizon, p)
        p.shader = LinearGradient(0f, horizon, 0f, H.toFloat(), intArrayOf(0xFF3A5363.toInt(), 0xFF0C1B27.toInt()), null, Shader.TileMode.CLAMP)
        cv.drawRect(0f, horizon, W.toFloat(), H.toFloat(), p)
        p.shader = null
        val shore = asset(context, "art/shore_${c.water.name.lowercase()}.webp") ?: asset(context, "art/shore_lake.webp")
        if (shore != null) {
            val h = shore.height * W / shore.width.toFloat()
            val dst = RectF(0f, horizon - h, W.toFloat(), horizon)
            cv.drawBitmap(shore, null, dst, p)
            cv.save()
            cv.scale(1f, -1f, 0f, horizon)
            p.alpha = 70
            cv.drawBitmap(shore, null, dst, p)
            p.alpha = 255
            cv.restore()
        }
        // The low sun's path on the water.
        p.shader = RadialGradient(W / 2f, horizon + 40f, 520f, intArrayOf(0x66F3B56A, 0x00F3B56A), null, Shader.TileMode.CLAMP)
        cv.drawRect(0f, horizon, W.toFloat(), H.toFloat(), p)
        // Darker top so the title reads on any sky.
        p.shader = LinearGradient(0f, 0f, 0f, 340f, intArrayOf(0xCC0A1424.toInt(), 0x000A1424), null, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, W.toFloat(), 340f, p)
        p.shader = null

        val serif = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val golden = c.golden != null
        val amber = if (golden) 0xFFFFD27A.toInt() else 0xFFF2B25C.toInt()

        text(cv, S.shareHeader[lang], W / 2f, 108f, 40f, amber, sans, Paint.Align.CENTER, spacing = 0.28f)
        text(cv, name(c, lang), W / 2f, 210f, 96f, 0xFFFFFFFF.toInt(), serif, Paint.Align.CENTER, maxWidth = 980f, shadow = true)
        val species = Fish.byId(c.species)
        val trophy = species != null && (c.cm - species.minCm) / (species.maxCm - species.minCm).toDouble() >= 0.75
        val tag = when {
            golden -> S.rarityVeryRare[lang]
            trophy -> S.trophy[lang]
            else -> null
        }
        if (tag != null) {
            p.textSize = 30f; p.typeface = sans
            val tw = p.measureText(tag.uppercase())
            p.color = 0x33FFD27A; cv.drawRoundRect(RectF(W / 2f - tw / 2 - 26, 248f, W / 2f + tw / 2 + 26, 298f), 25f, 25f, p)
            text(cv, tag.uppercase(), W / 2f, 284f, 30f, 0xFFFFD27A.toInt(), sans, Paint.Align.CENTER, spacing = 0.12f)
        }

        // The fish itself, lying across the water with a soft shadow.
        val fish = asset(context, "fish/${c.species}.webp")
        if (fish != null) {
            val fw = 900f
            val fh = fish.height * fw / fish.width
            val cy = 905f
            p.shader = RadialGradient(W / 2f, cy + fh * 0.42f, fw * 0.5f, intArrayOf(0x88000000.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
            cv.save(); cv.scale(1f, 0.18f, W / 2f, cy + fh * 0.42f)
            cv.drawCircle(W / 2f, cy + fh * 0.42f, fw * 0.5f, p)
            cv.restore()
            p.shader = null
            if (golden) p.colorFilter = ColorMatrixColorFilter(GOLD)
            cv.drawBitmap(fish, null, RectF(W / 2f - fw / 2, cy - fh / 2, W / 2f + fw / 2, cy + fh / 2), p)
            p.colorFilter = null
        }

        text(cv, "${weight(c, lang)}   ·   ${c.cm.roundToInt()} cm", W / 2f, 1128f, 80f, 0xFFFFFFFF.toInt(), sans, Paint.Align.CENTER, shadow = true)
        val clock = java.time.Instant.ofEpochMilli(c.epochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        val weather = S.caughtIn[lang].format(S.sky(c.sky)[lang], c.tempC.roundToInt(), "%02d:%02d".format(clock.hour, clock.minute))
        text(cv, weather, W / 2f, 1186f, 34f, 0xFFC4DAEA.toInt(), sans, Paint.Align.CENTER)

        // The game's name and the friend code along the bottom.
        p.color = 0xE60A1620.toInt()
        cv.drawRect(0f, 1232f, W.toFloat(), H.toFloat(), p)
        p.color = amber
        cv.drawRect(0f, 1232f, W.toFloat(), 1236f, p)
        text(cv, S.appName[lang], 56f, 1295f, 56f, amber, serif, Paint.Align.LEFT)
        text(cv, S.tagline[lang], 58f, 1330f, 26f, 0xFFB6C6D2.toInt(), sans, Paint.Align.LEFT)
        if (code.isNotEmpty()) {
            text(cv, S.shareCodeLabel[lang].uppercase(), W - 56f, 1280f, 24f, 0xFFB6C6D2.toInt(), sans, Paint.Align.RIGHT, spacing = 0.15f)
            text(cv, code, W - 56f, 1328f, 48f, 0xFFFFFFFF.toInt(), sans, Paint.Align.RIGHT, spacing = 0.18f)
        }
        return bmp
    }

    /** Opens the share sheet with the picture and a message. */
    fun send(context: Context, bmp: Bitmap, text: String, chooser: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "kimba.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".share", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooser))
    }

    /** Opens the share sheet with a message only. */
    fun sendText(context: Context, text: String, chooser: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, chooser))
    }

    private fun asset(context: Context, path: String): Bitmap? =
        runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it) } }.getOrNull()

    private fun text(
        cv: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, face: Typeface, align: Paint.Align,
        maxWidth: Float = 0f, spacing: Float = 0f, shadow: Boolean = false,
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.typeface = face
        p.textSize = size
        p.color = color
        p.textAlign = align
        p.letterSpacing = spacing
        if (maxWidth > 0f) {
            val w = p.measureText(s)
            if (w > maxWidth) p.textSize = size * maxWidth / w
        }
        if (shadow) p.setShadowLayer(14f, 0f, 4f, 0xAA000000.toInt())
        cv.drawText(s, x, y, p)
    }

    /** Same warm gold tone as the golden fish in the game. */
    private val GOLD = ColorMatrix(
        floatArrayOf(
            0.55f, 0.45f, 0.15f, 0f, 30f,
            0.40f, 0.42f, 0.10f, 0f, 18f,
            0.10f, 0.15f, 0.10f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}
