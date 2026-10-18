package lt.marcomultiservices.skaitaupats.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import lt.marcomultiservices.skaitaupats.core.Exercise
import lt.marcomultiservices.skaitaupats.core.Phrases

/** The letter's shape on the drawing square, as points the child's finger should pass over. */
private class Glyph(val paint: Paint, val x: Float, val baseline: Float, val points: List<Offset>, val brush: Float)

private fun glyphFor(letter: String, size: IntSize): Glyph {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = size.height * 0.78f
    }
    val fm = paint.fontMetrics
    val x = size.width / 2f
    val baseline = size.height / 2f - (fm.ascent + fm.descent) / 2f
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ALPHA_8)
    android.graphics.Canvas(bitmap).drawText(letter, x, baseline, paint)
    val brush = paint.textSize * 0.13f
    val step = (brush * 0.5f).toInt().coerceAtLeast(2)
    val points = buildList {
        for (py in 0 until size.height step step) for (px in 0 until size.width step step) {
            if ((bitmap.getPixel(px, py) ushr 24) > 128) add(Offset(px.toFloat(), py.toFloat()))
        }
    }
    bitmap.recycle()
    return Glyph(paint, x, baseline, points, brush)
}

/**
 * The child writes the letter with a finger over a pale guide. It counts as written once most of
 * the letter is covered without scribbling all over the square. "Toliau" appears after a few tries,
 * so nobody gets stuck here.
 */
@Composable
fun TraceLetterView(e: Exercise.TraceLetter, host: ExerciseHost) {
    val letter = e.letter.letter
    var size by remember { mutableStateOf(IntSize.Zero) }
    val glyph = remember(size) { if (size.width > 0) glyphFor(letter, size) else null }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val covered = remember(glyph) { BooleanArray(glyph?.points?.size ?: 0) }
    var inside by remember { mutableIntStateOf(0) }
    var outside by remember { mutableIntStateOf(0) }
    var tries by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(12_000); waited = true }

    fun clear() {
        strokes.clear(); current = emptyList(); covered.fill(false); inside = 0; outside = 0
    }

    fun touch(p: Offset) {
        val g = glyph ?: return
        val r2 = g.brush * g.brush
        var near = false
        g.points.forEachIndexed { i, q ->
            val d = (q - p).getDistanceSquared()
            if (d < r2) { covered[i] = true; near = true } else if (d < 4 * r2) near = true
        }
        if (near) inside++ else outside++
    }

    fun check() {
        val g = glyph ?: return
        tries++
        val coverage = covered.count { it }.toFloat() / g.points.size.coerceAtLeast(1)
        val messy = outside > (inside + outside) * 0.35f
        when {
            messy && coverage > 0.5f -> { host.speaker.say(Phrases.TRY_AGAIN); clear() }
            coverage >= 0.72f -> { solved = true; host.correct(emptyList()) }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoundButton(size = 52.dp, onClick = { host.speaker.say(Phrases.TRACE) })
            Text("Parašyk pirštu", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Soft)
        }
        Box(
            Modifier.fillMaxWidth(0.86f).aspectRatio(1f).clip(RoundedCornerShape(36.dp)).background(Palette.Paper)
                .border(4.dp, Palette.Line, RoundedCornerShape(36.dp))
                .onSizeChanged { size = it }
                .pointerInput(glyph, solved) {
                    if (solved) return@pointerInput
                    detectDragGestures(
                        onDragStart = { current = listOf(it); touch(it) },
                        onDrag = { change, _ -> current = current + change.position; touch(change.position) },
                        onDragEnd = { strokes += current; current = emptyList(); check() },
                        onDragCancel = { strokes += current; current = emptyList() },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val g = glyph ?: return@Canvas
                // Writing lines like in a school copybook.
                val lineColor = Palette.Line
                drawLine(lineColor, Offset(0f, g.baseline), Offset(this.size.width, g.baseline), strokeWidth = 3.dp.toPx())
                drawIntoCanvas { c ->
                    g.paint.color = Color(0xFFFFE3C7).toArgb()
                    c.nativeCanvas.drawText(letter, g.x, g.baseline, g.paint)
                }
                val ink = if (solved) Palette.Green else Palette.Orange
                (strokes + listOf(current)).filter { it.isNotEmpty() }.forEach { pts ->
                    if (pts.size == 1) {
                        drawCircle(ink, g.brush / 2, pts[0])
                    } else {
                        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                        drawPath(path, ink, style = Stroke(width = g.brush, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("↺", label = "Iš naujo", onClick = { clear() })
            if (!solved && (tries >= 3 || waited)) NextButton(onClick = host.next)
        }
    }
}
