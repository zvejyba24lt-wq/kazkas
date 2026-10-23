package lt.marcomultiservices.kimba.core

import kotlinx.serialization.Serializable
import kotlin.math.min
import kotlin.random.Random

/** A fish the player put back, which may be caught again later, bigger. */
@Serializable
data class Released(val species: String, val cm: Double, val water: Water, val day: Long)

/**
 * Catch and release. Putting a fish back gives up its sale money but earns experience and
 * good-angler points (double for a young fish). Released fish live on in the same water and
 * from the next day on may take the bait again, grown a little.
 */
object Release {
    const val POOL = 12
    const val RETURN_CHANCE = 0.4
    const val XP = 15

    /** A young fish, in the bottom third of its kind's size range. */
    fun young(s: Species, cm: Double): Boolean = (cm - s.minCm) / (s.maxCm - s.minCm).toDouble() < 0.3

    fun points(s: Species, cm: Double): Int = if (young(s, cm)) 2 else 1

    /** Puts [c] back; null for golden and legendary fish, which are kept for the book. */
    fun apply(p: Progress, c: Catch, day: Long): Progress? {
        if (c.golden != null) return null
        val s = Fish.byId(c.species) ?: return null
        return p.copy(
            coins = (p.coins - c.coins).coerceAtLeast(0),
            xp = p.xp + XP,
            released = (p.released + Released(c.species, c.cm, c.water, day)).takeLast(POOL),
            goodAngler = p.goodAngler + points(s, c.cm),
        )
    }

    /** A released fish of [species] on [water] that takes the bait again today, or null. */
    fun returning(p: Progress, species: String, water: Water, day: Long, rng: Random): Released? =
        p.released.firstOrNull { it.species == species && it.water == water && it.day < day }
            ?.takeIf { rng.nextDouble() < RETURN_CHANCE }

    /** Its length now: about 4 % a day for up to five days, never past a record fish. */
    fun grown(r: Released, s: Species, day: Long): Double =
        min(r.cm * (1 + 0.04 * min(day - r.day, 5L)) + 1.0, s.maxCm * 1.05)

    fun caughtAgain(p: Progress, r: Released): Progress = p.copy(released = p.released - r)
}
