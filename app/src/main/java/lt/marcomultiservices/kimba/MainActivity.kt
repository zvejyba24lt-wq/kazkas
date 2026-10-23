package lt.marcomultiservices.kimba

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import lt.marcomultiservices.kimba.ads.AdsConsent
import lt.marcomultiservices.kimba.data.Places
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.AppRoot
import lt.marcomultiservices.kimba.ui.KimbaTheme
import lt.marcomultiservices.kimba.ui.LocalLang
import lt.marcomultiservices.kimba.ui.Screen
import lt.marcomultiservices.kimba.ui.currentLang

class MainActivity : AppCompatActivity() {
    private val vm: GameViewModel by viewModels()
    private val startScreen = mutableStateOf<Pair<Screen, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDemo(intent)
        // Screenshot runs on the emulator skip the consent form so it cannot cover the game.
        if (!(BuildConfig.DEMO_HOOKS && intent?.getBooleanExtra("noConsent", false) == true)) AdsConsent.gather(this) { app.ads.preload() }
        app.billing.connect()
        val lang = currentLang(this)
        setContent {
            CompositionLocalProvider(LocalLang provides lang) {
                KimbaTheme { AppRoot(vm, startScreen.value) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lt.marcomultiservices.kimba.play.PlayGames.attach(this)
    }

    override fun onPause() {
        lt.marcomultiservices.kimba.play.PlayGames.detach(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDemo(intent)
    }

    /**
     * Demo and screenshot hooks: `-e place Vilnius`, `-e scene storm|dawnfog|night|snow|rain|dusk|day|live`,
     * `-e catch moon_pike`, `-e fight pike`, `-e screen book|forecast|settings|lake|shop|map|quests`,
     * `-e water lake|pond|river|sea`, `-e trip bodensee`, `-e tackle float|feeder|spin`, `--ez box true`, `--ez sharepreview true` (share picture of the shown catch), `-e friend CODE`, `-e ice 15` (ice cm), `-e stand pier|bank|boat`, `-e stormin 2` (storm due in hours, 0 = none), `--ez stormevent true`, `--ei coins 50000 --ei xp 20000` (test money).
     */
    private fun handleDemo(intent: Intent?) {
        if (!BuildConfig.DEMO_HOOKS) return
        val extras = intent?.extras ?: return
        if (extras.keySet().any { it in setOf("scene", "water", "trip", "screen") }) vm.resetLake()
        extras.getString("place")?.let { name -> Places.cities.firstOrNull { it.name == name }?.let { vm.choosePlace(it) } }
        if (extras.containsKey("coins") || extras.containsKey("xp")) vm.demoProgress(extras.getInt("coins", -1), extras.getInt("xp", -1))
        extras.getString("water")?.let { w -> lt.marcomultiservices.kimba.core.Water.entries.firstOrNull { it.name.equals(w, true) }?.let { vm.goHome(it) } }
        extras.getString("trip")?.let { id -> lt.marcomultiservices.kimba.core.Destinations.byId(id)?.let { vm.demoTrip(it) } }
        extras.getString("scene")?.let { vm.setDemo(it.takeIf { s -> s != "live" }) }
        extras.getString("catch")?.let { vm.showDemoCatch(it) }
        extras.getString("fight")?.let { vm.demoFight(it, net = extras.getBoolean("net", false)) }
        extras.getString("tackle")?.let { vm.demoTackle(it) }
        if (extras.containsKey("box")) vm.demoBox.value = extras.getBoolean("box")
        if (extras.keySet().any { it in setOf("scene", "water", "trip", "screen", "tackle") } && !extras.containsKey("box")) vm.demoBox.value = false
        extras.getString("cast")?.toFloatOrNull()?.let { vm.demoCast(it) }
        if (extras.containsKey("sharepreview")) vm.demoSharePreview.value = extras.getBoolean("sharepreview")
        extras.getString("friend")?.let { vm.enterCode(it) }
        if (extras.getBoolean("camp", false)) vm.demoCamp()
        extras.getString("ice")?.toDoubleOrNull()?.let { vm.setDemoIce(it) }
        extras.getString("stormin")?.toIntOrNull()?.let { vm.demoStorm(it) }
        if (extras.getBoolean("stormevent", false)) vm.demoStormEvent()
        if (extras.getBoolean("release", false)) vm.releaseCatch()
        extras.getString("stand")?.let { st -> lt.marcomultiservices.kimba.core.Stand.entries.firstOrNull { it.name.equals(st, true) }?.let { vm.demoStand(it) } }
        extras.getString("screen")?.let { s -> Screen.entries.firstOrNull { it.name.equals(s, ignoreCase = true) }?.let { startScreen.value = it to System.nanoTime() } }
    }
}
