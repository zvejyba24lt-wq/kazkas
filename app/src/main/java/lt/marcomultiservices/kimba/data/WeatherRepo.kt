package lt.marcomultiservices.kimba.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import lt.marcomultiservices.kimba.core.Forecast
import lt.marcomultiservices.kimba.core.Sky
import lt.marcomultiservices.kimba.core.Weather
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Hourly forecast from MET Norway (api.met.no, free, CC BY 4.0). Kept in a file and fetched
 * again at most every 45 minutes, as MET's terms ask for caching.
 */
class WeatherRepo(private val context: Context) {
    private val file get() = File(context.filesDir, "forecast.json")
    private val metaFile get() = File(context.filesDir, "forecast.meta")

    /** The cached forecast for [place], or null. */
    fun cached(place: Place): Forecast? {
        val meta = runCatching { metaFile.readText().split("|") }.getOrNull() ?: return null
        if (meta.size < 2 || meta[0] != key(place)) return null
        return runCatching { Forecast.parse(file.readText()) }.getOrNull()
    }

    /** Fetches a fresh forecast unless the cached one is recent; returns whatever is best available. */
    suspend fun forecast(place: Place, force: Boolean = false): Forecast? = withContext(Dispatchers.IO) {
        val meta = runCatching { metaFile.readText().split("|") }.getOrNull()
        val fresh = meta != null && meta.size >= 2 && meta[0] == key(place) &&
            System.currentTimeMillis() - (meta[1].toLongOrNull() ?: 0) < 45 * 60_000L
        if (fresh && !force) cached(place)?.let { return@withContext it }
        val text = runCatching { download(place) }.getOrNull()
        if (text != null) {
            val parsed = runCatching { Forecast.parse(text) }.getOrNull()
            if (parsed != null && parsed.hours.isNotEmpty()) {
                file.writeText(text)
                metaFile.writeText("${key(place)}|${System.currentTimeMillis()}")
                return@withContext parsed
            }
        }
        cached(place)
    }

    private fun key(p: Place) = "%.2f,%.2f".format(Locale.US, p.lat, p.lon)

    private fun download(place: Place): String {
        val url = URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.2f&lon=%.2f".format(Locale.US, place.lat, place.lon))
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        // MET Norway requires an identifying User-Agent.
        conn.setRequestProperty("User-Agent", "Kimba/0.1 github.com/zvejyba24lt-wq")
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        /** Typical weather for the season, used only when there is no forecast at all (first start offline). */
        fun seasonal(): Weather {
            val month = LocalDate.now().monthValue
            val temp = doubleArrayOf(-3.0, -2.0, 2.0, 9.0, 15.0, 18.0, 20.0, 19.0, 14.0, 8.0, 3.0, -1.0)[month - 1]
            return Weather(temp, 1013.0, 0.0, 0.5, 3.0, 0.0, Sky.PARTLY)
        }
    }
}

/** The phone's coarse location, as a [Place] named after the nearest town where possible. */
object DeviceLocation {
    @SuppressLint("MissingPermission")
    suspend fun find(context: Context, fallbackName: String): Place? = withContext(Dispatchers.IO) {
        val lm = context.getSystemService(LocationManager::class.java) ?: return@withContext null
        val loc = runCatching {
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
                .filter { lm.isProviderEnabled(it) || it == LocationManager.PASSIVE_PROVIDER }
                .mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        }.getOrNull() ?: if (Build.VERSION.SDK_INT >= 30) withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, context.mainExecutor) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        } else null
        loc ?: return@withContext null
        val name = runCatching {
            @Suppress("DEPRECATION")
            Geocoder(context).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
        }.getOrNull() ?: fallbackName
        Place(name, loc.latitude, loc.longitude, fromDevice = true)
    }
}
