package lt.marcomultiservices.kimba.game

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lt.marcomultiservices.kimba.app
import lt.marcomultiservices.kimba.audio.Sfx
import lt.marcomultiservices.kimba.core.Achievement
import lt.marcomultiservices.kimba.core.Achievements
import lt.marcomultiservices.kimba.core.Astro
import lt.marcomultiservices.kimba.core.Baits
import lt.marcomultiservices.kimba.core.Bite
import lt.marcomultiservices.kimba.core.BiteEngine
import lt.marcomultiservices.kimba.core.Catch
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.core.DayPhase
import lt.marcomultiservices.kimba.core.Fight
import lt.marcomultiservices.kimba.core.FightResult
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Forecast
import lt.marcomultiservices.kimba.core.GoldenWindow
import lt.marcomultiservices.kimba.core.Destination
import lt.marcomultiservices.kimba.core.Destinations
import lt.marcomultiservices.kimba.core.Economy
import lt.marcomultiservices.kimba.core.Invites
import lt.marcomultiservices.kimba.core.Item
import lt.marcomultiservices.kimba.core.acceptInvite
import lt.marcomultiservices.kimba.core.shared
import lt.marcomultiservices.kimba.core.thank
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.Quest
import lt.marcomultiservices.kimba.core.Quests
import lt.marcomultiservices.kimba.core.Reward
import lt.marcomultiservices.kimba.core.Streak
import lt.marcomultiservices.kimba.core.Tackle
import lt.marcomultiservices.kimba.core.Lure
import lt.marcomultiservices.kimba.core.Upgrade
import lt.marcomultiservices.kimba.core.Shop
import lt.marcomultiservices.kimba.core.Water
import lt.marcomultiservices.kimba.core.Sky
import lt.marcomultiservices.kimba.core.Weather
import lt.marcomultiservices.kimba.core.Windows
import lt.marcomultiservices.kimba.data.Place
import lt.marcomultiservices.kimba.data.Places
import lt.marcomultiservices.kimba.data.Saved
import lt.marcomultiservices.kimba.data.WeatherRepo
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.random.Random

enum class Stage { IDLE, CHARGING, FLYING, WAITING, BITE, FIGHT, LANDING, CAUGHT, LOST }

enum class LostReason { EARLY, MISSED, SNAPPED, ESCAPED, EMPTY }

/** First-play tips, each shown until the player has done that step once. */
enum class Tip { CAST, WAIT, STRIKE, REEL }

data class LakeState(
    val stage: Stage = Stage.IDLE,
    /** Uptime millis when charging began (the power meter runs from it). */
    val chargeStart: Long = 0,
    /** Cast distance 0..1 of the current cast. */
    val castDist: Double = 0.5,
    /** Increments with every cast, so the float flight animation restarts. */
    val castId: Int = 0,
    /** How deep the float is pulled: 0 resting, ~0.35 nibble, 1 bite; below 0 it lifts. */
    val dip: Float = 0f,
    /** Uptime millis of the last nibble (the float trembles after it) and how many came so far. */
    val nibbleAt: Long = 0,
    val nibbles: Int = 0,
    /** The rod of this cast: decides what the scene shows (float, rod tip, spinning lure). */
    val tackle: Tackle = Tackle.FLOAT,
    /** Uptime millis when the bait landed, and for a spinning lure the seconds it takes to wind it back. */
    val landedAt: Long = 0,
    val retrieve: Float = 0f,
    /** How far the spinning lure had come back (0..1) when the fish took it. */
    val takenAt: Float = 0f,
    /** A spinning lure on its way back: 0 where it landed, 1 at the bank. */
    val spin: Float = 0f,
    /** The player is winding the spinning reel. */
    val winding: Boolean = false,
    /** The electronic bite alarm is beeping. */
    val alarm: Boolean = false,
    val tension: Float = 0f,
    val fightDistance: Float = 1f,
    /** Where the fish was hooked, on the same scale as [fightDistance]. */
    val fightStart: Float = 1f,
    /** The hooked fish across the water, -1 left .. 1 right, and the way it swims. */
    val fishSide: Float = 0f,
    val fishHeading: Float = 1f,
    /** Where the player points the rod, -1 left .. 1 right. */
    val rodAim: Float = 0f,
    /** -1..1: how well the rod is held against the fish. */
    val steer: Float = 0f,
    /** The fish is at the bank, to be led into the landing net. */
    val atNet: Boolean = false,
    /** A jolt of the view (uptime ms and strength 0..1): the strike, a run, the fish out of the water. */
    val shakeAt: Long = 0,
    val shake: Float = 0f,
    val netReady: Boolean = false,
    val running: Boolean = false,
    val bite: Bite? = null,
    val lost: LostReason? = null,
    val caught: Catch? = null,
    val newSpecies: Boolean = false,
    val record: Boolean = false,
    val hint: Boolean = false,
    val reward: Reward? = null,
    val doubled: Boolean = false,
)

/** Things to celebrate in a dialog, one after another. */
sealed class GameEvent {
    class StreakBonus(val day: Int, val coins: Int, val casts: Int) : GameEvent()
    class LevelUp(val level: Int) : GameEvent()
    class QuestsDone(val quests: List<Quest>, val all: Boolean) : GameEvent()
    class Achieved(val list: List<Achievement>) : GameEvent()
    class Gift(val kind: GiftKind, val coins: Int, val casts: Int) : GameEvent()
}

/** Why a [GameEvent.Gift] came: a friend's code, a friend's thank-you code, a shared catch. */
enum class GiftKind { FRIEND, THANKS, SHARE }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = application.app.store
    private val repo = application.app.weather
    private val sfx = application.app.sfx
    private val rng = Random(System.nanoTime())
    private val zone: ZoneId = ZoneId.systemDefault()

    private val _saved = MutableStateFlow<Saved?>(null)
    val saved: StateFlow<Saved?> = _saved.asStateFlow()

    private val _forecast = MutableStateFlow<Forecast?>(null)
    val forecast: StateFlow<Forecast?> = _forecast.asStateFlow()

    private val _conditions = MutableStateFlow<Conditions?>(null)
    val conditions: StateFlow<Conditions?> = _conditions.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val _lake = MutableStateFlow(LakeState())
    val lake: StateFlow<LakeState> = _lake.asStateFlow()

    /** A fixed scene for screenshots and demos ("storm", "dawnfog", ...), set from the launch intent. */
    var demoScene: String? = null
        private set

    private val _events = MutableStateFlow<List<GameEvent>>(emptyList())
    val events: StateFlow<List<GameEvent>> = _events.asStateFlow()

    private var job: Job? = null
    private var reeling = false
    @Volatile private var rodAim = 0f
    /** The finger winds a spinning lure in while it is held. */
    @Volatile private var winding = false
    /** The app is in the background: a fight waits instead of going on without the player. */
    @Volatile private var paused = false
    private var resultsSinceAd = 0
    /** Place whose forecast is in [forecast]. */
    private var forecastFor: Place? = null

    init {
        viewModelScope.launch {
            store.flow.collect {
                _saved.value = it
                sfx.enabled = it.sound
                if (it.onboarded) dailyVisit()
                if (it.progress.inviteCode.isEmpty()) makeInviteCode()
                if (!it.referrerChecked) readReferrer()
            }
        }
        viewModelScope.launch {
            // Re-evaluate the conditions every minute; refetch the forecast when it is due.
            while (isActive) {
                refresh(force = false)
                delay(60_000)
            }
        }
    }

    /** Back to the idle lake, ending any cast or fight (demo hooks). */
    fun resetLake() {
        job?.cancel()
        _lake.value = LakeState(castId = _lake.value.castId)
    }

    fun setDemo(scene: String?) {
        if (scene == demoScene) return
        demoScene = scene
        viewModelScope.launch { refresh(force = false) }
    }

    private fun today(): Long = LocalDate.now(zone).toEpochDay()

    private var visitedDay = -1L

    /** Once a day: the return bonus. */
    private fun dailyVisit() {
        val day = today()
        if (visitedDay == day) return
        visitedDay = day
        viewModelScope.launch {
            var bonus: Streak.Bonus? = null
            store.update { s ->
                val (p, b) = Streak.visit(s.progress, day)
                bonus = b
                if (b == null) s else s.copy(progress = p.copy(quests = Quests.today(p.quests, day, p.level)), baits = s.baits.add(b.casts, System.currentTimeMillis(), p.boxSize))
            }
            bonus?.let { b -> _events.update { it + GameEvent.StreakBonus(b.day, b.coins, b.casts) } }
            checkAchievements()
        }
    }

    fun consumeEvent() = _events.update { it.drop(1) }

    // ---- Friends -------------------------------------------------------------------------

    private var codeMade = false
    private var referrerAsked = false

    private fun makeInviteCode() {
        if (codeMade) return
        codeMade = true
        viewModelScope.launch {
            store.update { s -> if (s.progress.inviteCode.isEmpty()) s.copy(progress = s.progress.copy(inviteCode = Invites.newCode(rng))) else s }
        }
    }

    /** A friend's store link carries `kimba=CODE`; Google Play passes it on once, on first start. */
    private fun readReferrer() {
        if (referrerAsked) return
        referrerAsked = true
        val client = runCatching { com.android.installreferrer.api.InstallReferrerClient.newBuilder(getApplication<Application>()).build() }.getOrNull() ?: return
        runCatching {
            client.startConnection(object : com.android.installreferrer.api.InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(code: Int) {
                    val ok = com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK
                    val unavailable = com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE
                    val referrer = if (code == ok) runCatching { client.installReferrer.installReferrer }.getOrNull() else null
                    runCatching { client.endConnection() }
                    if (code == unavailable) return
                    viewModelScope.launch {
                        store.update { it.copy(referrerChecked = true) }
                        referrer?.let { Regex("kimba=([A-Za-z0-9]{6})").find(it)?.groupValues?.get(1) }?.let { enterCode(it) }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {}
            })
        }
    }

    enum class CodeResult { FRIEND, THANKS, BAD }

    /** A friend's code (six letters) or a friend's thank-you code (ten). */
    fun enterCode(code: String, done: (CodeResult) -> Unit = {}) = viewModelScope.launch {
        var result = CodeResult.BAD
        store.update { s ->
            val now = System.currentTimeMillis()
            s.progress.acceptInvite(code)?.let { p ->
                result = CodeResult.FRIEND
                return@update s.copy(progress = p, baits = s.baits.add(Invites.NEWCOMER_CASTS, now, p.boxSize))
            }
            s.progress.thank(code)?.let { p ->
                result = CodeResult.THANKS
                return@update s.copy(progress = p, baits = s.baits.add(Invites.THANKS_CASTS, now, p.boxSize))
            }
            s
        }
        when (result) {
            CodeResult.FRIEND -> GameEvent.Gift(GiftKind.FRIEND, Invites.NEWCOMER_COINS, Invites.NEWCOMER_CASTS)
            CodeResult.THANKS -> GameEvent.Gift(GiftKind.THANKS, Invites.THANKS_COINS, Invites.THANKS_CASTS)
            CodeResult.BAD -> null
        }?.let { e -> _events.update { it + e } }
        done(result)
    }

    /** The player shared a catch: the first share of the day pays a few casts. */
    fun sharedCatch() = viewModelScope.launch {
        var paid = false
        store.update { s ->
            s.progress.shared(today())?.let { p ->
                paid = true
                s.copy(progress = p, baits = s.baits.add(Invites.SHARE_CASTS, System.currentTimeMillis(), p.boxSize))
            } ?: s
        }
        if (paid) _events.update { it + GameEvent.Gift(GiftKind.SHARE, 0, Invites.SHARE_CASTS) }
    }

    /** Demo hook: shows the share picture of the catch on screen. */
    val demoSharePreview = MutableStateFlow(false)

    /** Pays and announces goals reached outside a catch (streak, trips). */
    private suspend fun checkAchievements() {
        var got: List<Achievement> = emptyList()
        store.update { s ->
            val (p, new) = Achievements.check(s.progress, s.catches)
            got = new
            if (new.isEmpty()) s else s.copy(progress = p)
        }
        if (got.isNotEmpty()) _events.update { it + GameEvent.Achieved(got) }
    }

    /** Remembers that the player has done the step of [tip], so its tip is not shown again. */
    private fun learn(tip: Tip) {
        if (tip.name in (_saved.value?.tips ?: emptySet())) return
        viewModelScope.launch { store.update { it.copy(tips = it.tips + tip.name) } }
    }

    private fun knows(tip: Tip): Boolean = tip.name in (_saved.value?.tips ?: emptySet())

    /** Where the player fishes now: a trip destination or home. */
    fun fishingPlace(s: Saved? = _saved.value): Place? {
        val d = Destinations.byId(s?.progress?.trip)
        return if (d != null) Place(d.id, d.lat, d.lon) else s?.place
    }

    suspend fun refresh(force: Boolean) {
        val place = fishingPlace(store.current()) ?: return
        demoScene?.let { _conditions.value = demoConditions(it, place); return }
        if (forecastFor != place) { _forecast.value = null; forecastFor = place }
        repo.cached(place)?.let { if (_forecast.value == null) { _forecast.value = it; recompute(place) } }
        val f = repo.forecast(place, force)
        _offline.value = f == null || f.weatherAt(System.currentTimeMillis()) == null
        if (f != null) _forecast.value = f
        recompute(place)
    }

    private fun recompute(place: Place) {
        val now = System.currentTimeMillis()
        val w = _forecast.value?.weatherAt(now) ?: WeatherRepo.seasonal()
        _conditions.value = Conditions(w, now, place.lat, place.lon, zone)
    }

    fun goldenWindows(): List<GoldenWindow> {
        val s = _saved.value ?: return emptyList()
        val place = fishingPlace(s) ?: return emptyList()
        val f = _forecast.value ?: return emptyList()
        return Windows.golden(f, System.currentTimeMillis(), place.lat, place.lon, zone, goldens = Fish.goldensAt(s.progress.spot))
    }

    fun choosePlace(place: Place) {
        viewModelScope.launch {
            store.update { it.copy(place = place, onboarded = true) }
            _forecast.value = null
            refresh(force = true)
        }
    }

    fun setNotify(on: Boolean) = viewModelScope.launch { store.update { it.copy(notify = on) } }
    fun setSound(on: Boolean) = viewModelScope.launch { store.update { it.copy(sound = on) } }

    private fun updateProgress(f: (Progress) -> Progress?) = viewModelScope.launch {
        store.update { s -> f(s.progress)?.let { s.copy(progress = it) } ?: s }
    }

    fun buy(item: Item) = updateProgress { p -> p.buy(item)?.also { sfx.play(Sfx.Fx.COIN) } }
    fun equip(item: Item) = updateProgress { it.equip(item) }
    fun setNoAds(on: Boolean) = updateProgress { if (it.noAds == on) null else it.copy(noAds = on) }

    fun travel(d: Destination) {
        if (_lake.value.stage == Stage.FIGHT) return
        updateProgress { it.travel(d) }.invokeOnCompletion { afterMove() }
    }

    fun goHome(water: Water) {
        if (_lake.value.stage == Stage.FIGHT) return
        updateProgress { it.goHome(water) }.invokeOnCompletion { afterMove() }
    }

    private fun afterMove() {
        job?.cancel()
        _lake.value = LakeState(castId = _lake.value.castId)
        viewModelScope.launch { checkAchievements(); refresh(force = false) }
    }

    /** Reward for watching an ad when the casts are used up. */
    fun bonusCasts() = viewModelScope.launch {
        store.update { it.copy(baits = it.baits.add(5, System.currentTimeMillis(), it.progress.boxSize)) }
    }

    /** Reward for watching an ad after a catch: the fish's coins once more. */
    fun doubleCoins() {
        val s = _lake.value
        val catch = s.caught ?: return
        if (s.doubled || s.stage != Stage.CAUGHT) return
        _lake.update { it.copy(doubled = true) }
        updateProgress { it.copy(coins = it.coins + catch.coins) }
    }

    /** An interstitial after every fifth result, never during play. */
    fun interstitialDue(): Boolean {
        if (_saved.value?.progress?.noAds == true) return false
        resultsSinceAd++
        if (resultsSinceAd < 5) return false
        resultsSinceAd = 0
        return true
    }

    // ---- Fishing -------------------------------------------------------------------------

    fun pressCast(uptime: Long) {
        if (_lake.value.stage !in setOf(Stage.IDLE, Stage.CAUGHT, Stage.LOST)) return
        val baits = _saved.value?.baitsNow() ?: return
        if (baits.count <= 0) return
        _lake.value = LakeState(stage = Stage.CHARGING, chargeStart = uptime, castId = _lake.value.castId)
    }

    fun releaseCast(uptime: Long) {
        val s = _lake.value
        if (s.stage != Stage.CHARGING) return
        val power = castPower(uptime - s.chargeStart).toDouble()
        val c = _conditions.value ?: return
        job?.cancel()
        val p = _saved.value?.progress ?: return
        // The cast flies at once; the bait is taken from the box meanwhile (pressCast checked there is one).
        viewModelScope.launch {
            store.update { saved -> saved.baits.use(System.currentTimeMillis(), saved.progress.boxSize)?.let { saved.copy(baits = it) } ?: saved }
        }
        job = viewModelScope.launch {
            val rolled = BiteEngine.roll(c, power, rng, p.spot, p.gear)
            // The very first fish comes soon, after two clear nibbles, so the tips can teach it.
            val bite = if (knows(Tip.STRIKE)) rolled else
                Bite(rolled.species, rolled.golden, rolled.cm, rolled.waitSeconds.coerceIn(8.0, 11.0), listOf(3.0, 5.5), rolled.condition)
            learn(Tip.CAST)
            val tackle = p.tackle
            // A spinning lure is wound straight back: the take comes sooner, with fewer knocks before it.
            // A spinning lure is hunted while it is worked: the take comes sooner, with fewer knocks before it.
            val cast = if (tackle != Tackle.SPIN) bite else {
                val wait = (bite.waitSeconds * 0.5).coerceIn(3.0, 7.0)
                Bite(bite.species, bite.golden, bite.cm, wait, bite.nibbles.map { it * 0.5 }.filter { it < wait - 0.8 }.take(2), bite.condition)
            }
            _lake.value = LakeState(stage = Stage.FLYING, castDist = power, castId = s.castId + 1, bite = cast, tackle = tackle)
            sfx.play(Sfx.Fx.CAST, 0.7f + 0.3f * power.toFloat())
            delay(FLIGHT_MS)
            sfx.play(Sfx.Fx.PLOP, (if (tackle == Tackle.FEEDER) 0.8f else 0.5f) + 0.4f * (1 - power.toFloat()), rate = (if (tackle == Tackle.FEEDER) 0.75f else 0.9f) + 0.2f * rng.nextFloat())
            _lake.update { it.copy(landedAt = android.os.SystemClock.uptimeMillis(), retrieve = if (tackle == Tackle.SPIN) (cast.waitSeconds + 2.5).toFloat() else 0f) }
            if (tackle == Tackle.SPIN) spinRetrieve(cast) else waitForBite(cast)
        }
    }

    fun setWinding(on: Boolean) { winding = on }

    /**
     * The spinning lure comes back only while the player winds. A predator takes a lure that is
     * worked: wound with pauses now and then (the lure flutters down on a stop) it is hunted at
     * full rate, wound on and on without a stop at half. Brought to the bank untouched, the
     * cast is empty.
     */
    private suspend fun spinRetrieve(bite: Bite) = kotlinx.coroutines.coroutineScope {
        _lake.update { it.copy(stage = Stage.WAITING, dip = 0f, spin = 0f, winding = false) }
        val length = bite.waitSeconds + 3.0
        val nibbles = ArrayDeque(bite.nibbles)
        var pos = 0.0
        var hunted = 0.0
        var clock = 0.0
        var lastStop = -10.0
        var was = false
        while (true) {
            delay(30)
            if (paused) continue
            val dt = 0.03
            clock += dt
            val w = winding
            if (was && !w) lastStop = clock
            was = w
            val worked = clock - lastStop < 3.0
            if (w) {
                pos += dt / length
                hunted += dt * (if (worked) 1.0 else 0.5)
            } else if (clock - lastStop < 0.9) hunted += dt * 0.7
            _lake.update { it.copy(spin = pos.toFloat().coerceAtMost(1f), winding = w) }
            val next = nibbles.firstOrNull()
            if (next != null && hunted >= next) {
                // A knock on the lure: a fish follows and taps it.
                nibbles.removeFirst()
                buzz(15)
                sfx.play(Sfx.Fx.NIBBLE, 0.5f, rate = 1.1f + 0.2f * rng.nextFloat())
                _lake.update { it.copy(nibbleAt = android.os.SystemClock.uptimeMillis(), nibbles = it.nibbles + 1) }
            }
            if (hunted >= bite.waitSeconds) break
            if (pos >= 1.0) {
                winding = false
                _lake.update { it.copy(stage = Stage.LOST, lost = LostReason.EMPTY, winding = false) }
                return@coroutineScope
            }
        }
        val taken = pos.toFloat().coerceIn(0f, 1f)
        _lake.update { it.copy(stage = Stage.BITE, dip = 1f, takenAt = taken, winding = false) }
        learn(Tip.WAIT)
        buzz(longArrayOf(0, 60, 50, 90))
        sfx.play(Sfx.Fx.BITE, rate = if (bite.pull > 0.6) 0.85f else 1f)
        // A hit on a lure that is being wound is felt in the hand: the angler strikes at once.
        if (winding) { startFight(bite); return@coroutineScope }
        var left = if (!knows(Tip.STRIKE)) 1700L else 800L
        while (left > 0) { delay(50); if (!paused) left -= 50 }
        if (_lake.value.stage == Stage.BITE) _lake.update { it.copy(stage = Stage.LOST, lost = LostReason.MISSED, dip = 0f) }
    }

    /** A bite alarm sounds on a bottom rod once it is bought. */
    private fun hasAlarm(): Boolean = _lake.value.tackle == Tackle.FEEDER && _saved.value?.progress?.owns(Upgrade.ALARM) == true

    private suspend fun waitForBite(bite: Bite) = kotlinx.coroutines.coroutineScope {
        _lake.update { it.copy(stage = Stage.WAITING, dip = 0f) }
        var start = System.currentTimeMillis()
        val nibbles = ArrayDeque(bite.nibbles)
        while (true) {
            // In the background the wait holds still, so the bite is not missed unseen.
            if (paused) { delay(200); start += 200; continue }
            val t = (System.currentTimeMillis() - start) / 1000.0
            if (t >= bite.waitSeconds) break
            val next = nibbles.firstOrNull()
            if (next != null && t >= next) {
                nibbles.removeFirst()
                // A nibble: two or three quick twitches of the float, then it settles and trembles.
                buzz(18)
                sfx.play(Sfx.Fx.NIBBLE, 0.6f, rate = 0.85f + 0.3f * rng.nextFloat())
                _lake.update { it.copy(nibbleAt = android.os.SystemClock.uptimeMillis(), nibbles = it.nibbles + 1, alarm = hasAlarm()) }
                if (hasAlarm()) sfx.play(Sfx.Fx.ALARM, 0.7f)
                repeat(2 + rng.nextInt(2)) {
                    _lake.update { it.copy(dip = 0.18f + rng.nextFloat() * 0.25f) }
                    delay(110L + rng.nextInt(70))
                    _lake.update { it.copy(dip = 0.02f) }
                    delay(90L + rng.nextInt(80))
                }
                _lake.update { it.copy(dip = 0f, alarm = false) }
            }
            delay(30)
        }
        // Just before the bite the fish lifts the bait: the float rises a little, then goes under.
        _lake.update { it.copy(dip = -0.22f, nibbleAt = android.os.SystemClock.uptimeMillis()) }
        delay(260)
        val taken = _lake.value.let { st -> if (st.retrieve > 0f) ((android.os.SystemClock.uptimeMillis() - st.landedAt) / 1000f / st.retrieve).coerceIn(0f, 1f) else 0f }
        _lake.update { it.copy(stage = Stage.BITE, dip = 1f, takenAt = taken, alarm = hasAlarm()) }
        if (hasAlarm()) launch { repeat(4) { sfx.play(Sfx.Fx.ALARM); delay(140) } }
        learn(Tip.WAIT)
        buzz(longArrayOf(0, 60, 50, 90))
        sfx.play(Sfx.Fx.BITE, rate = if (bite.pull > 0.6) 0.85f else 1f)
        // A beginner gets more time to strike.
        val tackle = _lake.value.tackle
        val window = when {
            !knows(Tip.STRIKE) -> 1700L
            tackle == Tackle.FEEDER -> 1300L
            tackle == Tackle.SPIN -> 800L
            bite.golden != null || bite.pull > 0.8 -> 750L
            else -> 950L
        }
        // Only time in front of the player counts towards the strike window.
        var left = window
        while (left > 0) { delay(50); if (!paused) left -= 50 }
        if (_lake.value.stage == Stage.BITE) {
            _lake.update { it.copy(stage = Stage.LOST, lost = LostReason.MISSED, dip = 0f, alarm = false) }
        }
    }

    /** A tap on the lake: strikes during a bite, scares the fish while waiting. */
    fun strike() {
        val s = _lake.value
        when (s.stage) {
            Stage.BITE -> startFight(s.bite ?: return)
            Stage.WAITING -> {
                if (s.tackle == Tackle.SPIN) return
                // Striking at a nibble scares the fish: the wait starts over, a little longer.
                val bite = s.bite ?: return
                job?.cancel()
                _lake.update { it.copy(hint = true, dip = 0f) }
                job = viewModelScope.launch {
                    val again = Bite(bite.species, bite.golden, bite.cm, bite.waitSeconds * 0.6 + 3, bite.nibbles.map { it * 0.6 + 2 }, bite.condition)
                    // A spinning lure keeps coming back meanwhile: it needs the longer way.
                    _lake.update { st ->
                        val gone = (android.os.SystemClock.uptimeMillis() - st.landedAt) / 1000f
                        st.copy(bite = again, retrieve = if (st.retrieve > 0f) gone + again.waitSeconds.toFloat() + 2.5f else 0f)
                    }
                    launch { delay(2500); _lake.update { it.copy(hint = false) } }
                    waitForBite(again)
                }
            }
            else -> Unit
        }
    }

    private fun startFight(bite: Bite) {
        job?.cancel()
        reeling = false
        rodAim = 0f
        val gear = _saved.value?.progress?.gear
        // The fish is played in from where it took the bait: a short cast, or a spinner taken
        // near the bank, leaves less line to wind in. A little room stays for the fight itself.
        val st = _lake.value
        val out = (st.castDist * (1.0 - (if (st.tackle == Tackle.SPIN) st.takenAt.toDouble() else 0.0))).coerceIn(0.3, 1.0)
        val fight = Fight(bite.pull, rng, gear?.strength ?: 1.0, gear?.reel?.speed ?: 1.0, out)
        _lake.update { it.copy(stage = Stage.FIGHT, dip = 0f, tension = 0.3f, fightDistance = out.toFloat(), fightStart = out.toFloat(), alarm = false) }
        learn(Tip.STRIKE)
        thump(45, bite.pull)
        jolt(0.35f + 0.4f * bite.pull.toFloat())
        sfx.play(Sfx.Fx.SPLASH, 0.45f, rate = 1.2f)
        job = viewModelScope.launch {
            // The reel and drag loops stop however the fight ends, also when it is cancelled.
            coroutineContext[Job]?.invokeOnCompletion { sfx.hold(Sfx.Fx.REEL, false); sfx.hold(Sfx.Fx.DRAG, false) }
            var last = System.nanoTime()
            while (fight.result == null) {
                delay(16)
                val now = System.nanoTime()
                if (paused) {
                    sfx.hold(Sfx.Fx.REEL, false); sfx.hold(Sfx.Fx.DRAG, false)
                    last = now
                    continue
                }
                fight.step(((now - last) / 1e9).coerceAtMost(0.05), reeling, rodAim.toDouble())
                last = now
                if (fight.turned) buzz(12)
                if (fight.runStarted) {
                    thump(60, bite.pull)
                    jolt(0.25f + 0.5f * bite.pull.toFloat())
                    sfx.play(Sfx.Fx.SPLASH, 0.55f, rate = 1.1f + 0.2f * rng.nextFloat())
                }
                // The reel whirrs while it is wound; a running fish pulls line off against the drag, which screams.
                sfx.hold(Sfx.Fx.REEL, reeling && !fight.running, 0.5f, rate = (0.85 + 0.3 * (gear?.reel?.speed ?: 1.0)).toFloat())
                val dragging = fight.running || fight.tension > 0.9
                sfx.hold(Sfx.Fx.DRAG, dragging, (0.45 + 0.4 * fight.tension).toFloat().coerceAtMost(0.9f), rate = (0.8 + 0.35 * fight.currentPull).toFloat())
                // A warning before the line gives: it creaks and the phone ticks.
                if (fight.tension > 0.85) {
                    sfx.play(Sfx.Fx.STRAIN, 0.5f, minGapMs = 520)
                    if (fight.tension > 0.92) buzzTick()
                }
                _lake.update { it.copy(tension = fight.tension.toFloat(), fightDistance = fight.distance.toFloat(), running = fight.running, fishSide = fight.side.toFloat(), fishHeading = fight.heading.toFloat(), rodAim = rodAim, steer = fight.steer.toFloat(), atNet = fight.atNet, netReady = fight.netReady) }
            }
            sfx.hold(Sfx.Fx.REEL, false)
            sfx.hold(Sfx.Fx.DRAG, false)
            when (fight.result!!) {
                FightResult.LANDED -> landed(bite)
                FightResult.SNAPPED -> { buzz(120); sfx.play(Sfx.Fx.SNAP); _lake.update { it.copy(stage = Stage.LOST, lost = LostReason.SNAPPED) } }
                FightResult.ESCAPED -> { sfx.play(Sfx.Fx.PLOP, 0.5f, rate = 0.7f); _lake.update { it.copy(stage = Stage.LOST, lost = LostReason.ESCAPED) } }
            }
        }
    }

    fun setReeling(on: Boolean) { reeling = on }

    /** The rod pointed left (-1) or right (1) during a fight. */
    fun aimRod(aim: Float) { rodAim = aim.coerceIn(-1f, 1f) }

    fun setPaused(on: Boolean) {
        paused = on
        if (on) reeling = false
    }

    private suspend fun landed(bite: Bite) {
        val c = _conditions.value ?: return
        val saved = store.current()
        val spot = saved.progress.spot
        val base = Catch(
            species = bite.species.id,
            golden = bite.golden?.id,
            cm = bite.cm,
            grams = bite.grams,
            epochMillis = System.currentTimeMillis(),
            sky = c.weather.sky,
            tempC = c.weather.tempC,
            place = spot.trip ?: saved.place?.name ?: "",
            water = spot.water,
        )
        val key = base.golden ?: base.species
        val previous = saved.catches.filter { (it.golden ?: it.species) == key }
        val isNew = previous.isEmpty()
        val record = !isNew && previous.all { it.cm < base.cm }
        // The fish of the moment pays double.
        val hot = Fish.hotSpecies(c, spot) == bite.species
        val catch = base.copy(coins = Economy.coins(bite.species, bite.golden, bite.grams) * (if (hot) 2 else 1))
        var reward: Reward? = null
        var achieved: List<Achievement> = emptyList()
        store.update { s ->
            val (landed, r) = Economy.land(s.progress, catch, isNew, today())
            reward = r
            // Keep the log bounded, but never drop the best fish of a kind: the book and trophies need it.
            val all = s.catches + catch
            val best = all.groupBy { it.golden ?: it.species }.values.map { l -> l.maxBy { it.cm } }.toSet()
            val catches = if (all.size <= 600) all else all.filterIndexed { i, it -> i >= all.size - 600 || it in best }
            val (p, got) = Achievements.check(landed, catches)
            achieved = got
            val baits = if (r.allQuests) s.baits.add(Economy.ALL_QUESTS_CASTS, System.currentTimeMillis(), p.boxSize) else s.baits
            s.copy(catches = catches, progress = p, baits = baits, tips = s.tips + Tip.REEL.name)
        }
        // The fish comes out of the water on the line, dripping, then its card shows.
        buzz(longArrayOf(0, 40, 60, 40))
        sfx.play(Sfx.Fx.SPLASH)
        _lake.update { it.copy(stage = Stage.LANDING, tension = 0f) }
        jolt(0.3f)
        delay(LANDING_MS)
        sfx.play(Sfx.Fx.COIN, 0.8f)
        _lake.update { it.copy(stage = Stage.CAUGHT, caught = catch, newSpecies = isNew, record = record, reward = reward) }
        reward?.let { r ->
            if (r.quests.isNotEmpty()) _events.update { it + GameEvent.QuestsDone(r.quests, r.allQuests) }
            r.levelUp?.let { l -> _events.update { it + GameEvent.LevelUp(l) } }
        }
        if (achieved.isNotEmpty()) _events.update { it + GameEvent.Achieved(achieved) }
    }

    /** Test money and experience for screenshots. */
    fun demoProgress(coins: Int, xp: Int) = updateProgress { p ->
        p.copy(coins = if (coins >= 0) coins else p.coins, xp = if (xp >= 0) xp else p.xp)
    }

    /** Gives every rod and bait and picks [name] (for screenshots). */
    fun demoTackle(name: String) {
        val t = Tackle.entries.firstOrNull { it.name.equals(name, true) } ?: return
        updateProgress { p -> p.copy(owned = p.owned + Tackle.entries.map { it.key } + Lure.entries.map { it.key } + Upgrade.ALARM.key).equip(t) }
    }

    /** Opens (true) or closes (false) the tackle box on the lake (for screenshots); null once handled. */
    val demoBox = MutableStateFlow<Boolean?>(null)

    /** Casts with [power] 0..1 without touching the screen (for screenshots). */
    fun demoCast(power: Float) {
        resetLake()
        val now = android.os.SystemClock.uptimeMillis()
        pressCast(now - (power.coerceIn(0.05f, 1f) * 1600).toLong())
        releaseCast(now)
    }

    /** Goes to a destination without paying (for screenshots). */
    fun demoTrip(d: Destination) {
        updateProgress { it.copy(trips = it.trips + d.id, trip = d.id) }.invokeOnCompletion { afterMove() }
    }

    /** Starts a fight with a mid-sized fish directly (for screenshots). */
    fun demoFight(id: String, net: Boolean = false) {
        resetLake()
        val species = Fish.byId(id) ?: return
        val bite = Bite(species, null, (species.minCm + species.maxCm) / 2.0, 1.0, emptyList())
        _lake.value = LakeState(castDist = 0.7, castId = _lake.value.castId, bite = bite)
        // A fight held still mid-play (or at the net) so the screenshot shows its controls.
        job = viewModelScope.launch {
            delay(300)
            _lake.value = _lake.value.copy(
                stage = Stage.FIGHT, tension = 0.62f, fightDistance = if (net) 0.03f else 0.55f, fightStart = 0.7f,
                fishSide = if (net) 0.5f else 0.35f, fishHeading = 1f, rodAim = -0.2f, steer = 0.2f, atNet = net, tackle = Tackle.FLOAT,
            )
        }
    }

    /** Shows a catch card directly (for screenshots of the fish art). */
    fun showDemoCatch(id: String) {
        val golden = Fish.goldenById(id)
        val species = golden?.base ?: Fish.byId(id) ?: return
        // Show the fish over its own water, never a sea fish over the lake.
        val here = _saved.value?.progress?.spot
        if (here == null || !species.livesAt(here)) {
            val trip = species.onlyAt?.firstOrNull()
            val water = Destinations.byId(trip)?.water ?: species.waters.first()
            updateProgress { it.copy(trip = trip, trips = it.trips + listOfNotNull(trip), water = water) }
            viewModelScope.launch { refresh(force = false) }
        }
        val cm = species.minCm + (species.maxCm - species.minCm) * 0.7
        _lake.value = LakeState(
            stage = Stage.CAUGHT, castDist = 0.6,
            caught = Catch(species.id, golden?.id, kotlin.math.round(cm), species.grams(kotlin.math.round(cm)), System.currentTimeMillis(), Sky.CLEAR, 12.0, "", species.waters.first()),
            newSpecies = true,
        )
    }

    fun dismissResult() {
        if (_lake.value.stage == Stage.CAUGHT || _lake.value.stage == Stage.LOST) _lake.update { LakeState(castId = it.castId, castDist = it.castDist) }
    }

    private fun buzz(ms: Long) = buzz(longArrayOf(0, ms))

    private var lastTick = 0L

    /** A light tick, at most every 250 ms, while the line is close to snapping. */
    private fun buzzTick() {
        val now = System.currentTimeMillis()
        if (now - lastTick < 250) return
        lastTick = now
        buzz(8)
    }

    /** Shakes the view a little, harder for a stronger [power]. */
    private fun jolt(power: Float) = _lake.update { it.copy(shakeAt = android.os.SystemClock.uptimeMillis(), shake = power.coerceIn(0.15f, 1f)) }

    /** A buzz that is stronger for a heavier fish, where the phone can vary its strength. */
    private fun thump(ms: Long, power: Double) {
        val ctx = getApplication<Application>()
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Vibrator::class.java)
        }
        val amp = (90 + 165 * power.coerceIn(0.0, 1.0)).toInt()
        runCatching {
            if (vib?.hasAmplitudeControl() == true) vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, ms, 50, ms / 2), intArrayOf(0, amp, 0, amp / 2), -1))
            else vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, ms, 50, ms / 2), -1))
        }
    }

    private fun buzz(pattern: LongArray) {
        val ctx = getApplication<Application>()
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Vibrator::class.java)
        }
        runCatching { vib?.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    /** Demo weather and a time of today at which the sun stands right for the scene. */
    private fun demoConditions(scene: String, place: Place): Conditions {
        val (w, phase) = when (scene) {
            "storm" -> Weather(23.0, 1004.0, -4.0, 1.0, 9.0, 4.0, Sky.THUNDER) to DayPhase.DUSK
            "dawnfog" -> Weather(14.0, 1016.0, 0.0, 0.6, 0.5, 0.0, Sky.FOG) to DayPhase.DAWN
            "night" -> Weather(17.0, 1015.0, 0.0, 0.05, 1.0, 0.0, Sky.CLEAR) to DayPhase.NIGHT
            "snow" -> Weather(-3.0, 1020.0, 0.5, 0.9, 3.0, 0.6, Sky.SNOW) to DayPhase.DAY
            "rain" -> Weather(11.0, 1006.0, -3.0, 1.0, 5.0, 2.5, Sky.RAIN) to DayPhase.DAY
            "dusk" -> Weather(19.0, 1012.0, -1.0, 0.25, 2.0, 0.0, Sky.PARTLY) to DayPhase.DUSK
            else -> Weather(22.0, 1018.0, 0.0, 0.15, 2.5, 0.0, Sky.CLEAR) to DayPhase.DAY
        }
        val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val targetElev = when (phase) { DayPhase.NIGHT -> -25.0; DayPhase.DAWN -> -2.0; DayPhase.DUSK -> -1.0; DayPhase.DAY -> 40.0 }
        val candidates = (0 until 24 * 12).map { dayStart + it * 5 * 60_000L }.filter { t ->
            val ha = Astro.sunHourAngle(t, place.lon)
            when (phase) { DayPhase.DAWN -> ha < 0; DayPhase.DUSK -> ha > 0; else -> true }
        }
        val t = candidates.minBy { abs(Astro.sunElevation(it, place.lat, place.lon) - targetElev) }
        return Conditions(w, t, place.lat, place.lon, zone)
    }

    companion object {
        const val FLIGHT_MS = 2000L
        /** How long the landed fish is lifted out of the water before its card shows. */
        const val LANDING_MS = 1600L

        /** Power meter: rises and falls every 3.2 s while the button is held. */
        fun castPower(heldMs: Long): Float {
            val p = (heldMs % 3200L) / 1600f
            return (if (p <= 1f) p else 2f - p).coerceIn(0.05f, 1f)
        }

        val defaultPlace: Place get() = Places.cities.first()
    }
}

/** Baits now, for display. */
fun Saved.baitsNow(): Baits = baits.now(System.currentTimeMillis(), progress.boxSize)
