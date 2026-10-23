package lt.marcomultiservices.kimba.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import kotlin.math.abs

/** Hourly forecast from MET Norway's Locationforecast 2.0 (compact). */
class Forecast(val hours: List<Hour>) {
    class Hour(val epochMillis: Long, val tempC: Double, val pressureHpa: Double, val cloud: Double, val windMs: Double, val precipMm: Double, val sky: Sky)

    /** Weather at [epochMillis], from the forecast hour that contains it; null when outside the forecast. */
    fun weatherAt(epochMillis: Long): Weather? {
        if (hours.isEmpty()) return null
        val i = hours.indexOfLast { it.epochMillis <= epochMillis }.coerceAtLeast(0)
        val h = hours[i]
        if (abs(h.epochMillis - epochMillis) > 6 * 3_600_000L) return null
        val later = hours.firstOrNull { it.epochMillis >= h.epochMillis + 3 * 3_600_000L }
        val trend = if (later != null) (later.pressureHpa - h.pressureHpa) * 3 * 3_600_000.0 / (later.epochMillis - h.epochMillis) else 0.0
        return Weather(h.tempC, h.pressureHpa, trend, h.cloud, h.windMs, h.precipMm, h.sky)
    }

    /** Mean temperature and total rain over the forecast hours of [day] (local epoch day); null if none. */
    fun dayMean(day: Long, zone: java.time.ZoneId): Pair<Double, Double>? {
        val list = hours.filter { Instant.ofEpochMilli(it.epochMillis).atZone(zone).toLocalDate().toEpochDay() == day }
        if (list.isEmpty()) return null
        return list.sumOf { it.tempC } / list.size to list.sumOf { if (it.sky == Sky.SNOW || it.sky == Sky.SLEET) 0.0 else it.precipMm }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): Forecast {
            val series = json.parseToJsonElement(text).jsonObject["properties"]!!.jsonObject["timeseries"]!!.jsonArray
            val hours = series.mapNotNull { el ->
                val o = el.jsonObject
                val time = Instant.parse(o["time"]!!.jsonPrimitive.content).toEpochMilli()
                val data = o["data"]!!.jsonObject
                val d = data["instant"]!!.jsonObject["details"]!!.jsonObject
                val next = (data["next_1_hours"] ?: data["next_6_hours"])?.jsonObject
                val symbol = next?.get("summary")?.jsonObject?.get("symbol_code")?.jsonPrimitive?.content
                    ?: return@mapNotNull null
                val precip = next.num("details", "precipitation_amount") ?: 0.0
                val cloud = (d.num("cloud_area_fraction") ?: 50.0) / 100
                Hour(
                    epochMillis = time,
                    tempC = d.num("air_temperature") ?: 10.0,
                    pressureHpa = d.num("air_pressure_at_sea_level") ?: 1013.0,
                    cloud = cloud,
                    windMs = d.num("wind_speed") ?: 2.0,
                    precipMm = if (data["next_1_hours"] != null) precip else precip / 6,
                    sky = skyOf(symbol, cloud),
                )
            }
            return Forecast(hours)
        }

        private fun JsonObject.num(vararg path: String): Double? {
            var o: JsonObject = this
            for (key in path.dropLast(1)) o = o[key]?.jsonObject ?: return null
            return o[path.last()]?.jsonPrimitive?.doubleOrNull
        }

        /** Maps a MET Norway symbol code ("lightrainshowers_day", "fog", ...) to a [Sky]. */
        fun skyOf(symbol: String, cloud: Double): Sky {
            val s = symbol.substringBefore('_')
            return when {
                "thunder" in s -> Sky.THUNDER
                "sleet" in s -> Sky.SLEET
                "snow" in s -> Sky.SNOW
                "heavyrain" in s -> Sky.HEAVY_RAIN
                "lightrain" in s -> Sky.DRIZZLE
                "rain" in s -> Sky.RAIN
                s == "fog" -> Sky.FOG
                s == "cloudy" -> Sky.CLOUDY
                s == "partlycloudy" -> Sky.PARTLY
                s == "fair" -> if (cloud > 0.3) Sky.PARTLY else Sky.CLEAR
                s == "clearsky" -> Sky.CLEAR
                else -> if (cloud > 0.7) Sky.CLOUDY else Sky.PARTLY
            }
        }
    }
}
