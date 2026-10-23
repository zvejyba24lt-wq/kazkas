package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.remember
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
/** A moulded compartment: darker at the top where the wall shades it. */
private val Compartment = Brush.verticalGradient(listOf(Color(0xFF08120D), Color(0xFF142219), Color(0xFF1B2E23)))
private val Rim = Color(0x3384C29A)

/** The closed box among the round buttons: the bait on the hook, with a badge for the rod. Tapping opens it. */
@Composable
fun TackleBoxButton(progress: Progress, enabled: Boolean, onOpen: () -> Unit) {
    val sfx = LocalContext.current.app.sfx
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp).clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { sfx.play(Sfx.Fx.TAP, 0.6f); onOpen() }.padding(4.dp),
    ) {
        Box(Modifier.size(50.dp)) {
            Box(
                Modifier.size(46.dp).align(Alignment.Center).clip(CircleShape).background(Plastic).border(1.dp, Rim, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(32.dp)) { drawItemIcon(progress.gearHere.lure, false) }
            }
            // Which rod is in hand.
            Box(
                Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(Color(0xFF0F1D16)).border(1.dp, Rim, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(17.dp)) { drawItemIcon(progress.gearHere.tackle, false) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(t(S.baitsTitle), color = K.Text, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** The opened box: it slides up from the bottom; the cross, a tap outside or Back closes it. */
@Composable
fun TackleBox(open: Boolean, progress: Progress, onTackle: (Tackle) -> Unit, onBait: (Lure) -> Unit, onShop: () -> Unit, onClose: () -> Unit) {
    val sfx = LocalContext.current.app.sfx
    BackHandler(open, onClose)
    AnimatedVisibility(open, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose))
    }
    AnimatedVisibility(
        open,
        enter = slideInVertically(tween(320)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .clip(RoundedCornerShape(26.dp)).background(Plastic).border(1.dp, Rim, RoundedCornerShape(26.dp)),
            ) {
                // The lid edge: title, two metal latches and the close button.
                Row(
                    Modifier.fillMaxWidth().background(Lid).padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t(S.tackleBox), color = K.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Latch()
                    Spacer(Modifier.width(10.dp))
                    Latch()
                    Spacer(Modifier.width(14.dp))
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(0x33000000))
                            .clickable { sfx.play(Sfx.Fx.TAP, 0.6f); onClose() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Close, t(S.close), tint = K.Text, modifier = Modifier.size(24.dp)) }
                }
                Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp)) {
                    Text(t(S.rodKinds), color = Color(0xFFBFD8C6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (k in Tackle.entries) {
                            // Through the ice only the short winter float rod is fished.
                            val iceBlocked = progress.onIce && k != Tackle.FLOAT
                            val owned = progress.owns(k) && !iceBlocked
                            val sel = progress.gearHere.tackle == k
                            Slot(
                                Modifier.weight(1f), sel, owned,
                                onClick = { sfx.play(Sfx.Fx.TAP, 0.6f); if (owned) onTackle(k) else if (!iceBlocked) onShop() },
                            ) {
                                Canvas(Modifier.size(48.dp)) { drawItemIcon(k, !owned) }
                                Spacer(Modifier.height(4.dp))
                                Text(t(k.label).substringBefore(" ("), color = if (owned) K.Text else K.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                                if (iceBlocked) Locked(t(S.notOnIce)) else if (!owned) Locked(if (progress.level < k.level) t(S.needLevel, k.level) else t(S.inShop))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(t(S.baitsTitle), color = Color(0xFFBFD8C6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    // Baits for the rod in hand first, then the rest of the box.
                    val rod = progress.gearHere.tackle
                    val baits = Lure.entries.sortedBy { if (rod in it.tackle) 0 else 1 }
                    for (row in baits.chunked(3)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (l in row) {
                                val owned = progress.owns(l)
                                val fits = rod in l.tackle
                                // A bait for another rod can be picked when that rod is owned: the rod changes with it.
                                val usable = owned && (fits || (!progress.onIce && l.tackle.any { progress.owns(it) }))
                                Slot(
                                    Modifier.weight(1f), progress.gearHere.lure == l, owned && fits,
                                    onClick = {
                                        sfx.play(Sfx.Fx.TAP, 0.6f)
                                        when {
                                            usable -> { onBait(l); onClose() }
                                            !owned -> onShop()
                                        }
                                    },
                                ) {
                                    Canvas(Modifier.size(44.dp)) { drawItemIcon(l, !(owned && fits)) }
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
}

/** A small metal clasp on the lid. */
@Composable
private fun Latch() {
    Canvas(Modifier.size(22.dp, 14.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height * 0.3f)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE6EBEF), Color(0xFF8C969F))), cornerRadius = r)
        drawRoundRect(Color(0x55000000), topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.35f), size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.3f), cornerRadius = r)
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
