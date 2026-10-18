package lt.marcomultiservices.skaitaupats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.marcomultiservices.skaitaupats.ui.AppRoot
import lt.marcomultiservices.skaitaupats.ui.SkaitauTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SkaitauTheme { AppRoot() } }
        app.billing.connect()
        lifecycleScope.launch {
            // Bought copies never start the ad SDK.
            if (!app.progress.progress.first().adFree) app.ads.start(this@MainActivity)
        }
    }

    override fun onStop() {
        super.onStop()
        app.speaker.stop()
    }
}
