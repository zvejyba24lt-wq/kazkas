package lt.marcomultiservices.kimba.core

import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.ceil

/** The player's best fish for one day's challenge. */
@Serializable
data class ChallengeLog(val day: Long = 0, val grams: Int = 0, val cm: Double = 0.0)

/**
 * One challenge a day, the same for every player: the heaviest fish of one common kind. The
 * result is shared as a short line with a five-box bar, like Wordle, so friends can compare.
 */
object Challenge {
    private val START: Long = LocalDate.of(2026, 10, 1).toEpochDay()
    /** Coins for the first fish of the day's kind. */
    const val COINS = 150

    fun number(day: Long): Int = (day - START + 1).toInt()

    /** Common fish of every home lake; in winter only those that bite under the ice. */
    fun species(day: Long, month: Int): Species {
        val summer = listOf(Fish.roach, Fish.perch, Fish.bream, Fish.pike, Fish.rudd, Fish.tench, Fish.crucian, Fish.bleak)
        val winter = listOf(Fish.roach, Fish.perch, Fish.bream, Fish.pike, Fish.bleak, Fish.rudd)
        // Tench, crucian carp and rudd hardly feed in cold water: spring and autumn leave them out.
        val cold = listOf(Fish.roach, Fish.perch, Fish.bream, Fish.pike, Fish.bleak)
        val list = when (month) { 12, 1, 2 -> winter; 3, 4, 10, 11 -> cold; else -> summer }
        val h = (day * 2654435761L) ushr 7
        return list[(h % list.size).toInt()]
    }

    /** The log after [c], or null if it is not the day's kind or not heavier than today's best. */
    fun record(log: ChallengeLog, day: Long, month: Int, c: Catch): ChallengeLog? {
        if (c.golden != null || c.species != species(day, month).id) return null
        val best = if (log.day == day) log.grams else 0
        return if (c.grams > best) ChallengeLog(day, c.grams, c.cm) else null
    }

    /** 1..5 boxes: where the fish falls in its kind's size range. */
    fun boxes(log: ChallengeLog, s: Species): Int {
        if (log.grams <= 0) return 0
        val f = ((log.cm - s.minCm) / (s.maxCm - s.minCm)).coerceIn(0.0, 1.0)
        return ceil(f * 5).toInt().coerceIn(1, 5)
    }

    fun bar(boxes: Int): String = "🟩".repeat(boxes) + "⬜".repeat(5 - boxes)
}
