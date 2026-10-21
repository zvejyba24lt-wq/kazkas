package lt.marcomultiservices.kimba.ui.screens

import android.Manifest
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Phishing
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.marcomultiservices.kimba.core.Baits
import lt.marcomultiservices.kimba.core.Catch
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.DayPhase
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Destinations
import lt.marcomultiservices.kimba.core.Economy
import lt.marcomultiservices.kimba.core.Levels
import lt.marcomultiservices.kimba.core.Lure
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.Reward
import lt.marcomultiservices.kimba.app
import lt.marcomultiservices.kimba.audio.Sfx
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.foundation.interaction.collectIsPressedAsState
import lt.marcomultiservices.kimba.findActivity
import lt.marcomultiservices.kimba.game.GameEvent
import lt.marcomultiservices.kimba.ui.LocalLang
import lt.marcomultiservices.kimba.ui.fish.FishPicture
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TaskAlt
import lt.marcomultiservices.kimba.core.Sky
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.game.LostReason
import lt.marcomultiservices.kimba.game.Stage
import lt.marcomultiservices.kimba.game.baitsNow
import lt.marcomultiservices.kimba.ui.Glass
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.amberButtonColors
import lt.marcomultiservices.kimba.ui.fish.FishArt
import lt.marcomultiservices.kimba.ui.scene.LakeScene
import lt.marcomultiservices.kimba.ui.t
import kotlin.math.roundToInt

@Composable
fun LakeScreen(
    vm: GameViewModel,
    onBook: () -> Unit,
    onForecast: () -> Unit,
    onSettings: () -> Unit,
    onMap: () -> Unit,
    onShop: () -> Unit,
    onQuests: () -> Unit,
) {
    val conditions by vm.conditions.collectAsStateWithLifecycle()
    val lakeState = vm.lake.collectAsStateWithLifecycle()
    val lake = lakeState.value
    val saved by vm.saved.collectAsStateWithLifecycle()
    val stage = lake.stage
    var noCasts by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val progress = saved?.progress ?: Progress()
    val events by vm.events.collectAsStateWithLifecycle()
    /** Rewarded video, or the reward right away for players who bought "no ads". */
    fun rewarded(then: () -> Unit) {
        if (progress.noAds || activity == null) then() else context.app.ads.showRewarded(activity, then)
    }
    fun next() {
        if (activity != null && vm.interstitialDue()) context.app.ads.showInterstitial(activity) { vm.dismissResult() } else vm.dismissResult()
    }

    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val catches = saved?.catches?.size ?: 0
    LaunchedEffect(catches) {
        if (catches == 1 && Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // The sounds of the place: birds by day, crickets at night, rain, the river or the surf.
    val ambient = conditions?.let { c ->
        when {
            c.weather.sky in setOf(Sky.DRIZZLE, Sky.RAIN, Sky.HEAVY_RAIN, Sky.THUNDER, Sky.SLEET) -> Sfx.Amb.RAIN
            progress.spot.water == lt.marcomultiservices.kimba.core.Water.SEA -> Sfx.Amb.SEA
            progress.spot.water == lt.marcomultiservices.kimba.core.Water.RIVER -> Sfx.Amb.RIVER
            c.phase == DayPhase.NIGHT -> if (c.weather.tempC > 10) Sfx.Amb.NIGHT else Sfx.Amb.CALM
            c.weather.tempC < 3 || c.weather.sky == Sky.SNOW -> Sfx.Amb.CALM
            else -> Sfx.Amb.DAY
        }
    }
    LifecycleResumeEffect(ambient) {
        context.app.sfx.ambient(ambient)
        onPauseOrDispose { context.app.sfx.ambient(null) }
    }

    Box(Modifier.fillMaxSize().background(K.Night)) {
        conditions?.let { LakeScene(it, lakeState, Modifier.fillMaxSize(), progress.spot.water, progress.float) }

        // Taps on the lake strike; holding during the fight reels in.
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    when (vm.lake.value.stage) {
                        Stage.WAITING -> vm.strike()
                        Stage.BITE -> { vm.strike(); vm.setReeling(true); waitForUpOrCancellation(); vm.setReeling(false) }
                        Stage.FIGHT -> { vm.setReeling(true); waitForUpOrCancellation(); vm.setReeling(false) }
                        else -> Unit
                    }
                }
            },
        )

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
            conditions?.let { c ->
                val trip = Destinations.byId(progress.trip)
                val placeName = trip?.let { t(it.name) } ?: "${saved?.place?.name ?: ""} · ${t(progress.water.label)}"
                TopHud(c, placeName, progress, saved?.baitsNow(), vm.offline.collectAsStateWithLifecycle().value, onForecast, onSettings)
            }
            Spacer(Modifier.weight(1f))
            when (stage) {
                Stage.WAITING -> Hint(if (lake.hint) t(S.tooEarly) else t(S.waitForBite), warn = lake.hint)
                Stage.BITE -> StrikeBanner()
                Stage.FIGHT -> FightHud(lake.tension, lake.fightDistance, lake.running)
                else -> Unit
            }
            Spacer(Modifier.height(10.dp))
            if (stage == Stage.IDLE || stage == Stage.CHARGING) {
                LureRow(progress, enabled = stage == Stage.IDLE) { vm.equip(it) }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        NavButton(Icons.Rounded.Map, t(S.map), onMap)
                        NavButton(Icons.Rounded.MenuBook, t(S.book), onBook)
                    }
                    Spacer(Modifier.weight(1f))
                    CastButton(stage == Stage.CHARGING, lake.chargeStart,
                        onPress = {
                            val b = saved?.baitsNow()
                            if (b == null || b.count <= 0) noCasts = true else vm.pressCast(SystemClock.uptimeMillis())
                        },
                        onRelease = { vm.releaseCast(SystemClock.uptimeMillis()) },
                    )
                    Spacer(Modifier.weight(1f))
                    Column {
                        NavButton(Icons.Rounded.Storefront, t(S.shop), onShop)
                        val quests = progress.quests
                        val open = quests?.list?.count { !it.done } ?: 3
                        NavButton(Icons.Rounded.TaskAlt, t(S.quests), onQuests, badge = open.takeIf { it > 0 && quests?.day == java.time.LocalDate.now().toEpochDay() }?.toString())
                    }
                }
            }
        }

        AnimatedVisibility(stage == Stage.CAUGHT || stage == Stage.LOST, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                if (lake.caught != null && stage == Stage.CAUGHT) {
                    CatchCard(lake.caught, lake.newSpecies, lake.record, lake.reward, lake.doubled, progress.noAds, onDouble = { rewarded { vm.doubleCoins() } }) { next() }
                } else if (lake.lost != null) LostCard(lake.lost) { next() }
            }
        }

        if (noCasts) {
            Box(Modifier.fillMaxSize().background(Color(0x88000000)).clickable { noCasts = false }, contentAlignment = Alignment.Center) {
                NoCastsCard(saved?.baitsNow(), progress.noAds) { noCasts = false; rewarded { vm.bonusCasts() } }
            }
        }

        // Celebrations wait until the catch card is closed.
        val event = events.firstOrNull()
        if (event != null && stage != Stage.CAUGHT && stage != Stage.FIGHT) {
            EventDialog(event) { vm.consumeEvent() }
        }
    }
}

fun skyIcon(sky: Sky, phase: DayPhase): ImageVector = when (sky) {
    Sky.CLEAR -> if (phase == DayPhase.NIGHT) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
    Sky.PARTLY -> Icons.Rounded.WbCloudy
    Sky.CLOUDY -> Icons.Rounded.Cloud
    Sky.FOG -> Icons.Rounded.Dehaze
    Sky.DRIZZLE -> Icons.Rounded.Grain
    Sky.RAIN, Sky.HEAVY_RAIN -> Icons.Rounded.Umbrella
    Sky.SLEET, Sky.SNOW -> Icons.Rounded.AcUnit
    Sky.THUNDER -> Icons.Rounded.Thunderstorm
}

fun formatDuration(ms: Long): String {
    val m = (ms / 60_000).coerceAtLeast(0)
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "${m.coerceAtLeast(1)} min"
}

@Composable
private fun TopHud(c: Conditions, place: String, progress: Progress, baits: Baits?, offline: Boolean, onForecast: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Glass(Modifier.weight(1f, fill = false).clip(RoundedCornerShape(18.dp)).clickable(onClick = onForecast), padding = 10.dp) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(skyIcon(c.weather.sky, c.phase), null, tint = K.Amber, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${c.weather.tempC.roundToInt()}°", color = K.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(place, color = K.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // Non-breaking spaces inside each part: a long sky name wraps only at the dot.
                        Text("${t(S.sky(c.weather.sky)).replace(' ', '\u00A0')} · ${t(S.phase(c.phase)).replace(' ', '\u00A0')}", color = K.Muted, fontSize = 11.sp, maxLines = 2)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    BiteMeter(Fish.biteIndex(c, progress.spot))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.Insights, t(S.forecast), tint = K.Amber, modifier = Modifier.size(18.dp))
                }
                if (offline) Text(t(S.offline), color = K.Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp).width(190.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Glass(padding = 10.dp) {
                Column(horizontalAlignment = Alignment.End) {
                    CoinsLabel(progress.coins)
                    Spacer(Modifier.height(6.dp))
                    LevelBar(progress)
                    if (baits != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Phishing, null, tint = K.Amber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            // Bonus casts can go above the box size; then only the count is shown.
                            Text(if (baits.count > progress.boxSize) "${baits.count}" else "${baits.count}/${progress.boxSize}", color = K.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Text(t(S.casts), color = K.Muted, fontSize = 10.sp)
                        }
                        baits.nextIn(System.currentTimeMillis(), progress.boxSize)?.let { Text(t(S.nextCastIn, formatDuration(it)), color = K.Muted, fontSize = 10.sp) }
                    }
                }
            }
            Icon(Icons.Rounded.Settings, t(S.settings), tint = K.Text, modifier = Modifier.padding(top = 6.dp).size(38.dp).clip(CircleShape).background(Color(0x80101A28)).clickable(onClick = onSettings).padding(8.dp))
        }
    }
}

@Composable
fun CoinsLabel(coins: Int, size: Int = 18) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoinIcon(Modifier.size((size + 2).dp))
        Spacer(Modifier.width(5.dp))
        Text("%,d".format(coins).replace(',', ' '), color = K.Gold, fontSize = size.sp, fontWeight = FontWeight.Bold)
    }
}

/** A small embossed gold coin with a fish on it. */
@Composable
fun CoinIcon(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE6A0), Color(0xFFE0A030), Color(0xFFB87818)), Offset(0f, 0f), Offset(size.width, size.height)), r)
        drawCircle(Color(0xFF8A5A10), r * 0.78f, style = Stroke(r * 0.1f))
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x - r * 0.45f, center.y)
            quadraticTo(center.x, center.y - r * 0.38f, center.x + r * 0.3f, center.y)
            quadraticTo(center.x, center.y + r * 0.38f, center.x - r * 0.45f, center.y)
            moveTo(center.x + r * 0.28f, center.y)
            lineTo(center.x + r * 0.5f, center.y - r * 0.2f)
            lineTo(center.x + r * 0.5f, center.y + r * 0.2f)
            close()
        }
        drawPath(p, Color(0xFF8A5A10))
    }
}

@Composable
fun LevelBar(progress: Progress, width: Int = 120) {
    val lvl = progress.level
    val from = Levels.xpFor(lvl)
    val to = Levels.xpFor(lvl + 1)
    val f = ((progress.xp - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.End) {
        Text(t(S.level, lvl), color = K.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Canvas(Modifier.width(width.dp).height(6.dp)) {
            val cr = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            drawRoundRect(Color(0x33FFFFFF), cornerRadius = cr)
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF5B8BD0), Color(0xFF8FD3FF))), size = Size(size.width * f, size.height), cornerRadius = cr)
        }
    }
}

@Composable
private fun BiteMeter(index: Int) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t(S.biteIndex), color = K.Muted, fontSize = 11.sp)
            Spacer(Modifier.width(6.dp))
            Text("$index", color = K.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.width(150.dp).height(6.dp)) {
            drawRoundRect(Color(0x33FFFFFF), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFF5B8BD0), Color(0xFF8DC26F), K.Amber, Color(0xFFFF7A4A)), 0f, size.width),
                size = Size(size.width * index / 100f, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun Hint(text: String, warn: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Glass(radius = 30.dp, padding = 12.dp) {
            Text(text, color = if (warn) K.Amber else K.Text, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StrikeBanner() {
    val pulse by rememberInfiniteTransition(label = "strike").animateFloat(0.92f, 1.12f, infiniteRepeatable(tween(220), RepeatMode.Reverse), label = "s")
    Box(Modifier.fillMaxWidth().padding(bottom = 60.dp), contentAlignment = Alignment.Center) {
        Text(
            t(S.strike), color = K.Amber, fontSize = 54.sp, fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.displaySmall.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(0f, 4f), 12f)),
            modifier = Modifier.scale(pulse),
        )
    }
}

@Composable
private fun FightHud(tension: Float, distance: Float, running: Boolean) {
    Glass(Modifier.fillMaxWidth(), padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (running) t(S.fishRuns) else t(S.reelHint), color = if (running) K.Danger else K.Text, fontSize = 14.sp, fontWeight = if (running) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            // Line tension: green is safe, red snaps.
            Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                val r = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF3E8E5A), Color(0xFF8DC26F), K.Amber, K.Danger), 0f, size.width), cornerRadius = r, alpha = 0.35f)
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF3E8E5A), Color(0xFF8DC26F), K.Amber, K.Danger), 0f, size.width), size = Size(size.width * tension.coerceIn(0f, 1f), size.height), cornerRadius = r)
                val x = size.width * 0.97f
                drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
            }
            Spacer(Modifier.height(10.dp))
            // Distance to the landing net.
            Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                val y = size.height / 2
                drawLine(Color(0x55FFFFFF), Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(K.Reed, 7.dp.toPx(), Offset(6.dp.toPx(), y), style = Stroke(2.dp.toPx()))
                val fx = 14.dp.toPx() + (size.width - 30.dp.toPx()) * distance.coerceIn(0f, 1f)
                FishArt.draw(this, "roach", false, Rect(fx - 14.dp.toPx(), y - 7.dp.toPx(), fx + 14.dp.toPx(), y + 7.dp.toPx()), silhouette = true)
                drawCircle(K.Amber, 3.dp.toPx(), Offset(fx - 14.dp.toPx(), y))
            }
        }
    }
}

@Composable
private fun LureRow(progress: Progress, enabled: Boolean, onPick: (Lure) -> Unit) {
    val sfx = LocalContext.current.app.sfx
    val owned = Lure.entries.filter { progress.owns(it) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t(S.lure), color = K.Muted, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        for (l in owned) {
            val sel = l == progress.lure
            Box(
                Modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) K.Amber else Color(0x99101A28))
                    .border(1.dp, if (sel) K.Amber else K.Line, RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled) { sfx.play(Sfx.Fx.TAP, 0.5f); onPick(l) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(t(l.label), color = if (sel) Color(0xFF2A1A06) else K.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, label: String, onClick: () -> Unit, badge: String? = null) {
    val sfx = LocalContext.current.app.sfx
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.9f else 1f, label = "press")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp).scale(scale).clip(RoundedCornerShape(14.dp))
            .clickable(interaction, indication = null) { sfx.play(Sfx.Fx.TAP, 0.6f); onClick() }.padding(4.dp),
    ) {
        Box(Modifier.size(50.dp)) {
            Box(Modifier.size(46.dp).align(Alignment.Center).clip(CircleShape).background(Color(0xB3101A28)).border(1.dp, K.Line, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = K.Text, modifier = Modifier.size(24.dp))
            }
            if (badge != null) {
                Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(K.Danger), contentAlignment = Alignment.Center) {
                    Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = K.Text, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** Hold to charge the cast; the ring shows the power, release to throw. */
@Composable
private fun CastButton(charging: Boolean, chargeStart: Long, onPress: () -> Unit, onRelease: () -> Unit) {
    val press by rememberUpdatedState(onPress)
    val release by rememberUpdatedState(onRelease)
    var now by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(charging) {
        while (charging) withFrameNanos { now = SystemClock.uptimeMillis() }
    }
    val power = if (charging) GameViewModel.castPower(now - chargeStart) else 0f
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(0.4f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "g")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(108.dp).pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    press()
                    waitForUpOrCancellation()
                    release()
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(K.Amber.copy(alpha = 0.35f * glow), Color.Transparent), center, r), r)
                drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFC97A), Color(0xFFE08A2E))), r * 0.72f)
                drawCircle(Color(0x66FFFFFF), r * 0.72f, style = Stroke(1.5.dp.toPx()))
                drawArc(Color(0x33FFFFFF), -90f, 360f, false, Offset(r * 0.1f, r * 0.1f), Size(r * 1.8f, r * 1.8f), style = Stroke(6.dp.toPx()))
                if (charging) {
                    drawArc(
                        Brush.sweepGradient(listOf(Color(0xFF8DC26F), K.Amber, K.Danger, Color(0xFF8DC26F)), center),
                        -90f, 360f * power, false, Offset(r * 0.1f, r * 0.1f), Size(r * 1.8f, r * 1.8f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Icon(Icons.Rounded.Phishing, null, tint = Color(0xFF2A1A06), modifier = Modifier.size(40.dp))
        }
        Text(if (charging) "${(power * 100).roundToInt()} %" else t(S.cast), color = K.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CatchCard(c: Catch, isNew: Boolean, record: Boolean, reward: Reward?, doubled: Boolean, noAds: Boolean, onDouble: () -> Unit, onNext: () -> Unit) {
    val species = Fish.byId(c.species) ?: return
    val golden = c.golden?.let { Fish.goldenById(it) }
    val time = lt.marcomultiservices.kimba.ui.scene.rememberSceneTime()
    Glass(Modifier.padding(20.dp).fillMaxWidth(), radius = 26.dp, padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            val header = when {
                golden != null -> t(S.goldenCaught)
                isNew -> t(S.newSpecies)
                record -> t(S.newRecord)
                else -> t(S.caught)
            }
            Text(header, color = if (golden != null) K.Gold else K.Amber, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(golden?.name?.let { t(it) } ?: t(species.name), style = MaterialTheme.typography.displaySmall, color = K.Text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            // The fish swings up into view, dripping, and the numbers count up.
            val enter = remember(c) { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(c) { enter.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 120f)) }
            val drip = remember(c) { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(c) { drip.animateTo(1f, tween(1400)) }
            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(18.dp)).background(Brush.radialGradient(listOf(if (golden != null) Color(0x55FFD27A) else Color(0x335B8BD0), Color.Transparent)))) {
                FishPicture(
                    c.species, c.golden != null, time,
                    Modifier.fillMaxSize().graphicsLayer {
                        val p = enter.value
                        translationY = (1f - p) * 140f
                        rotationZ = kotlin.math.sin(p * 9f) * (1f - p.coerceAtMost(1f)) * 14f
                        scaleX = 0.75f + 0.25f * p
                        scaleY = 0.75f + 0.25f * p
                        alpha = p.coerceIn(0f, 1f)
                    },
                )
                Canvas(Modifier.fillMaxSize()) {
                    val d = drip.value
                    if (d < 1f) for (i in 0 until 14) {
                        val start = (i * 0.07f) % 0.6f
                        val k = ((d - start) / 0.4f).coerceIn(0f, 1f)
                        if (k <= 0f || k >= 1f) continue
                        val x = size.width * (0.22f + (i * 0.137f) % 0.56f)
                        val y = size.height * (0.45f + k * 0.6f)
                        drawOval(Color(0xCCBFE3FF).copy(alpha = 1f - k), Offset(x - 2.5f, y - 5f), Size(5f, 9f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val count = remember(c) { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(c) { count.animateTo(1f, tween(700, delayMillis = 250)) }
            val grams = (c.grams * count.value).roundToInt()
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Stat(t(S.length), "%.0f cm".format(c.cm * count.value))
                Stat(t(S.weight), if (c.grams >= 1000) "%.2f kg".format(grams / 1000.0) else "$grams g")
            }
            if (golden != null) {
                Spacer(Modifier.height(8.dp))
                Text(t(golden.condition), color = K.Gold, fontSize = 13.sp, textAlign = TextAlign.Center)
            } else {
                Spacer(Modifier.height(8.dp))
                Text(t(species.habit), color = K.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            if (reward != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t(S.plusCoins(c.coins * (if (doubled) 2 else 1))), color = K.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(14.dp))
                    Text(t(S.plusXp, reward.xp), color = Color(0xFF8FD3FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (reward != null && c.coins > 0) {
                OutlinedButton(onDouble, enabled = !doubled, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                    Text(if (doubled) t(S.doubled) else if (noAds) t(S.doubleCoinsNoAds) else t(S.doubleCoins), color = K.Gold)
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(onNext, colors = amberButtonColors(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(S.again)) }
        }
    }
}

@Composable
private fun EventDialog(event: GameEvent, onClose: () -> Unit) {
    val lang = LocalLang.current
    val (title, body) = when (event) {
        is GameEvent.StreakBonus -> (if (event.day <= 1) t(S.firstDayTitle) else t(S.streakTitle, event.day)) to t(S.streakBody, event.coins, event.casts)
        is GameEvent.LevelUp -> t(S.levelUp) to t(S.levelUpBody, event.level)
        is GameEvent.QuestsDone -> t(S.questsDone) to (
            event.quests.joinToString("\n") { "✓ " + it.text(lang) + "  +" + it.coins } +
                if (event.all) "\n\n" + t(S.allQuestsDone, Economy.ALL_QUESTS_COINS, Economy.ALL_QUESTS_CASTS) else ""
            )
    }
    val sfx = LocalContext.current.app.sfx
    LaunchedEffect(event) { sfx.play(if (event is GameEvent.LevelUp) Sfx.Fx.LEVEL else Sfx.Fx.COIN) }
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        Glass(Modifier.padding(28.dp).fillMaxWidth(), radius = 26.dp, padding = 22.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                when (event) {
                    is GameEvent.LevelUp -> Text("${event.level}", style = MaterialTheme.typography.displaySmall.copy(fontSize = 56.sp), color = Color(0xFF8FD3FF))
                    else -> CoinIcon(Modifier.size(48.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, color = K.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(body, color = K.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Button(onClose, colors = amberButtonColors(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(S.great)) }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = K.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = K.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun LostCard(reason: LostReason, onNext: () -> Unit) {
    val (title, hint) = when (reason) {
        LostReason.SNAPPED -> S.snapped to S.snappedHint
        LostReason.ESCAPED -> S.escaped to S.escapedHint
        LostReason.MISSED, LostReason.EARLY -> S.missed to S.missedHint
    }
    Glass(Modifier.padding(28.dp).fillMaxWidth(), radius = 26.dp, padding = 22.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(t(title), style = MaterialTheme.typography.headlineSmall, color = K.Text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(t(hint), color = K.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(onNext, colors = amberButtonColors(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(S.again)) }
        }
    }
}

@Composable
private fun NoCastsCard(baits: Baits?, noAds: Boolean, onBonus: () -> Unit) {
    Glass(Modifier.padding(28.dp).fillMaxWidth(), radius = 26.dp, padding = 22.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Air, null, tint = K.Amber, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(6.dp))
            Text(t(S.noCasts), style = MaterialTheme.typography.headlineSmall, color = K.Text)
            Spacer(Modifier.height(6.dp))
            Text(t(S.noCastsBody, formatDuration(baits?.nextIn(System.currentTimeMillis()) ?: 0)), color = K.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onBonus, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(S.bonusCasts), color = K.Amber) }
            if (!noAds) Text(t(S.bonusNote), color = K.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
