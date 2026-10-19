package lt.marcomultiservices.skaitaupats.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

object Palette {
    val Cream = Color(0xFFFFF7E8)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2D2A4A)
    val Soft = Color(0xFF7A7593)
    val Orange = Color(0xFFFF8A3D)
    val OrangeDark = Color(0xFFE56A1C)
    val Blue = Color(0xFF3D7BFF)
    val BlueDark = Color(0xFF2A5CD6)
    val Red = Color(0xFFF0505A)
    val Green = Color(0xFF34B36B)
    val GreenDark = Color(0xFF23904F)
    val Purple = Color(0xFF8E6CEF)
    val PurpleDark = Color(0xFF6B4AD0)
    val Yellow = Color(0xFFFFC93C)
    val Locked = Color(0xFFD9D4CC)
    val Line = Color(0xFFEADFCB)

    /** Syllables alternate colours, as in Lithuanian first-grade readers. */
    val Syllables = listOf(Blue, Red)
}

/** The screens are laid out for a window of at least this size; smaller phones get everything scaled down. */
private const val DESIGN_WIDTH_DP = 400f
private const val DESIGN_HEIGHT_DP = 780f

@Composable
fun SkaitauTheme(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val system = LocalDensity.current
        // The letters are already very large, so the system font size is only allowed to grow them a little.
        val fontScale = system.fontScale.coerceAtMost(1.15f)
        val fit = minOf(1f, maxWidth.value / DESIGN_WIDTH_DP, maxHeight.value / (DESIGN_HEIGHT_DP * fontScale)).coerceAtLeast(0.6f)
        CompositionLocalProvider(LocalDensity provides Density(system.density * fit, fontScale)) {
            Themed(content)
        }
    }
}

@Composable
private fun Themed(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Orange,
            onPrimary = Color.White,
            secondary = Palette.Blue,
            background = Palette.Cream,
            surface = Palette.Paper,
            onBackground = Palette.Ink,
            onSurface = Palette.Ink,
        ),
        content = content,
    )
}
