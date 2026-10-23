package lt.marcomultiservices.kimba.core

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.random.Random

/** What will take the bait on this cast, decided when the float lands. */
class Bite(
    val species: Species,
    val golden: Golden?,
    val cm: Double,
    /** Seconds until the real bite. */
    val waitSeconds: Double,
    /** Seconds (from landing) of small nibbles before the bite, which must not be struck. */
    val nibbles: List<Double>,
    /** Body condition: fish of the same length differ in weight by about ±10 %. */
    val condition: Double = 1.0,
) {
    val grams: Int get() = (species.grams(cm) * condition).toInt()

    /** Fight strength: species power scaled by size. */
    val pull: Double get() {
        val s = (cm - species.minCm) / (species.maxCm - species.minCm)
        return species.power * (0.6 + 0.8 * s)
    }
}

object BiteEngine {
    /** Chance that a bite of a species whose golden form is active is the golden one. */
    const val GOLDEN_CHANCE = 0.3

    /** [cast] is the cast distance 0..1: near casts reach reed fish, far casts the deep-water predators. */
    fun roll(c: Conditions, cast: Double, rng: Random, spot: Spot = Spot.HOME_LAKE, gear: Gear = Gear()): Bite {
        val act = Fish.activities(c, spot)
        // A boat takes the bait further out, where the deep-water fish are.
        // A feeder is thrown further, to the deeper bottom.
        val reach = (cast + (if (gear.boat) 0.2 else 0.0) + (if (gear.tackle == Tackle.FEEDER) 0.12 else 0.0)).coerceAtMost(1.2)
        val weights = act.mapValues { (s, a) ->
            val d = (s.depth - reach) / 0.35
            a * gear.lure.affinity(s) * gear.tackle.affinity(s) * (exp(-d * d) + 0.04) * (if (s.shy) gear.line.shyFactor else 1.0)
        }
        var r = rng.nextDouble() * weights.values.sum()
        var species = weights.keys.last()
        for ((s, w) in weights) {
            r -= w
            if (r <= 0) { species = s; break }
        }
        val lure = gear.lure
        val golden = Fish.goldens.firstOrNull { it.base == species && it.active(c) }
            ?.takeIf { rng.nextDouble() < GOLDEN_CHANCE }
        // Most fish are small; trophies are rare (about 1 in 80 bites lands in the top fifth).
        val size = sizeFraction(rng.nextDouble(), (if (golden != null) 2.0 else 5.5) * lure.sizeBias * (if (gear.tackle == Tackle.FEEDER) 0.85 else 1.0))
        val cm = species.minCm + (species.maxCm - species.minCm) * size
        val index = (Fish.biteIndex(c, spot) / 100.0) * (weights.values.sum() / act.values.sum()).coerceIn(0.5, 1.3)
        val wait = (16.0 - 11.0 * index) * (0.6 + 0.8 * rng.nextDouble()) * (if (gear.boat) 0.75 else 1.0)
        val nibbles = buildList {
            var t = 1.2 + rng.nextDouble() * 1.5
            while (t < wait - 1.0) {
                add(t)
                t += 1.0 + rng.nextDouble() * 2.5
            }
        }
        // Anglers measure to the whole centimetre.
        return Bite(species, golden, kotlin.math.round(cm), wait, nibbles, 0.9 + rng.nextDouble() * 0.2)
    }

    /** Exponential distribution with rate [lambda], cut off at 1: where in its size range a fish falls. */
    fun sizeFraction(u: Double, lambda: Double): Double = -ln(1 - u * (1 - exp(-lambda))) / lambda
}

enum class FightResult { LANDED, SNAPPED, ESCAPED }

/** Seconds a hooked fish may be left without reeling before it gets off. */
const val IDLE_LIMIT = 8.0

/**
 * The drill after the strike. The player holds to reel in; the fish pulls, and every few
 * seconds makes a run. Reeling during a run overloads the line; giving no line tension for
 * too long lets the hook fall out.
 */
class Fight(
    private val pull: Double,
    private val rng: Random,
    /** Rod and line strength: tension is measured against it. */
    private val strength: Double = 1.0,
    /** Reel speed multiplier. */
    private val reelSpeed: Double = 1.0,
    /** How far out the fish took the bait: 1 = a full cast, less when it bit close to the bank. */
    start: Double = 1.0,
) {
    /** Remaining distance to the landing net, 0..1 of a full cast. */
    var distance = start.coerceIn(0.05, 1.0); private set
    /** Line tension 0..1+, snaps when held above ~1. */
    var tension = 0.3; private set
    var stamina = 1.0; private set
    var running = false; private set
    var result: FightResult? = null; private set
    /** Becomes true for one step when the fish starts a run (for haptics and sound). */
    var runStarted = false; private set

    private var runLeft = 0.0
    private var calmLeft = 1.0 + rng.nextDouble() * 1.5
    private var overload = 0.0
    private var slack = 0.0
    /** Seconds without reeling: a fish left alone swims into the weeds and gets off. */
    private var idle = 0.0

    val currentPull: Double get() = pull * (0.35 + 0.65 * stamina) * (if (running) 2.0 else 1.0)

    fun step(dt: Double, reeling: Boolean) {
        if (result != null) return
        runStarted = false
        if (running) {
            runLeft -= dt
            if (runLeft <= 0) { running = false; calmLeft = 1.2 + rng.nextDouble() * 2.3 }
        } else {
            calmLeft -= dt
            if (calmLeft <= 0) { running = true; runStarted = true; runLeft = 0.5 + rng.nextDouble() * 0.8 }
        }
        val p = currentPull
        val target = (if (reeling) 0.28 + p * 0.42 else 0.04 + p * 0.18) / strength
        tension += (target - tension) * min(1.0, dt * 4)
        distance += if (reeling) -dt * 0.16 * reelSpeed * (1 - min(0.85, p * 0.45)) else dt * 0.05 * p
        distance = distance.coerceAtMost(1.0)
        stamina = (stamina - dt * (if (running) 0.06 else 0.025) * (if (reeling) 1.4 else 0.8)).coerceAtLeast(0.0)
        overload = if (tension > 0.97) overload + dt else 0.0
        slack = if (tension < 0.12) slack + dt else (slack - dt * 2).coerceAtLeast(0.0)
        idle = if (reeling) 0.0 else idle + dt
        result = when {
            overload > 0.35 -> FightResult.SNAPPED
            slack > 2.2 || idle > IDLE_LIMIT -> FightResult.ESCAPED
            distance <= 0 -> FightResult.LANDED
            else -> null
        }
    }
}

/** Baits refill one every [REFILL_MS] up to the bait box size [max]. Bonus baits may go above it. */
@Serializable
data class Baits(val count: Int, val sinceMillis: Long) {
    fun now(nowMillis: Long, max: Int = MAX): Baits {
        if (count >= max) return Baits(count, nowMillis)
        val gained = ((nowMillis - sinceMillis) / REFILL_MS).toInt()
        if (gained <= 0) return this
        val n = min(max, count + gained)
        return Baits(n, if (n >= max) nowMillis else sinceMillis + gained * REFILL_MS)
    }

    fun use(nowMillis: Long, max: Int = MAX): Baits? {
        val b = now(nowMillis, max)
        if (b.count <= 0) return null
        return Baits(b.count - 1, if (b.count >= max) nowMillis else b.sinceMillis)
    }

    fun add(n: Int, nowMillis: Long, max: Int = MAX): Baits {
        val b = now(nowMillis, max)
        return Baits(b.count + n, b.sinceMillis)
    }

    /** Milliseconds until the next bait, or null when full. */
    fun nextIn(nowMillis: Long, max: Int = MAX): Long? {
        val b = now(nowMillis, max)
        return if (b.count >= max) null else b.sinceMillis + REFILL_MS - nowMillis
    }

    companion object {
        const val MAX = 10
        const val REFILL_MS = 15 * 60_000L
    }
}

@Serializable
data class Catch(
    val species: String,
    val golden: String? = null,
    val cm: Double,
    val grams: Int,
    val epochMillis: Long,
    val sky: Sky,
    val tempC: Double,
    val place: String,
    val water: Water = Water.LAKE,
    val coins: Int = 0,
)
