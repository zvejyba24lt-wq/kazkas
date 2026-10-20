package lt.marcomultiservices.kimba.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import lt.marcomultiservices.kimba.core.L
import lt.marcomultiservices.kimba.core.Lang

/** The app's language: the in-app choice, else the phone's, else English. */
fun currentLang(context: Context): Lang {
    val chosen = AppCompatDelegate.getApplicationLocales()
    val tag = if (!chosen.isEmpty) chosen[0]?.language else context.resources.configuration.locales[0]?.language
    return when (tag) {
        "lt" -> Lang.LT
        "de" -> Lang.DE
        else -> Lang.EN
    }
}

val LocalLang = staticCompositionLocalOf { Lang.LT }

@Composable
@ReadOnlyComposable
fun t(text: L): String = text[LocalLang.current]

@Composable
@ReadOnlyComposable
fun t(text: L, vararg args: Any): String = text[LocalLang.current].format(*args)
