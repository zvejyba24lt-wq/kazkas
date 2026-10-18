package lt.marcomultiservices.skaitaupats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.marcomultiservices.skaitaupats.audio.Speaker
import lt.marcomultiservices.skaitaupats.core.Course
import lt.marcomultiservices.skaitaupats.core.LessonKind
import lt.marcomultiservices.skaitaupats.core.Phrases
import lt.marcomultiservices.skaitaupats.data.Progress

/** One sticker for every finished lesson; the empty places show what is still to come. */
@Composable
fun AlbumScreen(course: Course, progress: Progress, speaker: Speaker, onBack: () -> Unit) {
    val lessons = course.lessons
    val have = lessons.count { progress.finished(it.id) }
    Column(Modifier.fillMaxSize().background(Palette.Cream).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("←", label = "Atgal", onClick = onBack)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Mano lipdukai", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Text("$have iš ${lessons.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.Soft)
            }
        }
        // How far the album is filled.
        Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(14.dp).clip(CircleShape).background(Palette.Line)) {
            Box(Modifier.fillMaxWidth(have.toFloat() / lessons.size).height(14.dp).clip(CircleShape).background(Palette.Yellow))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(lessons, key = { it.id }) { lesson ->
                val got = progress.finished(lesson.id)
                val shape = RoundedCornerShape(18.dp)
                val ring = when (lesson.kind) {
                    LessonKind.LETTER -> Palette.Orange
                    LessonKind.READING -> Palette.Blue
                    LessonKind.STORY -> Palette.Purple
                }
                Box(
                    Modifier.aspectRatio(1f).clip(shape)
                        .background(if (got) Palette.Paper else Color(0xFFF1EBE0))
                        .border(3.dp, if (got) ring else Palette.Line, shape)
                        .semantics { contentDescription = "Lipdukas ${lesson.id}" }
                        .clickable { if (got) speaker.say(Phrases.PRAISE.random()) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (got) {
                        Text(lesson.sticker, fontSize = 34.sp)
                    } else {
                        Text("${lesson.id}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC9C0B2))
                    }
                }
            }
        }
    }
}
