package lt.marcomultiservices.kimba.ui.scene

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Optional pictures that replace parts of the drawn scene. Put a file named after a slot into
 * `app/src/main/assets/art/` (`<slot>.webp`, `.png` or `.jpg`, transparent background) and
 * that part of the scene is drawn from the picture instead of code. Without the file the
 * procedural drawing is used, so the game always works. The slots, their sizes and anchors
 * are listed in `docs/GRAFIKA.md`.
 */
object ArtPack {
    private var context: Context? = null
    private var files: Set<String> = emptySet()
    private var fishFiles: Set<String> = emptySet()
    private val cache = HashMap<String, ImageBitmap?>()

    /** Called once when the app starts. */
    fun init(context: Context) {
        this.context = context.applicationContext
        files = runCatching { context.assets.list("art")?.toSet() }.getOrNull() ?: emptySet()
        fishFiles = runCatching { context.assets.list("fish")?.toSet() }.getOrNull() ?: emptySet()
    }

    /** The fish photo from `assets/fish/<id>.webp`, shown in the scene when a fish leaps or is landed. */
    fun fish(id: String): ImageBitmap? = load("fish", id, fishFiles)

    /** The picture for [slot], or null to draw it in code. */
    operator fun get(slot: String): ImageBitmap? = load("art", slot, files)

    private fun load(dir: String, name: String, names: Set<String>): ImageBitmap? {
        if (names.isEmpty()) return null
        return synchronized(cache) {
            cache.getOrPut("$dir/$name") {
                val ctx = context ?: return@getOrPut null
                val file = listOf("webp", "png", "jpg").map { "$name.$it" }.firstOrNull { it in names } ?: return@getOrPut null
                runCatching { ctx.assets.open("$dir/$file").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
        }
    }
}

/**
 * Draws [img] stretched into [rect]. [light] tints it like the code-drawn parts (pass
 * `colors.lit(Color.White)` so it darkens at night and warms at sunset); white keeps it as is.
 */
fun DrawScope.drawArt(img: ImageBitmap, rect: Rect, light: Color = Color.White, alpha: Float = 1f, filter: ColorFilter? = null) {
    drawImage(
        img,
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt().coerceAtLeast(1), rect.height.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
        colorFilter = filter ?: if (light == Color.White) null else ColorFilter.lighting(light, Color.Black),
    )
}

/**
 * [img] drawn in [rect] as vertical strips, each shifted up or down by [bend] (strip centre from
 * 0 at the left edge to 1 at the right, returning pixels), so a fish picture can flex its body
 * and beat its tail instead of moving like a board.
 */
fun DrawScope.drawBentArt(img: ImageBitmap, rect: Rect, bend: (Float) -> Float, light: Color = Color.White, alpha: Float = 1f, filter: ColorFilter? = null, strips: Int = 18) {
    val cf = filter ?: if (light == Color.White) null else ColorFilter.lighting(light, Color.Black)
    val sw = img.width / strips.toFloat()
    val dw = rect.width / strips
    for (i in 0 until strips) {
        val sx = (i * sw).roundToInt()
        val sxEnd = if (i == strips - 1) img.width else ((i + 1) * sw).roundToInt()
        val dy = bend((i + 0.5f) / strips)
        val dx = rect.left + i * dw
        drawImage(
            img,
            srcOffset = IntOffset(sx, 0),
            srcSize = IntSize((sxEnd - sx).coerceAtLeast(1), img.height),
            dstOffset = IntOffset(dx.roundToInt(), (rect.top + dy).roundToInt()),
            // One pixel wider so neighbouring strips never show a seam.
            dstSize = IntSize((dw + 1.5f).roundToInt().coerceAtLeast(1), rect.height.roundToInt().coerceAtLeast(1)),
            alpha = alpha,
            colorFilter = cf,
        )
    }
}

