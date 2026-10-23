package lt.marcomultiservices.kimba.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import lt.marcomultiservices.kimba.BuildConfig
import lt.marcomultiservices.kimba.core.DayPhase
import lt.marcomultiservices.kimba.core.Sky
import lt.marcomultiservices.kimba.ads.AdsConsent
import lt.marcomultiservices.kimba.app
import lt.marcomultiservices.kimba.findActivity
import lt.marcomultiservices.kimba.data.DeviceLocation
import lt.marcomultiservices.kimba.data.Place
import lt.marcomultiservices.kimba.data.Places
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.LocalLang
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.t

@Composable
fun SettingsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    ScreenFrame(t(S.settings), onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp, 4.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Where the weather comes from: the current place up top, the list folded away until asked for.
            var choosing by remember { mutableStateOf(false) }
            Card(Icons.Rounded.Place, t(S.place)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(saved?.place?.name ?: "–", color = K.Text, fontSize = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        if (choosing) t(S.close) else t(S.change), color = K.Amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { choosing = !choosing }.padding(10.dp, 6.dp),
                    )
                }
                AnimatedVisibility(choosing) {
                    Column(Modifier.padding(top = 10.dp)) { PlacePicker(selected = saved?.place) { vm.choosePlace(it); choosing = false } }
                }
            }

            Card(Icons.Rounded.Translate, t(S.language)) { LanguagePicker() }

            // Turned on without the phone's permission it would stay silent: ask for it then.
            val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            Card(Icons.Rounded.Tune, null) {
                ToggleRow(Icons.Rounded.NotificationsActive, t(S.notifications), saved?.notify ?: true) { on ->
                    vm.setNotify(on)
                    if (on && android.os.Build.VERSION.SDK_INT >= 33 &&
                        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(K.Line))
                ToggleRow(Icons.Rounded.VolumeUp, t(S.sound), saved?.sound ?: true) { vm.setSound(it) }
            }

            Card(Icons.Rounded.Block, t(S.noAds)) { NoAdsBox(saved?.progress?.noAds == true) }

            val privacyRequired by AdsConsent.privacyOptionsRequired.collectAsStateWithLifecycle()
            Card(Icons.Rounded.Shield, null) {
                if (privacyRequired && activity != null) {
                    LinkRow(t(S.privacyOptions)) { AdsConsent.showPrivacyOptions(activity) }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                }
                LinkRow(t(S.privacyPolicy)) {
                    runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(BuildConfig.PRIVACY_URL))) }
                }
            }

            Text(
                "Kimba ${BuildConfig.VERSION_NAME}  ·  " + t(S.metCredit), color = Color(0x88B4BCC6), fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
            // Only in test builds: never part of the Play build.
            if (BuildConfig.DEMO_HOOKS) AdminPanel(vm, saved?.progress?.level ?: 1)
        }
    }
}

/** One settings group: a paper-edged card with a round badge, like a page in a tackle notebook. */
@Composable
private fun Card(icon: ImageVector, title: String?, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x2EF2B25C), Color(0x14FFFFFF))))
            .border(1.dp, Color(0x40F2B25C), shape).padding(14.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(icon)
                Spacer(Modifier.width(10.dp))
                SectionTitle(title)
            }
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

@Composable
private fun Badge(icon: ImageVector) {
    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0x33F2B25C)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = K.Amber, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onChange(!on) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(icon)
        Spacer(Modifier.width(12.dp))
        Text(label, color = K.Text, fontSize = 15.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = K.Amber, checkedThumbColor = Color(0xFF2A1A06)))
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = K.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = K.Muted)
    }
}

@Composable
private fun NoAdsBox(owned: Boolean) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val product by context.app.billing.product.collectAsStateWithLifecycle()
    var failed by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(t(S.noAdsBody), color = K.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(10.dp))
        if (owned) {
            Text(t(S.bought), color = K.Reed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
            androidx.compose.material3.Button(
                { failed = activity == null || !context.app.billing.launch(activity) },
                colors = lt.marcomultiservices.kimba.ui.amberButtonColors(), modifier = Modifier.fillMaxWidth().height(46.dp),
            ) { Text(t(S.buy) + (product?.oneTimePurchaseOfferDetails?.formattedPrice?.let { " · $it" } ?: "")) }
            if (failed) Text(t(S.notAvailable), color = K.Amber, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun LanguagePicker() {
    val lang = LocalLang.current
    val explicit = !AppCompatDelegate.getApplicationLocales().isEmpty
    val options = listOf(
        null to t(S.phoneLanguage), "lt" to "Lietuvių", "de" to "Deutsch", "en" to "English",
    )
    Chips(options.map { it.second }, selected = options.indexOfFirst { (tag, _) -> if (tag == null) !explicit else explicit && tag == lang.name.lowercase() }) { i ->
        val tag = options[i].first
        AppCompatDelegate.setApplicationLocales(if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, label ->
            val sel = i == selected
            val shape = RoundedCornerShape(18.dp)
            Box(
                Modifier.clip(shape).background(if (sel) K.Amber else Color(0x1FFFFFFF)).border(1.dp, if (sel) K.Amber else K.Line, shape)
                    .clickable { onPick(i) }.padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(label, color = if (sel) Color(0xFF2A1A06) else K.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** "Use my location" plus a list of cities. */
@Composable
fun PlacePicker(selected: Place?, onPick: (Place) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val fallbackName = t(S.myPlace)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { failed = true; return@rememberLauncherForActivityResult }
        busy = true
        scope.launch {
            val p = DeviceLocation.find(context, fallbackName)
            busy = false
            if (p != null) onPick(p) else failed = true
        }
    }
    Column {
        OutlinedButton({ failed = false; permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = K.Amber)
            else Icon(Icons.Rounded.MyLocation, null, tint = K.Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (busy) t(S.locating) else t(S.useMyPlace), color = K.Text)
        }
        if (failed) Text(t(S.noLocation), color = K.Amber, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(10.dp))
        Chips(Places.cities.map { it.name }, selected = Places.cities.indexOfFirst { it.name == selected?.name && selected?.fromDevice != true }) { onPick(Places.cities[it]) }
    }
}

/** A temporary test menu: level, coins, casts, all gear, and any month, time of day, sky or ice. */
@Composable
private fun AdminPanel(vm: GameViewModel, level: Int) {
    val world by vm.admin.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x22FF5A4A)).border(1.dp, Color(0x88FF5A4A), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("ADMIN (laikinas, tik testinėje versijoje)", color = Color(0xFFFF8A7A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Lygis (dabar $level)", color = K.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        val levels = listOf(1, 2, 3, 5, 7, 10, 12, 15, 20, 25, 30)
        Chips(levels.map { "$it" }, levels.indexOf(level)) { vm.adminLevel(levels[it]) }
        Spacer(Modifier.height(12.dp))
        Text("Monetos ir užmetimai", color = K.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        val money = listOf("+1 000" to 1_000, "+10 000" to 10_000, "+100 000" to 100_000, "Į nulį" to -100_000_000)
        Chips(money.map { it.first } + listOf("+10 užmetimų", "Visos meškerės ir masalai"), -1) { i ->
            when {
                i < money.size -> vm.adminCoins(money[i].second)
                i == money.size -> vm.adminCasts()
                else -> vm.adminAllGear()
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Mėnuo (metų laikas)", color = K.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        val months = listOf("Sau", "Vas", "Kov", "Bal", "Geg", "Bir", "Lie", "Rgp", "Rgs", "Spa", "Lap", "Gru")
        Chips(listOf("Tikras") + months, world.month ?: 0) { vm.setAdminWorld(world.copy(month = it.takeIf { m -> m > 0 })) }
        Spacer(Modifier.height(12.dp))
        Text("Paros metas", color = K.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        val phases = listOf(DayPhase.DAWN, DayPhase.DAY, DayPhase.DUSK, DayPhase.NIGHT)
        Chips(listOf("Tikras") + phases.map { t(S.phase(it)) }, world.phase?.let { phases.indexOf(it) + 1 } ?: 0) { vm.setAdminWorld(world.copy(phase = phases.getOrNull(it - 1))) }
        Spacer(Modifier.height(12.dp))
        Text("Orai", color = K.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        val skies = Sky.entries
        Chips(listOf("Tikri") + skies.map { t(S.sky(it)) }, world.sky?.let { skies.indexOf(it) + 1 } ?: 0) { vm.setAdminWorld(world.copy(sky = skies.getOrNull(it - 1))) }
        Spacer(Modifier.height(12.dp))
        Text("Ledas namų ežere", color = K.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        val ice = listOf<Pair<String, Double?>>("Tikras" to null, "Nėra" to 0.0, "Kraštuose 5 cm" to 5.0, "Laiko 16 cm" to 16.0)
        Chips(ice.map { it.first }, -1) { vm.adminIce(ice[it].second) }
        Spacer(Modifier.height(10.dp))
        Text("Orai ir metų laikas lieka, kol paspausi „Tikras“ arba iš naujo paleisi žaidimą.", color = K.Muted, fontSize = 11.sp)
    }
}
