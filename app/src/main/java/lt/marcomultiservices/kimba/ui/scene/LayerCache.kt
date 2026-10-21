package lt.marcomultiservices.kimba.ui.scene

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/**
 * Keeps a layer that does not move (shores, the pier) as a picture, so it is drawn once
 * instead of every frame. It is redrawn only when [key] changes, e.g. when the light changes.
 */
class LayerCache {
    private var key: Any? = null
    private var image: ImageBitmap? = null

    fun draw(scope: DrawScope, key: Any, area: Rect, block: DrawScope.() -> Unit) {
        val r = Rect(area.left.coerceAtLeast(0f), area.top.coerceAtLeast(0f), area.right.coerceAtMost(scope.size.width), area.bottom.coerceAtMost(scope.size.height))
        val w = r.width.roundToInt()
        val h = r.height.roundToInt()
        if (w <= 0 || h <= 0) return
        var img = image
        if (img == null || key != this.key || img.width != w || img.height != h) {
            img = ImageBitmap(w, h)
            CanvasDrawScope().draw(scope, scope.layoutDirection, Canvas(img), Size(w.toFloat(), h.toFloat())) {
                translate(-r.left, -r.top) { block() }
            }
            image = img
            this.key = key
        }
        scope.drawImage(img, Offset(r.left.roundToInt().toFloat(), r.top.roundToInt().toFloat()))
    }
}
