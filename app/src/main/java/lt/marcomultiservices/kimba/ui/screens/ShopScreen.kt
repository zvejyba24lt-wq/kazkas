package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.marcomultiservices.kimba.core.FloatStyle
import lt.marcomultiservices.kimba.core.Item
import lt.marcomultiservices.kimba.core.L
import lt.marcomultiservices.kimba.core.Line
import lt.marcomultiservices.kimba.core.Lure
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.Reel
import lt.marcomultiservices.kimba.core.Rod
import lt.marcomultiservices.kimba.core.Upgrade
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.amberButtonColors
import lt.marcomultiservices.kimba.ui.scene.drawFloat
import lt.marcomultiservices.kimba.ui.t
import kotlin.math.roundToInt

@Composable
fun ShopScreen(vm: GameViewModel, onBack: () -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val p = saved?.progress ?: Progress()
    val sections: List<Pair<L, List<Item>>> = listOf(
        S.lures to Lure.entries, S.rods to Rod.entries, S.reels to Reel.entries,
        S.lines to Line.entries, S.upgrades to Upgrade.entries, S.floats to FloatStyle.entries,
    )
    ScreenFrame(t(S.shop), onBack) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            CoinsLabel(p.coins, 22)
            Spacer(Modifier.weight(1f))
            LevelBar(p, 110)
        }
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp)) {
            item { Text(t(S.shopNote), color = K.Muted, fontSize = 12.sp, lineHeight = 16.sp) }
            for ((title, items) in sections) {
                item(key = "t" + title.get(lt.marcomultiservices.kimba.core.Lang.EN)) {
                    Spacer(Modifier.height(18.dp))
                    SectionTitle(t(title))
                    Spacer(Modifier.height(6.dp))
                }
                items(items, key = { it.key }) { item -> ShopRow(item, p, { vm.buy(item) }, { vm.equip(item) }) }
            }
        }
    }
}

private fun isEquipped(p: Progress, item: Item): Boolean = when (item) {
    is Lure -> p.lure == item
    is Rod -> p.rod == item
    is Reel -> p.reel == item
    is Line -> p.line == item
    is FloatStyle -> p.float == item
    else -> false
}

@Composable
private fun note(item: Item): String = when (item) {
    is Lure -> t(item.note)
    is Rod -> t(S.strengthNote, ((item.strength - 1) * 100).roundToInt())
    is Reel -> t(S.speedNote, ((item.speed - 1) * 100).roundToInt())
    is Line -> t(S.lineNote, (item.strength * 100).roundToInt(), ((1 - item.shyFactor) * 100).roundToInt())
    is FloatStyle -> t(S.lookNote)
    is Upgrade -> t(item.note)
    else -> ""
}

@Composable
private fun ShopRow(item: Item, p: Progress, onBuy: () -> Unit, onEquip: () -> Unit) {
    val owned = p.owns(item)
    val equipped = isEquipped(p, item)
    val lockedByLevel = p.level < item.level
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0x26FFFFFF), Color(0x0FFFFFFF))))
            .border(1.dp, if (equipped) K.Amber else K.Line, shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x33101A28)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(46.dp)) { drawItemIcon(item, lockedByLevel && !owned) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t(item.label), color = K.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(note(item), color = K.Muted, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(8.dp))
        when {
            equipped -> Text(t(S.equipped), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            owned && item is Upgrade -> Text(t(S.owned), color = K.Reed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            owned -> OutlinedButton(onEquip, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(t(S.equip), color = K.Text, fontSize = 13.sp) }
            lockedByLevel -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = K.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text(t(S.needLevel, item.level), color = K.Muted, fontSize = 12.sp)
            }
            else -> Button(onBuy, enabled = p.coins >= item.price, colors = amberButtonColors(), contentPadding = PaddingValues(horizontal = 12.dp)) {
                CoinIcon(Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("%,d".format(item.price).replace(',', ' '), fontSize = 13.sp)
            }
        }
    }
}

/** Small drawn icons for the shop rows. */
private fun DrawScope.drawItemIcon(item: Item, dim: Boolean) {
    val a = if (dim) 0.35f else 1f
    val w = size.width
    val h = size.height
    when (item) {
        is Lure -> when (item) {
            Lure.WORM, Lure.MAGGOT -> {
                val col = if (item == Lure.WORM) Color(0xFFC8746A) else Color(0xFFF2E8C8)
                val p = Path().apply { moveTo(w * 0.15f, h * 0.6f); cubicTo(w * 0.35f, h * 0.2f, w * 0.55f, h * 0.9f, w * 0.85f, h * 0.4f) }
                drawPath(p, col.copy(alpha = a), style = Stroke(if (item == Lure.WORM) w * 0.12f else w * 0.16f, cap = StrokeCap.Round))
                for (i in 1..4) drawCircle(Color.Black.copy(alpha = 0.15f * a), w * 0.02f, Offset(w * (0.2f + i * 0.13f), h * (0.55f - (i % 2) * 0.08f)))
            }
            Lure.CORN, Lure.BOILIE -> {
                val col = if (item == Lure.CORN) Color(0xFFF5C83A) else Color(0xFFB2603A)
                for ((x, y) in listOf(0.35f to 0.4f, 0.6f to 0.45f, 0.48f to 0.66f)) {
                    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f * a), col.copy(alpha = a)), Offset(w * (x - 0.05f), h * (y - 0.05f)), w * 0.18f), w * (if (item == Lure.BOILIE) 0.16f else 0.12f), Offset(w * x, h * y))
                }
            }
            Lure.SPINNER, Lure.PILK -> {
                drawLine(Color(0xFFB0B8C0).copy(alpha = a), Offset(w * 0.5f, h * 0.05f), Offset(w * 0.5f, h * 0.95f), strokeWidth = w * 0.03f)
                drawOval(Brush.linearGradient(listOf(Color(0xFFF4F4F4).copy(alpha = a), Color(0xFF9AA4AE).copy(alpha = a), if (item == Lure.PILK) Color(0xFFE04030).copy(alpha = a) else Color(0xFFD8B040).copy(alpha = a))), Offset(w * 0.32f, h * 0.18f), Size(w * 0.36f, h * (if (item == Lure.PILK) 0.62f else 0.5f)))
                drawCircle(Color(0xFFE04030).copy(alpha = a), w * 0.05f, Offset(w * 0.5f, h * 0.85f))
            }
            Lure.WOBBLER -> {
                drawOval(Brush.verticalGradient(listOf(Color(0xFF2E7A4A).copy(alpha = a), Color(0xFFE8E0A0).copy(alpha = a))), Offset(w * 0.12f, h * 0.32f), Size(w * 0.72f, h * 0.34f))
                drawCircle(Color.Black.copy(alpha = a), w * 0.05f, Offset(w * 0.26f, h * 0.45f))
                drawLine(Color(0xFFDDE8F0).copy(alpha = 0.8f * a), Offset(w * 0.12f, h * 0.55f), Offset(w * 0.02f, h * 0.7f), strokeWidth = w * 0.08f)
            }
            Lure.LIVEBAIT -> lt.marcomultiservices.kimba.ui.fish.FishArt.draw(this, "roach", false, androidx.compose.ui.geometry.Rect(Offset(w * 0.05f, 0f), Size(w * 0.9f, h)), silhouette = dim)
        }
        is Rod -> {
            val col = when (item) { Rod.BASIC -> Color(0xFFC8A060); Rod.CARBON -> Color(0xFF30343A); Rod.PRO -> Color(0xFF2A4A7A); Rod.LEGEND -> Color(0xFFD8A830) }
            drawLine(col.copy(alpha = a), Offset(w * 0.15f, h * 0.9f), Offset(w * 0.9f, h * 0.1f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
            drawLine(Color(0xFF3A2A1A).copy(alpha = a), Offset(w * 0.15f, h * 0.9f), Offset(w * 0.32f, h * 0.72f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
            for (i in 1..3) drawCircle(Color(0xFFC0C8D0).copy(alpha = a), w * 0.03f, Offset(w * (0.35f + i * 0.14f), h * (0.68f - i * 0.15f)), style = Stroke(w * 0.015f))
        }
        is Reel -> {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFE8ECF0).copy(alpha = a), Color(0xFF6A7480).copy(alpha = a))), w * 0.34f, Offset(w * 0.5f, h * 0.5f))
            drawCircle(Color(0xFF2A3038).copy(alpha = a), w * 0.12f, Offset(w * 0.5f, h * 0.5f))
            val speed = Reel.entries.indexOf(item)
            for (i in 0..speed) drawArc(Color(0xFFF2B25C).copy(alpha = a), -90f + i * 25f, 18f, false, Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.8f), style = Stroke(w * 0.05f))
            drawLine(Color(0xFF2A3038).copy(alpha = a), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.88f, h * 0.2f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
        }
        is Line -> {
            val th = w * (0.015f + Line.entries.indexOf(item) * 0.02f)
            drawCircle(Color(0xFF3A6A8A).copy(alpha = a), w * 0.32f, Offset(w * 0.5f, h * 0.5f))
            for (i in 0..5) drawCircle(Color(0xFFDDEEFF).copy(alpha = 0.7f * a), w * (0.12f + i * 0.035f), Offset(w * 0.5f, h * 0.5f), style = Stroke(th))
        }
        is FloatStyle -> drawFloat(Offset(w * 0.5f, h * 0.62f), w * 0.9f, item, glow = item == FloatStyle.GLOW, alpha = a)
        is Upgrade -> when (item) {
            Upgrade.BOAT -> {
                val hull = Path().apply { moveTo(w * 0.05f, h * 0.5f); lineTo(w * 0.95f, h * 0.5f); lineTo(w * 0.8f, h * 0.75f); lineTo(w * 0.2f, h * 0.75f); close() }
                drawPath(hull, Brush.verticalGradient(listOf(Color(0xFF8A5A30).copy(alpha = a), Color(0xFF5A3418).copy(alpha = a)), h * 0.5f, h * 0.75f))
                drawLine(Color(0xFFDDC8A0).copy(alpha = a), Offset(w * 0.05f, h * 0.5f), Offset(w * 0.95f, h * 0.5f), strokeWidth = w * 0.04f)
                drawLine(Color(0xFF6A4A2A).copy(alpha = a), Offset(w * 0.6f, h * 0.45f), Offset(w * 0.92f, h * 0.15f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
                drawLine(Color(0xFF5B8BD0).copy(alpha = 0.6f * a), Offset(0f, h * 0.82f), Offset(w, h * 0.82f), strokeWidth = w * 0.03f)
            }
            else -> {
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF5A7A4A).copy(alpha = a), Color(0xFF3A5030).copy(alpha = a))), Offset(w * 0.12f, h * 0.35f), Size(w * 0.76f, h * 0.45f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f))
                drawRoundRect(Color(0xFF7A9A6A).copy(alpha = a), Offset(w * 0.08f, h * 0.25f), Size(w * 0.84f, h * 0.14f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
                val n = if (item == Upgrade.BOX_10) "10" else "7"
                drawContext.canvas.nativeCanvas.drawText(n, w * 0.5f, h * 0.7f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; alpha = (255 * a).toInt(); textSize = h * 0.32f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
            }
        }
    }
}
