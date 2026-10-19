package lt.marcomultiservices.skaitaupats.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lt.marcomultiservices.skaitaupats.core.Exercise
import lt.marcomultiservices.skaitaupats.core.Lt
import lt.marcomultiservices.skaitaupats.core.Phrases
import lt.marcomultiservices.skaitaupats.core.WordDef
import kotlin.math.roundToInt

private val Shape = RoundedCornerShape(28.dp)

@Composable
private fun Instruction(text: String, onSound: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        if (onSound != null) {
            SoundButton(size = 52.dp, onClick = onSound)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Soft, textAlign = TextAlign.Center)
    }
}

/** A word drawn with its syllables in alternating colours. */
@Composable
private fun SyllableWord(word: WordDef, fontSize: Int, modifier: Modifier = Modifier) {
    // Shrinks to fit when the word is too long for its place (kup-ra-nu-ga-ris), never wraps.
    BasicText(
        buildAnnotatedString {
            word.syllables.forEachIndexed { i, s ->
                withStyle(SpanStyle(color = Palette.Syllables[i % 2])) { append(s) }
            }
        },
        modifier = modifier,
        style = TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = fontSize.sp, stepSize = 2.sp),
    )
}

private enum class Mark { NONE, RIGHT, WRONG }

/** One answer tile in a multiple-choice exercise. */
@Composable
private fun ChoiceTile(
    mark: Mark,
    shake: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val (face, edge) = when (mark) {
        Mark.RIGHT -> Color(0xFFDDF6E6) to Palette.Green
        Mark.WRONG -> Color(0xFFFFE1E1) to Palette.Red
        Mark.NONE -> Palette.Paper to Palette.Line
    }
    Chunky(face, edge, modifier.shakeOn(shake), corner = 28.dp, label = label, onClick = onClick) { content() }
}

/** State shared by the choice exercises: marks each tile and reports right or wrong answers. */
private class Choice<T>(private val host: ExerciseHost, private val answer: T) {
    val marks = mutableStateMapOf<T, Mark>()
    val shakes = mutableStateMapOf<T, Int>()
    var solved by mutableStateOf(false)

    fun pick(option: T, say: List<String> = emptyList(), letter: String? = null) {
        if (solved) return
        if (option == answer) {
            solved = true
            marks[option] = Mark.RIGHT
            host.correct(say)
        } else {
            marks[option] = Mark.WRONG
            shakes[option] = (shakes[option] ?: 0) + 1
            host.mistake(letter)
        }
    }
}

@Composable
fun LetterIntroView(e: Exercise.LetterIntro, host: ExerciseHost) {
    val speaking by host.speaker.speaking.collectAsState()
    val l = e.letter
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Chunky(
            Palette.Paper, Palette.Line, Modifier.size(260.dp), corner = 40.dp, label = "Raidė",
            onClick = { host.speaker.say(Phrases.intro(l)) },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Palette.Orange)) { append(l.letter.uppercase()) }
                        withStyle(SpanStyle(color = Palette.Blue)) { append(l.letter) }
                    },
                    fontSize = 120.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(l.emoji, fontSize = 64.sp)
            Spacer(Modifier.width(14.dp))
            Text(l.word.removeSuffix("."), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        }
        SoundButton(speaking = speaking != null) { host.speaker.say(Phrases.intro(l)) }
        NextButton(onClick = host.next)
    }
}

@Composable
fun FindLetterView(e: Exercise.FindLetter, host: ExerciseHost) {
    val choice = remember(e) { Choice(host, e.answer) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Instruction("Surask raidę") { host.speaker.say(e.prompt) }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            e.options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { opt ->
                        ChoiceTile(choice.marks[opt] ?: Mark.NONE, choice.shakes[opt] ?: 0, opt, Modifier.size(140.dp), onClick = {
                            choice.pick(opt, letter = e.letter.letter)
                        }) {
                            Text(opt, fontSize = 84.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuildSyllableView(e: Exercise.BuildSyllable, host: ExerciseHost) {
    var merged by remember(e) { mutableStateOf(false) }
    var drag by remember(e) { mutableFloatStateOf(0f) }
    val gap = remember(e) { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    fun merge() {
        if (merged) return
        merged = true
        scope.launch {
            gap.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
            host.speaker.say(e.syllable)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(36.dp)) {
        Instruction("Sujunk raides") { host.speaker.say(e.prompt) }
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            val spread = 50f * gap.value
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chunky(
                    Palette.Paper, Palette.Line,
                    Modifier.offset(x = (-spread).dp).size(130.dp), corner = 28.dp, label = e.first,
                    onClick = { merge() },
                ) { Text(e.first, fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Blue) }
                Chunky(
                    Palette.Paper, Palette.Line,
                    Modifier
                        .offset { IntOffset(with(density) { (spread.dp).roundToPx() } + drag.roundToInt(), 0) }
                        .size(130.dp)
                        .pointerInput(e) {
                            detectDragGestures(
                                onDragEnd = { if (drag < -60f) merge(); drag = 0f },
                                onDrag = { change, amount -> change.consume(); if (!merged) drag = (drag + amount.x).coerceAtMost(0f) },
                            )
                        },
                    corner = 28.dp, label = e.second, onClick = { merge() },
                ) { Text(e.second, fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Red) }
            }
        }
        AnimatedVisibility(merged, enter = fadeIn() + scaleIn()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.syllable, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                    Spacer(Modifier.width(16.dp))
                    SoundButton(size = 60.dp) { host.speaker.say(e.syllable) }
                }
                NextButton(onClick = host.next)
            }
        }
    }
}

@Composable
fun HearSyllableView(e: Exercise.HearSyllable, host: ExerciseHost) {
    val choice = remember(e) { Choice(host, e.target) }
    val speaking by host.speaker.speaking.collectAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
        Text("Kurį skiemenį girdi?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Soft)
        SoundButton(size = 110.dp, speaking = speaking == e.target) { host.speaker.say(e.target) }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            e.options.forEach { opt ->
                ChoiceTile(choice.marks[opt] ?: Mark.NONE, choice.shakes[opt] ?: 0, opt, Modifier.size(104.dp), onClick = {
                    choice.pick(opt, say = listOf(e.target))
                }) { Text(opt, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink) }
            }
        }
    }
}

@Composable
fun ReadWordView(e: Exercise.ReadWord, host: ExerciseHost) {
    val w = e.word
    var heard by remember(e) { mutableStateOf(false) }
    val speaking by host.speaker.speaking.collectAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(26.dp)) {
        Instruction("Spausk skiemenis ir skaityk") { host.speaker.say(e.prompt) }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Long words (fo-to-a-pa-ra-tas) shrink their tiles so all syllables stay on one line on screen.
            val natural = w.syllables.sumOf { 40 + it.length * 32 } + 10 * (w.syllables.size - 1)
            val k = minOf(1f, maxWidth.value / natural)
            Row(horizontalArrangement = Arrangement.spacedBy((10 * k).dp)) {
                w.syllables.forEachIndexed { i, s ->
                    val color = Palette.Syllables[i % 2]
                    val active = speaking == s.lowercase()
                    Chunky(
                        if (active) color else Palette.Paper, if (active) color.copy(alpha = 0.7f) else Palette.Line,
                        Modifier.height((110 * k).coerceAtLeast(64f).dp).width(((40 + s.length * 32) * k).dp), corner = (24 * k).dp, label = s,
                        onClick = { heard = true; host.speaker.say(s.lowercase()) },
                    ) { Text(s, fontSize = (52 * k).sp, fontWeight = FontWeight.ExtraBold, color = if (active) Color.White else color, maxLines = 1, softWrap = false) }
                }
            }
        }
        Text("⬇", fontSize = 30.sp, color = Palette.Line)
        Chunky(
            Palette.Paper, Palette.Line, Modifier.fillMaxWidth(0.9f).height(120.dp), corner = 28.dp, label = w.text,
            onClick = { heard = true; host.speaker.say(w.text) },
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                SyllableWord(w, 54, Modifier.weight(1f, fill = false))
                AnimatedVisibility(heard && w.emoji != null, enter = scaleIn() + fadeIn()) {
                    Text("  ${w.emoji}", fontSize = 54.sp)
                }
            }
        }
        NextButton(enabled = heard, onClick = host.next)
    }
}

@Composable
fun WordPictureView(e: Exercise.WordPicture, host: ExerciseHost) {
    val choice = remember(e) { Choice(host, e.word) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
        Instruction("Perskaityk ir surask paveikslėlį") { host.speaker.say(e.prompt) }
        Box(Modifier.clip(Shape).background(Palette.Paper).border(3.dp, Palette.Line, Shape).padding(horizontal = 30.dp, vertical = 18.dp)) {
            SyllableWord(e.word, 58)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            e.options.forEach { opt ->
                ChoiceTile(
                    choice.marks[opt] ?: Mark.NONE, choice.shakes[opt] ?: 0, opt.text,
                    Modifier.weight(1f).aspectRatio(0.85f),
                    onClick = { choice.pick(opt, say = listOf(e.word.text)) },
                ) { Text(opt.emoji ?: "", fontSize = 60.sp) }
            }
        }
    }
}

@Composable
fun PictureWordView(e: Exercise.PictureWord, host: ExerciseHost) {
    val choice = remember(e) { Choice(host, e.word) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Instruction("Kuris žodis tinka?") { host.speaker.say(e.prompt) }
        Box(Modifier.size(170.dp).clip(Shape).background(Palette.Paper).border(3.dp, Palette.Line, Shape), contentAlignment = Alignment.Center) {
            Text(e.word.emoji ?: "", fontSize = 100.sp)
        }
        e.options.forEach { opt ->
            ChoiceTile(
                choice.marks[opt] ?: Mark.NONE, choice.shakes[opt] ?: 0, opt.text,
                Modifier.fillMaxWidth(0.85f).height(92.dp),
                onClick = { choice.pick(opt, say = listOf(e.word.text)) },
            ) { SyllableWord(opt, 40) }
        }
    }
}

/** A sentence or a story page: tap a word to hear it, or listen to the whole line read with highlighting. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceView(text: String, page: Exercise.StoryPage?, host: ExerciseHost) {
    val tokens = remember(text) { Lt.tokens(text) }
    val speaking by host.speaker.speaking.collectAsState()
    var progress by remember(text) { mutableFloatStateOf(-1f) }
    var listened by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(speaking) {
        if (speaking == text) {
            while (true) { progress = host.speaker.fraction(); delay(40) }
        } else progress = -1f
    }
    // Which token is being read: spread the recording over the words by their length.
    val reading = remember(progress, tokens) {
        if (progress < 0f) -1 else {
            val weights = tokens.map { Lt.bare(it).length + 2 }
            val target = progress * weights.sum()
            var acc = 0f
            weights.indexOfFirst { acc += it; acc >= target }.coerceAtLeast(0)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (page != null) {
            Text(page.story.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.Purple)
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(Shape)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFEDE6FF), Color(0xFFFFF1DC)))),
                contentAlignment = Alignment.Center,
            ) { Text(page.page.emoji, fontSize = 120.sp) }
        } else {
            Instruction("Perskaityk sakinį") { host.speaker.say(Phrases.READ_SENTENCE) }
        }
        FlowRow(
            Modifier.fillMaxWidth().clip(Shape).background(Palette.Paper).border(3.dp, Palette.Line, Shape).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tokens.forEachIndexed { i, tok ->
                val bare = Lt.bare(tok)
                val active = i == reading || (progress < 0f && speaking == bare)
                Box(
                    Modifier
                        .semantics { contentDescription = bare }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) Palette.Yellow else Color.Transparent)
                        .clickable(enabled = bare.isNotEmpty()) { host.speaker.say(bare) }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) { Text(tok, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SoundButton(size = 76.dp, speaking = speaking == text) { listened++; host.speaker.say(text) }
            NextButton(enabled = page != null || listened > 0, onClick = host.next)
        }
        if (page != null) {
            Text("${page.index + 1} / ${page.story.pages.size}", fontSize = 18.sp, color = Palette.Soft, fontWeight = FontWeight.Bold)
        }
    }
}
