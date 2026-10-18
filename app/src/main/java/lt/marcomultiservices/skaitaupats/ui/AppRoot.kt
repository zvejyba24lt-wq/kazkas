package lt.marcomultiservices.skaitaupats.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import lt.marcomultiservices.skaitaupats.BuildConfig
import lt.marcomultiservices.skaitaupats.app
import lt.marcomultiservices.skaitaupats.billing.BillingManager
import lt.marcomultiservices.skaitaupats.data.Progress

private sealed interface Screen {
    data object Home : Screen
    data class Lesson(val id: Int) : Screen
    data object Gate : Screen
    data object Parents : Screen
}

/**
 * Whether a lesson may be opened: the course is unlocked for it, and the one before is finished.
 * Test builds open every lesson so the whole course can be checked.
 */
fun Progress.canOpen(id: Int): Boolean =
    BuildConfig.TEST_UNLOCK || ((id == 1 || finished(id - 1) || finished(id)) && paidFor(id))

fun Progress.paidFor(id: Int): Boolean = id <= BillingManager.FREE_LESSONS || owned || BuildConfig.TEST_UNLOCK

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
            )
            is Screen.Lesson -> LessonScreen(
                lesson = app.course.lessons.first { it.id == s.id },
                speaker = app.speaker,
                onClose = { go("home") },
                onFinished = { stars, mistakes ->
                    scope.launch { app.progress.finishLesson(s.id, stars, mistakes) }
                },
            )
            Screen.Gate -> ParentGate(onPassed = { go("parents") }, onBack = { go("home") })
            Screen.Parents -> ParentsScreen(
                course = app.course,
                progress = p,
                billing = app.billing,
                onReset = { scope.launch { app.progress.reset() } },
                onBack = { go("home") },
            )
        }
    }
}
