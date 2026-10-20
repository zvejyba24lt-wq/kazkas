package lt.marcomultiservices.kimba.ui.scene

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay

/** True on the Android emulator, which renders on the CPU and cannot keep up with 60 fps scenes. */
val isEmulator: Boolean = Build.FINGERPRINT.contains("generic") || Build.HARDWARE.contains("ranchu") || Build.PRODUCT.contains("sdk")

/** Seconds since the composable appeared, ticking every frame (a few times a second on the emulator). */
@Composable
fun rememberSceneTime(): MutableFloatState {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            if (isEmulator) delay(250)
            withFrameNanos { time.floatValue = (it - start) / 1e9f }
        }
    }
    return time
}
