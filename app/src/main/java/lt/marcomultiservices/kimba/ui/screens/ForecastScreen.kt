package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.marcomultiservices.kimba.core.Astro
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.DayPhase
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.GoldenWindow
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.LocalLang
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.fish.FishArt
import lt.marcomultiservices.kimba.ui.t
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ForecastScreen(vm: GameViewModel, onBack: () -> Unit) {
    val c by vm.conditions.collectAsStateWithLifecycle()
    val forecast by vm.forecast.collectAsStateWithLifecycle()
    val windows = remember(forecast, c) { vm.goldenWindows() }
    ScreenFrame(t(S.forecast), onBack) {
        val cond = c ?: return@ScreenFrame
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp, 4.dp, 16.dp, 24.dp)) {
            NowCard(cond)
            Spacer(Modifier.height(18.dp))
            SectionTitle(t(S.activeNow))
            Spacer(Modifier.height(8.dp))
            ActiveFish(cond)
            Text(t(S.nearFar), color = K.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(18.dp))
            SectionTitle(t(S.goldenWindows))
            Spacer(Modifier.height(8.dp))
            if (windows.isEmpty()) {
                val now = System.currentTimeMillis()
                Text(
                    t(S.noWindows, Astro.daysUntilMoon(now, 0.5).roundToInt(), Astro.daysUntilMoon(now, 0.0).roundToInt()),
                    color = K.Muted, fontSize = 14.sp,
                )
            } else {
                windows.take(8).forEach { WindowCard(it) }
            }
            Spacer(Modifier.height(24.dp))
            Text(t(S.metCredit), color = Color(0x88B4BCC6), fontSize = 11.sp)
        }
    }
}

@Composable
private fun NowCard(c: Conditions) {
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(Color(0x332F6FBF), Color(0x14FFFFFF)))).border(1.dp, K.Line, shape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(skyIcon(c.weather.sky, c.phase), null, tint = K.Amber, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("${c.weather.tempC.roundToInt()}° · ${t(S.sky(c.weather.sky))}", style = MaterialTheme.typography.headlineSmall, color = K.Text)
                Text("${t(S.phase(c.phase))} · ${t(S.biteIndex)} ${Fish.biteIndex(c)}", color = K.Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        val trend = when {
            c.weather.pressureTrend < -0.8 -> t(S.falling)
            c.weather.pressureTrend > 0.8 -> t(S.risingP)
            else -> t(S.steady)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Fact(t(S.water), "${c.waterTempC.roundToInt()}°")
            Fact(t(S.pressure), "${c.weather.pressureHpa.roundToInt()} · $trend")
            Fact(t(S.wind), "${c.weather.windMs.roundToInt()} m/s")
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(22.dp)) {
                drawCircle(Color(0xFF2A3348))
                val lit = c.moonIllumination.toFloat()
                drawCircle(Color(0xFFF1F0E6), radius = size.minDimension / 2 * (0.25f + 0.75f * lit), alpha = 0.4f + 0.6f * lit)
            }
            Spacer(Modifier.width(8.dp))
            Text("${t(S.moon)}: ${t(S.moonName(c.moonAge))} (${(c.moonIllumination * 100).roundToInt()} %)", color = K.Text, fontSize = 13.sp)
        }
        val tips = buildList {
            if (c.falling > 0.3) add(S.tipFalling)
            if (c.rain >= 0.3) add(S.tipRain)
            if (c.phase == DayPhase.DAWN || c.phase == DayPhase.DUSK) add(S.tipDusk)
            if (c.waterTempC < 6) add(S.tipCold)
            if (c.weather.cloud < 0.3 && c.phase == DayPhase.DAY) add(S.tipSun)
        }
        if (tips.isNotEmpty()) Spacer(Modifier.height(10.dp))
        tips.forEach { Text("• ${t(it)}", color = K.Reed, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)) }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(value, color = K.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = K.Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ActiveFish(c: Conditions) {
    val act = remember(c) { Fish.activities(c).entries.sortedByDescending { it.value }.take(5) }
    val max = act.first().value
    act.forEach { (s, a) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.width(64.dp).height(30.dp)) {
                val w = size.width * 0.95f
                FishArt.draw(this, s.id, false, Rect(Offset(0f, size.height / 2 - w / 2), Size(w, w)))
            }
            Spacer(Modifier.width(10.dp))
            Text(t(s.name), color = K.Text, fontSize = 14.sp, modifier = Modifier.width(110.dp))
            Canvas(Modifier.weight(1f).height(8.dp)) {
                drawRoundRect(Color(0x22FFFFFF), cornerRadius = CornerRadius(4.dp.toPx()))
                drawRoundRect(
                    Brush.horizontalGradient(listOf(Color(0xFF5B8BD0), K.Reed, K.Amber), 0f, size.width),
                    size = Size(size.width * (a / max).toFloat().coerceIn(0.04f, 1f), size.height), cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun WindowCard(w: GoldenWindow) {
    val lang = LocalLang.current
    val locale = when (lang) { lt.marcomultiservices.kimba.core.Lang.LT -> Locale("lt"); lt.marcomultiservices.kimba.core.Lang.DE -> Locale.GERMAN; else -> Locale.ENGLISH }
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    fun fmt(ms: Long): String {
        val z = Instant.ofEpochMilli(ms).atZone(zone)
        val day = if (z.toLocalDate() == LocalDate.now(zone)) "" else z.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) + " "
        return day + DateTimeFormatter.ofPattern("HH:mm").format(z)
    }
    val active = w.startMillis <= now
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(if (active) 0x55FFD27A else 0x30FFD27A), Color(0x10FFD27A))))
            .border(1.dp, Color(0x66FFD27A), shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.width(70.dp).height(34.dp)) {
            val ww = size.width * 0.95f
            FishArt.draw(this, w.golden.base.id, true, Rect(Offset(0f, size.height / 2 - ww / 2), Size(ww, ww)))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t(w.golden.name), color = K.Gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(t(w.golden.condition), color = K.Muted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (active) Text(t(S.activeWindow), color = K.Gold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("${if (active) t(S.now) else fmt(w.startMillis)} – ${fmt(w.endMillis)}", color = K.Text, fontSize = 13.sp)
        }
    }
}
