package lt.marcomultiservices.kimba.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Sun and moon positions, accurate to well under a degree, which is plenty for a sky and a bite forecast. */
object Astro {
    private const val SYNODIC_MONTH = 29.530588853
    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI
    private fun julian(epochMillis: Long) = epochMillis / 86_400_000.0 + 2_440_587.5
    private fun wrap(x: Double, m: Double) = x - m * floor(x / m)

    /** Height of the sun above the horizon in degrees (negative below it). */
    fun sunElevation(epochMillis: Long, lat: Double, lon: Double): Double {
        val n = julian(epochMillis) - 2_451_545.0
        val meanLon = wrap(280.460 + 0.9856474 * n, 360.0)
        val anomaly = rad(wrap(357.528 + 0.9856003 * n, 360.0))
        val eclLon = rad(meanLon + 1.915 * sin(anomaly) + 0.020 * sin(2 * anomaly))
        val obliquity = rad(23.439 - 0.0000004 * n)
        val ra = atan2(cos(obliquity) * sin(eclLon), cos(eclLon))
        val dec = asin(sin(obliquity) * sin(eclLon))
        val gmstHours = wrap(18.697374558 + 24.06570982441908 * n, 24.0)
        val hourAngle = rad(gmstHours * 15 + lon) - ra
        val la = rad(lat)
        return deg(asin(sin(la) * sin(dec) + cos(la) * cos(dec) * cos(hourAngle)))
    }

    /** Horizontal direction of the sun, 0 = due south, negative = east (morning), positive = west. */
    fun sunHourAngle(epochMillis: Long, lon: Double): Double {
        val n = julian(epochMillis) - 2_451_545.0
        val meanLon = wrap(280.460 + 0.9856474 * n, 360.0)
        val anomaly = rad(wrap(357.528 + 0.9856003 * n, 360.0))
        val eclLon = rad(meanLon + 1.915 * sin(anomaly) + 0.020 * sin(2 * anomaly))
        val obliquity = rad(23.439 - 0.0000004 * n)
        val ra = deg(atan2(cos(obliquity) * sin(eclLon), cos(eclLon)))
        val gmstHours = wrap(18.697374558 + 24.06570982441908 * n, 24.0)
        return wrap(gmstHours * 15 + lon - ra + 180, 360.0) - 180
    }

    /** Moon age as a fraction of the cycle: 0 = new moon, 0.5 = full moon. */
    fun moonAge(epochMillis: Long): Double =
        wrap((julian(epochMillis) - 2_451_550.1) / SYNODIC_MONTH, 1.0)

    /** Lit fraction of the moon's disc, 0..1. */
    fun moonIllumination(epochMillis: Long): Double = (1 - cos(2 * PI * moonAge(epochMillis))) / 2

    /** Days until the next moment the moon age reaches [target] (0 new, 0.5 full). */
    fun daysUntilMoon(epochMillis: Long, target: Double): Double =
        wrap(target - moonAge(epochMillis), 1.0) * SYNODIC_MONTH

    /**
     * Times the sun crosses the horizon on the day containing [dayStartMillis] (local midnight),
     * found by stepping through the day. Null where the sun does not rise or set.
     */
    fun sunriseSunset(dayStartMillis: Long, lat: Double, lon: Double): Pair<Long?, Long?> {
        val step = 5 * 60_000L
        var rise: Long? = null
        var set: Long? = null
        var prev = sunElevation(dayStartMillis, lat, lon) + 0.833
        var t = dayStartMillis + step
        while (t <= dayStartMillis + 86_400_000L) {
            val e = sunElevation(t, lat, lon) + 0.833
            if (prev < 0 && e >= 0 && rise == null) rise = t
            if (prev >= 0 && e < 0 && set == null) set = t
            prev = e
            t += step
        }
        return rise to set
    }
}
