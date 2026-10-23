package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.marcomultiservices.kimba.core.Catch
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.Story
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.drawArt
import lt.marcomultiservices.kimba.ui.t

/*
 * Old Jonas, the storyteller. Picture slots (see docs/GRAFIKA.md): `jonas_1` (friendly),
 * `jonas_2` (telling a secret), `jonas_3` (proud), square, transparent background.
 */

/** Jonas's face for [mood] 1..3 in the square [r]. */
fun DrawScope.drawJonas(r: Rect, mood: Int) {
    ArtPack["jonas_$mood"]?.let { drawArt(it, r); return }
    val u = r.width
    val cx = r.center.x
    val top = r.top
    // Shoulders in a fishing vest over a checked shirt.
    drawOval(Color(0xFF5C6B45), Offset(cx - u * 0.45f, top + u * 0.72f), Size(u * 0.9f, u * 0.6f))
    drawOval(Color(0xFF8C3A2E), Offset(cx - u * 0.12f, top + u * 0.74f), Size(u * 0.24f, u * 0.4f))
    // Face.
    drawOval(Brush.radialGradient(listOf(Color(0xFFE8BC98), Color(0xFFC8946C)), Offset(cx - u * 0.05f, top + u * 0.42f), u * 0.3f), Offset(cx - u * 0.22f, top + u * 0.22f), Size(u * 0.44f, u * 0.52f))
    // White beard and moustache.
    val beard = Path().apply {
        moveTo(cx - u * 0.22f, top + u * 0.5f)
        quadraticTo(cx - u * 0.2f, top + u * 0.86f, cx, top + u * 0.88f)
        quadraticTo(cx + u * 0.2f, top + u * 0.86f, cx + u * 0.22f, top + u * 0.5f)
        quadraticTo(cx, top + u * 0.62f, cx - u * 0.22f, top + u * 0.5f)
        close()
    }
    drawPath(beard, Color(0xFFEDEBE6))
    drawOval(Color(0xFFDAD7D0), Offset(cx - u * 0.12f, top + u * 0.56f), Size(u * 0.24f, u * 0.06f))
    // Eyes: crinkled when proud, one brow up when telling a secret.
    val eye = Color(0xFF2A1E16)
    if (mood == 3) {
        drawArc(eye, 200f, 140f, false, Offset(cx - u * 0.13f, top + u * 0.4f), Size(u * 0.08f, u * 0.05f), style = Stroke(u * 0.015f))
        drawArc(eye, 200f, 140f, false, Offset(cx + u * 0.05f, top + u * 0.4f), Size(u * 0.08f, u * 0.05f), style = Stroke(u * 0.015f))
    } else {
        drawCircle(eye, u * 0.022f, Offset(cx - u * 0.09f, top + u * 0.43f))
        drawCircle(eye, u * 0.022f, Offset(cx + u * 0.09f, top + u * 0.43f))
    }
    val browUp = if (mood == 2) u * 0.04f else 0f
    drawLine(Color(0xFFCFCBC2), Offset(cx - u * 0.14f, top + u * 0.37f - browUp), Offset(cx - u * 0.04f, top + u * 0.36f - browUp), strokeWidth = u * 0.025f, cap = StrokeCap.Round)
    drawLine(Color(0xFFCFCBC2), Offset(cx + u * 0.04f, top + u * 0.36f), Offset(cx + u * 0.14f, top + u * 0.37f), strokeWidth = u * 0.025f, cap = StrokeCap.Round)
    // Old green wool cap.
    drawArc(Color(0xFF3E5236), 180f, 180f, true, Offset(cx - u * 0.25f, top + u * 0.1f), Size(u * 0.5f, u * 0.34f))
    drawRect(Color(0xFF34452D), Offset(cx - u * 0.26f, top + u * 0.25f), Size(u * 0.52f, u * 0.05f))
    drawCircle(Color(0xFF4E6644), u * 0.04f, Offset(cx, top + u * 0.1f))
}

/** Jonas's current task on the quests screen: his words, the task and how far along it is. */
@Composable
fun JonasCard(p: Progress, catches: List<Catch>) {
    val step = Story.current(p)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0x33A8C77A), Color(0x14A8C77A))))
            .border(1.dp, Color(0x66A8C77A), RoundedCornerShape(18.dp)).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Canvas(Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)).background(Color(0x33FFFFFF))) {
            drawJonas(Rect(0f, 0f, size.width, size.height), if (step == null) 3 else 1)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t(S.jonasName), color = Color(0xFFA8C77A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            if (step == null) {
                Text(t(S.jonasDone), color = K.Text, fontSize = 14.sp, lineHeight = 19.sp)
            } else {
                Text("„${t(step.talk)}“", color = K.Text, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                val have = step.progress(p, Story.since(p, catches), java.time.ZoneId.systemDefault()).coerceAtMost(step.target)
                val shown = if (step.target >= 100) "${have} / ${step.target} g" else "$have / ${step.target}"
                Text("${t(step.task)} · $shown", color = K.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(t(S.jonasReward, step.coins), color = K.Muted, fontSize = 11.sp)
            }
        }
    }
}
