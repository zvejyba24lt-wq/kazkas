package lt.marcomultiservices.kimba.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.screens.BookScreen
import lt.marcomultiservices.kimba.ui.screens.ForecastScreen
import lt.marcomultiservices.kimba.ui.screens.LakeScreen
import lt.marcomultiservices.kimba.ui.screens.SettingsScreen
import lt.marcomultiservices.kimba.ui.screens.WelcomeScreen

enum class Screen { LAKE, BOOK, FORECAST, SETTINGS }

@Composable
fun AppRoot(vm: GameViewModel, startScreen: Pair<Screen, Long>?) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.LAKE) }
    LaunchedEffect(startScreen) { startScreen?.let { screen = it.first } }
    val s = saved ?: run { Box(Modifier.fillMaxSize().background(K.Night)); return }

    if (!s.onboarded || s.place == null) {
        WelcomeScreen { vm.choosePlace(it) }
        return
    }
    BackHandler(screen != Screen.LAKE) { screen = Screen.LAKE }
    AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { current ->
        when (current) {
            Screen.LAKE -> LakeScreen(vm, { screen = Screen.BOOK }, { screen = Screen.FORECAST }, { screen = Screen.SETTINGS })
            Screen.BOOK -> BookScreen(vm) { screen = Screen.LAKE }
            Screen.FORECAST -> ForecastScreen(vm) { screen = Screen.LAKE }
            Screen.SETTINGS -> SettingsScreen(vm) { screen = Screen.LAKE }
        }
    }
}
