package lt.marcomultiservices.skaitaupats.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import lt.marcomultiservices.skaitaupats.BuildConfig
import lt.marcomultiservices.skaitaupats.app
import lt.marcomultiservices.skaitaupats.core.FreePlan
import lt.marcomultiservices.skaitaupats.data.Progress
import lt.marcomultiservices.skaitaupats.data.today

private sealed interface Screen {
    data object Home : Screen
    data class Lesson(val id: Int) : Screen
    data object Gate : Screen
    data object Parents : Screen
    data object Album : Screen
}

/** The lesson before is finished. Test builds open lessons in any order so the whole course can be checked. */
fun Progress.inOrder(id: Int): Boolean = BuildConfig.TEST_UNLOCK || id == 1 || finished(id - 1) || finished(id)

/** The free version's daily limit allows it: a repeat, or not too many new lessons today. */
fun Progress.withinLimit(id: Int, day: Long = today()): Boolean = adFree || FreePlan.mayOpen(finished(id), newToday(day))

fun Progress.canOpen(id: Int, day: Long = today()): Boolean = inOrder(id) && withinLimit(id, day)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.app
    val progress by app.progress.progress.collectAsState(initial = null)
    var screenKey by rememberSaveable { mutableStateOf("home") }
    val screen: Screen = remember(screenKey) {
        when {
            screenKey.startsWith("lesson:") -> Screen.Lesson(screenKey.removePrefix("lesson:").toInt())
            screenKey == "gate" -> Screen.Gate
            screenKey == "parents" -> Screen.Parents
            screenKey == "album" -> Screen.Album
            else -> Screen.Home
        }
    }
    val scope = rememberCoroutineScope()
    fun go(key: String) {
        app.speaker.stop()
        screenKey = key
    }
    BackHandler(enabled = screen != Screen.Home) { go("home") }

    val p = progress ?: return
    AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { s ->
        when (s) {
            Screen.Home -> HomeScreen(
                course = app.course,
                progress = p,
                speaker = app.speaker,
                onOpen = { go("lesson:$it") },
                onParents = { go("gate") },
                onAlbum = { go("album") },
            )
            is Screen.Lesson -> {
                // A saved screen can name a lesson that no longer exists after a course update.
                val lesson = app.course.lessons.firstOrNull { it.id == s.id }
                if (lesson == null) {
                    LaunchedEffect(Unit) { go("home") }
                } else {
                    LessonScreen(
                        lesson = lesson,
                        speaker = app.speaker,
                        firstTime = rememberSaveable(s.id) { !p.finished(s.id) },
                        onClose = { go("home") },
                        onDone = {
                            if (p.adFree) go("home") else app.ads.showAfterLesson(context as Activity) { go("home") }
                        },
                        onFinished = { stars, mistakes ->
                            scope.launch { app.progress.finishLesson(s.id, stars, mistakes) }
                        },
                    )
                }
            }
            Screen.Album -> AlbumScreen(course = app.course, progress = p, speaker = app.speaker, onBack = { go("home") })
            Screen.Gate -> ParentGate(onPassed = { go("parents") }, onBack = { go("home") })
            Screen.Parents -> ParentsScreen(
                course = app.course,
                progress = p,
                billing = app.billing,
                onReset = { scope.launch { app.progress.reset() } },
                onTestOwned = { scope.launch { app.progress.setTestOwned(it) } },
                onBack = { go("home") },
            )
        }
    }
}
