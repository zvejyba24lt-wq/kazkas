package lt.marcomultiservices.kimba.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object K {
    val Night = Color(0xFF0E1A2B)
    val Ink = Color(0xFF0A121E)
    val Panel = Color(0xCC0F1A28)
    val PanelSolid = Color(0xFF142233)
    val Line = Color(0x33FFFFFF)
    val Amber = Color(0xFFF2B25C)
    val Gold = Color(0xFFFFD27A)
    val Reed = Color(0xFF8DC26F)
    val Danger = Color(0xFFFF6A50)
    val Text = Color(0xFFF3EFE6)
    val Muted = Color(0xFFB4BCC6)
}

private val Display = FontFamily.Serif

@Composable
fun KimbaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = K.Amber, onPrimary = Color(0xFF2A1A06), background = K.Night, surface = K.PanelSolid,
            onSurface = K.Text, onBackground = K.Text, secondary = K.Reed,
        ),
        typography = Typography(
            displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
            headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.3.sp),
        ),
        content = content,
    )
}

/** A frosted dark panel floating over the lake. */
@Composable
fun Glass(modifier: Modifier = Modifier, radius: Dp = 18.dp, padding: Dp = 12.dp, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xD9142233), Color(0xE60B1420))))
            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x10FFFFFF)))), shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun amberButtonColors() = ButtonDefaults.buttonColors(containerColor = K.Amber, contentColor = Color(0xFF2A1A06))

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
}

/**
 * Room at the top for the status bar when it shows, and for the camera cut-out when the game
 * runs full screen with the bars hidden.
 */
fun Modifier.safeTopPadding(): Modifier = windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
