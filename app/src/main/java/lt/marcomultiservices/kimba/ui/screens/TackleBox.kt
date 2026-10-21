package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.marcomultiservices.kimba.app
import lt.marcomultiservices.kimba.audio.Sfx
import lt.marcomultiservices.kimba.core.Lure
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.Tackle
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.t

// The tackle box on the lake: a closed box shows the rod and bait in use; opened, it has the
// three kinds of rod on top and a compartment for every bait below.

private val Plastic = Brush.verticalGradient(listOf(Color(0xFF3B5A45), Color(0xFF243B2D)))
private val Lid = Brush.verticalGradient(listOf(Color(0xFF4C6E56), Color(0xFF2E4A38)))
private val Compartment = Brush.verticalGradient(listOf(Color(0xFF0F1D16), Color(0xFF1A2E23)))
private val Rim = Color(0x3384C29A)

/** The closed box: what is on the line now. Tapping opens it. */
@Composable
fun TackleBoxButton(progress: Progress, enabled: Boolean, onOpen: () -> Unit) {
    val sfx = LocalContext.current.app.sfx
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.clip(shape).background(Plastic).border(1.dp, Rim, shape)
            .clickable(enabled = enabled) { sfx.play(Sfx.Fx.TAP, 0.6f); onOpen() }
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Compartment), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(28.dp)) { drawItemIcon(progress.tackle, false) }
        }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Compartment), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(28.dp)) { drawItemIcon(progress.gear.lure, false) }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(t(S.tackleBox), color = Color(0xFFBFD8C6), fontSize = 10.sp)
            Text(t(progress.tackle.label) + " · " + t(progress.gear.lure.label), color = K.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Rounded.KeyboardArrowUp, null, tint = K.Amber, modifier = Modifier.size(20.dp))
    }
}

/** The opened box over the lake. */
@Composable
fun TackleBox(progress: Progress, onTackle: (Tackle) -> Unit, onBait: (Lure) -> Unit, onShop: () -> Unit, onClose: () -> Unit) {
    val sfx = LocalContext.current.app.sfx
    Box(
        Modifier.fillMaxSize().background(Color(0x88000000)).clickable(MutableInteractionSource(), null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp)
                .clickable(MutableInteractionSource(), null) {}
                .clip(RoundedCornerShape(26.dp)).background(Plastic).border(1.dp, Rim, RoundedCornerShape(26.dp)),
        ) {
            // The lid, opened back, with its hinge.
            Box(Modifier.fillMaxWidth().height(34.dp).background(Lid), contentAlignment = Alignment.Center) {
                Box(Modifier.width(64.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x55000000)))
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp)) {
                Text(t(S.rodKinds), color = Color(0xFFBFD8C6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (k in Tackle.entries) {
                        val owned = progress.owns(k)
                        val sel = progress.tackle == k
                        Slot(
                            Modifier.weight(1f), sel, owned,
                            onClick = { sfx.play(Sfx.Fx.TAP, 0.6f); if (owned) onTackle(k) else onShop() },
                        ) {
                            Canvas(Modifier.size(44.dp)) { drawItemIcon(k, !owned) }
                            Spacer(Modifier.height(4.dp))
                            Text(t(k.label).substringBefore(" ("), color = if (owned) K.Text else K.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                            if (!owned) Locked(if (progress.level < k.level) t(S.needLevel, k.level) else t(S.inShop))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(t(S.baitsTitle), color = Color(0xFFBFD8C6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                // Baits for the rod in hand first, then the rest of the box.
                val baits = Lure.entries.sortedBy { if (progress.tackle in it.tackle) 0 else 1 }
                for (row in baits.chunked(3)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (l in row) {
                            val owned = progress.owns(l)
                            val fits = progress.tackle in l.tackle
                            // A bait for another rod can be picked when that rod is owned: the rod changes with it.
                            val usable = owned && (fits || l.tackle.any { progress.owns(it) })
                            Slot(
                                Modifier.weight(1f), progress.gear.lure == l, owned && fits,
                                onClick = {
                                    sfx.play(Sfx.Fx.TAP, 0.6f)
                                    when {
                                        usable -> { onBait(l); onClose() }
                                        !owned -> onShop()
                                    }
                                },
                            ) {
                                Canvas(Modifier.size(40.dp)) { drawItemIcon(l, !(owned && fits)) }
                                Spacer(Modifier.height(4.dp))
                                Text(t(l.label), color = if (owned) K.Text else K.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
                                when {
                                    !owned -> Locked(if (progress.level < l.level) t(S.needLevel, l.level) else t(S.inShop))
                                    !fits -> Text(t(when (l.tackle) { setOf(Tackle.SPIN) -> S.onlySpin; setOf(Tackle.FEEDER) -> S.onlyFeeder; else -> S.floatOrFeeder }), color = K.Amber, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** One compartment of the box. */
@Composable
private fun Slot(modifier: Modifier, selected: Boolean, bright: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(Compartment)
            .border(if (selected) 2.dp else 1.dp, if (selected) K.Amber else if (bright) Rim else Color(0x1AFFFFFF), shape)
            .clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun Locked(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Lock, null, tint = K.Muted, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(2.dp))
        Text(text, color = K.Muted, fontSize = 10.sp, maxLines = 1)
    }
}
