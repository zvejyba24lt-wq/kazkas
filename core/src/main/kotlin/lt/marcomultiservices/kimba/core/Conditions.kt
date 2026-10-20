package lt.marcomultiservices.kimba.core

import java.time.Instant
import java.time.ZoneId
import kotlin.math.exp

enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, HEAVY_RAIN, SLEET, SNOW, THUNDER }

enum class DayPhase { NIGHT, DAWN, DAY, DUSK }

/** Weather at one moment, as the forecast describes it. */
data class Weather(
    val tempC: Double,
    val pressureHpa: Double,
    /** Pressure change over the next three hours in hPa; negative = falling (a front is coming). */
    val pressureTrend: Double,
    /** Cloud cover 0..1. */
    val cloud: Double,
    val windMs: Double,
    /** Precipitation over the next hour in mm. */
    val precipMm: Double,
    val sky: Sky,
)

/** Everything the lake and the fish react to at one moment and place. */
class Conditions(
    val weather: Weather,
    val epochMillis: Long,
    val lat: Double,
    val lon: Double,
    val zone: ZoneId,
) {
    private val local = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val month: Int = local.monthValue
    val hour: Double = local.hour + local.minute / 60.0

    val sunElevation: Double = Astro.sunElevation(epochMillis, lat, lon)
    val sunHourAngle: Double = Astro.sunHourAngle(epochMillis, lon)
    val moonAge: Double = Astro.moonAge(epochMillis)
    val moonIllumination: Double = Astro.moonIllumination(epochMillis)

    val phase: DayPhase = when {
        sunElevation < -9 -> DayPhase.NIGHT
        sunElevation < 7 -> if (sunHourAngle < 0) DayPhase.DAWN else DayPhase.DUSK
        else -> DayPhase.DAY
    }

    /** Water warms and cools slowly, so it follows the season and only a little of today's air. */
    val waterTempC: Double = run {
        val seasonal = doubleArrayOf(2.0, 2.0, 4.0, 9.0, 14.0, 19.0, 21.0, 20.0, 16.0, 11.0, 6.0, 3.0)[month - 1]
        val seasonalAir = doubleArrayOf(-3.0, -2.0, 2.0, 9.0, 15.0, 18.0, 20.0, 19.0, 14.0, 8.0, 3.0, -1.0)[month - 1]
        (seasonal + 0.2 * (weather.tempC - seasonalAir)).coerceIn(0.5, 28.0)
    }

    val rain: Double = when (weather.sky) {
        Sky.DRIZZLE -> 0.3
        Sky.RAIN -> 0.6
        Sky.HEAVY_RAIN -> 1.0
        Sky.THUNDER -> 0.85
        Sky.SLEET -> 0.5
        else -> (weather.precipMm / 3).coerceIn(0.0, 0.3)
    }
    val falling: Double = (-weather.pressureTrend / 3).coerceIn(0.0, 1.0)
    val rising: Double = (weather.pressureTrend / 3).coerceIn(0.0, 1.0)
    val fullMoon: Boolean get() = moonIllumination > 0.93
    val newMoon: Boolean get() = moonIllumination < 0.07
    val frost: Boolean get() = weather.tempC <= 0.0
    val dark: Boolean get() = phase == DayPhase.NIGHT

    fun bell(x: Double, opt: Double, width: Double): Double {
        val d = (x - opt) / width
        return exp(-d * d)
    }

    /** Weight for the time of day. */
    fun byPhase(night: Double, dawn: Double, day: Double, dusk: Double): Double = when (phase) {
        DayPhase.NIGHT -> night
        DayPhase.DAWN -> dawn
        DayPhase.DAY -> day
        DayPhase.DUSK -> dusk
    }

    fun withWeather(w: Weather) = Conditions(w, epochMillis, lat, lon, zone)
}
