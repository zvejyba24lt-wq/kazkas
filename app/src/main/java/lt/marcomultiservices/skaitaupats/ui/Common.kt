package lt.marcomultiservices.skaitaupats.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A chunky, "pressable" card: a darker edge below the face sinks in when pressed. */
@Composable
fun Chunky(
    color: Color,
    edge: Color,
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    label: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val depth by animateFloatAsState(if (pressed) 1f else 6f, label = "depth")
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .semantics { if (label != null) contentDescription = label }
            .clip(shape)
            .background(edge)
            .then(
                if (onClick != null) Modifier.clickable(source, indication = null, enabled = enabled, onClick = onClick) else Modifier,
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = (6f - depth).dp, bottom = depth.dp)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** Big round speaker button that replays a sound. */
@Composable
fun SoundButton(modifier: Modifier = Modifier, size: Dp = 72.dp, speaking: Boolean = false, onClick: () -> Unit) {
    val pulse by animateFloatAsState(if (speaking) 1.12f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "pulse")
    Chunky(Palette.Blue, Palette.BlueDark, modifier.size(size).scale(pulse), corner = size / 2, label = "Klausyk", onClick = onClick) {
        Text("🔊", fontSize = (size.value * 0.42f).sp)
    }
}

/** Green "next" button. */
@Composable
fun NextButton(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.35f
    Chunky(
        Palette.Green.copy(alpha = alpha), Palette.GreenDark.copy(alpha = alpha),
        modifier.size(width = 200.dp, height = 76.dp), corner = 38.dp, label = "Toliau", enabled = enabled, onClick = onClick,
    ) {
        Text("Toliau ➜", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Shakes its content sideways once each time [trigger] changes to a new value. */
@Composable
fun Modifier.shakeOn(trigger: Int): Modifier {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) offset.animateTo(x, tween(55))
    }
    return this.graphicsLayer { translationX = offset.value * density }
}

/** A burst of stars and confetti, played once each time [trigger] increases. */
@Composable
fun Celebration(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.animateTo(1f, tween(1100, easing = LinearEasing)) }
    val bits = remember(trigger) {
        val r = Random(trigger)
        List(26) {
            Bit(
                angle = r.nextFloat() * 2f * PI.toFloat(),
                speed = 0.35f + r.nextFloat() * 0.65f,
                color = listOf(Palette.Yellow, Palette.Orange, Palette.Blue, Palette.Green, Palette.Purple, Palette.Red)[it % 6],
                star = it % 3 == 0,
                spin = r.nextFloat() * 720f - 360f,
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val p = t.value
        if (p >= 1f) return@Canvas
        val center = Offset(size.width / 2, size.height * 0.42f)
        val reach = size.minDimension * 0.55f
        for (b in bits) {
            val d = reach * b.speed * (1 - (1 - p) * (1 - p))
            val pos = Offset(center.x + cos(b.angle) * d, center.y + sin(b.angle) * d + 260f * p * p)
            val alpha = (1f - p).coerceIn(0f, 1f)
            rotate(b.spin * p, pos) {
                if (b.star) drawPath(starPath(pos, 22f), b.color.copy(alpha = alpha))
                else drawRect(b.color.copy(alpha = alpha), Offset(pos.x - 8f, pos.y - 5f), androidx.compose.ui.geometry.Size(16f, 10f))
            }
        }
    }
}

private class Bit(val angle: Float, val speed: Float, val color: Color, val star: Boolean, val spin: Float)

fun starPath(c: Offset, r: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + i * PI / 5
        val x = c.x + (cos(a) * rr).toFloat()
        val y = c.y + (sin(a) * rr).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Three stars, filled up to [count]. */
@Composable
fun StarRow(count: Int, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = size * 3 + 8.dp, height = size)) {
        val r = this.size.height / 2
        for (i in 0 until 3) {
            val c = Offset(r + i * (r * 2 + 4.dp.toPx()), r)
            drawPath(starPath(c, r), if (i < count) Palette.Yellow else Palette.Locked)
        }
    }
}

@Composable
fun BigStar(filled: Boolean, size: Dp) {
    Canvas(Modifier.size(size)) {
        drawPath(starPath(center, this.size.minDimension / 2), if (filled) Palette.Yellow else Palette.Locked)
    }
}

/** Small round pill used for top-bar buttons. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, label: String = text, onClick: () -> Unit) {
    Box(
        modifier
            .semantics { contentDescription = label }
            .clip(CircleShape)
            .background(Palette.Paper)
            .border(2.dp, Palette.Line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text, color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Wobble(content: @Composable () -> Unit) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            angle.animateTo(4f, tween(900))
            angle.animateTo(-4f, tween(900))
        }
    }
    Box(Modifier.rotate(angle.value)) { content() }
}
