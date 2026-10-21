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
    private val cache = HashMap<String, ImageBitmap?>()

    /** Called once when the app starts. */
    fun init(context: Context) {
        this.context = context.applicationContext
        files = runCatching { context.assets.list("art")?.toSet() }.getOrNull() ?: emptySet()
    }

    /** The picture for [slot], or null to draw it in code. */
    operator fun get(slot: String): ImageBitmap? {
        if (files.isEmpty()) return null
        return synchronized(cache) {
            cache.getOrPut(slot) {
                val ctx = context ?: return@getOrPut null
                val file = listOf("webp", "png", "jpg").map { "$slot.$it" }.firstOrNull { it in files } ?: return@getOrPut null
                runCatching { ctx.assets.open("art/$file").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
            }
        }
    }
}

/**
 * Draws [img] stretched into [rect]. [light] tints it like the code-drawn parts (pass
 * `colors.lit(Color.White)` so it darkens at night and warms at sunset); white keeps it as is.
 */
fun DrawScope.drawArt(img: ImageBitmap, rect: Rect, light: Color = Color.White, alpha: Float = 1f) {
    drawImage(
        img,
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt().coerceAtLeast(1), rect.height.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
        colorFilter = if (light == Color.White) null else ColorFilter.lighting(light, Color.Black),
    )
}
