package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.marcomultiservices.kimba.core.CampItem
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.campCount
import lt.marcomultiservices.kimba.core.ownsCamp
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.amberButtonColors
import lt.marcomultiservices.kimba.ui.scene.ArtPack
import lt.marcomultiservices.kimba.ui.scene.SceneColors
import lt.marcomultiservices.kimba.ui.scene.drawArt
import lt.marcomultiservices.kimba.ui.t
import kotlin.math.sin

/**
 * The angler's camp: a little scene by the water that fills up with each piece bought, and the
 * list of pieces. Pictures: `camp_bg` (the empty clearing) and `camp_<piece>` for each piece
 * (see docs/GRAFIKA.md); without them the pieces are drawn in code.
 */
@Composable
fun CampScreen(vm: GameViewModel, onBack: () -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val conditions by vm.conditions.collectAsStateWithLifecycle()
    // Visiting a friend: their camp pieces and their best fish, from the card they shared.
    val visit by vm.visiting.collectAsStateWithLifecycle()
    val friend = visit
    val p = if (friend != null) Progress(owned = friend.camp.map { "camp:$it" }.toSet()) else saved?.progress ?: Progress()
    val visitCatches = friend?.best?.mapNotNull { m -> lt.marcomultiservices.kimba.core.Fish.byId(m.species)?.let { sp -> lt.marcomultiservices.kimba.core.Catch(m.species, m.golden, m.cm, sp.grams(m.cm), 0L, lt.marcomultiservices.kimba.core.Sky.CLEAR, 0.0, "") } }
    val time = lt.marcomultiservices.kimba.ui.scene.rememberSceneTime()
    ScreenFrame(if (friend != null) t(S.friendCamp, friend.code) else t(S.campTitle), { onBack(); vm.visiting.value = null }) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp)) {
            item {
                val colors = conditions?.let { SceneColors(it) }
                Canvas(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(20.dp))) {
                    drawCamp(p, colors, time.floatValue)
                }
                // The keep pond: the biggest fish of each kind caught, swimming.
                val best = (visitCatches ?: saved?.catches ?: emptyList()).groupBy { it.golden ?: it.species }.values.map { l -> l.maxBy { it.grams } }.sortedByDescending { it.grams }.take(10)
                Spacer(Modifier.height(14.dp))
                lt.marcomultiservices.kimba.ui.SectionTitle(t(S.pondTitle))
                Spacer(Modifier.height(4.dp))
                Text(t(if (best.isEmpty()) S.pondEmpty else S.pondNote), color = K.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(8.dp))
                Canvas(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp))) { drawKeepPond(best, time.floatValue) }
                Spacer(Modifier.height(10.dp))
                if (friend == null) Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinsLabel(p.coins, 20)
                    Spacer(Modifier.weight(1f))
                    Text(t(S.campIncome, CampItem.DAILY_PER_ITEM * p.campCount()), color = K.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(if (friend != null) t(S.friendCampNote, friend.level) else t(S.campNote), color = K.Muted, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(12.dp))
            }
            if (friend == null) items(CampItem.entries.toList(), key = { it.name }) { item ->
                CampRow(item, p) { vm.buyCamp(item) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CampRow(item: CampItem, p: Progress, onBuy: () -> Unit) {
    val owned = p.ownsCamp(item)
    val open = p.level >= item.level
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (owned) Brush.horizontalGradient(listOf(Color(0x408DC26F), Color(0x148DC26F))) else Brush.horizontalGradient(listOf(Color(0x26FFFFFF), Color(0x0FFFFFFF))))
            .border(1.dp, if (owned) Color(0x888DC26F) else K.Line, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(52.dp)) { drawPiece(item, Offset(size.width / 2, size.height * 0.92f), size.height * 0.85f, null, 0f) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t(item.label), color = K.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(t(item.note), color = K.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        when {
            owned -> Icon(Icons.Rounded.Check, null, tint = K.Reed)
            !open -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = K.Muted, modifier = Modifier.size(16.dp))
                Text(" " + t(S.needLevel, item.level), color = K.Muted, fontSize = 12.sp)
            }
            else -> Button(onBuy, enabled = p.coins >= item.price, colors = amberButtonColors(), contentPadding = PaddingValues(horizontal = 12.dp)) {
                CoinIcon(Modifier.size(16.dp))
                Text(" ${item.price}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Where each piece stands in the clearing (x, ground y as fractions) and how tall it is (fraction of the height). */
private val layout = mapOf(
    CampItem.CABIN to Triple(0.8f, 0.6f, 0.42f),
    CampItem.FLAG to Triple(0.93f, 0.66f, 0.5f),
    CampItem.TENT to Triple(0.2f, 0.74f, 0.36f),
    CampItem.LANTERN to Triple(0.36f, 0.76f, 0.2f),
    CampItem.SMOKER to Triple(0.66f, 0.8f, 0.26f),
    CampItem.CHAIR to Triple(0.38f, 0.92f, 0.2f),
    CampItem.FIRE to Triple(0.53f, 0.94f, 0.16f),
    CampItem.DOG to Triple(0.72f, 0.97f, 0.15f),
)

private fun DrawScope.drawCamp(p: Progress, colors: SceneColors?, t: Float) {
    val w = size.width
    val h = size.height
    val bg = ArtPack["camp_bg"]
    if (bg != null) {
        drawArt(bg, Rect(0f, 0f, w, h), colors?.lit(Color.White) ?: Color.White)
    } else {
        // Sky, the lake behind, the far forest and the grassy clearing.
        drawRect(Brush.verticalGradient(listOf(colors?.skyTop ?: Color(0xFF3D7DCC), colors?.skyHorizon ?: Color(0xFFBCDBF0)), 0f, h * 0.45f), size = Size(w, h * 0.45f))
        drawRect(Brush.verticalGradient(listOf(Color(0xFF5E8FB0), Color(0xFF2C5670)), h * 0.44f, h * 0.56f), Offset(0f, h * 0.44f), Size(w, h * 0.12f))
        val forest = Path().apply {
            moveTo(0f, h * 0.45f)
            var x = 0f
            var k = 0
            while (x <= w) {
                val top = h * (0.3f + 0.08f * sin(k * 1.7f) + 0.04f * sin(k * 3.1f))
                lineTo(x + w * 0.02f, top); lineTo(x + w * 0.04f, h * 0.45f)
                x += w * 0.04f; k++
            }
            lineTo(w, h * 0.45f); close()
        }
        drawPath(forest, colors?.lit(Color(0xFF2C4A2A)) ?: Color(0xFF2C4A2A))
        drawRect(Brush.verticalGradient(listOf(colors?.lit(Color(0xFF6F8F3A)) ?: Color(0xFF6F8F3A), colors?.lit(Color(0xFF3F5A22)) ?: Color(0xFF3F5A22)), h * 0.55f, h), Offset(0f, h * 0.55f), Size(w, h * 0.45f))
    }
    // Back to front, so nearer pieces stand in front.
    for ((item, pos) in layout.entries.sortedBy { it.value.second }) {
        if (!p.ownsCamp(item)) continue
        drawPiece(item, Offset(w * pos.first, h * pos.second), h * pos.third, colors, t)
    }
}

/** One camp piece standing on the ground at [base], [height] tall: its picture, or a drawing. */
private fun DrawScope.drawPiece(item: CampItem, base: Offset, height: Float, colors: SceneColors?, t: Float) {
    val art = ArtPack["camp_" + item.name.lowercase()]
    if (art != null) {
        val wd = height * art.width / art.height
        drawArt(art, Rect(base.x - wd / 2, base.y - height, base.x + wd / 2, base.y), colors?.lit(Color.White) ?: Color.White)
        // The picture is still; fire, smoke and light are alive in code on top of it.
        when (item) {
            CampItem.FIRE -> drawFire(Offset(base.x, base.y - height * 0.3f), height * 0.9f, t)
            CampItem.SMOKER -> drawSmoke(Offset(base.x + wd * 0.2f, base.y - height), height, t)
            CampItem.LANTERN -> drawGlow(Offset(base.x, base.y - height * 0.75f), height, colors)
            else -> Unit
        }
        return
    }
    fun c(col: Color) = colors?.lit(col) ?: col
    val u = height
    when (item) {
        CampItem.TENT -> {
            val tent = Path().apply { moveTo(base.x - u * 0.8f, base.y); lineTo(base.x, base.y - u); lineTo(base.x + u * 0.8f, base.y); close() }
            drawPath(tent, c(Color(0xFFD9822B)))
            val side = Path().apply { moveTo(base.x, base.y - u); lineTo(base.x + u * 0.8f, base.y); lineTo(base.x + u * 0.3f, base.y); close() }
            drawPath(side, c(Color(0xFFA85F1C)))
            val door = Path().apply { moveTo(base.x - u * 0.2f, base.y); lineTo(base.x, base.y - u * 0.55f); lineTo(base.x + u * 0.2f, base.y); close() }
            drawPath(door, c(Color(0xFF3A2410)))
        }
        CampItem.FIRE -> {
            drawLine(c(Color(0xFF5A3A1E)), Offset(base.x - u * 0.6f, base.y), Offset(base.x + u * 0.5f, base.y - u * 0.25f), strokeWidth = u * 0.14f, cap = StrokeCap.Round)
            drawLine(c(Color(0xFF6B4524)), Offset(base.x + u * 0.6f, base.y), Offset(base.x - u * 0.5f, base.y - u * 0.25f), strokeWidth = u * 0.14f, cap = StrokeCap.Round)
            drawFire(Offset(base.x, base.y - u * 0.12f), u, t)
        }
        CampItem.CHAIR -> {
            val col = c(Color(0xFF2F6B4F))
            drawRect(col, Offset(base.x - u * 0.35f, base.y - u * 0.55f), Size(u * 0.7f, u * 0.12f))
            drawRect(col, Offset(base.x - u * 0.35f, base.y - u * 1.05f), Size(u * 0.12f, u * 0.55f))
            drawRect(c(Color(0xFF3F8A66)), Offset(base.x - u * 0.33f, base.y - u * 1.0f), Size(u * 0.5f, u * 0.45f))
            drawLine(c(Color(0xFF333333)), Offset(base.x - u * 0.3f, base.y - u * 0.45f), Offset(base.x + u * 0.3f, base.y), strokeWidth = u * 0.05f)
            drawLine(c(Color(0xFF333333)), Offset(base.x + u * 0.3f, base.y - u * 0.45f), Offset(base.x - u * 0.3f, base.y), strokeWidth = u * 0.05f)
        }
        CampItem.LANTERN -> {
            drawLine(c(Color(0xFF4A3420)), base, Offset(base.x, base.y - u), strokeWidth = u * 0.06f)
            drawLine(c(Color(0xFF4A3420)), Offset(base.x, base.y - u), Offset(base.x + u * 0.3f, base.y - u), strokeWidth = u * 0.05f)
            val night = 1f - (colors?.daylight ?: 1f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD27A).copy(alpha = 0.25f + 0.5f * night), Color.Transparent), Offset(base.x + u * 0.3f, base.y - u * 0.8f), u * 0.9f), u * 0.9f, Offset(base.x + u * 0.3f, base.y - u * 0.8f))
            drawRect(Color(0xFFFFD27A), Offset(base.x + u * 0.22f, base.y - u * 0.92f), Size(u * 0.16f, u * 0.22f))
        }
        CampItem.DOG -> {
            val col = c(Color(0xFF8A5A2E))
            drawOval(col, Offset(base.x - u * 0.6f, base.y - u * 0.55f), Size(u * 1.0f, u * 0.4f))
            drawCircle(col, u * 0.22f, Offset(base.x + u * 0.45f, base.y - u * 0.62f))
            drawOval(c(Color(0xFF5A3A1E)), Offset(base.x + u * 0.35f, base.y - u * 0.82f), Size(u * 0.14f, u * 0.25f))
            for (dx in listOf(-0.45f, -0.25f, 0.1f, 0.3f)) drawRect(col, Offset(base.x + u * dx, base.y - u * 0.25f), Size(u * 0.08f, u * 0.25f))
            val wag = sin(t * 10f) * u * 0.1f
            drawLine(col, Offset(base.x - u * 0.58f, base.y - u * 0.45f), Offset(base.x - u * 0.85f, base.y - u * 0.7f + wag), strokeWidth = u * 0.07f, cap = StrokeCap.Round)
        }
        CampItem.SMOKER -> {
            drawRect(c(Color(0xFF5A5F66)), Offset(base.x - u * 0.25f, base.y - u * 0.75f), Size(u * 0.5f, u * 0.75f))
            drawRect(c(Color(0xFF3A3E44)), Offset(base.x - u * 0.06f, base.y - u * 1.0f), Size(u * 0.12f, u * 0.25f))
            drawSmoke(Offset(base.x, base.y - u), u, t)
        }
        CampItem.FLAG -> {
            drawLine(c(Color(0xFFB0B0B0)), base, Offset(base.x, base.y - u), strokeWidth = u * 0.03f)
            val wave = Path().apply {
                moveTo(base.x, base.y - u)
                for (i in 1..8) { val f = i / 8f; lineTo(base.x - u * 0.45f * f, base.y - u + u * 0.03f * sin(t * 5f + f * 6f)) }
                for (i in 8 downTo 0) { val f = i / 8f; lineTo(base.x - u * 0.45f * f, base.y - u * 0.75f + u * 0.03f * sin(t * 5f + f * 6f)) }
                close()
            }
            drawPath(wave, c(Color(0xFFE0A030)))
        }
        CampItem.CABIN -> {
            drawRect(c(Color(0xFF7A4A28)), Offset(base.x - u * 0.6f, base.y - u * 0.6f), Size(u * 1.2f, u * 0.6f))
            for (k in 1..4) drawLine(c(Color(0xFF5A3418)), Offset(base.x - u * 0.6f, base.y - u * 0.12f * k), Offset(base.x + u * 0.6f, base.y - u * 0.12f * k), strokeWidth = 1.5f)
            val roof = Path().apply { moveTo(base.x - u * 0.72f, base.y - u * 0.58f); lineTo(base.x, base.y - u); lineTo(base.x + u * 0.72f, base.y - u * 0.58f); close() }
            drawPath(roof, c(Color(0xFF3E2A1C)))
            drawRect(c(Color(0xFF3A2410)), Offset(base.x - u * 0.12f, base.y - u * 0.38f), Size(u * 0.24f, u * 0.38f))
            drawRect(Color(0xFFFFD27A).copy(alpha = 0.4f + 0.5f * (1f - (colors?.daylight ?: 1f))), Offset(base.x + u * 0.22f, base.y - u * 0.42f), Size(u * 0.22f, u * 0.16f))
        }
    }
    drawOval(Color.Black.copy(alpha = 0.18f), Offset(base.x - u * 0.6f, base.y - u * 0.04f), Size(u * 1.2f, u * 0.08f))
}

/** Clear green water seen from above the bank, with the player's best fish swimming to and fro. */
private fun DrawScope.drawKeepPond(fish: List<lt.marcomultiservices.kimba.core.Catch>, t: Float) {
    val w = size.width
    val h = size.height
    val bg = ArtPack["pond_bg"]
    if (bg != null) drawArt(bg, Rect(0f, 0f, w, h))
    else {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2E6B6A), Color(0xFF173B44), Color(0xFF0E2A33)), 0f, h))
        // Light rays and pebbles on the bottom.
        for (k in 0 until 5) {
            val x = w * (0.1f + 0.2f * k) + sin(t * 0.3f + k) * w * 0.02f
            drawLine(Color.White.copy(alpha = 0.05f), Offset(x, 0f), Offset(x - w * 0.12f, h), strokeWidth = w * 0.05f)
        }
        for (k in 0 until 30) {
            val x = w * ((k * 0.137f) % 1f)
            val y = h * (0.82f + 0.15f * ((k * 0.311f) % 1f))
            drawOval(Color(0xFF6E6A58).copy(alpha = 0.6f), Offset(x, y), Size(w * 0.03f, h * 0.025f))
        }
    }
    val biggest = fish.maxOfOrNull { it.grams }?.toFloat() ?: 1f
    for ((i, c) in fish.withIndex()) {
        val img = lt.marcomultiservices.kimba.ui.scene.ArtPack.fish(c.species) ?: continue
        // Each fish swims a slow lap at its own depth and speed, turning at the ends.
        val speed = 0.03f + 0.02f * ((i * 0.37f) % 1f)
        val ph = (t * speed + i * 0.23f) % 2f
        val right = ph < 1f
        val f = if (right) ph else 2f - ph
        val ease = f * f * (3 - 2 * f)
        val len = w * (0.16f + 0.18f * kotlin.math.sqrt(c.grams / biggest))
        val fh = len * img.height / img.width
        val x = -len * 0.2f + (w - len * 0.6f) * ease
        val y = h * (0.12f + 0.62f * ((i * 0.618f) % 1f)) + sin(t * 1.3f + i) * h * 0.02f
        val tint = if (lt.marcomultiservices.kimba.core.Fish.gold(c.golden)) androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(0.55f, 0.45f, 0.15f, 0f, 30f, 0.40f, 0.42f, 0.10f, 0f, 18f, 0.10f, 0.15f, 0.10f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))) else null
        // Seen through the water: a bit faded and blue, with a shadow on the bottom.
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(x + len * 0.1f, y + fh * 1.4f), Size(len * 0.8f, fh * 0.3f))
        withTransform({ scale(if (right) -1f else 1f, 1f, Offset(x + len / 2, y + fh / 2)) }) {
            drawImage(
                img, dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()), dstSize = androidx.compose.ui.unit.IntSize(len.toInt().coerceAtLeast(1), fh.toInt().coerceAtLeast(1)),
                alpha = 0.92f, colorFilter = tint,
            )
        }
    }
    drawRect(Color(0xFF2E6B6A).copy(alpha = 0.18f))
}

/**
 * A living campfire at [base] (where the flames start), [u] tall: a warm glow on the ground,
 * three flame tongues licking at their own pace, sparks rising and a thin smoke above.
 */
private fun DrawScope.drawFire(base: Offset, u: Float, t: Float) {
    val flicker = 0.85f + 0.1f * sin(t * 11f) + 0.05f * sin(t * 23f + 1f)
    drawCircle(Brush.radialGradient(listOf(Color(0x77FFB040).copy(alpha = 0.45f * flicker), Color.Transparent), base, u * 1.8f), u * 1.8f, base)
    for (i in 0 until 3) {
        val x = base.x + (i - 1) * u * 0.18f
        val hgt = u * (0.75f + 0.25f * sin(t * (7f + i * 2.3f) + i * 1.7f)) * (if (i == 1) 1.25f else 0.9f)
        val lean = sin(t * (3.1f + i) + i) * u * 0.1f
        val w = u * (if (i == 1) 0.34f else 0.24f)
        val tongue = Path().apply {
            moveTo(x - w, base.y)
            quadraticTo(x - w * 0.9f, base.y - hgt * 0.6f, x + lean, base.y - hgt)
            quadraticTo(x + w * 0.9f, base.y - hgt * 0.55f, x + w, base.y)
            close()
        }
        drawPath(tongue, Brush.verticalGradient(listOf(Color(0xFFFF6A1A).copy(alpha = 0.85f), Color(0xFFFFD27A)), base.y - hgt, base.y))
        val core = Path().apply {
            moveTo(x - w * 0.45f, base.y)
            quadraticTo(x - w * 0.4f, base.y - hgt * 0.35f, x + lean * 0.5f, base.y - hgt * 0.55f)
            quadraticTo(x + w * 0.4f, base.y - hgt * 0.3f, x + w * 0.45f, base.y)
            close()
        }
        drawPath(core, Color(0xFFFFF2C4).copy(alpha = 0.9f))
    }
    // Sparks.
    for (k in 0 until 7) {
        val ph = (t * (0.5f + 0.13f * k) + k * 0.37f) % 1f
        val sx = base.x + sin(k * 2.7f + t * 1.5f) * u * 0.3f * ph
        drawCircle(Color(0xFFFFC46B).copy(alpha = 1f - ph), u * 0.025f, Offset(sx, base.y - u * (0.5f + 1.6f * ph)))
    }
    drawSmoke(Offset(base.x, base.y - u * 1.2f), u * 0.8f, t)
}

/** Grey smoke curling up from [top], drifting a little with the wind. */
private fun DrawScope.drawSmoke(top: Offset, u: Float, t: Float) {
    for (k in 0 until 5) {
        val ph = (t * 0.35f + k * 0.2f) % 1f
        drawCircle(Color(0xFFD8D8D8).copy(alpha = 0.3f * (1f - ph)), u * (0.08f + 0.2f * ph), Offset(top.x + u * 0.3f * ph + u * 0.06f * sin(t + k), top.y - u * 1.0f * ph))
    }
}

/** The lantern's warm light, stronger as it gets dark. */
private fun DrawScope.drawGlow(at: Offset, u: Float, colors: SceneColors?) {
    val night = 1f - (colors?.daylight ?: 1f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD27A).copy(alpha = 0.2f + 0.5f * night), Color.Transparent), at, u * 0.9f), u * 0.9f, at)
}

