package lt.marcomultiservices.kimba.core

import java.time.ZoneId

/** A stretch of time in which a golden fish can bite. */
class GoldenWindow(val golden: Golden, val startMillis: Long, val endMillis: Long)

object Windows {
    private const val HOUR = 3_600_000L

    /**
     * Upcoming golden windows from [fromMillis] over the forecast (hourly steps). Moon
     * conditions are known far ahead; weather ones only as far as the forecast reaches.
     */
    fun golden(forecast: Forecast, fromMillis: Long, lat: Double, lon: Double, zone: ZoneId, hours: Int = 60, goldens: List<Golden> = Fish.goldens): List<GoldenWindow> {
        val start = fromMillis - fromMillis % HOUR
        val result = mutableListOf<GoldenWindow>()
        for (g in goldens) {
            var open: Long? = null
            for (i in 0..hours) {
                val t = start + i * HOUR
                val w = forecast.weatherAt(t) ?: break
                val on = g.active(Conditions(w, t, lat, lon, zone))
                if (on && open == null) open = t
                if (!on && open != null) { result += GoldenWindow(g, open, t); open = null }
            }
            if (open != null) result += GoldenWindow(g, open, start + (hours + 1) * HOUR)
        }
        return result.sortedBy { it.startMillis }
    }
}
