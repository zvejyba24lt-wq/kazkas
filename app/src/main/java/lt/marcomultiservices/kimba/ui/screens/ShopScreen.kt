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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.rotate
import lt.marcomultiservices.kimba.ui.scene.layers.drawTreble
import kotlin.math.cos
import kotlin.math.sin
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
import lt.marcomultiservices.kimba.core.Tackle
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.amberButtonColors
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.drawArt
import lt.marcomultiservices.kimba.ui.scene.layers.drawFloat
import lt.marcomultiservices.kimba.ui.t
import kotlin.math.roundToInt

@Composable
fun ShopScreen(vm: GameViewModel, onBack: () -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val p = saved?.progress ?: Progress()
    val sections: List<Pair<L, List<Item>>> = listOf(
        S.rodKinds to Tackle.entries, S.lures to Lure.entries, S.rods to Rod.entries, S.reels to Reel.entries,
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
    is Lure -> p.gear.lure == item
    is Tackle -> p.tackle == item
    is Rod -> p.rod == item
    is Reel -> p.reel == item
    is Line -> p.line == item
    is FloatStyle -> p.float == item
    else -> false
}

@Composable
private fun note(item: Item): String = when (item) {
    is Lure -> t(item.note) + " · " + t(when (item.tackle) { setOf(Tackle.SPIN) -> S.onlySpin; setOf(Tackle.FEEDER) -> S.onlyFeeder; else -> S.floatOrFeeder })
    is Tackle -> t(item.note)
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
internal fun DrawScope.drawItemIcon(item: Item, dim: Boolean) {
    val a = if (dim) 0.35f else 1f
    val w = size.width
    val h = size.height
    // A picture in assets/art/item_<kind>_<name>.webp (e.g. item_lure_worm) replaces the drawing.
    ArtPack["item_" + item.key.replace(':', '_').lowercase()]?.let { img ->
        drawArt(img, androidx.compose.ui.geometry.Rect(0f, 0f, w, h), alpha = a)
        return
    }
    when (item) {
        is Lure -> when (item) {
            Lure.WORM -> {
                // A segmented earthworm with its saddle.
                val path = Path().apply { moveTo(w * 0.12f, h * 0.62f); cubicTo(w * 0.3f, h * 0.18f, w * 0.55f, h * 0.95f, w * 0.88f, h * 0.38f) }
                drawPath(path, Color(0xFF7A3A36).copy(alpha = a), style = Stroke(w * 0.15f, cap = StrokeCap.Round))
                drawPath(path, Color(0xFFC8746A).copy(alpha = a), style = Stroke(w * 0.11f, cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = 0.25f * a), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
                val m = android.graphics.PathMeasure(path.asAndroidPath(), false)
                val pos = FloatArray(2)
                for (k in 1..9) {
                    m.getPosTan(m.length * k / 10f, pos, null)
                    drawCircle(Color.Black.copy(alpha = 0.18f * a), w * 0.018f, Offset(pos[0], pos[1]))
                }
                m.getPosTan(m.length * 0.3f, pos, null)
                drawCircle(Color(0xFFE0907E).copy(alpha = a), w * 0.07f, Offset(pos[0], pos[1]))
            }
            Lure.MAGGOT -> {
                for ((x, y, rot) in listOf(Triple(0.32f, 0.38f, -30f), Triple(0.62f, 0.42f, 25f), Triple(0.46f, 0.68f, -5f))) {
                    rotate(rot, Offset(w * x, h * y)) {
                        drawOval(Brush.verticalGradient(listOf(Color(0xFFFFF7E0).copy(alpha = a), Color(0xFFD8C89A).copy(alpha = a)), h * (y - 0.08f), h * (y + 0.08f)), Offset(w * (x - 0.17f), h * (y - 0.08f)), Size(w * 0.34f, h * 0.16f))
                        for (k in 1..4) drawLine(Color(0x55A08850), Offset(w * (x - 0.17f + k * 0.068f), h * (y - 0.07f)), Offset(w * (x - 0.17f + k * 0.068f), h * (y + 0.07f)), strokeWidth = w * 0.01f, alpha = a)
                        drawCircle(Color(0xFF3A2A1A).copy(alpha = a), w * 0.018f, Offset(w * (x + 0.13f), h * y))
                    }
                }
            }
            Lure.CORN, Lure.BOILIE -> {
                val corn = item == Lure.CORN
                val col = if (corn) Color(0xFFF5C83A) else Color(0xFFB2603A)
                val spots = if (corn) listOf(0.33f to 0.4f, 0.62f to 0.42f, 0.47f to 0.66f) else listOf(0.36f to 0.42f, 0.62f to 0.5f)
                for ((x, y) in spots) {
                    val c = Offset(w * x, h * y)
                    val r = w * (if (corn) 0.14f else 0.2f)
                    if (corn) {
                        val kernel = Path().apply {
                            moveTo(c.x, c.y - r); cubicTo(c.x + r * 1.2f, c.y - r, c.x + r, c.y + r, c.x, c.y + r * 1.05f)
                            cubicTo(c.x - r, c.y + r, c.x - r * 1.2f, c.y - r, c.x, c.y - r); close()
                        }
                        drawPath(kernel, Brush.radialGradient(listOf(Color(0xFFFFF0A0).copy(alpha = a), col.copy(alpha = a), Color(0xFFD08A10).copy(alpha = a)), Offset(c.x - r * 0.3f, c.y - r * 0.4f), r * 1.4f))
                        drawOval(Color(0xFFFFF6D0).copy(alpha = 0.8f * a), Offset(c.x - r * 0.25f, c.y + r * 0.35f), Size(r * 0.5f, r * 0.45f))
                    } else {
                        drawCircle(Brush.radialGradient(listOf(Color(0xFFE8A070).copy(alpha = a), col.copy(alpha = a), Color(0xFF6A3018).copy(alpha = a)), Offset(c.x - r * 0.35f, c.y - r * 0.35f), r * 1.3f), r, c)
                        for (k in 0 until 6) drawCircle(Color(0xFF4A2010).copy(alpha = 0.5f * a), w * 0.012f, Offset(c.x + r * 0.6f * cos(k * 1.7f), c.y + r * 0.6f * sin(k * 2.3f)))
                    }
                }
                if (!corn) {
                    // Hair rig: the boilies on a fine hair below a hook.
                    drawLine(Color(0xFFDDD6C8).copy(alpha = a), Offset(w * 0.36f, h * 0.42f), Offset(w * 0.62f, h * 0.5f), strokeWidth = w * 0.012f)
                    drawArc(Color(0xFF8A939C).copy(alpha = a), 0f, 200f, false, Offset(w * 0.66f, h * 0.12f), Size(w * 0.2f, h * 0.26f), style = Stroke(w * 0.04f, cap = StrokeCap.Round))
                }
            }
            Lure.SPINNER, Lure.PILK -> {
                val pilk = item == Lure.PILK
                val top = h * 0.12f
                val bh = h * (if (pilk) 0.6f else 0.48f)
                val bw = w * (if (pilk) 0.2f else 0.3f)
                drawCircle(Color(0xFF9AA4AE).copy(alpha = a), w * 0.05f, Offset(w * 0.5f, h * 0.08f), style = Stroke(w * 0.02f))
                val body = Path().apply {
                    moveTo(w * 0.5f, top)
                    cubicTo(w * 0.5f + bw, top + bh * 0.1f, w * 0.5f + bw * 1.1f, top + bh * 0.8f, w * 0.5f, top + bh)
                    cubicTo(w * 0.5f - bw * 1.1f, top + bh * 0.8f, w * 0.5f - bw, top + bh * 0.1f, w * 0.5f, top)
                    close()
                }
                drawPath(body, Brush.linearGradient(listOf(Color(0xFFF6F8FA).copy(alpha = a), Color(0xFF8E98A2).copy(alpha = a), Color(0xFFE2E6EA).copy(alpha = a)), Offset(w * 0.3f, top), Offset(w * 0.7f, top + bh)))
                drawOval(Color.White.copy(alpha = 0.7f * a), Offset(w * 0.5f - bw * 0.55f, top + bh * 0.15f), Size(bw * 0.5f, bh * 0.35f))
                if (pilk) drawRect(Color(0xFFE04030).copy(alpha = a), Offset(w * 0.5f - bw * 0.7f, top + bh * 0.55f), Size(bw * 1.4f, bh * 0.12f))
                else drawOval(Color(0xFFD8402E).copy(alpha = a), Offset(w * 0.5f - bw * 0.25f, top + bh * 0.6f), Size(bw * 0.5f, bh * 0.16f))
                drawTreble(Offset(w * 0.5f, top + bh), w * 0.24f, Color(0xFF8E98A2).copy(alpha = a))
            }
            Lure.WOBBLER -> {
                // A perch-coloured crankbait with a clear diving lip and two trebles.
                val body = Path().apply {
                    moveTo(w * 0.12f, h * 0.44f)
                    cubicTo(w * 0.2f, h * 0.24f, w * 0.7f, h * 0.26f, w * 0.88f, h * 0.42f)
                    cubicTo(w * 0.7f, h * 0.6f, w * 0.25f, h * 0.62f, w * 0.12f, h * 0.44f)
                    close()
                }
                drawPath(body, Brush.verticalGradient(listOf(Color(0xFF2E6A3E).copy(alpha = a), Color(0xFF9CC060).copy(alpha = a), Color(0xFFF0E0A0).copy(alpha = a)), h * 0.27f, h * 0.6f))
                for (k in 0 until 4) drawLine(Color(0xFF1E3A22).copy(alpha = 0.6f * a), Offset(w * (0.32f + k * 0.12f), h * 0.3f), Offset(w * (0.3f + k * 0.12f), h * 0.46f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                drawCircle(Color(0xFFF4E8B0).copy(alpha = a), w * 0.055f, Offset(w * 0.22f, h * 0.4f))
                drawCircle(Color.Black.copy(alpha = a), w * 0.03f, Offset(w * 0.22f, h * 0.4f))
                val lip = Path().apply { moveTo(w * 0.14f, h * 0.48f); lineTo(w * 0.02f, h * 0.62f); lineTo(w * 0.1f, h * 0.66f); lineTo(w * 0.18f, h * 0.52f); close() }
                drawPath(lip, Color(0xCCDDE8F0).copy(alpha = 0.8f * a))
                drawTreble(Offset(w * 0.42f, h * 0.58f), w * 0.2f, Color(0xFF8E98A2).copy(alpha = a))
                drawTreble(Offset(w * 0.8f, h * 0.48f), w * 0.2f, Color(0xFF8E98A2).copy(alpha = a))
            }
            Lure.LIVEBAIT -> lt.marcomultiservices.kimba.ui.fish.FishArt.draw(this, "roach", false, androidx.compose.ui.geometry.Rect(Offset(w * 0.05f, 0f), Size(w * 0.9f, h)), silhouette = dim)
        }
        is Tackle -> {
            // A rod with what makes it that kind: a float, a feeder cage with a quiver tip, a spinning reel.
            drawLine(Color(0xFF2A2E34).copy(alpha = a), Offset(w * 0.12f, h * 0.92f), Offset(w * 0.8f, h * 0.1f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
            drawLine(Color(0xFF8E6A47).copy(alpha = a), Offset(w * 0.12f, h * 0.92f), Offset(w * 0.27f, h * 0.74f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            when (item) {
                Tackle.FLOAT -> {
                    drawLine(Color(0xCCF2F0EA).copy(alpha = a), Offset(w * 0.8f, h * 0.1f), Offset(w * 0.82f, h * 0.5f), strokeWidth = w * 0.015f)
                    drawFloat(Offset(w * 0.82f, h * 0.62f), h * 0.36f, FloatStyle.CLASSIC, alpha = a)
                }
                Tackle.FEEDER -> {
                    drawLine(Color(0xFFFF7A2E).copy(alpha = a), Offset(w * 0.69f, h * 0.24f), Offset(w * 0.8f, h * 0.1f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
                    drawLine(Color(0xCCF2F0EA).copy(alpha = a), Offset(w * 0.8f, h * 0.1f), Offset(w * 0.84f, h * 0.55f), strokeWidth = w * 0.015f)
                    drawOval(Color(0xFF6E7781).copy(alpha = a), Offset(w * 0.76f, h * 0.55f), Size(w * 0.16f, h * 0.24f), style = Stroke(w * 0.03f))
                    drawOval(Color(0xFF3A3F45).copy(alpha = a), Offset(w * 0.77f, h * 0.74f), Size(w * 0.14f, h * 0.07f))
                }
                Tackle.SPIN -> {
                    drawCircle(Color(0xFF9AA5B0).copy(alpha = a), w * 0.1f, Offset(w * 0.36f, h * 0.76f))
                    drawCircle(Color(0xFF2A2F36).copy(alpha = a), w * 0.05f, Offset(w * 0.36f, h * 0.76f))
                    drawLine(Color(0xCCF2F0EA).copy(alpha = a), Offset(w * 0.8f, h * 0.1f), Offset(w * 0.84f, h * 0.48f), strokeWidth = w * 0.015f)
                    drawOval(Brush.horizontalGradient(listOf(Color(0xFFF4F4F4).copy(alpha = a), Color(0xFF9AA4AE).copy(alpha = a))), Offset(w * 0.77f, h * 0.5f), Size(w * 0.14f, h * 0.26f))
                    drawCircle(Color(0xFFD8402E).copy(alpha = a), w * 0.03f, Offset(w * 0.84f, h * 0.65f))
                }
            }
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
            Upgrade.ALARM -> {
                drawRoundRect(Color(0xFF1C1F24).copy(alpha = a), Offset(w * 0.18f, h * 0.38f), Size(w * 0.64f, h * 0.3f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f))
                drawLine(Color(0xFF2A2E34).copy(alpha = a), Offset(w * 0.05f, h * 0.36f), Offset(w * 0.95f, h * 0.36f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
                drawCircle(Brush.radialGradient(listOf(Color(0xAAFF3B2E), Color(0x00FF3B2E)), Offset(w * 0.66f, h * 0.53f), w * 0.2f), w * 0.2f, Offset(w * 0.66f, h * 0.53f), alpha = a)
                drawCircle(Color(0xFFFF3B2E).copy(alpha = a), w * 0.06f, Offset(w * 0.66f, h * 0.53f))
                drawLine(Color(0xFF5C4128).copy(alpha = a), Offset(w * 0.5f, h * 0.68f), Offset(w * 0.5f, h * 0.95f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
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
