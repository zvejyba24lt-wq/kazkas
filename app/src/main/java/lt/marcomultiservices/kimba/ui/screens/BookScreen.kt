package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.IceSkating
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.marcomultiservices.kimba.core.Catch
import lt.marcomultiservices.kimba.core.Destinations
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.LocalLang
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.t

@Composable
fun ScreenFrame(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val sfx = (androidx.compose.ui.platform.LocalContext.current.applicationContext as lt.marcomultiservices.kimba.KimbaApp).sfx
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF13233A), K.Ink)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, t(S.back), tint = K.Text,
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable { sfx.play(lt.marcomultiservices.kimba.audio.Sfx.Fx.TAP, 0.5f); onBack() }.padding(10.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, color = K.Text)
            }
            content()
        }
    }
}

@Composable
fun BookScreen(vm: GameViewModel, onBack: () -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val catches = saved?.catches ?: emptyList()
    val time = lt.marcomultiservices.kimba.ui.scene.rememberSceneTime()
    val caughtKinds = catches.map { it.golden ?: it.species }.toSet()
    val total = Fish.all.size + Fish.goldens.size

    // Filter: null = everything, a water, or only the golden fish.
    var filter by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("all") }
    val species = Fish.all.filter { filter == "all" || it.waters.any { w -> w.name == filter } }
    val goldens = Fish.goldens.filter { filter == "all" || filter == "golden" || it.base.waters.any { w -> w.name == filter } }

    ScreenFrame(t(S.book), onBack) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp)) {
            item {
                Text(t(S.bookProgress, caughtKinds.size, total), color = K.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                    val cr = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    drawRoundRect(Color(0x33FFFFFF), cornerRadius = cr)
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Color(0xFF8DC26F), K.Gold)),
                        size = androidx.compose.ui.geometry.Size(size.width * caughtKinds.size / total.toFloat(), size.height), cornerRadius = cr,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    val chips = listOf("all" to t(S.allFish)) + Water.entries.map { it.name to t(it.label) } + ("golden" to t(S.goldenFish))
                    for ((key, label) in chips) {
                        val sel = key == filter
                        Text(
                            label, color = if (sel) Color(0xFF2A1A06) else K.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 6.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (sel) K.Amber else Color(0x22FFFFFF))
                                .border(1.dp, if (sel) K.Amber else K.Line, RoundedCornerShape(16.dp))
                                .clickable { filter = key }.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // The legends first: one named giant per home water, its clues coming one a day.
            if (filter == "all" || filter == "golden") {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionTitle(t(S.legendsTitle))
                    Spacer(Modifier.height(4.dp))
                    Text(t(S.legendsNote), color = K.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                }
                items(Fish.legends, key = { it.id }) { g ->
                    val got = catches.filter { it.golden == g.id }.maxByOrNull { it.grams }
                    val known = saved?.progress?.clues(java.time.LocalDate.now().toEpochDay()) ?: 1
                    LegendCard(g, got, known, time)
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
            if (filter != "golden") items(species, key = { it.id }) { s ->
                val mine = catches.filter { it.species == s.id && it.golden == null }
                val lang = LocalLang.current
                val where = s.onlyAt?.mapNotNull { Destinations.byId(it) }?.joinToString { it.name[lang] }
                    ?: Water.entries.filter { it in s.waters }.joinToString { it.label[lang] }
                FishCard(s.id, false, t(s.name), t(s.habit), mine, time, where)
            }
            if (goldens.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(18.dp))
                    SectionTitle(t(S.goldenFish))
                    Spacer(Modifier.height(10.dp))
                }
                items(goldens, key = { it.id }) { g ->
                    val mine = catches.filter { it.golden == g.id }
                    FishCard(g.base.id, true, t(g.name), t(g.condition), mine, time)
                }
            }
        }
    }
}

@Composable
private fun FishCard(species: String, golden: Boolean, name: String, note: String, mine: List<Catch>, timeState: androidx.compose.runtime.MutableFloatState, where: String? = null) {
    val known = mine.isNotEmpty()
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(shape)
            .background(if (golden && known) Brush.horizontalGradient(listOf(Color(0x40FFD27A), Color(0x14FFD27A))) else Brush.horizontalGradient(listOf(Color(0x26FFFFFF), Color(0x0FFFFFFF))))
            .border(1.dp, if (golden) Color(0x55FFD27A) else K.Line, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lt.marcomultiservices.kimba.ui.fish.FishPicture(species, golden, timeState.takeIf { known }, Modifier.width(118.dp).height(64.dp), silhouette = !known)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (known || golden) name else "???", color = if (golden) K.Gold else K.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(note, color = K.Muted, fontSize = 12.sp, lineHeight = 16.sp)
            if (where != null) Text(where, color = Color(0xFF8FD3FF), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            if (known) {
                val best = mine.maxBy { it.cm }
                val weight = if (best.grams >= 1000) "%.2f kg".format(best.grams / 1000.0) else "${best.grams} g"
                Text("${t(S.best)}: %.0f cm · %s".format(best.cm, weight), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(t(S.count, mine.size), color = K.Muted, fontSize = 11.sp)
                // Weather stamps: lit for each real weather this kind was caught in.
                val got = mine.flatMap { lt.marcomultiservices.kimba.core.Stamp.of(it) }.toSet()
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)) {
                    for (st in lt.marcomultiservices.kimba.core.Stamp.entries) {
                        val on = st in got
                        androidx.compose.material3.Icon(
                            stampIcon(st), t(st.label),
                            tint = if (on) stampColor(st) else Color(0x33FFFFFF),
                            modifier = Modifier.size(18.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (on) stampColor(st).copy(alpha = 0.18f) else Color.Transparent).padding(2.dp),
                        )
                    }
                }
            } else {
                Text(t(S.unknown), color = Color(0x88B4BCC6), fontSize = 11.sp)
            }
        }
    }
}

private fun stampIcon(s: lt.marcomultiservices.kimba.core.Stamp) = when (s) {
    lt.marcomultiservices.kimba.core.Stamp.SUN -> Icons.Rounded.WbSunny
    lt.marcomultiservices.kimba.core.Stamp.RAIN -> Icons.Rounded.Umbrella
    lt.marcomultiservices.kimba.core.Stamp.FOG -> Icons.Rounded.Dehaze
    lt.marcomultiservices.kimba.core.Stamp.SNOW -> Icons.Rounded.AcUnit
    lt.marcomultiservices.kimba.core.Stamp.NIGHT -> Icons.Rounded.NightsStay
    lt.marcomultiservices.kimba.core.Stamp.ICE -> Icons.Rounded.IceSkating
}

private fun stampColor(s: lt.marcomultiservices.kimba.core.Stamp) = when (s) {
    lt.marcomultiservices.kimba.core.Stamp.SUN -> Color(0xFFFFC94D)
    lt.marcomultiservices.kimba.core.Stamp.RAIN -> Color(0xFF6FB6FF)
    lt.marcomultiservices.kimba.core.Stamp.FOG -> Color(0xFFC8CDD2)
    lt.marcomultiservices.kimba.core.Stamp.SNOW -> Color(0xFFE6F2FF)
    lt.marcomultiservices.kimba.core.Stamp.NIGHT -> Color(0xFFB59BFF)
    lt.marcomultiservices.kimba.core.Stamp.ICE -> Color(0xFF9FE3F0)
}

@Composable
private fun LegendCard(g: lt.marcomultiservices.kimba.core.Golden, got: Catch?, known: Int, timeState: androidx.compose.runtime.MutableFloatState) {
    val lang = LocalLang.current
    val shape = RoundedCornerShape(18.dp)
    val violet = Color(0xFFC79BFF)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0x33C79BFF), Color(0x0FC79BFF))))
            .border(1.dp, Color(0x66C79BFF), shape)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            lt.marcomultiservices.kimba.ui.fish.FishPicture(g.base.id, false, timeState.takeIf { got != null }, Modifier.width(118.dp).height(64.dp), silhouette = got == null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t(g.name), color = violet, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${t(g.base.name)} · ${g.legendOf?.label?.get(lang) ?: ""}", color = K.Muted, fontSize = 12.sp)
                if (got != null) Text(t(S.legendDone, lt.marcomultiservices.kimba.core.Plural.kg(got.grams, lang)), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        for ((i, clue) in g.clues.withIndex()) {
            val open = got != null || i < known
            Text((if (open) "✦ " + t(clue) else "✧ " + t(S.clueLocked)), color = if (open) K.Text else Color(0x88B4BCC6), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
