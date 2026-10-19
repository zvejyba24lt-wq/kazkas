package lt.marcomultiservices.skaitaupats.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import lt.marcomultiservices.skaitaupats.audio.Speaker
import lt.marcomultiservices.skaitaupats.core.Exercise
import lt.marcomultiservices.skaitaupats.core.Lesson
import lt.marcomultiservices.skaitaupats.core.Phrases
import lt.marcomultiservices.skaitaupats.core.Stars

/** What an exercise can ask of the lesson around it. */
class ExerciseHost(
    val speaker: Speaker,
    /** Moves on to the next exercise. */
    val next: () -> Unit,
    /** Praises, celebrates and then moves on; [say] is spoken first (e.g. the word just read). */
    val correct: (say: List<String>) -> Unit,
    /** Records a wrong answer, optionally against a letter for the parents' overview. */
    val mistake: (letter: String?) -> Unit,
)

@Composable
fun LessonScreen(
    lesson: Lesson,
    speaker: Speaker,
    /** The lesson has not been finished before, so finishing it adds a sticker to the album. */
    firstTime: Boolean,
    onClose: () -> Unit,
    /** The child tapped "back to the map" on the finished screen. */
    onDone: () -> Unit,
    onFinished: (stars: Int, mistakesByLetter: Map<String, Int>) -> Unit,
) {
    var index by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    var mistakes by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    // Saved with the lesson, so a recreated screen keeps the mistakes for the parents' overview.
    var byLetter by rememberSaveable(lesson.id) { mutableStateOf(mapOf<String, Int>()) }
    var reported by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var celebrate by remember { mutableIntStateOf(0) }
    var advancing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val total = lesson.exercises.size
    val finished = index >= total

    val host = remember(lesson.id) {
        ExerciseHost(
            speaker = speaker,
            next = { index++ },
            correct = { say ->
                if (!advancing) {
                    advancing = true
                    celebrate++
                    // Move on once the praise has been said, so the next prompt does not cut it off.
                    val spoken = CompletableDeferred<Unit>()
                    speaker.say(say + Phrases.PRAISE.random()) { spoken.complete(Unit) }
                    scope.launch {
                        delay(900)
                        withTimeoutOrNull(2_500) { spoken.await() }
                        advancing = false
                        index++
                    }
                }
            },
            mistake = { letter ->
                mistakes++
                if (letter != null) byLetter = byLetter + (letter to (byLetter[letter] ?: 0) + 1)
                speaker.say(Phrases.TRY_AGAIN)
            },
        )
    }

    BackHandler(enabled = finished) { onDone() }

    LaunchedEffect(finished) {
        if (finished && !reported) {
            reported = true
            onFinished(Stars.forMistakes(mistakes), byLetter)
            celebrate++
            speaker.say(if (firstTime) listOf(Phrases.LESSON_DONE, Phrases.STICKER) else listOf(Phrases.LESSON_DONE))
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.Cream)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Leaving a finished lesson counts as "back to the map", so the free version's ad is not skipped.
                Pill("✕", label = "Uždaryti", onClick = if (finished) onDone else onClose)
                Spacer(Modifier.size(12.dp))
                val fill by animateFloatAsState((index.coerceAtMost(total)).toFloat() / total, label = "progress")
                Box(Modifier.weight(1f).height(18.dp).clip(CircleShape).background(Palette.Line)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill).clip(CircleShape).background(Palette.Green))
                }
                Spacer(Modifier.size(12.dp))
                Text(lesson.badge, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Orange)
            }
            AnimatedContent(
                targetState = index,
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "exercise",
            ) { i ->
                Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    if (i >= total) {
                        Finished(Stars.forMistakes(mistakes), if (firstTime) lesson.sticker else null, onDone)
                    } else {
                        val e = lesson.exercises[i]
                        LaunchedEffect(i) { if (e.prompt.isNotEmpty()) speaker.say(e.prompt) }
                        // The outgoing exercise stays tappable while it slides away; a second tap on its
                        // "Toliau" must not skip the next exercise.
                        val own = remember(i) {
                            ExerciseHost(host.speaker, next = { if (index == i) index++ }, correct = host.correct, mistake = host.mistake)
                        }
                        ExerciseView(e, own)
                    }
                }
            }
        }
        Celebration(celebrate)
    }
}

@Composable
private fun ExerciseView(e: Exercise, host: ExerciseHost) {
    when (e) {
        is Exercise.LetterIntro -> LetterIntroView(e, host)
        is Exercise.FindLetter -> FindLetterView(e, host)
        is Exercise.TraceLetter -> TraceLetterView(e, host)
        is Exercise.BuildSyllable -> BuildSyllableView(e, host)
        is Exercise.HearSyllable -> HearSyllableView(e, host)
        is Exercise.ReadWord -> ReadWordView(e, host)
        is Exercise.WordPicture -> WordPictureView(e, host)
        is Exercise.PictureWord -> PictureWordView(e, host)
        is Exercise.ReadSentence -> SentenceView(e.text, null, host)
        is Exercise.StoryPage -> SentenceView(e.page.text, e, host)
    }
}

@Composable
private fun Finished(stars: Int, sticker: String?, onClose: () -> Unit) {
    var shown by remember { mutableIntStateOf(0) }
    var stickerIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repeat(stars) { delay(350); shown++ }
        delay(300)
        stickerIn = true
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("🦉", fontSize = 84.sp)
        Text("Puiku!", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 3) {
                val s by animateFloatAsState(if (i < shown) 1f else 0.6f, label = "star")
                Box(Modifier.scale(s)) { BigStar(i < shown, 56.dp) }
            }
        }
        if (sticker != null) {
            val pop by animateFloatAsState(if (stickerIn) 1f else 0f, spring(Spring.DampingRatioMediumBouncy), label = "sticker")
            Row(
                Modifier.scale(pop).clip(RoundedCornerShape(24.dp)).background(Palette.Paper)
                    .border(3.dp, Palette.Yellow, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(sticker, fontSize = 44.sp)
                Text("  Naujas lipdukas!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Orange)
            }
        }
        Spacer(Modifier.height(8.dp))
        Chunky(Palette.Orange, Palette.OrangeDark, Modifier.size(width = 240.dp, height = 80.dp), corner = 40.dp, label = "Į žemėlapį", onClick = onClose) {
            Text("Į žemėlapį", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
