package lt.marcomultiservices.kimba.core

import java.time.Instant
import java.time.ZoneId

/**
 * Today's fishing at a glance, for the home-screen widget: how well fish bite now, the kind that
 * bites best now, and the best hour still ahead today, all from the real forecast.
 */
class BiteToday(val weather: Weather, val index: Int, val hot: Species?, val bestMillis: Long, val bestIndex: Int)

object Today {
    fun of(f: Forecast, now: Long, lat: Double, lon: Double, zone: ZoneId, spot: Spot = Spot.HOME_LAKE, ice: Boolean = false): BiteToday? {
        val w = f.weatherAt(now) ?: return null
        val c = Conditions(w, now, lat, lon, zone)
        val index = Fish.biteIndex(c, spot)
        val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        // Every forecast hour left today (and now itself); the best one wins, the earlier on a tie.
        val times = listOf(now) + f.hours.map { it.epochMillis }.filter { it in (now + 1) until end }
        var best = now to index
        for (t in times) {
            val hw = f.weatherAt(t) ?: continue
            val i = Fish.biteIndex(Conditions(hw, t, lat, lon, zone), spot)
            if (i > best.second) best = t to i
        }
        return BiteToday(w, index, Fish.hotSpecies(c, spot, ice), best.first, best.second)
    }
}
