package lt.marcomultiservices.skaitaupats.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.marcomultiservices.skaitaupats.audio.Speaker
import lt.marcomultiservices.skaitaupats.core.Course
import lt.marcomultiservices.skaitaupats.core.Lesson
import lt.marcomultiservices.skaitaupats.core.LessonKind
import lt.marcomultiservices.skaitaupats.core.Phrases
import lt.marcomultiservices.skaitaupats.data.Progress
import kotlin.math.sin

private val ROW = 132.dp

@Composable
fun HomeScreen(
    course: Course,
    progress: Progress,
    speaker: Speaker,
    onOpen: (Int) -> Unit,
    onParents: () -> Unit,
) {
    val lessons = course.lessons
    val current = lessons.firstOrNull { !progress.finished(it.id) }?.id ?: lessons.last().id
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem((current - 2).coerceAtLeast(0)) }
    val done = lessons.count { progress.finished(it.id) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFEFD2), Palette.Cream, Color(0xFFE9F3FF)))),
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Palette.Paper).border(3.dp, Palette.Yellow, CircleShape)
                    .clickable { speaker.say(Phrases.WELCOME) },
                contentAlignment = Alignment.Center,
            ) { Wobble { Text("🦉", fontSize = 38.sp) } }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Skaitau Pats", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(3, size = 14.dp)
                    Text(
                        "  ${lessons.sumOf { progress.stars[it.id] ?: 0 }} · $done/${lessons.size}",
                        fontSize = 15.sp, color = Palette.Soft, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Pill("🔒 Tėvams", label = "Tėvams", onClick = onParents)
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val width = maxWidth
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp, top = 8.dp)) {
                itemsIndexed(lessons, key = { _, l -> l.id }) { i, lesson ->
                    LessonNode(
                        lesson = lesson,
                        x = nodeX(i, width),
                        prevX = if (i == 0) null else nodeX(i - 1, width),
                        stars = progress.stars[lesson.id] ?: 0,
                        open = progress.canOpen(lesson.id),
                        paid = progress.paidFor(lesson.id),
                        isCurrent = lesson.id == current,
                        onClick = {
                            when {
                                progress.canOpen(lesson.id) -> onOpen(lesson.id)
                                !progress.paidFor(lesson.id) -> { speaker.say(Phrases.LOCKED); onParents() }
                                else -> speaker.say(Phrases.TRY_AGAIN)
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun nodeX(i: Int, width: Dp): Dp {
    val amplitude = (width - 140.dp) / 2
    return width / 2 + amplitude * sin(i * 0.9).toFloat() * 0.9f
}

@Composable
private fun LessonNode(
    lesson: Lesson,
    x: Dp,
    prevX: Dp?,
    stars: Int,
    open: Boolean,
    paid: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val (face, edge) = when {
        !open -> Palette.Locked to Color(0xFFBDB6AA)
        lesson.kind == LessonKind.LETTER -> Palette.Orange to Palette.OrangeDark
        lesson.kind == LessonKind.READING -> Palette.Blue to Palette.BlueDark
        else -> Palette.Purple to Palette.PurpleDark
    }
    val size = if (lesson.kind == LessonKind.STORY) 96.dp else 84.dp
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ring")

    Box(Modifier.fillMaxWidth().height(ROW)) {
        // Dotted trail from the previous lesson.
        if (prevX != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    color = Palette.Line,
                    start = Offset(prevX.toPx(), -18.dp.toPx()),
                    end = Offset(x.toPx(), 14.dp.toPx()),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 30f)),
                )
            }
        }
        Column(
            Modifier.offset(x = x - size / 2).width(size).padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCurrent && open) {
                    Canvas(Modifier.size(size).scale(ring)) {
                        drawCircle(Palette.Yellow.copy(alpha = 0.55f), style = Stroke(6.dp.toPx()))
                    }
                }
                Chunky(face, edge, Modifier.size(size), corner = size / 2, label = "Pamoka ${lesson.id}", onClick = onClick) {
                    Text(
                        lesson.badge,
                        color = if (open) Color.White else Color(0xFF8E877B),
                        fontSize = if (lesson.kind == LessonKind.LETTER) 32.sp else 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
                if (!paid) Text("🔒", fontSize = 22.sp, modifier = Modifier.align(Alignment.TopEnd))
            }
            Spacer(Modifier.height(4.dp))
            when {
                stars > 0 -> StarRow(stars, size = 16.dp)
                lesson.kind == LessonKind.STORY -> Text(
                    lesson.title, fontSize = 12.sp, color = Palette.Soft, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}
