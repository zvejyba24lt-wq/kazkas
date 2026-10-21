package lt.marcomultiservices.kimba.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lt.marcomultiservices.kimba.core.Destination
import lt.marcomultiservices.kimba.core.Destinations
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.amberButtonColors
import lt.marcomultiservices.kimba.ui.t
import kotlin.math.PI
import kotlin.math.cos


/** Natural Earth 1:50m land, lakes and rivers (public domain), clipped to Central Europe. */
@Serializable
private class MapData(val bbox: List<Double>, val land: List<List<Double>>, val lakes: List<List<Double>>, val rivers: List<List<Double>>)

private object MapAsset {
    private var data: MapData? = null
    fun get(context: Context): MapData? = data ?: runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString(MapData.serializer(), context.assets.open("europe.json").bufferedReader().use { it.readText() })
    }.getOrNull()?.also { data = it }
}

/** Equirectangular projection, squeezed by cos(latitude) so the map keeps its shape. */
private class Projection(bbox: List<Double>, val w: Float, val h: Float) {
    private val west = bbox[0]
    private val south = bbox[1]
    private val east = bbox[2]
    private val north = bbox[3]
    val aspect: Float = (((east - west) * cos((south + north) / 2 * PI / 180)) / (north - south)).toFloat()
    fun x(lon: Double) = ((lon - west) / (east - west)).toFloat() * w
    fun y(lat: Double) = ((north - lat) / (north - south)).toFloat() * h
    fun at(lat: Double, lon: Double) = Offset(x(lon), y(lat))
}

@Composable
fun MapScreen(vm: GameViewModel, onBack: () -> Unit, onFish: () -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val p = saved?.progress ?: Progress()
    val home = saved?.place
    val context = LocalContext.current
    val map = remember { MapAsset.get(context) }
    var selected by rememberSaveable { mutableStateOf(p.trip) }
    val dest = Destinations.byId(selected)

    ScreenFrame(t(S.map), onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp, 0.dp, 16.dp, 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinsLabel(p.coins, 20)
                Spacer(Modifier.weight(1f))
                LevelBar(p, 110)
            }
            Spacer(Modifier.height(10.dp))
            if (map != null) {
                val proj0 = remember(map) { Projection(map.bbox, 1f, 1f) }
                BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(proj0.aspect).clip(RoundedCornerShape(20.dp)).border(1.dp, K.Line, RoundedCornerShape(20.dp))) {
                    val time = lt.marcomultiservices.kimba.ui.scene.rememberSceneTime()
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(map) {
                            detectTapGestures { tap ->
                                val proj = Projection(map.bbox, size.width.toFloat(), size.height.toFloat())
                                val hit = Destinations.all.minByOrNull { d -> (proj.at(d.lat, d.lon) - tap).getDistance() }
                                if (hit != null && (proj.at(hit.lat, hit.lon) - tap).getDistance() < 34.dp.toPx()) selected = hit.id
                                else if (home != null && (proj.at(home.lat, home.lon) - tap).getDistance() < 34.dp.toPx()) selected = null
                            }
                        },
                    ) {
                        val proj = Projection(map.bbox, size.width, size.height)
                        drawMap(map, proj)
                        if (home != null && home.lon in map.bbox[0]..map.bbox[2] && home.lat in map.bbox[1]..map.bbox[3]) {
                            val c = proj.at(home.lat, home.lon)
                            drawCircle(Color(0x558FD3FF), 13.dp.toPx(), c)
                            drawCircle(Color(0xFF8FD3FF), 6.dp.toPx(), c)
                            drawCircle(Color.White, 6.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                        }
                        for (d in Destinations.all) {
                            val c = proj.at(d.lat, d.lon)
                            val open = d.id in p.trips
                            val reachable = p.level >= d.level
                            val here = d.id == p.trip
                            val sel = d.id == selected
                            if (here) {
                                val pulse = (time.floatValue * 0.8f) % 1f
                                drawCircle(K.Amber.copy(alpha = 0.6f * (1 - pulse)), (8 + 16 * pulse).dp.toPx(), c, style = Stroke(2.dp.toPx()))
                            }
                            drawPin(c, when {
                                open -> K.Amber
                                reachable -> Color(0xFFF3EFE6)
                                else -> Color(0xFF6A7380)
                            }, sel, d.water)
                        }
                    }
                }
                Text("© Natural Earth", color = Color(0x66B4BCC6), fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.height(12.dp))
            if (dest != null) DestinationCard(dest, p, onGo = { vm.travel(dest); onFish() }) else HomeCard(p, home?.name ?: "", onGo = { w -> vm.goHome(w); onFish() })
            Spacer(Modifier.height(18.dp))
            SectionTitle(t(S.trips))
            Spacer(Modifier.height(4.dp))
            Text(t(S.tripsNote), color = K.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected == null) Color(0x33F2B25C) else Color(0x1AFFFFFF)).clickable { selected = null }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Home, null, tint = Color(0xFF8FD3FF), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("${t(S.home)} · ${home?.name ?: ""}", color = K.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (p.trip == null) Text(t(S.here), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            for (d in Destinations.all) {
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected == d.id) Color(0x33F2B25C) else Color(0x1AFFFFFF)).clickable { selected = d.id }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(flag(d.country), fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t(d.name), color = K.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(t(d.water.label), color = K.Muted, fontSize = 12.sp)
                    }
                    when {
                        d.id == p.trip -> Text(t(S.here), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        d.id in p.trips -> Text("✓", color = K.Reed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        p.level < d.level -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null, tint = K.Muted, modifier = Modifier.size(14.dp))
                            Text(" ${d.level}", color = K.Muted, fontSize = 12.sp)
                        }
                        else -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinIcon(Modifier.size(14.dp)); Text(" ${d.price}", color = K.Gold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun flag(country: String) = when (country) { "LT" -> "🇱🇹"; "DE" -> "🇩🇪"; "AT" -> "🇦🇹"; else -> "🏳" }

@Composable
private fun DestinationCard(d: Destination, p: Progress, onGo: () -> Unit) {
    val specials = d.specials
    val count = Fish.all.count { it.livesAt(d.spot) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x14FFFFFF)))).border(1.dp, K.Line, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(flag(d.country), fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(t(d.name), color = K.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(t(d.story), color = K.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text("${t(d.water.label)} · ${t(S.fishHere, count)}", color = K.Text, fontSize = 13.sp)
        if (specials.isNotEmpty()) Text(t(S.onlyHere, specials.joinToString { t(it.name) }), color = K.Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        when {
            d.id == p.trip -> Text(t(S.here), color = K.Amber, fontWeight = FontWeight.Bold)
            p.level < d.level -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = K.Muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(t(S.needLevel, d.level), color = K.Muted)
            }
            else -> Button(onGo, enabled = p.canTravel(d), colors = amberButtonColors(), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(t(S.go))
                if (d.id !in p.trips) {
                    Spacer(Modifier.width(10.dp))
                    CoinIcon(Modifier.size(16.dp))
                    Text(" ${d.price}")
                }
            }
        }
    }
}

@Composable
private fun HomeCard(p: Progress, homeName: String, onGo: (Water) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x14FFFFFF)))).border(1.dp, K.Line, RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Home, null, tint = Color(0xFF8FD3FF))
            Spacer(Modifier.width(8.dp))
            Text("${t(S.home)} · $homeName", color = K.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(t(S.homeNote), color = K.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (w in Water.entries) {
                val open = p.level >= w.homeLevel
                val here = p.trip == null && p.water == w
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (here) K.Amber else Color(0x1FFFFFFF))
                        .border(1.dp, if (here) K.Amber else K.Line, RoundedCornerShape(14.dp))
                        .clickable(enabled = open) { onGo(w) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.size(34.dp, 22.dp)) { drawWaterIcon(w, if (open) 1f else 0.35f) }
                    Spacer(Modifier.height(4.dp))
                    Text(t(w.label), color = if (here) Color(0xFF2A1A06) else if (open) K.Text else K.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (!open) Text(t(S.needLevel, w.homeLevel), color = K.Muted, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

private fun DrawScope.drawWaterIcon(w: Water, a: Float) {
    val water = Color(0xFF5B9BD0).copy(alpha = a)
    val green = Color(0xFF4E8A3A).copy(alpha = a)
    when (w) {
        Water.LAKE -> {
            drawOval(water, Offset(size.width * 0.05f, size.height * 0.35f), androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 0.6f))
            drawPath(Path().apply { moveTo(size.width * 0.1f, size.height * 0.4f); lineTo(size.width * 0.25f, 0f); lineTo(size.width * 0.4f, size.height * 0.4f); close() }, green)
        }
        Water.POND -> {
            drawOval(water, Offset(size.width * 0.15f, size.height * 0.3f), androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.65f))
            drawCircle(green, size.height * 0.15f, Offset(size.width * 0.4f, size.height * 0.6f))
            drawCircle(green, size.height * 0.1f, Offset(size.width * 0.62f, size.height * 0.72f))
        }
        Water.RIVER -> {
            val p = Path().apply { moveTo(0f, size.height * 0.3f); cubicTo(size.width * 0.4f, size.height * 0.1f, size.width * 0.5f, size.height * 0.9f, size.width, size.height * 0.7f) }
            drawPath(p, water, style = Stroke(size.height * 0.32f, cap = StrokeCap.Round))
        }
        Water.SEA -> {
            drawRect(water, Offset(0f, size.height * 0.4f), androidx.compose.ui.geometry.Size(size.width, size.height * 0.6f))
            for (i in 0..2) {
                val y = size.height * (0.55f + i * 0.15f)
                drawPath(Path().apply { moveTo(size.width * 0.1f, y); quadraticTo(size.width * 0.3f, y - 4f, size.width * 0.5f, y); quadraticTo(size.width * 0.7f, y + 4f, size.width * 0.9f, y) }, Color.White.copy(alpha = 0.6f * a), style = Stroke(1.5f))
            }
        }
    }
}

private fun polygon(points: List<Double>, proj: Projection, close: Boolean): Path = Path().apply {
    var i = 0
    while (i + 1 < points.size) {
        val x = proj.x(points[i])
        val y = proj.y(points[i + 1])
        if (i == 0) moveTo(x, y) else lineTo(x, y)
        i += 2
    }
    if (close) close()
}

/** A hand-tinted map: deep sea, warm land with a soft shore glow, lakes and rivers. */
private fun DrawScope.drawMap(map: MapData, proj: Projection) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF16324A), Color(0xFF0E2236))))
    // Faint graticule.
    for (lon in 4..28 step 4) drawLine(Color(0x14FFFFFF), Offset(proj.x(lon.toDouble()), 0f), Offset(proj.x(lon.toDouble()), size.height), strokeWidth = 1f)
    for (lat in 46..58 step 2) drawLine(Color(0x14FFFFFF), Offset(0f, proj.y(lat.toDouble())), Offset(size.width, proj.y(lat.toDouble())), strokeWidth = 1f)
    val land = map.land.map { polygon(it, proj, true) }
    for (path in land) drawPath(path, Color(0x335B9BD0), style = Stroke(8.dp.toPx(), join = StrokeJoin.Round))
    for (path in land) {
        drawPath(path, Brush.verticalGradient(listOf(Color(0xFF3E5A3A), Color(0xFF5C6A3E), Color(0xFF6E6A44))))
        drawPath(path, Color(0xFFCFC39A).copy(alpha = 0.55f), style = Stroke(1.2f, join = StrokeJoin.Round))
    }
    val water = Color(0xFF3F7FB0)
    for (r in map.rivers) drawPath(polygon(r, proj, false), water.copy(alpha = 0.85f), style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    for (l in map.lakes) drawPath(polygon(l, proj, true), water)
}

/** A map pin with a small water symbol. */
private fun DrawScope.drawPin(c: Offset, color: Color, selected: Boolean, water: Water) {
    val r = (if (selected) 11 else 8).dp.toPx()
    val tip = Offset(c.x, c.y)
    val head = Offset(c.x, c.y - r * 1.7f)
    val pin = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(tip.x - r * 0.4f, tip.y - r * 0.8f, head.x - r, head.y + r * 0.4f, head.x - r, head.y)
        arcTo(androidx.compose.ui.geometry.Rect(head.x - r, head.y - r, head.x + r, head.y + r), 180f, 180f, false)
        cubicTo(head.x + r, head.y + r * 0.4f, tip.x + r * 0.4f, tip.y - r * 0.8f, tip.x, tip.y)
        close()
    }
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(tip.x - r * 0.5f, tip.y - r * 0.15f), androidx.compose.ui.geometry.Size(r, r * 0.3f))
    drawPath(pin, color)
    drawPath(pin, Color.Black.copy(alpha = 0.35f), style = Stroke(1.5f))
    drawCircle(when (water) { Water.SEA -> Color(0xFF2E6A9A); Water.RIVER -> Color(0xFF3F8FC0); else -> Color(0xFF4A9AD0) }, r * 0.45f, head)
    if (selected) drawCircle(Color.White, r * 1.25f, head, style = Stroke(2.dp.toPx()))
}

