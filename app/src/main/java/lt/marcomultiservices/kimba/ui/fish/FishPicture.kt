package lt.marcomultiservices.kimba.ui.fish

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Real fish photos. Drop a cut-out picture (transparent background, head to the left) into
 * `app/src/main/assets/fish/<species id>.webp` (or .png/.jpg) and it replaces the drawing
 * everywhere: catch card and fish book. Golden fish use the same photo with a gold tone.
 */
object FishPhotos {
    private val cache = HashMap<String, ImageBitmap?>()
    private var names: Set<String>? = null

    fun get(context: Context, species: String): ImageBitmap? = synchronized(cache) {
        cache.getOrPut(species) {
            val files = names ?: (context.assets.list("fish")?.toSet() ?: emptySet()).also { names = it }
            val file = listOf("webp", "png", "jpg").map { "$species.$it" }.firstOrNull { it in files } ?: return@getOrPut null
            runCatching {
                context.assets.open("fish/$file").use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                    BitmapFactory.decodeStream(input, null, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    /** Warm gold tone for golden fish photos. */
    val goldFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.55f, 0.45f, 0.15f, 0f, 30f,
                0.40f, 0.42f, 0.10f, 0f, 18f,
                0.10f, 0.15f, 0.10f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )

    /** Black silhouette for fish not caught yet. */
    val silhouetteFilter: ColorFilter = ColorFilter.colorMatrix(
        ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 20f, 0f, 0f, 0f, 0f, 30f, 0f, 0f, 0f, 0f, 45f, 0f, 0f, 0f, 0.8f, 0f)),
    )
}

/** The fish's photo if one was added, otherwise its drawing (gently swimming when [time] is given). */
@Composable
fun FishPicture(species: String, golden: Boolean, time: MutableFloatState?, modifier: Modifier = Modifier, silhouette: Boolean = false) {
    val context = LocalContext.current
    val photo = remember(species) { FishPhotos.get(context, species) }
    if (photo != null) {
        Box(modifier) {
            Image(
                photo, null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit,
                colorFilter = when {
                    silhouette -> FishPhotos.silhouetteFilter
                    golden -> FishPhotos.goldFilter
                    else -> null
                },
            )
        }
        return
    }
    Canvas(modifier) {
        val t = time?.floatValue ?: 0f
        val bob = if (time != null) kotlin.math.sin(t * 1.6f) * 4f else 0f
        val w = size.width * 0.88f
        FishArt.draw(this, species, golden, Rect(Offset((size.width - w) / 2, size.height / 2 - w / 2 + bob), Size(w, w)), t, silhouette)
    }
}
