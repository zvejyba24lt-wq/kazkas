package lt.marcomultiservices.kimba.core

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
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
) {
    val grams: Int get() = species.grams(cm)

    /** Fight strength: species power scaled by size. */
    val pull: Double get() {
        val s = (cm - species.minCm) / (species.maxCm - species.minCm)
        return species.power * (0.6 + 0.8 * s)
    }
}

/** What hangs on the hook. Each lure draws different fish. */
enum class Lure(val label: L, private val likes: Map<String, Double>) {
    WORM(L("Slieka", "Wurm", "Worm"), mapOf("eel" to 1.6, "perch" to 1.3, "tench" to 1.3, "burbot" to 1.4, "catfish" to 1.2)),
    CORN(
        L("Kukurūzas", "Mais", "Sweetcorn"),
        mapOf("carp" to 3.0, "crucian" to 2.0, "tench" to 1.8, "bream" to 1.8, "roach" to 1.3, "rudd" to 1.3,
            "pike" to 0.05, "zander" to 0.05, "perch" to 0.1, "eel" to 0.1, "catfish" to 0.1, "burbot" to 0.1),
    ),
    SPINNER(
        L("Blizgė", "Blinker", "Spinner"),
        mapOf("pike" to 3.0, "perch" to 2.2, "zander" to 2.2, "catfish" to 1.2,
            "roach" to 0.05, "bleak" to 0.05, "rudd" to 0.05, "crucian" to 0.03, "tench" to 0.03, "bream" to 0.05, "carp" to 0.03, "eel" to 0.1, "burbot" to 0.2),
    );

    fun affinity(s: Species): Double = likes[s.id] ?: 1.0
}

object BiteEngine {
    /** Chance that a bite of a species whose golden form is active is the golden one. */
    const val GOLDEN_CHANCE = 0.3

    /** [cast] is the cast distance 0..1: near casts reach reed fish, far casts the deep-water predators. */
    fun roll(c: Conditions, cast: Double, rng: Random, lure: Lure = Lure.WORM): Bite {
        val act = Fish.activities(c)
        val weights = act.mapValues { (s, a) -> a * lure.affinity(s) * (1 - 0.75 * abs(s.depth - cast)) }
        var r = rng.nextDouble() * weights.values.sum()
        var species = weights.keys.last()
        for ((s, w) in weights) {
            r -= w
            if (r <= 0) { species = s; break }
        }
        val golden = Fish.goldens.firstOrNull { it.base == species && it.active(c) }
            ?.takeIf { rng.nextDouble() < GOLDEN_CHANCE }
        // Most fish are small; trophies are rare.
        val size = rng.nextDouble().pow(if (golden != null) 1.2 else 2.2)
        val cm = species.minCm + (species.maxCm - species.minCm) * size
        val index = (Fish.biteIndex(c) / 100.0) * (weights.values.sum() / act.values.sum()).coerceIn(0.5, 1.3)
        val wait = (16.0 - 11.0 * index) * (0.6 + 0.8 * rng.nextDouble())
        val nibbles = buildList {
            var t = 1.2 + rng.nextDouble() * 1.5
            while (t < wait - 1.0) {
                add(t)
                t += 1.0 + rng.nextDouble() * 2.5
            }
        }
        return Bite(species, golden, (cm * 10).toInt() / 10.0, wait, nibbles)
    }
}

enum class FightResult { LANDED, SNAPPED, ESCAPED }

/**
 * The drill after the strike. The player holds to reel in; the fish pulls, and every few
 * seconds makes a run. Reeling during a run overloads the line; giving no line tension for
 * too long lets the hook fall out.
 */
class Fight(private val pull: Double, private val rng: Random) {
    /** Remaining distance to the landing net, 1 = where it was hooked. */
    var distance = 1.0; private set
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
        val target = if (reeling) 0.28 + p * 0.42 else 0.04 + p * 0.18
        tension += (target - tension) * min(1.0, dt * 4)
        distance += if (reeling) -dt * 0.16 * (1 - min(0.85, p * 0.45)) else dt * 0.05 * p
        distance = distance.coerceAtMost(1.0)
        stamina = (stamina - dt * (if (running) 0.06 else 0.025) * (if (reeling) 1.4 else 0.8)).coerceAtLeast(0.0)
        overload = if (tension > 0.97) overload + dt else 0.0
        slack = if (tension < 0.12) slack + dt else (slack - dt * 2).coerceAtLeast(0.0)
        result = when {
            overload > 0.35 -> FightResult.SNAPPED
            slack > 2.2 -> FightResult.ESCAPED
            distance <= 0 -> FightResult.LANDED
            else -> null
        }
    }
}

/** Baits refill one every [REFILL_MS] up to [MAX]. */
@Serializable
data class Baits(val count: Int, val sinceMillis: Long) {
    fun now(nowMillis: Long): Baits {
        if (count >= MAX) return Baits(MAX, nowMillis)
        val gained = ((nowMillis - sinceMillis) / REFILL_MS).toInt()
        if (gained <= 0) return this
        val n = min(MAX, count + gained)
        return Baits(n, if (n >= MAX) nowMillis else sinceMillis + gained * REFILL_MS)
    }

    fun use(nowMillis: Long): Baits? {
        val b = now(nowMillis)
        if (b.count <= 0) return null
        return Baits(b.count - 1, if (b.count >= MAX) nowMillis else b.sinceMillis)
    }

    fun add(n: Int, nowMillis: Long): Baits {
        val b = now(nowMillis)
        return Baits(b.count + n, b.sinceMillis)
    }

    /** Milliseconds until the next bait, or null when full. */
    fun nextIn(nowMillis: Long): Long? {
        val b = now(nowMillis)
        return if (b.count >= MAX) null else b.sinceMillis + REFILL_MS - nowMillis
    }

    companion object {
        const val MAX = 5
        const val REFILL_MS = 30 * 60_000L
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
)
