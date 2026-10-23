package lt.marcomultiservices.kimba.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Ice on the home water, grown from the real temperatures the game sees each day.
 *
 * Growth follows Stefan's law, the rule limnologists use: thickness ≈ α·√(frost degree-days),
 * with α ≈ 2.5 cm for a lake without much snow. A week at −5 °C makes about 15 cm. Warm days
 * melt it again, rain faster. Ponds are shallow and freeze sooner; rivers and the sea stay open.
 */
@Serializable
data class IceLog(val cm: Double = 0.0, val day: Long = 0)

object Ice {
    /** Thickness at which the game lets the player walk out and fish. */
    const val SAFE_CM = 10.0
    /** Below this the shore only shows a thin rim of ice. */
    const val RIM_CM = 2.0
    private const val ALPHA = 2.5

    /**
     * Brings [log] up to [day] with that day's mean temperature. Days the game did not see are
     * taken to be like today. A first log starts at zero, except in a deep frost in midwinter,
     * when the water is surely already frozen.
     */
    fun update(log: IceLog, day: Long, meanC: Double, rainMm: Double, month: Int): IceLog {
        if (log.day == day) return log
        if (log.day == 0L) {
            val start = if (month in setOf(12, 1, 2) && meanC <= -4.0) 12.0 else 0.0
            return IceLog(step(start, meanC, rainMm), day)
        }
        val days = (day - log.day).coerceIn(1, 14)
        var cm = log.cm
        repeat(days.toInt()) { cm = step(cm, meanC, rainMm) }
        return IceLog(cm, day)
    }

    private fun step(cm: Double, meanC: Double, rainMm: Double): Double = if (meanC < 0) {
        sqrt(cm * cm + ALPHA * ALPHA * -meanC)
    } else {
        // About 1.2 cm a day per degree above zero, more under rain.
        max(0.0, cm - 1.2 * meanC - 0.4 * rainMm)
    }

    /** Thickness on [water]: ponds a little more, flowing and salt water none. */
    fun on(water: Water, log: IceLog): Double = when (water) {
        Water.LAKE -> log.cm
        Water.POND -> log.cm * 1.25
        Water.RIVER, Water.SEA -> 0.0
    }

    fun frozen(water: Water, log: IceLog): Boolean = on(water, log) >= SAFE_CM

    /**
     * How each fish takes a bait under the ice. Perch, roach and burbot are the winter fish;
     * carp, tench, catfish and eel lie still in the mud until spring.
     */
    fun affinity(s: Species): Double = when (s.id) {
        "perch", "burbot" -> 2.0
        "roach", "bleak", "whitefish" -> 1.6
        "bream", "pike", "zander", "rudd" -> 1.0
        "crucian" -> 0.3
        "carp", "tench", "catfish", "eel", "grass_carp" -> 0.03
        else -> 0.8
    }
}
