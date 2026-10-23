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
import lt.marcomultiservices.kimba.ui.scene.layers.drawSurprise
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
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
import lt.marcomultiservices.kimba.share.ShareCard
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import lt.marcomultiservices.kimba.game.GameEvent
import lt.marcomultiservices.kimba.game.GiftKind
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
import lt.marcomultiservices.kimba.game.Tip
import lt.marcomultiservices.kimba.core.Tackle
import lt.marcomultiservices.kimba.core.Unlocks
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
    var boxOpen by remember { mutableStateOf(false) }
    val demoBox by vm.demoBox.collectAsStateWithLifecycle()
    LaunchedEffect(demoBox) { demoBox?.let { boxOpen = it; vm.demoBox.value = null } }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val progress = saved?.progress ?: Progress()
    val events by vm.events.collectAsStateWithLifecycle()
    /** Rewarded video, or the reward right away for players who bought "no ads". */
    fun rewarded(then: () -> Unit) {
        if (progress.noAds || activity == null) then() else context.app.ads.showRewarded(activity, then)
    }
    val lang = LocalLang.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    /** The catch as a picture with the player's friend code, to the share sheet. */
    fun shareCatch(c: Catch) {
        val code = progress.inviteCode
        scope.launch {
            val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { ShareCard.render(context, c, lang, code) }
            val text = S.shareText[lang].format(ShareCard.name(c, lang), ShareCard.weight(c, lang), code, ShareCard.link(code))
            if (runCatching { ShareCard.send(context, bmp, text, S.shareChooser[lang]) }.isSuccess) vm.sharedCatch()
        }
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
        vm.setPaused(false)
        onPauseOrDispose { context.app.sfx.ambient(null); vm.setPaused(true) }
    }

    Box(Modifier.fillMaxSize().background(K.Night)) {
        conditions?.let { LakeScene(
                it, lakeState, Modifier.fillMaxSize(), progress.spot.water, progress.float, progress.gearHere.tackle, progress.owns(lt.marcomultiservices.kimba.core.Upgrade.ALARM),
                iceCm = if (progress.trip == null) lt.marcomultiservices.kimba.core.Ice.on(progress.water, progress.ice) else 0.0,
                stand = progress.standHere,
            ) }

        // Taps on the lake strike; holding during the fight reels in.
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    // During the fight the finger reels while held and steers the rod by where it is.
                    suspend fun AwaitPointerEventScope.play(start: androidx.compose.ui.geometry.Offset) {
                        vm.setReeling(true)
                        vm.aimRod((start.x / size.width - 0.5f) * 2.4f)
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            vm.aimRod((c.position.x / size.width - 0.5f) * 2.4f)
                        }
                        vm.setReeling(false)
                    }
                    when (vm.lake.value.stage) {
                        // A spinning lure is wound in while the finger is down, and pauses when it lifts.
                        Stage.WAITING -> if (vm.lake.value.tackle == Tackle.SPIN) {
                            vm.setWinding(true)
                            waitForUpOrCancellation()
                            vm.setWinding(false)
                        } else vm.strike()
                        Stage.BITE -> { vm.strike(); play(down.position) }
                        Stage.FIGHT -> play(down.position)
                        else -> Unit
                    }
                }
            },
        )

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
            conditions?.let { c ->
                val trip = Destinations.byId(progress.trip)
                val placeName = trip?.let { t(it.name) } ?: "${saved?.place?.name ?: ""} · ${t(progress.water.label)}"
                TopHud(c, placeName, progress, saved?.baitsNow(), vm.offline.collectAsStateWithLifecycle().value, vm.stormAt.collectAsStateWithLifecycle().value, onForecast, onSettings, onBook)
            }
            // First-play tips: each shows until the player has done that step once.
            val known = saved?.tips
            // A tip waits while a dialog (daily gift, level up) is open, so the two never overlap.
            fun tip(t: Tip) = known != null && t.name !in known && events.isEmpty()
            // While a cast is out, words sit high on the screen: the bottom belongs to the thumbs.
            Spacer(Modifier.height(10.dp))
            when (stage) {
                Stage.WAITING -> when {
                    !lake.hint && tip(Tip.WAIT) -> Coach(Tip.WAIT, tackle = lake.tackle)
                    lake.hint -> Hint(t(if (lake.tackle == Tackle.FLOAT) S.tooEarly else S.tooEarlyTip), warn = true)
                    lake.tackle == Tackle.SPIN && !lake.winding -> Hint(t(S.waitSpin), warn = false)
                    else -> Unit
                }
                Stage.BITE -> if (tip(Tip.STRIKE)) Coach(Tip.STRIKE)
                Stage.FIGHT -> if (tip(Tip.REEL)) Coach(Tip.REEL) else FightCue(lake.running, lake.atNet, lake.fishSide, lake.fishHeading, lake.steer, progress.onIce)
                else -> Unit
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(10.dp))
            if (stage == Stage.IDLE || stage == Stage.CHARGING) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        // The tackle box sits with the other buttons; it shows the bait on the hook.
                        TackleBoxButton(progress, enabled = stage == Stage.IDLE) { boxOpen = true }
                        NavButton(Icons.Rounded.Map, t(S.map), onMap)
                    }
                    Spacer(Modifier.weight(1f))
                    CastButton(stage == Stage.CHARGING, lake.chargeStart, ice = progress.onIce,
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

        // The fight: the tension meter at the right edge, the rod's steer track above the thumbs.
        if (stage == Stage.FIGHT) {
            TensionGauge(lake.tension, lake.fightDistance, lake.running, lake.tackle, Modifier.align(androidx.compose.ui.BiasAlignment(1f, 0.32f)).padding(end = 10.dp))
            SteerBar(lake.rodAim, lake.fishHeading, lake.atNet, lake.fishSide, lake.running, Modifier.align(androidx.compose.ui.BiasAlignment(0f, 1f)).navigationBarsPadding().padding(bottom = 18.dp))
        }

        // The strike call sits above the middle of the screen, clear of the thumbs.
        if (stage == Stage.BITE) Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.BiasAlignment(0f, -0.3f)) { StrikeBanner() }

        AnimatedVisibility(stage == Stage.CAUGHT || stage == Stage.LOST, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                if (lake.caught != null && stage == Stage.CAUGHT) {
                    CatchCard(lake.caught, lake.newSpecies, lake.record, lake.reward, lake.doubled, progress.noAds, lake.returnedFrom, onDouble = { rewarded { vm.doubleCoins() } }, onShare = { shareCatch(lake.caught) }, onRelease = { vm.releaseCatch() }) { next() }
                } else if (lake.surprise != null && stage == Stage.CAUGHT) {
                    SurpriseCard(lake.surprise, lake.foundLure, onCove = { vm.travel(lt.marcomultiservices.kimba.core.Destinations.secret); next() }) { next() }
                } else if (lake.lost != null) LostCard(lake.lost) { next() }
            }
        }

        TackleBox(boxOpen && (stage == Stage.IDLE || stage == Stage.CHARGING), progress, onTackle = { vm.equip(it) }, onBait = { vm.equip(it) }, onShop = { boxOpen = false; onShop() }, onClose = { boxOpen = false })

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

        // Demo hook: the share picture of the catch, as other apps would get it.
        val preview by vm.demoSharePreview.collectAsStateWithLifecycle()
        val shown = lake.caught
        if (preview && shown != null) {
            val img = remember(shown) { ShareCard.render(context, shown, lang, progress.inviteCode).asImageBitmap() }
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { vm.demoSharePreview.value = false }, contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(img, null, Modifier.fillMaxWidth())
            }
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
private fun TopHud(c: Conditions, place: String, progress: Progress, baits: Baits?, offline: Boolean, stormAt: Long?, onForecast: () -> Unit, onSettings: () -> Unit, onBook: () -> Unit) {
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
                // The fish the weather suits best right now, worth double.
                Fish.hotSpecies(c, progress.spot, progress.onIce)?.let { h ->
                    Text(t(S.hotNow, t(h.name)), color = K.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp).width(190.dp))
                }
                // The ice on the home water, grown from the real frost.
                val ice = if (progress.trip == null) lt.marcomultiservices.kimba.core.Ice.on(progress.water, progress.ice) else 0.0
                if (ice >= lt.marcomultiservices.kimba.core.Ice.RIM_CM) {
                    Text(
                        t(if (progress.onIce) S.iceOn else S.iceForming, ice.roundToInt()), color = Color(0xFFBFE3FF), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp).width(190.dp),
                    )
                }
                // A real thunderstorm now (coins ×2) or coming within hours (fish feed before it).
                val storm = when {
                    lt.marcomultiservices.kimba.core.Storm.on(c.weather) -> t(S.stormNow)
                    stormAt != null -> t(S.stormSoon, ((stormAt - c.epochMillis) / 3_600_000.0).roundToInt().coerceAtLeast(1))
                    else -> null
                }
                if (storm != null) Text(
                    storm, color = Color(0xFFFFE07A), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp).width(190.dp),
                )
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
                    // What the next level opens, so there is always something to fish towards.
                    Unlocks.next(progress.level)?.let { n ->
                        val lang = LocalLang.current
                        Text(
                            t(S.nextUnlock, n.level, n.names.first()[lang] + if (n.names.size > 1) " +${n.names.size - 1}" else ""),
                            color = K.Muted, fontSize = 10.sp, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp).width(120.dp),
                        )
                    }
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
            Row {
                Icon(Icons.Rounded.MenuBook, t(S.book), tint = K.Text, modifier = Modifier.padding(top = 6.dp).size(38.dp).clip(CircleShape).background(Color(0x80101A28)).clickable(onClick = onBook).padding(8.dp))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.Settings, t(S.settings), tint = K.Text, modifier = Modifier.padding(top = 6.dp).size(38.dp).clip(CircleShape).background(Color(0x80101A28)).clickable(onClick = onSettings).padding(8.dp))
            }
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

/** A first-play tip card; [arrow] points down at the cast button. */
@Composable
private fun Coach(tip: Tip, arrow: Boolean = false, tackle: Tackle = Tackle.FLOAT) {
    val (title, body) = when (tip) {
        Tip.CAST -> S.tipCastTitle to S.tipCast
        Tip.WAIT -> S.tipWaitTitle to when (tackle) { Tackle.FEEDER -> S.tipWaitFeeder; Tackle.SPIN -> S.tipWaitSpin; else -> S.tipWait }
        Tip.STRIKE -> S.tipStrikeTitle to S.tipStrike
        Tip.REEL -> S.tipReelTitle to S.tipReel
    }
    val bounce by rememberInfiniteTransition(label = "coach").animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "b")
    run {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Glass(Modifier.fillMaxWidth().border(1.dp, K.Amber.copy(alpha = 0.55f), RoundedCornerShape(22.dp)), radius = 22.dp, padding = 16.dp) {
                Column {
                    Text(t(S.tipStep, tip.ordinal + 1), color = K.Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(t(title), color = K.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(t(body), color = K.Text.copy(alpha = 0.88f), fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
            if (arrow) {
                Canvas(Modifier.padding(top = 4.dp).size(28.dp, 22.dp).graphicsLayer { translationY = bounce * 8.dp.toPx() }) {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.1f, size.height * 0.2f)
                        lineTo(size.width * 0.5f, size.height * 0.8f)
                        lineTo(size.width * 0.9f, size.height * 0.2f)
                    }
                    drawPath(p, K.Amber, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
private fun StrikeBanner() {
    val pulse by rememberInfiniteTransition(label = "strike").animateFloat(0.92f, 1.12f, infiniteRepeatable(tween(220), RepeatMode.Reverse), label = "s")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            t(S.strike), color = K.Amber, fontSize = 54.sp, fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.displaySmall.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(0f, 4f), 12f)),
            modifier = Modifier.scale(pulse),
        )
    }
}

/** One line high on the screen during the fight: what the fish does and which way to lead the rod. */
@Composable
private fun FightCue(running: Boolean, atNet: Boolean, side: Float, heading: Float, steer: Float, ice: Boolean = false) {
    val (cue, color) = when {
        running -> t(S.fishRuns) to K.Danger
        atNet && kotlin.math.abs(side) >= 0.4f -> t(if (side > 0f) S.steerLeft else S.steerRight) to K.Amber
        atNet -> t(if (ice) S.holeLead else S.netLead) to Color(0xFF8DC26F)
        steer > 0.35f -> t(S.steerGood) to Color(0xFF8DC26F)
        heading > 0f -> t(S.steerLeft) to K.Amber
        else -> t(S.steerRight) to K.Amber
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Glass(radius = 30.dp, padding = 12.dp) {
            Text(cue, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

/**
 * The line-tension meter standing at the right edge: it fills from the bottom, green while
 * safe, red in the top band where the line snaps; above it, the line still out in metres.
 */
@Composable
private fun TensionGauge(tension: Float, distance: Float, running: Boolean, tackle: Tackle, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "gauge").animateFloat(0.4f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "p")
    val full = when (tackle) { Tackle.FEEDER -> 60; Tackle.SPIN -> 45; else -> 25 }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Glass(radius = 14.dp, padding = 8.dp) {
            Text("${(distance.coerceIn(0f, 1f) * full).toInt().coerceAtLeast(1)} m", color = K.Amber, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.size(30.dp, 250.dp)) {
            val r = androidx.compose.ui.geometry.CornerRadius(size.width / 2)
            val colors = listOf(K.Danger, K.Amber, Color(0xFF8DC26F), Color(0xFF3E8E5A))
            drawRoundRect(Color(0xB3101A28), cornerRadius = r)
            drawRoundRect(Brush.verticalGradient(colors, 0f, size.height), cornerRadius = r, alpha = 0.22f)
            val f = tension.coerceIn(0f, 1f)
            val top = size.height * (1f - f)
            drawRoundRect(Brush.verticalGradient(colors, 0f, size.height), topLeft = Offset(0f, top), size = Size(size.width, size.height - top), cornerRadius = r)
            // The snapping point.
            val y = size.height * 0.04f
            drawLine(Color.White, Offset(size.width * 0.15f, y), Offset(size.width * 0.85f, y), strokeWidth = 2.dp.toPx())
            drawRoundRect(if (running) K.Danger.copy(alpha = pulse) else K.Line, cornerRadius = r, style = Stroke(if (running) 3.dp.toPx() else 1.dp.toPx()))
        }
    }
}

/**
 * Where the finger leads the rod: a track across the screen with the rod's knob on it and the
 * side to lead towards lit green, so it is plain that sliding the finger steers.
 */
@Composable
private fun SteerBar(aim: Float, heading: Float, atNet: Boolean, side: Float, running: Boolean, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "steer").animateFloat(0f, 1f, infiniteRepeatable(tween(700)), label = "p")
    Canvas(modifier.fillMaxWidth(0.82f).height(46.dp)) {
        val y = size.height / 2
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(Color(0x99101A28), cornerRadius = r)
        // The good side: against the fish, or the middle when netting.
        val (from, to) = when {
            atNet -> if (kotlin.math.abs(side) < 0.4f) -0.25f to 0.25f else if (side > 0f) -1f to -0.2f else 0.2f to 1f
            heading > 0f -> -1f to -0.15f
            else -> 0.15f to 1f
        }
        fun x(v: Float) = size.width * (0.5f + 0.46f * v)
        if (!running) {
            drawRoundRect(Color(0x558DC26F), topLeft = Offset(x(from), 4.dp.toPx()), size = Size(x(to) - x(from), size.height - 8.dp.toPx()), cornerRadius = r)
            // Chevrons running towards the good side.
            val dir = if (atNet && kotlin.math.abs(side) < 0.4f) 0f else if ((from + to) < 0f) -1f else 1f
            if (dir != 0f) for (k in 0..2) {
                val cx = x(dir * (0.25f + 0.22f * k + 0.1f * pulse))
                val a = (1f - k * 0.3f) * (0.4f + 0.6f * pulse)
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - dir * 6.dp.toPx(), y - 9.dp.toPx())
                    lineTo(cx + dir * 4.dp.toPx(), y)
                    lineTo(cx - dir * 6.dp.toPx(), y + 9.dp.toPx())
                }
                drawPath(p, Color(0xFFBFF0A0).copy(alpha = a), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
        // The rod's knob where the finger holds it.
        val kx = x(aim.coerceIn(-1f, 1f))
        drawCircle(Color.Black.copy(alpha = 0.3f), 15.dp.toPx(), Offset(kx, y + 2.dp.toPx()))
        drawCircle(K.Amber, 14.dp.toPx(), Offset(kx, y))
        drawCircle(Color.White.copy(alpha = 0.8f), 5.dp.toPx(), Offset(kx, y))
    }
}

/** A small coloured label on the catch card. */
@Composable
private fun Tag(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.16f)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
private fun CastButton(charging: Boolean, chargeStart: Long, ice: Boolean = false, onPress: () -> Unit, onRelease: () -> Unit) {
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
        Text(if (ice) t(S.lower) else if (charging) "${(power * 100).roundToInt()} %" else t(S.cast), color = K.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CatchCard(c: Catch, isNew: Boolean, record: Boolean, reward: Reward?, doubled: Boolean, noAds: Boolean, returnedFrom: Double?, onDouble: () -> Unit, onShare: () -> Unit, onRelease: () -> Unit, onNext: () -> Unit) {
    val species = Fish.byId(c.species) ?: return
    val golden = c.golden?.let { Fish.goldenById(it) }
    val time = lt.marcomultiservices.kimba.ui.scene.rememberSceneTime()
    Glass(Modifier.padding(20.dp).fillMaxWidth(), radius = 26.dp, padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            val header = when {
                golden?.legend == true -> t(S.legendCaught)
                golden != null -> t(S.goldenCaught)
                isNew -> t(S.newSpecies)
                record -> t(S.newRecord)
                else -> t(S.caught)
            }
            Text(header, color = if (golden?.legend == true) Color(0xFFC79BFF) else if (golden != null) K.Gold else K.Amber, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(golden?.name?.let { t(it) } ?: t(species.name), style = MaterialTheme.typography.displaySmall, color = K.Text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            // How rare the kind is, and a trophy mark for a fish near the top of its size range.
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val (rarity, rc) = when {
                    golden?.legend == true -> t(S.legend) to Color(0xFFC79BFF)
                    golden != null -> t(S.rarityVeryRare) to K.Gold
                    species.abundance >= 0.8 -> t(S.rarityCommon) to Color(0xFF9FB3C2)
                    species.abundance >= 0.5 -> t(S.rarityUncommon) to Color(0xFF8DC26F)
                    species.abundance >= 0.25 -> t(S.rarityRare) to Color(0xFF6FB6FF)
                    else -> t(S.rarityVeryRare) to Color(0xFFC79BFF)
                }
                Tag(rarity, rc)
                val frac = (c.cm - species.minCm) / (species.maxCm - species.minCm).toDouble()
                if (frac >= 0.75) Tag(t(S.trophy), K.Gold)
                if (reward?.challenge == true) Tag(t(S.challengeTag), K.Amber)
                if (reward?.storm == true) Tag(t(S.stormTag), Color(0xFFFFE07A))
                if (returnedFrom != null) Tag(t(S.returnedTag, returnedFrom.roundToInt(), c.cm.roundToInt()), Color(0xFF8DC26F))
            }
            Spacer(Modifier.height(10.dp))
            // The fish swings up into view, dripping, and the numbers count up.
            val enter = remember(c) { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(c) { enter.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 120f)) }
            val drip = remember(c) { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(c) { drip.animateTo(1f, tween(1400)) }
            // A glint runs once along the wet fish.
            val shine = remember(c) { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(c) { shine.animateTo(1f, tween(900, delayMillis = 650)) }
            // The trophy photo: the angler lifts the fish out across both hands.
            val context = LocalContext.current
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(18.dp)).background(Brush.radialGradient(listOf(if (golden != null) Color(0x55FFD27A) else Color(0x335B8BD0), Color.Transparent)))) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                val boxH = hPx * 1.25f
                val boxW = boxH * 0.8f
                val figure = android.graphics.RectF(wPx / 2 - boxW / 2, hPx * 0.04f, wPx / 2 + boxW / 2, hPx * 0.04f + boxH)
                val (hl, hr, hy) = lt.marcomultiservices.kimba.share.Angler.hands(figure)
                val fishW = ((hr - hl) / 0.55f * (c.cm / 50.0).toFloat().coerceIn(0.85f, 1.3f)).coerceAtMost(wPx * 0.98f)
                Canvas(Modifier.fillMaxSize()) {
                    drawIntoCanvas { lt.marcomultiservices.kimba.share.Angler.body(context, it.nativeCanvas, figure) }
                }
                FishPicture(
                    c.species, Fish.gold(c.golden), time,
                    Modifier
                        .offset { androidx.compose.ui.unit.IntOffset((wPx / 2 - fishW / 2).roundToInt(), (hy - fishW * 0.27f).roundToInt()) }
                        .size(with(density) { fishW.toDp() }, with(density) { (fishW * 0.5f).toDp() })
                        .graphicsLayer {
                        val p = enter.value
                        translationY = (1f - p) * 140f
                        rotationZ = kotlin.math.sin(p * 9f) * (1f - p.coerceAtMost(1f)) * 14f
                        scaleX = 0.75f + 0.25f * p
                        scaleY = 0.75f + 0.25f * p
                        alpha = p.coerceIn(0f, 1f)
                    },
                )
                Canvas(Modifier.fillMaxSize()) {
                    // Hands lift with the fish as it swings up.
                    val lift = (1f - enter.value) * 140f
                    drawIntoCanvas { cv ->
                        cv.nativeCanvas.save()
                        cv.nativeCanvas.translate(0f, lift)
                        lt.marcomultiservices.kimba.share.Angler.hands(context, cv.nativeCanvas, figure)
                        cv.nativeCanvas.restore()
                    }
                }
                Canvas(Modifier.fillMaxSize()) {
                    val g = shine.value
                    if (g > 0f && g < 1f) {
                        val x = size.width * (-0.3f + 1.6f * g)
                        drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent), Offset(x - 60f, 0f), Offset(x + 60f, size.height)))
                    }
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
            // The real weather it was caught in: the story of this fish.
            Spacer(Modifier.height(6.dp))
            val clock = java.time.Instant.ofEpochMilli(c.epochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            Text(t(S.caughtIn, t(S.sky(c.sky)), c.tempC.roundToInt(), "%02d:%02d".format(clock.hour, clock.minute)), color = Color(0xFF8FB4D0), fontSize = 12.sp, textAlign = TextAlign.Center)
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
            // Anglers love to show a fish off: the picture carries the friend code to whoever sees it.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onShare, modifier = Modifier.weight(1f).height(46.dp)) {
                    Icon(Icons.Rounded.Share, null, tint = K.Text, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t(S.shareCatch), color = K.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Catch and release: gives up the fish's coins for experience and good-angler points.
                if (c.golden == null) OutlinedButton(onRelease, enabled = !c.released && !doubled, modifier = Modifier.weight(1f).height(46.dp)) {
                    Text(t(if (c.released) S.releasedDone else S.release), color = if (c.released) Color(0xFF8DC26F) else K.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(8.dp))
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
        is GameEvent.Achieved -> t(S.achieved) to event.list.joinToString("\n\n") { "🏆 " + it.title[lang] + "\n" + it.text[lang] + "  +" + it.coins }
        is GameEvent.Frozen -> t(S.frozenTitle) to t(S.frozenBody, event.cm)
        is GameEvent.Story -> t(S.jonasWell) to listOfNotNull(
            t(S.plusCoins(event.done.coins)),
            event.done.opens?.let { t(S.jonasOpens, t(it.label)) },
            if (event.done.revealsClues) t(S.jonasClues) else null,
            event.next?.let { "„" + t(it.talk) + "“\n\n" + t(S.jonasNext, t(it.task)) } ?: t(S.jonasDone),
        ).joinToString("\n\n")
        is GameEvent.Released -> t(S.releasedTitle) to t(S.releasedBody, lt.marcomultiservices.kimba.core.Release.XP, event.points, event.total)
        is GameEvent.Storm -> t(S.stormTitle) to (if (event.boat) t(S.stormBoat) + "\n\n" else "") + t(S.stormBody)
        is GameEvent.Gift -> {
            val gifts = listOfNotNull(if (event.coins > 0) S.plusCoins(event.coins)[lang] else null, if (event.casts > 0) S.plusCasts(event.casts)[lang] else null).joinToString("   ")
            when (event.kind) {
                GiftKind.FRIEND -> t(S.giftFriendTitle) to gifts + "\n\n" + t(S.giftFriendNote)
                GiftKind.THANKS -> t(S.giftThanksTitle) to gifts
                GiftKind.SHARE -> t(S.giftShareTitle) to gifts
                GiftKind.CAMP -> t(S.giftCampTitle) to gifts
            }
        }
    }
    val sfx = LocalContext.current.app.sfx
    LaunchedEffect(event) { sfx.play(if (event is GameEvent.LevelUp) Sfx.Fx.LEVEL else Sfx.Fx.COIN) }
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        Glass(Modifier.padding(28.dp).fillMaxWidth(), radius = 26.dp, padding = 22.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                when (event) {
                    is GameEvent.LevelUp -> Text("${event.level}", style = MaterialTheme.typography.displaySmall.copy(fontSize = 56.sp), color = Color(0xFF8FD3FF))
                    is GameEvent.Achieved -> TrophyIcon(Modifier.size(56.dp))
                    is GameEvent.Story -> Canvas(Modifier.size(72.dp).clip(CircleShape).background(Color(0x33FFFFFF))) {
                        drawJonas(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), if (event.next == null) 3 else if (event.done.revealsClues) 2 else 3)
                    }
                    is GameEvent.Storm -> Icon(Icons.Rounded.Thunderstorm, null, tint = Color(0xFFFFE07A), modifier = Modifier.size(52.dp))
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
private fun SurpriseCard(s: lt.marcomultiservices.kimba.core.Surprise, lure: lt.marcomultiservices.kimba.core.Lure?, onCove: () -> Unit, onNext: () -> Unit) {
    val time = lt.marcomultiservices.kimba.ui.scene.rememberSceneTime()
    val reward = when (s) {
        lt.marcomultiservices.kimba.core.Surprise.SUNKEN_ROD -> if (lure != null) t(S.foundLure, t(lure.label)) else t(S.plusCoins(s.coins))
        lt.marcomultiservices.kimba.core.Surprise.BOTTLE -> t(S.foundCove)
        else -> listOfNotNull(if (s.coins > 0) t(S.plusCoins(s.coins)) else null, "+${s.xp} XP").joinToString("   ")
    }
    Glass(Modifier.padding(28.dp).fillMaxWidth(), radius = 26.dp, padding = 22.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(t(S.surpriseHeader).uppercase(), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(t(s.label), style = MaterialTheme.typography.headlineSmall, color = K.Text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Canvas(Modifier.size(150.dp)) {
                drawSurprise(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), s, time.floatValue)
            }
            Spacer(Modifier.height(8.dp))
            Text(t(s.note), color = K.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(reward, color = K.Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            if (s == lt.marcomultiservices.kimba.core.Surprise.BOTTLE) {
                OutlinedButton(onCove, modifier = Modifier.fillMaxWidth().height(46.dp)) { Text(t(S.goCove), color = K.Text) }
                Spacer(Modifier.height(8.dp))
            }
            Button(onNext, colors = amberButtonColors(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(t(S.again)) }
        }
    }
}

@Composable
private fun LostCard(reason: LostReason, onNext: () -> Unit) {
    val (title, hint) = when (reason) {
        LostReason.SNAPPED -> S.snapped to S.snappedHint
        LostReason.ESCAPED -> S.escaped to S.escapedHint
        LostReason.MISSED, LostReason.EARLY -> S.missed to S.missedHint
        LostReason.EMPTY -> S.empty to S.emptyHint
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

/** A small gold cup for achievements. */
@Composable
fun TrophyIcon(modifier: Modifier, earned: Boolean = true) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val gold = if (earned) Brush.linearGradient(listOf(Color(0xFFFFE6A0), Color(0xFFE0A030), Color(0xFFB87818)), Offset(0f, 0f), Offset(w, h))
            else Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x33FFFFFF)), Offset(0f, 0f), Offset(w, h))
        val cup = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.22f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.12f)
            cubicTo(w * 0.78f, h * 0.45f, w * 0.66f, h * 0.6f, w * 0.54f, h * 0.64f)
            lineTo(w * 0.54f, h * 0.76f)
            cubicTo(w * 0.62f, h * 0.78f, w * 0.68f, h * 0.82f, w * 0.7f, h * 0.9f)
            lineTo(w * 0.3f, h * 0.9f)
            cubicTo(w * 0.32f, h * 0.82f, w * 0.38f, h * 0.78f, w * 0.46f, h * 0.76f)
            lineTo(w * 0.46f, h * 0.64f)
            cubicTo(w * 0.34f, h * 0.6f, w * 0.22f, h * 0.45f, w * 0.22f, h * 0.12f)
            close()
        }
        val handles = Stroke(w * 0.06f, cap = StrokeCap.Round)
        drawArc(gold, 90f, 180f, false, Offset(w * 0.08f, h * 0.18f), Size(w * 0.28f, h * 0.26f), style = handles)
        drawArc(gold, -90f, 180f, false, Offset(w * 0.64f, h * 0.18f), Size(w * 0.28f, h * 0.26f), style = handles)
        drawPath(cup, gold)
        if (earned) drawCircle(Color.White.copy(alpha = 0.55f), w * 0.05f, Offset(w * 0.36f, h * 0.24f))
    }
}
