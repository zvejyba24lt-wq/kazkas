package lt.marcomultiservices.skaitaupats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import lt.marcomultiservices.skaitaupats.ui.AppRoot
import lt.marcomultiservices.skaitaupats.ui.SkaitauTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SkaitauTheme { AppRoot() } }
        lifecycleScope.launch {
            // Bought copies never start the ad SDK: wait until Play has been asked about earlier purchases.
            withTimeoutOrNull(8_000) { app.billing.checked.first { it } }
            if (!app.progress.progress.first().adFree) app.ads.start(this@MainActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up purchases finished outside the app and refunds.
        app.billing.connect()
    }

    override fun onStop() {
        super.onStop()
        app.speaker.stop()
    }
}
