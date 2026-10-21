package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.t

@Composable
fun ScreenFrame(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF13233A), K.Ink)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, t(S.back), tint = K.Text,
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack).padding(10.dp),
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

    ScreenFrame(t(S.book), onBack) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp)) {
            item {
                Text(t(S.bookProgress, caughtKinds.size, total), color = K.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
            }
            items(Fish.all, key = { it.id }) { s ->
                val mine = catches.filter { it.species == s.id && it.golden == null }
                val where = s.onlyAt?.mapNotNull { Destinations.byId(it) }?.joinToString { t(it.name) }
                    ?: Water.entries.filter { it in s.waters }.joinToString { t(it.label) }
                FishCard(s.id, false, t(s.name), t(s.habit), mine, time, where)
            }
            item {
                Spacer(Modifier.height(18.dp))
                SectionTitle(t(S.goldenFish))
                Spacer(Modifier.height(10.dp))
            }
            items(Fish.goldens, key = { it.id }) { g ->
                val mine = catches.filter { it.golden == g.id }
                FishCard(g.base.id, true, t(g.name), t(g.condition), mine, time)
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
                Text("${t(S.best)}: %.1f cm · %s".format(best.cm, weight), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(t(S.count, mine.size), color = K.Muted, fontSize = 11.sp)
            } else {
                Text(t(S.unknown), color = Color(0x88B4BCC6), fontSize = 11.sp)
            }
        }
    }
}
