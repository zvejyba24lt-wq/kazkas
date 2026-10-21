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
import lt.marcomultiservices.kimba.core.Item
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

enum class LostReason { EARLY, MISSED, SNAPPED, ESCAPED }

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
    /** The electronic bite alarm is beeping. */
    val alarm: Boolean = false,
    val tension: Float = 0f,
    val fightDistance: Float = 1f,
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
}

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
    private var resultsSinceAd = 0
    /** Place whose forecast is in [forecast]. */
    private var forecastFor: Place? = null

    init {
        viewModelScope.launch {
            store.flow.collect {
                _saved.value = it
                sfx.enabled = it.sound
                if (it.onboarded) dailyVisit()
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
        store.update { it.copy(baits = it.baits.add(3, System.currentTimeMillis(), it.progress.boxSize)) }
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
        job = viewModelScope.launch {
            var ok = false
            store.update { saved ->
                val used = saved.baits.use(System.currentTimeMillis(), saved.progress.boxSize)
                ok = used != null
                if (used != null) saved.copy(baits = used) else saved
            }
            if (!ok) { _lake.value = LakeState(castId = s.castId); return@launch }
            val p = store.current().progress
            val rolled = BiteEngine.roll(c, power, rng, p.spot, p.gear)
            // The very first fish comes soon, after two clear nibbles, so the tips can teach it.
            val bite = if (knows(Tip.STRIKE)) rolled else
                Bite(rolled.species, rolled.golden, rolled.cm, rolled.waitSeconds.coerceIn(8.0, 11.0), listOf(3.0, 5.5), rolled.condition)
            learn(Tip.CAST)
            val tackle = p.tackle
            // A spinning lure is wound straight back: the take comes sooner, with fewer knocks before it.
            val cast = if (tackle != Tackle.SPIN) bite else {
                val wait = (bite.waitSeconds * 0.5).coerceIn(3.5, 9.0)
                Bite(bite.species, bite.golden, bite.cm, wait, bite.nibbles.map { it * 0.5 }.filter { it < wait - 0.8 }.take(2), bite.condition)
            }
            _lake.value = LakeState(stage = Stage.FLYING, castDist = power, castId = s.castId + 1, bite = cast, tackle = tackle)
            sfx.play(Sfx.Fx.CAST, 0.7f + 0.3f * power.toFloat())
            delay(FLIGHT_MS)
            sfx.play(Sfx.Fx.PLOP, (if (tackle == Tackle.FEEDER) 0.8f else 0.5f) + 0.4f * (1 - power.toFloat()), rate = (if (tackle == Tackle.FEEDER) 0.75f else 0.9f) + 0.2f * rng.nextFloat())
            _lake.update { it.copy(landedAt = android.os.SystemClock.uptimeMillis(), retrieve = if (tackle == Tackle.SPIN) (cast.waitSeconds + 2.5).toFloat() else 0f) }
            waitForBite(cast)
        }
    }

    /** A bite alarm sounds on a bottom rod once it is bought. */
    private fun hasAlarm(): Boolean = _lake.value.tackle == Tackle.FEEDER && _saved.value?.progress?.owns(Upgrade.ALARM) == true

    private suspend fun waitForBite(bite: Bite) = kotlinx.coroutines.coroutineScope {
        _lake.update { it.copy(stage = Stage.WAITING, dip = 0f) }
        val start = System.currentTimeMillis()
        val nibbles = ArrayDeque(bite.nibbles)
        while (true) {
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
        delay(when {
            !knows(Tip.STRIKE) -> 1700L
            tackle == Tackle.FEEDER -> 1300L
            tackle == Tackle.SPIN -> 800L
            bite.golden != null || bite.pull > 0.8 -> 750L
            else -> 950L
        })
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
        val gear = _saved.value?.progress?.gear
        val fight = Fight(bite.pull, rng, gear?.strength ?: 1.0, gear?.reel?.speed ?: 1.0)
        _lake.update { it.copy(stage = Stage.FIGHT, dip = 0f, tension = 0.3f, fightDistance = 1f, alarm = false) }
        learn(Tip.STRIKE)
        buzz(40)
        sfx.play(Sfx.Fx.SPLASH, 0.45f, rate = 1.2f)
        val reelGap = (120 / (gear?.reel?.speed ?: 1.0)).toLong()
        job = viewModelScope.launch {
            var last = System.nanoTime()
            while (fight.result == null) {
                delay(16)
                val now = System.nanoTime()
                fight.step(((now - last) / 1e9).coerceAtMost(0.05), reeling)
                last = now
                if (fight.runStarted) {
                    buzz(longArrayOf(0, 30, 40, 30, 40, 30))
                    sfx.play(Sfx.Fx.SPLASH, 0.55f, rate = 1.1f + 0.2f * rng.nextFloat())
                }
                if (reeling && !fight.running) sfx.play(Sfx.Fx.REEL, 0.45f, rate = 0.95f + 0.1f * rng.nextFloat(), minGapMs = reelGap)
                // A warning before the line gives: it creaks and the phone ticks.
                if (fight.tension > 0.85) {
                    sfx.play(Sfx.Fx.STRAIN, 0.5f, minGapMs = 520)
                    if (fight.tension > 0.92) buzzTick()
                }
                _lake.update { it.copy(tension = fight.tension.toFloat(), fightDistance = fight.distance.toFloat(), running = fight.running) }
            }
            when (fight.result!!) {
                FightResult.LANDED -> landed(bite)
                FightResult.SNAPPED -> { buzz(120); sfx.play(Sfx.Fx.SNAP); _lake.update { it.copy(stage = Stage.LOST, lost = LostReason.SNAPPED) } }
                FightResult.ESCAPED -> { sfx.play(Sfx.Fx.PLOP, 0.5f, rate = 0.7f); _lake.update { it.copy(stage = Stage.LOST, lost = LostReason.ESCAPED) } }
            }
        }
    }

    fun setReeling(on: Boolean) { reeling = on }

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
        val catch = base.copy(coins = Economy.coins(bite.species, bite.golden, bite.grams))
        var reward: Reward? = null
        var achieved: List<Achievement> = emptyList()
        store.update { s ->
            val (landed, r) = Economy.land(s.progress, catch, isNew, today())
            reward = r
            // Keep the log bounded; the book only needs the best and the count per species.
            val catches = (s.catches + catch).takeLast(600)
            val (p, got) = Achievements.check(landed, catches)
            achieved = got
            val baits = if (r.allQuests) s.baits.add(Economy.ALL_QUESTS_CASTS, System.currentTimeMillis(), p.boxSize) else s.baits
            s.copy(catches = catches, progress = p, baits = baits, tips = s.tips + Tip.REEL.name)
        }
        // The fish comes out of the water on the line, dripping, then its card shows.
        buzz(longArrayOf(0, 40, 60, 40))
        sfx.play(Sfx.Fx.SPLASH)
        _lake.update { it.copy(stage = Stage.LANDING, tension = 0f) }
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
    fun demoFight(id: String) {
        val species = Fish.byId(id) ?: return
        val bite = Bite(species, null, (species.minCm + species.maxCm) / 2.0, 1.0, emptyList())
        _lake.value = LakeState(castDist = 0.7, castId = _lake.value.castId, bite = bite)
        startFight(bite)
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
